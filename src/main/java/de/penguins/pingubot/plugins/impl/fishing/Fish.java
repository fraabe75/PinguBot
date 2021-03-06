package de.penguins.pingubot.plugins.impl.fishing;

import de.penguins.pingubot.database.entities.UserEntity;
import de.penguins.pingubot.database.repositories.UserRepository;
import de.penguins.pingubot.plugins.GuildMessageReactionAddPlugin;
import de.penguins.pingubot.plugins.GuildMessageReceivedPlugin;
import de.penguins.pingubot.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class Fish extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private final UserRepository userRepository;
    private final List<Message> runningFishGames;
    private final List<Message> waitingFishGames;

    private final String[] emotes = {
            "U+1F41F", // fish
            "U+1F40B", // whale
            "U+1F420", // tropical fish
            "U+1F42C", // dolphin
            "U+1F433", // whale 2
            "U+1F4A7", // droplet
            "U+1F30A", // ocean
            "U+1F421", // blowfish
    };

    public Fish(UserRepository userRepository) {
        setName("Fishing");
        setDescription("Go fishing!");
        addCommands("fish", "f");
        this.userRepository = userRepository;
        this.runningFishGames = new ArrayList<>();
        this.waitingFishGames = new ArrayList<>();
    }

    public static MessageEmbed help() {
        return new EmbedBuilder().setTitle("Petri Heil!")
                .setDescription("You find yourself in the need of some more, delicious fish?")
                .addField("fish", "start the fishing competition", false)
                .setFooter("Shortcuts: 'f'")
                .build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        TextChannel channel = event.getChannel();

        if (runningFishGames.stream().anyMatch(m -> m.getChannel().equals(channel))) {
            channel.sendMessage("Can't start a new fishing competition while there is already one happening" +
                    " in this channel.").queue();
        } else {
            Optional<Message> lastMessageToDelete;
            if ((lastMessageToDelete =
                    waitingFishGames.stream().filter(m -> m.getChannel().equals(channel)).findAny()).isPresent()) {
                waitingFishGames.remove(waitingFishGames.indexOf(lastMessageToDelete.get())).delete().queue();
            }
            channel.sendMessage(
                    new EmbedBuilder().setTitle("Time to go fishing!")
                            .setDescription("""
                                    Click the fish emote. **Only the fish emote!**

                                    Be careful, all non-fish reactions cost you a fish!""")
                            .setFooter("\uD83D\uDC1F : start, \uD83D\uDEB1 : abort")
                            .setColor(Color.BLUE).build()
            ).queue(m -> {
                waitingFishGames.add(m);
                m.addReaction("U+1F41F").queue();
                m.addReaction("U+1F6B1").queue();
            });
        }
        return true;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {
        if (event.getUser().isBot()) {
            return false;
        }
        TextChannel ch = event.getChannel();
        Optional<Message> messageOption = ch.getIterableHistory()
                .complete()
                .stream()
                .filter(message -> message.getId().equals(messageId))
                .findAny();
        if (messageOption.isPresent()) {
            Message m = messageOption.get();
            if (waitingFishGames.contains(m)) {
                waitingFishGames.remove(m);
                if (event.getReaction().getReactionEmote().getEmoji().equals("\uD83D\uDC1F")) {
                    startFishingCompetition(m);
                } else {
                    m.delete().queue();
                }
            } else if (!event.getUser().isBot() &&
                    runningFishGames.contains(m) &&
                    event.getReaction().retrieveUsers().stream().anyMatch(User::isBot)) {
                ch.clearReactionsById(messageId, event.getReactionEmote().getEmoji()).queue();

                UserEntity user;
                user = UserEntity.getUserByIdLong(
                        event.getGuild().getMember(event.getUser()),
                        event.getUser(),
                        userRepository
                );
                String memberName = user.getUserName();

                switch (event.getReactionEmote().getName()) {
                    case "\uD83D\uDC1F", "\uD83D\uDC20", "\uD83D\uDC21" -> {
                        int fishNum = (int) (Math.random() * 3) + 1;
                        ch.sendMessage(
                                "+" +
                                        fishNum +
                                        " :fish: for " +
                                        memberName
                        ).complete();
                        user.addFish(fishNum);
                    }
                    default -> {
                        ch.sendMessage(
                                "-1 :fish: for " +
                                        memberName
                        ).complete();
                        user.subFish(1);
                    }
                }
                userRepository.saveAndFlush(user);
                return true;
            }
        }
        return false;
    }

    private void startFishingCompetition(Message origM) {
        origM.clearReactions().queue();
        origM.editMessage(new EmbedBuilder().setTitle("Time to go fishing!")
                .setDescription("""
                        Click the fish emote. **Only the fish emote!**

                        Be careful, all non-fish reactions cost you a fish!""")
                .setColor(Color.BLUE).build()
        ).queue(m -> {
            try {
                runningFishGames.add(m);
                List<String> localEmotes = Arrays.stream(emotes).collect(Collectors.toList());

                for (int i = 0; i < 8; i++) {
                    Collections.shuffle(localEmotes);
                    localEmotes.stream().limit(5).forEach(e -> m.addReaction(e).queue());
                    Thread.sleep(3000);

                    m.clearReactions().queue();
                    Thread.sleep(500);
                }
                m.clearReactions().queue();
                m.editMessage(
                        new EmbedBuilder().setTitle("Fishing time is over!")
                                .setFooter("Start another fishing event with 'dp! fish'")
                                .build()
                ).queue();
                runningFishGames.remove(m);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
