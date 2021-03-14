package com.example.demo.plugins.impl;

import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.database.entities.UserEntity;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManager extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;

    public UserManager(UserRepository userRepository) {
        setName("UserManager");
        addCommands("fish", "score", "mateability", "elo", "rank");

        this.userRepository = userRepository;
    }

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

        switch (command) {
            case "fish", "score", "elo", "rank", "mateability", "" -> event.getChannel()
                                                                           .sendMessage(getUserProfileEmbed(user))
                                                                           .queue();
        }

        return true;
    }

    private MessageEmbed getUserProfileEmbed(UserEntity user) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(user.getUserName() + "'s profile:");
        builder.addField(new MessageEmbed.Field("Fish", ((Long) user.getFish()).toString(), true));
        builder.addField(new MessageEmbed.Field("Mateability", ((Long) user.getMateability()).toString(), true));
        builder.setDescription("Collect and bet your fish.\nWins improve your popularity \n" +
                               "and therefore your mateability factor.");
        return builder.build();
    }

    private String getRank(UserEntity user) {
        List<UserEntity> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "mateability"));
        int place = (int) (users.indexOf(user) / (double) users.size() * 10) + 1;
        return switch(place) {
            case 9 -> "Königspinguin";
            case 8 -> "Eselspinguin";
            case 7 -> "Goldschopfpinguin";
            case 6 -> "Adeliepinguin";
            case 5 -> "Haubenpinguin";
            case 4 -> "Magellan-Pinguin";
            case 3 -> "Humbold-Pinguin";
            case 2 -> "Felsenpinguin";
            case 1 -> "Galapagospinguin";
            default -> "Kaiserpinguin";
        };
    }
}
