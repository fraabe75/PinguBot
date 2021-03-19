package com.example.demo.plugins.impl.blackjack;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Blackjack extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private final HashMap<User, Game> games = new HashMap<>();
    private final UserRepository userRepository;

    public Blackjack(UserRepository userRepository) {
        setName("Blackjack");
        setDescription("Play a fun game of Blackjack!");
        addCommands("b", "bj", "blackjack");
        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Blackjack Help");
        builder.setDescription(
                "Las Vegas is the only place I know\nwhere money really talks. It says goodbye.\n~ Frank Sinatra");
        builder.addField("blackjack play <value>", "start a new game", false);
        builder.addField("blackjack end", "end the current game", false);
        builder.addField("blackjack rules", "rules and payout rates", false);
        builder.setFooter("Shortcuts: 'bj', 'b'");
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        TextChannel channel = event.getChannel();
        User user = event.getAuthor();
        Member member = event.getMember();

        switch (param.trim().split(" ")[0]) {
            case "start", "play", "new", "game", "p" -> {
                if (!games.containsKey(user)) {
                    long bet;
                    try {
                        bet = Integer.parseInt(param.trim().split(" ")[1]);
                        UserEntity player = UserEntity.getUserByIdLong(member, user, userRepository);
                        if (bet <= 0 || bet > player.getFish()) {
                            throw new Exception();
                        }
                        player.subFish(bet);
                        userRepository.saveAndFlush(player);
                    } catch (Exception e) {
                        channel.sendMessage("Invalid bet!").queue();
                        return true;
                    }
                    Game game = new Game(userRepository);
                    games.put(event.getAuthor(), game);
                    game.player = user;
                    game.member = member;
                    game.bet = bet;
                    channel.sendMessage(game.hit(false)).queue(message -> {
                        message.addReaction("\u261D").queue();
                        message.addReaction("\u270B").queue();
                        game.messageId = message.getId();
                        if (game.userScore == 21) {
                            game.updateAccount(1);
                            channel.sendMessage("Blackjack! You won " + game.bet * 3 / 2 + " \uD83D\uDC1F !").queue();
                            channel.removeReactionById(game.messageId, "\u261D").queue();
                            channel.removeReactionById(game.messageId, "\u270B").queue();
                            games.remove(user);
                        }
                    });
                } else {
                    channel.sendMessage("You are already playing!").queue();
                }
            }
            case "end", "terminate", "e" -> {
                if (games.containsKey(user)) {
                    games.get(user).updateAccount(2);
                    games.remove(user);
                    channel.sendMessage("Terminated your current game!").queue();
                } else {
                    channel.sendMessage("No game active!").queue();
                }
            }
            case "rules" -> {
                channel.sendMessage(new EmbedBuilder()
                        .setTitle("Blackjack Rules & Payout")
                        .addField("Rules", "https://de.wikipedia.org/wiki/Black_Jack", false)
                        .addField("Payout", "Blackjack 3:2, win 1:1", false).build()
                ).queue();
            }
            default -> channel.sendMessage(help()).queue();
        }
        return true;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {

        if (event.getUser().isBot() || !games.containsKey(event.getUser()) ||
            !games.get(event.getUser()).messageId.equals(messageId)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        Game game = games.get(event.getUser());

        //hit
        if (event.getReactionEmote().getEmoji().equals("\u261D")) {
            channel.editMessageById(game.messageId, game.hit(true))
                   .queue(message -> message.removeReaction("\u261D", game.player).queue());
            if (game.userScore > 21) {
                channel.removeReactionById(game.messageId, "\u261D").queue();
                channel.removeReactionById(game.messageId, "\u270B").queue();
                games.remove(event.getUser());
            }
            return true;
        }

        //stand
        if (event.getReactionEmote().getEmoji().equals("\u270B")) {
            channel.editMessageById(game.messageId, game.stand())
                   .queue(message -> message.removeReaction("\u270B", game.player).queue());
            channel.removeReactionById(game.messageId, "\u261D").queue();
            channel.removeReactionById(game.messageId, "\u270B").queue();
            games.remove(event.getUser());
            return true;
        }

        return false;
    }

    static class Game extends Blackjack {

        private User player;
        private Member member;
        private String messageId;
        private int userScore;
        private int dealerScore;
        private int numberOfCards;
        private long bet;

        private final ArrayList<Cards> dealerCards;
        private final ArrayList<Cards> playerCards;
        private final Stack<Cards> kartenstapel;

        public Game(UserRepository userRepository) {
            super(userRepository);
            this.dealerCards = new ArrayList<>();
            this.playerCards = new ArrayList<>();
            this.kartenstapel = new Stack<>();
            this.numberOfCards = 2;
            this.bet = 0;

            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            Collections.shuffle(kartenstapel);
            dealerCards.add(0, kartenstapel.pop());
            playerCards.add(0, kartenstapel.pop());
            dealerCards.add(1, kartenstapel.pop());
            playerCards.add(1, kartenstapel.pop());

            userScore = calculateScore(true);
        }

        private MessageEmbed hit(Boolean newCard) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Blackjack");
            builder.setDescription("Player: " + (member == null || member.getNickname() == null ? player.getName() : member.getNickname()) + "\nStakes: " + bet + " \uD83D\uDC1F");
            builder.addField("Cards of dealer:", "secret card\n" + dealerCards.get(1).getName()
                                                 + "\n\nDealer score: " + dealerCards.get(1).getValue(), false);
            builder.addField("Your cards:", getCards(playerCards, newCard)
                                            + "\nYour score: " + calculateScore(true), false);
            builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
            userScore = calculateScore(true);
            if (newCard) {
                numberOfCards++;
            }
            if (userScore > 21) {
                updateAccount(2);
                builder.addField("Result", "Bust! You lost " + bet + " \uD83D\uDC1F", false);
            }
            return builder.build();
        }

        private MessageEmbed stand() {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Blackjack");
            builder.setDescription("Player: " + (member == null || member.getNickname() == null ? player.getName() : member.getNickname()) + "\nStakes: " + bet + " \uD83D\uDC1F");
            dealerScore = calculateScore(false);
            while (dealerScore < 17) {
                getCards(dealerCards, true);
                dealerScore = calculateScore(false);
            }
            builder.addField("Cards of dealer:", getCards(dealerCards, false)
                                                 + "\nDealer score: " + dealerScore, false);
            builder.addField("Your cards:", getCards(playerCards, false)
                                            + "\nYour score: " + userScore, false);
            builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
            if (dealerScore > 21 || dealerScore < userScore) {
                updateAccount(1);
                builder.addField("Result", "Winner! You won " + bet + " \uD83D\uDC1F", false);
            } else if (dealerScore == userScore) {
                updateAccount(0);
                builder.addField("Result", "Stand off! You regain " + bet + " \uD83D\uDC1F", false);
            } else {
                updateAccount(2);
                builder.addField("Result", "You lost " + bet + " \uD83D\uDC1F", false);
            }
            return builder.build();
        }

        private String getCards(ArrayList<Cards> list, boolean nextCard) {
            StringBuilder sb = new StringBuilder();
            sb.append(list.get(0).getName()).append("\n").append(list.get(1).getName()).append("\n");
            int i = 2;
            while (i < list.size()) {
                sb.append(list.get(i).getName()).append("\n");
                i++;
            }
            if (nextCard) {
                Cards card = kartenstapel.pop();
                list.add(i, card);
                sb.append(card.getName());
            }
            return sb.toString();
        }

        private int calculateScore(boolean player) {
            int sum, numberOfAces;
            if (player) {
                sum = playerCards.stream().map(Cards::getValue).mapToInt(x -> x).sum();
                numberOfAces = (int) playerCards.stream().map(Cards::getValue).filter(x -> x == 11).count();
            } else {
                sum = dealerCards.stream().map(Cards::getValue).mapToInt(x -> x).sum();
                numberOfAces = (int) dealerCards.stream().map(Cards::getValue).filter(x -> x == 11).count();
            }
            while (numberOfAces > 0) {
                if (sum > 21) {
                    sum -= 10;
                    numberOfAces--;
                } else {
                    break;
                }
            }
            return sum;
        }

        private void updateAccount(int result) {
            UserEntity user = UserEntity.getUserByIdLong(null, player, super.userRepository);
            switch (result) {
                //stand off
                case 0 -> user.addFish(bet);
                //win
                case 1 -> {
                    user.addFish(bet * 2L);
                    user.addMateability(1);
                    if (numberOfCards == 2 && userScore == 21) {
                        user.addFish(bet / 2);
                        user.addMateability(1);
                    }
                }
                //loose
                case 2 -> user.subMateability(1);
            }
            super.userRepository.saveAndFlush(user);
        }
    }

}
