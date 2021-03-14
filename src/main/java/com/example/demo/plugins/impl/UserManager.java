package com.example.demo.plugins.impl;

import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.database.entities.UserEntity;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManager extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;

    @Autowired
    private RankClasses rankClasses;

    public UserManager(UserRepository userRepository) {
        setName("UserManager");
        setPrefix("user");
        setDescription("Displays userprofile stuff");
        addCommands("fish", "score", "mateability", "elo", "rank", "help");

        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("User Help");
        builder.setDescription("UserManager here, how may I help you?");
        builder.addField(
                "'user' + ['fish', 'score', 'mateability', 'elo', 'rank']:",
                "display your current stats",
                false
        );
        return builder.build();
    }

    // TODO: implement global rank
    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        if (!commands().contains(command)) {
            return false;
        }

        long userID = event.getAuthor().getIdLong();
        UserEntity user;

        if (!userRepository.existsById(userID)) {
            event.getChannel().sendMessage(
                    "Sorry, but we don't know you yet. But you now have a brand new profile!"
            ).queue();
            userRepository.saveAndFlush(user = new UserEntity(userID, event.getAuthor().getName()));
        } else {
            user = userRepository.getOne(userID);
        }

        event.getChannel().sendMessage(
                switch (command) {
                    case "fish", "score", "elo", "rank", "mateability", "" -> getUserProfileEmbed(user);
                    default -> help();
                }
        ).queue();

        return true;
    }

    private MessageEmbed getUserProfileEmbed(UserEntity user) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(user.getUserName() + "'s profile:");
        builder.addField("Fish", ((Long) user.getFish()).toString(), true);
        builder.addField("Mateability", ((Long) user.getMateability()).toString(), true);
        builder.setDescription("""
                               Collect and bet your fish.
                               Wins improve your popularity\s
                               and therefore your mateability factor.""");

        RankClasses.Rank userRank = getRank(user);
        builder.addField("Rank", userRank.getDe(), true);
        //builder.setThumbnail(userRank.getImg());

        return builder.build();
    }

    private RankClasses.Rank getRank(UserEntity user) {
        List<Long> userIDs = userRepository.findAll(Sort.by(Sort.Direction.ASC, "mateability"))
                                           .stream().map(UserEntity::getUserId).collect(Collectors.toList());
        int numPlayers = userIDs.size() == 0 ? 1 : userIDs.size();
        int place = (int) ((userIDs.indexOf(user.getUserId()) + 1.0) / numPlayers * 10) - 1;

        return rankClasses.getRankClasses()
                          .entrySet()
                          .stream()
                          .filter(e -> e.getValue().getLvl() == place)
                          .findAny()
                          .get().getValue();
    }
}
