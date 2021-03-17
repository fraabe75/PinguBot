package com.example.demo.plugins.impl.fishing;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class Fish extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private final UserRepository userRepository;
    private final List<Long> fishGames;

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
        if (!event.getUser().isBot() && fishGames.contains(Long.parseLong(messageId))) {
            event.getChannel()
                 .removeReactionById(messageId, event.getReactionEmote().getEmoji(), event.getUser())
                 .queue();
            event.getChannel()
                 .removeReactionById(messageId, event.getReactionEmote().getEmoji())
                 .queue();
            switch(event.getReactionEmote().getName()) {
                case "\uD83D\uDC1F", "\uD83D\uDC20" -> System.out.println("Fish");
                default -> System.out.println("no Fish!");
            }
            return true;
        }
        return false;
    }

    private void startFishingCompetition(TextChannel ch) {
        List<String> emotes = new ArrayList<>();
        emotes.add("U+1F41F"); // fish
        emotes.add("U+1F40B"); // whale
        emotes.add("U+1F420"); // tropical fish
        emotes.add("U+1F42C"); // dolphin
        emotes.add("U+1F4A7"); // droplet
        emotes.add("U+1F30A"); // ocean
        emotes.add("U+1F433"); // whale 2

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Time to go fishing!")
                .setDescription("Click the fish emote. **Only the fish emote!**" +
                                "\n\nBe careful, all non-fish reactions cost you a fish!");

        ch.sendMessage(builder.build())
          .queue(m -> {
              fishGames.add(m.getIdLong());
              long startTime = System.currentTimeMillis();
              List<String> lastAdded = new LinkedList<>();

              while (!Thread.interrupted() && System.currentTimeMillis() - startTime < 10_000) {
                  Collections.shuffle(lastAdded);
                  lastAdded.subList(0, Math.min(3, lastAdded.size())).forEach(e -> m.removeReaction(e).queue());

                  Collections.shuffle(emotes);
                  emotes.stream().limit(4).forEach(e -> {
                      m.addReaction(e).queue();
                      lastAdded.add(e);
                  });
                  try {
                      Thread.sleep(100);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
              }
              fishGames.remove(m.getIdLong());
          });
    }
}
