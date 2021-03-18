package com.example.demo.plugins.impl.fishing;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.internal.requests.Route;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class Fish extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private final UserRepository userRepository;
    private final List<Long> fishGames;
    private final String[] emotes = {
            "U+1F41F", // fish
            "U+1F40B", // whale
            "U+1F420", // tropical fish
            "U+1F42C", // dolphin
            "U+1F433", // whale 2
            "U+1F4A7", // droplet
            "U+1F30A", // ocean
    };

    public Fish(UserRepository userRepository) {
        setName("Fish");
        setDescription("Go fishing!");
        addCommands("fish");
        this.userRepository = userRepository;
        this.fishGames = new ArrayList<>();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        TextChannel channel = event.getChannel();

        MessageHistory channelHistory = channel.getHistory();
        if (fishGames.stream().map(channelHistory::getMessageById).findAny().isPresent()) {
            channel.sendMessage("Can't start a new fishing competition while there is already one happening" +
                                " in this channel.").queue();
        } else {
            startFishingCompetition(channel);
        }
        return true;
    }

    @Override public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {
        TextChannel ch = event.getChannel();

        if (!event.getUser().isBot() && fishGames.contains(Long.parseLong(messageId))) {
            ch.clearReactionsById(messageId, event.getReactionEmote().getEmoji()).queue();

            UserEntity user;
            user = UserEntity.getUserByIdLong(
                    event.getGuild().getMember(event.getUser()),
                    event.getUser(),
                    userRepository
            );
            String memberName = user.getUserName();

            switch (event.getReactionEmote().getName()) {
                case "\uD83D\uDC1F", "\uD83D\uDC20" -> {
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
            return true;
        }
        return false;
    }

    private void startFishingCompetition(TextChannel ch) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Time to go fishing!")
                .setDescription("""
                                Click the fish emote. **Only the fish emote!**

                                Be careful, all non-fish reactions cost you a fish!""");

        ch.sendMessage(builder.build())
          .queue(m -> {
              try {
                  fishGames.add(m.getIdLong());
                  List<String> localEmotes = Arrays.stream(emotes).collect(Collectors.toList());

                  for (int i = 0; i < 8; i++) {
                      Collections.shuffle(localEmotes);
                      localEmotes.stream().limit(6).forEach(e -> {
                          m.addReaction(e).queue();
                      });
                      Thread.sleep(5000);

                      m.clearReactions().queue();
                      Thread.sleep(500);
                  }
                  m.clearReactions().queue();
                  m.editMessage(
                          new EmbedBuilder().setTitle("Fishing time is over!")
                                            .setFooter("Start another fishing event with 'dp! fish'")
                                            .build()
                  ).queue();
                  fishGames.remove(m.getIdLong());
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
          });
    }
}
