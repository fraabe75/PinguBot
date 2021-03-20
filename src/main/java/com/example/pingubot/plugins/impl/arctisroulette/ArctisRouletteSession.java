package com.example.pingubot.plugins.impl.arctisroulette;

import com.example.pingubot.database.entities.UserEntity;
import com.example.pingubot.database.repositories.UserRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ArctisRouletteSession {
    private final UserRepository userRep;
    private final TextChannel channel;
    private final Message message;
    private final Random gen;
    private boolean waiting;
    private Message uMessage;
    private final long startingPlayer;
    private final Map<UserEntity, Integer> players;
    private final List<UserEntity> playerOrder;
    private final long startingBet;
    private int currPlayerIndex;
    private final List<String> fallingGifs = List.of(
            "https://media.giphy.com/media/Ur8qw9UJEhNuw/source.gif",
            "https://media.giphy.com/media/BvBEozfsXWWHe/source.gif",
            "https://media.giphy.com/media/f2HiQKaEkaKwo/source.gif",
            "https://media.giphy.com/media/Eq49yQGJL835K/source.gif",
            "https://media.giphy.com/media/iN3LCFAxq2euaK3p03/source.gif",
            "https://media.giphy.com/media/qxYkv4GuyYbbG/source.gif",
            "https://media.giphy.com/media/jiiRUIaVpG89i/source.gif",
            "https://media.giphy.com/media/2aNV8HhubqM1y/source.gif",
            "https://media.giphy.com/media/289LjteeoUNvBBoCcf/source.gif",
            "https://media.giphy.com/media/3oriNU1IYJKhgZwWVW/source.gif",
            "https://media.giphy.com/media/28LGlfos6cb0NcmzhG/source.gif",
            "https://media.giphy.com/media/TpkhbFd6ap0pq/source.gif",
            "https://media.giphy.com/media/jWtObk5nPsiNG/source.gif",
            "https://media.giphy.com/media/ZfkoYy1w5506A/source.gif",
            "https://media.giphy.com/media/rSzvcaltZQTm/source.gif",
            "https://media.giphy.com/media/8nbGW1erXutpu/source.gif",
            "https://media.giphy.com/media/Wz7gk4e2Pxcmk/source.gif",
            "https://media.giphy.com/media/nAfzRC9fW8KDm/source.gif",
            "https://media.giphy.com/media/RJL3WB6GLuNqM/source.gif",
            "https://media.giphy.com/media/RUOylT65h8s1y/source.gif",
            "https://media.giphy.com/media/l0HlCL7x115yy1S2k/source.gif",
            "https://media.giphy.com/media/4KALRmOb8uwbC/source.gif",
            "https://media.giphy.com/media/KD1u7Iy7j8bQeoFlyH/source.gif",
            "https://media.giphy.com/media/3o84TRb3QysW4gQNZm/source.gif",
            "https://media.giphy.com/media/l3vRcQzQARIkhawYU/source.gif",
            "https://media.giphy.com/media/X8lDnSvUS3Mhq/source.gif",
            "https://media.giphy.com/media/q1THMpJ5wrFFC/source.gif"
    );

    public ArctisRouletteSession(UserRepository userRep, TextChannel channel, Message message, long startingBet) {
        this.userRep = userRep;
        this.channel = channel;
        this.waiting = true;
        this.players = new HashMap<>();
        this.playerOrder = new LinkedList<>();
        this.currPlayerIndex = 0;
        this.gen = new Random();
        this.startingPlayer = message.getAuthor().getIdLong();
        this.startingBet = startingBet;

        joinPlayer(message.getMember(), message.getAuthor());

        this.message = channel.sendMessage(gameMessage()).complete();
        this.message.addReaction("\uD83D\uDCB8").queue();
        this.message.addReaction("\u25B6\uFE0F").queue();
        this.message.addReaction("\u274C").queue();
    }

    private MessageEmbed gameMessage() {
        EmbedBuilder builder = new EmbedBuilder();
        if (isWaiting()) {
            builder.setTitle("It's time to join!");
            builder.addField(
                    ":money_with_wings: : Join the game",
                    "Be careful, this costs you " + startingBet + " :fish:!",
                    false);
            builder.addField(
                    ":arrow_forward: | :x: : Start / Stop the game",
                    "Only the player who initialized " +
                    "this session may use this.",
                    false);
            builder.addField(
                    "Current players:",
                    players.keySet().stream().map(UserEntity::getUserName).collect(Collectors.joining(", ")),
                    false
            );
        } else {
            builder.setTitle("Jump! Jump!");
            builder.addField("Current turn: " + playerOrder.get(currPlayerIndex).getUserName(), "", true);
            AtomicInteger index = new AtomicInteger(1);
            builder.addField(
                    "Player ordering:",
                    playerOrder.stream().map(
                            u -> (index.get() == currPlayerIndex - 1 ? "**" : "") +
                                 index.get() +
                                 ". " +
                                 u.getUserName() +
                                 (index.getAndIncrement() == currPlayerIndex - 1 ? "**" : "")
                    ).collect(Collectors.joining("\n")),
                    false
            );
        }
        return builder.build();
    }

    public boolean isWaiting() {
        return waiting;
    }

    public Message getMessage() {
        return message;
    }

    public static MessageEmbed help() {
        return new EmbedBuilder().setTitle("Arctic Roulette Help")
                                 .setDescription(
                                         """
                                         Don't play this game. Don't get pressured into playing.
                                         You will loose. Nearly every player regrets playing it.
                                         """
                                 ).addField(
                        "Basic principle:",
                        """
                        1. A fearless penguin starts a round by placing the first bet, like a small blind.
                        (Penguin Ltd. is legally required to tell you that this action is not refundable.)\s
                        
                        2. Other foolish penguins join the competition by betting their lunch money.
                        (This again is not refundable!)\s
                        
                        3. All playing penguins place themselves on a thin floating sheet of ice.\s
                        4. One after another, the penguins jump up.\s
                        5. The last penguin who didn't fall into the cold water wins all the bets. \s
                        """,
                        false).setFooter(
                        """
                        Shotcuts: ar, arc
                                                    
                        Disclaimer: Penguin Ltd. has to legally state that all participants are trained
                        actors and aware of the risks involved. Please do not try to replicate the actions
                        performed at home. This game does not promote illegal gambling. Any written or spoken 
                        advise is given as a personal opinion, not a fact. Gambling can be addictive.
                        """)
                                 .addField(
                                         "arctis (<bet-amount> | all)",
                                         "start a new game and lure other players into joining.",
                                         false
                                 ).build();
    }

    private boolean joinPlayer(Member mem, User user) {
        UserEntity userEntity = UserEntity.getUserByIdLong(mem, user, userRep);
        if (userEntity.getFish() < startingBet) {
            if (uMessage != null) {
                uMessage.delete().queue();
            }
            channel.sendMessage("Sorry, but you're too broke to participate in games with your rich friends.")
                   .queue(m -> uMessage = m);
        } else if (players.keySet().stream().anyMatch(uE -> uE.getUserId().equals(userEntity.getUserId()))) {
            if (uMessage != null) {
                uMessage.delete().queue();
            }
            channel.sendMessage("You can't join twice, dumdum!").queue(m -> uMessage = m);
        } else {
            userEntity.subFish(startingBet);
            userRep.saveAndFlush(userEntity);
            players.put(userEntity, 100);
        }
        return false;
    }

    private boolean startGame(User user) {
        if (user.getIdLong() == startingPlayer) {
            if (players.size() >= 2) {
                if (uMessage != null) {
                    uMessage.delete().queue();
                }
                waiting = false;
                message.clearReactions().complete();
                playerOrder.addAll(players.keySet());
                Collections.shuffle(playerOrder);
                message.editMessage(gameMessage()).queue();
                message.addReaction("\uD83C\uDD99").queue();
            } else {
                channel.sendMessage("You godda at least find another player!").queue(m -> uMessage = m);
            }
        }
        return false;
    }

    private boolean deleteGame(User user) {
        return user.getIdLong() == startingPlayer;
    }

    public boolean reactionAdded(GuildMessageReactionAddEvent e) {
        message.removeReaction(e.getReactionEmote().getEmoji(), e.getUser()).queue();
        if (isWaiting()) {
            return switch (e.getReactionEmote().getEmoji()) {
                case "\u25B6\uFE0F" -> startGame(e.getUser());
                case "\uD83D\uDCB8" -> joinPlayer(e.getMember(), e.getUser());
                case "\u274C" -> deleteGame(e.getUser());
                default -> false;
            };
        }
        if (e.getReactionEmote().getEmoji().equals("\uD83C\uDD99")) {
            if (uMessage != null) {
                uMessage.delete().queue();
            }

            UserEntity currPlayer = playerOrder.get(currPlayerIndex);
            if (e.getUser().getIdLong() != currPlayer.getUserId()) {
                UserEntity otherPlayer = UserEntity.getUserByIdLong(e.getMember(), e.getUser(), userRep);
                channel.sendMessage(
                        "Don't force that poor little penguin to jump! (" +
                        otherPlayer.getUserName() +
                        " tried to manipulate " +
                        currPlayer.getUserName() +
                        ")").queue(m -> uMessage = m);
            }
            int currIceHealth = players.get(currPlayer);
            currIceHealth -= gen.nextInt(101);
            if (currIceHealth <= 0) {
                channel.sendMessage(new EmbedBuilder().setTitle("Platsch!")
                                                      .setDescription("You fell into the cold water.\nBetter start a " +
                                                                      "diet before competing again...")
                                                      .setImage(fallingGifs.get(gen.nextInt(fallingGifs.size())))
                                                      .build()
                ).queue(m -> uMessage = m);
            } else if (currIceHealth < 33) {
                channel.sendMessage("You hear load cracking noises as you touch down...").queue(m -> uMessage = m);
            } else if (currIceHealth < 66) {
                channel.sendMessage("The first visible cracks start to form...").queue(m -> uMessage = m);
            } else {
                channel.sendMessage("Apparently your piece of ice survived your last jump quite well...")
                       .queue(m -> uMessage = m);
            }
            players.put(currPlayer, currIceHealth);
            if (currIceHealth <= 0) {
                playerOrder.remove(currPlayer);
                currPlayer.subMateability(1);
                userRep.saveAndFlush(currPlayer);
            }
            if (playerOrder.size() == 1) {
                channel.sendMessage(new EmbedBuilder().setTitle("Last men standing!")
                                                      .addField(
                                                              playerOrder.get(0).getUserName(),
                                                              "You won " + players.size() * startingBet + " :fish:\n" +
                                                              "and +1 :penguin: Mateability!",
                                                              false
                                                      ).build()
                ).queue();
                playerOrder.get(0).addFish(players.size() * startingBet);
                playerOrder.get(0).addMateability(1);
                userRep.saveAndFlush(playerOrder.get(0));
                return true;
            }
            currPlayerIndex = (currPlayerIndex + 1) % players.size();
            message.editMessage(gameMessage()).queue();
        }
        return false;
    }
}
