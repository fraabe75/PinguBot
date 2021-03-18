package com.example.demo.plugins.impl.mia;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Mia extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;

    public Mia(UserRepository userRepository) {
        setName("Mia");
        setDescription("Play a funny dice game!");
        addCommands("mia", "m");
        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Mia Help");
        builder.setDescription("You might know this game as 'Meiern' or 'Mäxchen'");
        builder.addField("mia play <value>", "start a new game", false);
        builder.addField("mia rules", "rules and payout rates", false);
        builder.setFooter("Shortcuts: 'm'");
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        TextChannel channel = event.getChannel();

        switch (param.trim().split(" ")[0]) {
            case "start", "play", "new", "game", "p" -> {

                long bet;
                UserEntity player = UserEntity.getUserByIdLong(event.getMember(), event.getAuthor(), userRepository);

                try {
                    bet = Integer.parseInt(param.trim().split(" ")[1]);
                    if (bet < 0 || bet > player.getFish()) {
                        throw new Exception();
                    }
                    player.subFish(bet);
                } catch (Exception e) {
                    channel.sendMessage("Invalid bet!").queue();
                    return true;
                }

                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Mia");
                builder.setDescription("Player: " + player.getUserName() + "\nStakes: " + bet);

                boolean running = true;
                int miaPlayer = 1;
                int lastThrow = 1;

                while (running) {
                    int firstRoll = (int) (Math.random() * 6) + 1;
                    int secondRoll = (int) (Math.random() * 6) + 1;
                    int totalRoll;
                    String result;

                    if (firstRoll > secondRoll) {
                        totalRoll = firstRoll * 10 + secondRoll;
                    } else {
                        totalRoll = secondRoll * 10 + firstRoll;
                    }

                    if (totalRoll == 21) {
                        running = false;
                        result = "Mia!";
                    } else if ((firstRoll != secondRoll && (lastThrow / 10) == (lastThrow % 10)) || totalRoll <= lastThrow) {
                        running = false;
                        result = "This throw is not higher!";
                    } else {
                        result = "This throw is higher!";
                    }

                    if (miaPlayer == 1) {
                        builder.addField("Your throw: " + totalRoll, "" + result, false);
                    } else {
                        builder.addField("Dealer throw: " + totalRoll, "" + result, false);
                    }
                    lastThrow = totalRoll;
                    if(totalRoll != 21) {
                        miaPlayer *= -1;
                    }
                }

                if (miaPlayer == 1) {
                    player.addFish(bet * 2L);
                    player.addMateability(1);
                    builder.addField("You won " + bet + " \uD83D\uDC1F !", "Your last throw could not be beaten!", false);
                } else {
                    player.subMateability(1);
                    builder.addField("You loose!", "Your score is not higher than the dealer's score!", false);
                }
                userRepository.saveAndFlush(player);
                channel.sendMessage(builder.build()).queue();
            }
            case "rules", "r" -> {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle("Mia Rules & Payout")
                        .addField("Rules", "https://en.wikipedia.org/wiki/Mia_(game)", false)
                        .addField("Payout", "win 2:1", false).build()
                ).queue();
            }
            default -> channel.sendMessage(help()).queue();
        }
        return true;
    }
}
