package com.example.demo.plugins.impl;

import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.database.entities.UserEntity;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserManager extends Plugin implements GuildMessageReceivedPlugin {
    private final UserRepository userRepository;
    private final RankClasses rankClasses;

    public UserManager(UserRepository userRepository, RankClasses rankClasses) {
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
                "stats",
                "levelup",
                "level"
        );
        this.userRepository = userRepository;
        this.rankClasses = rankClasses;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("User Help");
        builder.setDescription("UserManager here, how may I help you?");
        builder.addField("score <@username>", "display current stats", false);
        builder.addField("global", "display the global ranking", false);
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

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
            case "score", "elo", "mateability" -> {
                generateUserProfile(event.getChannel(), param, user);
            }
            case "global", "rank", "ranks", "stats" -> event.getChannel().sendMessage(globalRank(user)).queue();
            case "level", "levelup" -> {
                event.getChannel().sendMessage(level(userID)).queue();
            }
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
            for (int i = Math.max(startIndex - 2, 0); i < userList.size() && i <= startIndex + 2; i++) {
                positions.append(i);
                positions.append(". ");
                if (i == startIndex) positions.append("*");
                positions.append(userList.get(i).getUserName());
                positions.append(" - ");
                positions.append(userList.get(i).getMateability());
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

    private String level(long userId) {
        UserEntity user = userRepository.findById(userId).get();
        //min. humboldt -> dynamic ranking
        if(user.getRank().equals("humboldt")) {
            return "You can only reach higher levels through a higher mateability!";
        }
        if(user.getRank().equals("emperor")) {
            return "You are already the best penguin!";
        }

        RankClasses.Rank newRank = rankClasses.getRankClasses().entrySet()
                .stream().filter(x -> x.getValue().getLvl()
                        == rankClasses.getRankClasses().get(user.getRank()).getLvl() + 1)
                .findFirst().get().getValue();

        if(user.getFish() >= newRank.getCost()) {
            user.subFish(newRank.getCost());
            user.setRank(newRank.getEn());
            userRepository.saveAndFlush(user);
            return "Congratulations! You are now a " + newRank.getEn() + "!";
        } else {
            return "You don't have enough fish to grow!";
        }
    }

    private RankClasses.Rank getRank(UserEntity user) {

        Map<String, RankClasses.Rank> ranks = rankClasses.getRankClasses();

        List<UserEntity> userList = new ArrayList<>(userRepository.findAll(
                Sort.by(Sort.Direction.DESC, "mateability")));
        Map<Long, Long> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getMateability));

        //emperor
        UserEntity emperor = userList.stream().filter(x -> x.getFish() >= ranks.get("emperor")
                .getCost()).sorted().findFirst().orElse(userList.stream().findFirst().get());
        if(user.getUserId().equals(emperor.getUserId())) {
            return ranks.get("emperor");
        }

        if(user.getRank().equals("humboldt")) {
            int place = userList.stream().map(UserEntity::getUserId).collect(Collectors.toList())
                    .indexOf(user.getUserId()) * 8 / (userList.size() - 1);

            return ranks.entrySet()
                    .stream()
                    .filter(stringRankEntry -> stringRankEntry.getValue().getLvl() == place)
                    .findAny()
                    .get()
                    .getValue();
        } else {
            return ranks.get(user.getRank());
        }

    }
}
