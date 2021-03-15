package com.example.demo.plugins.impl;

import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.database.entities.UserEntity;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserManager extends Plugin implements GuildMessageReceivedPlugin {
    private final UserRepository userRepository;

    @Autowired
    private RankClasses rankClasses;

    public UserManager(UserRepository userRepository) {
        setName("User");
        setDescription("Displays userprofile stuff");
        addCommands(
                "score",
                "mateability",
                "elo",
                "rank",
                "ranks",
                "user",
                "global",
                "statistic"
        );
        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("User Help");
        builder.setDescription("UserManager here, how may I help you?");
        builder.addField(
                "user [<username> | self]:",
                "display current stats",
                false
        );
        builder.addField(
                "(statistic | global | rank):",
                "display the global ranking",
                false
        );
        return builder.build();
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
            case "score", "elo", "mateability", "user" -> {
                generateUserProfile(event.getChannel(), param, user);
            }
            case "global", "rank", "ranks", "statistic" -> event.getChannel().sendMessage(globalRank(user)).queue();
            default -> event.getChannel().sendMessage(help()).queue();
        }

        return true;
    }

    private void generateUserProfile(TextChannel channel, String param, UserEntity user) {
        if (param.isBlank() || param.equalsIgnoreCase("self")) {
            sendUserProfileEmbed(user, channel);
        } else {
            param = param.replace("@", "");
            UserEntity foundMember = null;

            if (param.matches("^<!\\d+>$")) {
                long userID = Long.parseLong(param.substring(2, param.length() - 1));
                if (userRepository.existsById(userID)) {
                    foundMember = userRepository.getOne(userID);
                }
            } else {
                String proposedName = param.substring(0, param.contains("#") ? param.indexOf("#") : param.length());
                foundMember = userRepository.findAll()
                                            .stream()
                                            .filter(u -> u.getUserName().equalsIgnoreCase(proposedName))
                                            .findAny()
                                            .orElse(null);
            }

            if (foundMember == null) {
                channel.sendMessage(
                        "Sorry, but our penguins weren't able to find any colony member identified by \""
                        + param
                        + "\"."
                ).queue();
            } else {
                sendUserProfileEmbed(foundMember, channel);
            }
        }
    }

    private MessageEmbed globalRank(UserEntity user) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("Highscores:");
        List<UserEntity> userList = userRepository.findAll(Sort.by(Sort.Direction.DESC, "mateability"));
        List<Long> userIdList = userList.stream().map(UserEntity::getUserId).collect(Collectors.toList());

        StringBuilder topTen = new StringBuilder();
        for (int i = 0; i < userList.size() && i < 10; i++) {
            topTen.append(i);
            topTen.append(". ");
            topTen.append(userList.get(i).getUserName());
            topTen.append(" - ");
            topTen.append(userList.get(i).getMateability());
            topTen.append("\n");
        }
        builder.addField("Global Top Ten:", topTen.toString(), false);

        if (!userIdList.subList(0, userList.size() <= 9 ? userList.size() - 1 : 9).contains(user.getUserId())) {
            StringBuilder positions = new StringBuilder("...\n");
            int startIndex = userIdList.indexOf(user.getUserId());
            for (int i = startIndex - 2; i < userList.size() && i <= startIndex + 2; i++) {
                positions.append(i);
                positions.append(". ");
                if (i == startIndex) positions.append("*");
                positions.append(userList.get(i).getUserName());
                if (i == startIndex) positions.append("*");
                positions.append("\n");
            }
            positions.append("...");
            builder.addField("Your are currently on position " + startIndex + ":", positions.toString(), false);
        }

        return builder.build();
    }

    private void sendUserProfileEmbed(UserEntity user, TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(user.getUserName() + "'s profile:");
        builder.addField("Fish", ((Long) user.getFish()).toString() + " :fish:", true);
        builder.addField("Mateability", ((Long) user.getMateability()).toString() + " :penguin:", true);
        builder.setDescription("""
                               Collect fish and improve your\s
                               mateability by winning games.""");

        RankClasses.Rank userRank = getRank(user);
        ClassPathResource file = new ClassPathResource(userRank.getImg());

        builder.addField("Rank", userRank.getEn(), true);
        builder.setThumbnail("attachment://" + file.getFilename());
        try {
            channel.sendMessage(builder.build())
                   .addFile(file.getFile(), Objects.requireNonNull(file.getFilename()))
                   .queue();
        } catch (IOException e) {
            System.err.println("Couldn't load profile picture: " + e.getMessage());
        }
    }

    private RankClasses.Rank getRank(UserEntity user) {
        List<Long> userIDs = userRepository.findAll(Sort.by(Sort.Direction.ASC, "mateability"))
                                           .stream().map(UserEntity::getUserId).collect(Collectors.toList());
        int place = userIDs.indexOf(user.getUserId()) * 9 / (userIDs.size() - 1);
        return rankClasses.getRankClasses()
                          .entrySet()
                          .stream()
                          .filter(e -> e.getValue().getLvl() == place)
                          .findAny()
                          .get()
                          .getValue();
    }
}
