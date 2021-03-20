package com.example.pingubot.plugins.impl.usermanager;

import com.example.pingubot.database.entities.UserEntity;
import com.example.pingubot.database.repositories.UserRepository;
import com.example.pingubot.plugins.GuildMessageReceivedPlugin;
import com.example.pingubot.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserManager extends Plugin implements GuildMessageReceivedPlugin {
    private final UserRepository userRepository;
    private final Map<String, RankClasses.Rank> rankClasses;

    public UserManager(UserRepository userRepository, RankClasses rankClasses) {
        setName("User");
        setDescription("Displays userprofile stuff.");
        addCommands(
                "user", "u",
                "score", "mateability", "elo", "s",
                "global", "stats", "rank", "ranks", "g",
                "levelup", "level", "lvl", "l",
                "send", "transaction"
        );
        this.userRepository = userRepository;
        this.rankClasses = rankClasses.getRankClasses();
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("User Help");
        builder.setDescription("UserManager here, how may I help you?");
        builder.addField("score <@username>", "display current stats", false);
        builder.addField("global", "display the global ranking", false);
        builder.addField("level", "eat fish to level up", false);
        builder.addField("send <@username> <value>", "send fish to your friends", false);
        return builder.build();
    }

    public static MessageEmbed helpLevel() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Level Help");
        builder.setDescription("""
                Eat fish to grow and level up!
                                               
                As soon as you grew enough to become a Humboldt (adult state)
                penguin, your rank will be determined dynamically,
                base on your place in the colony's social structure.
                                               
                Remember: As soon as you are an adult (humboldt penguin or higher),\s
                your mateability is the only thing that counts!
                                               
                Get the highest mateability to become the next emperor!\s""");
        //update prizes from application.properties
        builder.addField("Baby: Fairy penguin", "You need 100 fish to level up!", false);
        builder.addField("0: Galapagos penguin", "You need 200 fish to level up!", false);
        builder.addField("1: Fjordland penguin", "You need 300 fish to level up!", false);
        builder.addField("2: Rockhopper penguin", "You need 500 fish to level up!", false);
        builder.addField("3: Snares penguin", "You need 1000 fish to level up!", false);
        builder.addField("4: Humboldt penguin", "meet me in the Munich zoo", false);
        builder.addField("5: Macaroni penguin", "Macaroni's can hop as well hop as waddle", false);
        builder.addField("6: Magellanic penguin", "visit me on the Falkland Islands", false);
        builder.addField("7: Adelie penguin", "we waddle around about 1.5 mph", false);
        builder.addField("8: Gentoo penguin", "maybe you should try gentoo linux", false);
        builder.addField("9: Yelloweyed penguin", "yelloweyed penguins are the fourth longest penguins", false);
        builder.addField("10: Royal penguin", "royal blood flows through your veins now", false);
        builder.addField("11: King penguin", "only a few steps away from the throne", false);
        builder.addField("12: Emperor penguin", "the one and only champion", false);
        builder.setImage("https://i.redd.it/eyymtmpph1u01.jpg");
        builder.setFooter("Find the global rank with: 'dp! global'");
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        TextChannel channel = event.getChannel();
        UserEntity user = UserEntity.getUserByIdLong(event.getMember(), event.getAuthor(), userRepository);

        if (command.equals("u") || command.equals("user")) {
            command = param.split(" ")[0];
            param = param.substring(command.length()).trim();
        }
        switch (command) {
            case "score", "elo", "mateability", "s" -> generateUserProfile(event.getChannel(), param, user);
            case "global", "rank", "ranks", "stats", "g" -> event.getChannel().sendMessage(globalRank(user)).queue();
            case "level", "levelup", "lvl", "l" -> {
                channel.sendMessage(level(user)).queue();
                generateUserProfile(event.getChannel(), "", user);
            }
            case "send", "transaction" -> {
                if (transaction(user, param)) {
                    channel.sendMessage("Transaction successful! Your new balance:").queue();
                    generateUserProfile(channel, "", user);
                } else {
                    channel.sendMessage("Transaction denied! Please try again.").queue();
                }
            }
            default -> channel.sendMessage(help()).queue();
        }

        return true;
    }

    private void generateUserProfile(TextChannel channel, String param, UserEntity user) {
        if (param.isBlank() || param.equalsIgnoreCase("self")) {
            sendUserProfileEmbed(user, channel);
        } else {
            Optional<UserEntity> dbMember = parseDatabaseUser(param);
            if (dbMember.isEmpty()) {
                channel.sendMessage(
                        "Sorry, but our penguins weren't able to find any colony member identified by \""
                                + param
                                + "\"."
                ).queue();
            } else {
                sendUserProfileEmbed(dbMember.get(), channel);
            }
        }
    }

    private Optional<UserEntity> parseDatabaseUser(String search) {
        if (search.matches("^<@!\\d+>$")) {
            long userID = Long.parseLong(search.substring(3, search.length() - 1));
            return userRepository.findById(userID);
        } else {
            String proposedName = search.substring(0, search.contains("#") ? search.indexOf("#") : search.length())
                    .replace("@", "");
            return userRepository.findAll()
                    .stream()
                    .filter(u -> u.getUserName().equalsIgnoreCase(proposedName))
                    .findAny();
        }
    }

    private MessageEmbed globalRank(UserEntity user) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle("Global Top Ten:");
        List<UserEntity> userList = userRepository.findAll(Sort.by(Sort.Direction.DESC, "mateability", "fish"));
        List<Long> userIdList = userList.stream().map(UserEntity::getUserId).collect(Collectors.toList());

        builder.setDescription(":fish: : fish | :penguin: : mateability");

        builder.addField(
                "Emperor of the colony:",
                userList.get(0).getUserName() +
                        " (" +
                        userList.get(0).getFish() +
                        " | " +
                        userList.get(0).getMateability() +
                        ")",
                false
        );

        for (int i = 1; i < userList.size() && i < 10; i++) {
            builder.addField(
                    (i + 1) + ". " + userList.get(i).getUserName(),
                    "(" + userList.get(i).getFish() + " | " + userList.get(i).getMateability() + ")",
                    true
            );
        }

        if (!userIdList.subList(0, userList.size() <= 9 ? userList.size() - 1 : 9).contains(user.getUserId())) {
            StringBuilder positions = new StringBuilder("...\n");
            int startIndex = userIdList.indexOf(user.getUserId());
            for (int i = Math.max(startIndex - 1, 0); i < userList.size() && i <= startIndex + 1; i++) {
                positions.append(i + 1);
                positions.append(". ");
                if (i == startIndex) {
                    positions.append("*");
                }
                positions.append(userList.get(i).getUserName());
                if (i == startIndex) {
                    positions.append("*");
                }
                positions.append(" - (");
                positions.append(userList.get(i).getFish());
                positions.append(" :fish: | ");
                positions.append(userList.get(i).getMateability());
                positions.append(" :penguin: )");
                positions.append("\n");
            }
            positions.append("...");
            builder.addField(
                    "Your are currently on position " + (startIndex + 1) + ":",
                    positions.toString(),
                    false
            );
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

    private String level(UserEntity user) {
        //min. humboldt -> dynamic ranking
        if (getRank(user).getEn().equals("Emperor penguin")) {
            return "You are already the greatest penguin of all time!";
        }
        if (user.getRank().equals("dynamic")) {
            return "You can only reach higher levels through a higher mateability! (You are already an adult)";
        }

        Optional<Map.Entry<String, RankClasses.Rank>> newRank = rankClasses.entrySet()
                .stream()
                .filter(x -> x.getValue().getLvl() ==
                        rankClasses.get(user.getRank())
                                .getLvl() + 1)
                .findAny();
        if (newRank.isEmpty()) {
            return "Database error!";
        }

        if (user.getFish() >= newRank.get().getValue().getCost()) {
            user.subFish(newRank.get().getValue().getCost());
            if (newRank.get().getKey().equals("humboldt")) {
                user.setRank("dynamic");
                userRepository.saveAndFlush(user);
                return "Congratulations! You are now part of the dynamic rank system!";
            } else {
                user.setRank(newRank.get().getKey());
                userRepository.saveAndFlush(user);
                return "Congratulations! You reached the next level!";
            }
        } else {
            return "You don't have enough fish to grow! Next level requires " +
                    newRank.get().getValue().getCost() +
                    " :fish:!";
        }
    }

    private RankClasses.Rank getRank(UserEntity user) {

        List<Long> dynamicUserList = new ArrayList<>(userRepository.findAllByRankDynamic());

        if (user.getRank().equals("dynamic")) {
            //emperor
            UserEntity emperor = userRepository.getOne(dynamicUserList.stream().findFirst().orElseThrow());
            if (user.getUserId().equals(emperor.getUserId())) {
                return rankClasses.get("emperor");
            }
            //dynamic ranks
            int place = dynamicUserList.indexOf(user.getUserId()) * 8 / dynamicUserList.size();
            return rankClasses.entrySet().stream()
                    .filter(stringRankEntry -> (stringRankEntry.getValue().getLvl() - 11) * (-1) == place)
                    .findAny().orElseThrow().getValue();
        }
        return rankClasses.getOrDefault(user.getRank(), rankClasses.get("fairy"));
    }

    private synchronized boolean transaction(UserEntity sender, String param) {
        try {
            long amount = Long.parseLong(param.split(" ")[1].trim());
            Optional<UserEntity> receiver = parseDatabaseUser(param.split(" ")[0].trim());

            if (receiver.isPresent() && !sender.getUserId().equals(receiver.get().getUserId())) {
                if (amount <= 0 || amount > sender.getFish()) {
                    return false;
                }
                sender.subFish(amount);
                receiver.get().addFish(amount);
                userRepository.save(sender);
                userRepository.saveAndFlush(receiver.get());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
