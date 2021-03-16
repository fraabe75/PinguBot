package com.example.demo.plugins.impl.blackjack;

import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
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

    public Blackjack() {
        setName("Blackjack");
        setDescription("Play a fun game of Blackjack!");
        addCommands("b", "bj", "blackjack");
    }

    public static MessageEmbed help() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Blackjack Help");
        builder.setDescription("Las Vegas is the only place I know\nwhere money really talks. It says goodbye.\n~ Frank Sinatra");
        builder.addField("blackjack play", "start a new game", false);
        builder.addField("blackjack place <value>", "place your bet", false);
        builder.addField("blackjack end", "end the current game", false);
        builder.setFooter("Shortcuts: 'bj', 'b'");
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        User user = event.getAuthor();

        switch (param.trim().split(" ")[0]) {
            case "start", "play", "new", "game" -> {
                if (!games.containsKey(user)) {
                    Game game = new Game();
                    games.put(event.getAuthor(), game);
                    channel.sendMessage(game.hit(false)).queue(message -> {
                        message.addReaction("\u261D").queue();
                        message.addReaction("\u270B").queue();
                        game.messageId = message.getId();
                    });
                    game.player = user;
                    System.out.println(game.dealerCards.get(0));
                } else {
                    channel.sendMessage("You are already playing!").queue();
                }
            }
            case "place", "bet", "set"  -> {
                try {
                    games.get(user).bet = Integer.parseInt(param.trim().split(" ")[1]);
                } catch (Exception e) {
                    channel.sendMessage("Invalid value!").queue();
                }



            }
            case "end", "terminate" -> {
                channel.sendMessage("Terminated your current game.").queue();
            }
            default -> channel.sendMessage(help()).queue();
        }
        return true;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {

        if (event.getUser().isBot() || !games.containsKey(event.getUser()) || !games.get(event.getUser()).messageId.equals(messageId)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        Game game = games.get(event.getUser());

        //hit
        if (event.getReactionEmote().getEmoji().equals("\u261D")) {
            channel.editMessageById(game.messageId, game.hit(true)).queue(message -> message.removeReaction("\u261D", game.player).queue());
            if (game.userScore > 21) {
                channel.sendMessage("Bust! Du hast leider verloren!").queue();
            } else if(game.userScore == 21) {
                channel.sendMessage("Blackjack! Du hast gewonnen!").queue();
            } else {
                return true;
            }
            channel.removeReactionById(game.messageId, "\u261D").queue();
            channel.removeReactionById(game.messageId, "\u270B").queue();
            games.remove(event.getUser());
            return true;
        }

        //stand
        if (event.getReactionEmote().getEmoji().equals("\u270B")) {
            channel.editMessageById(game.messageId, game.stand()).queue(message -> message.removeReaction("\u270B", game.player).queue());
            if (game.dealerScore > 21 || game.dealerScore <= game.userScore) {
                channel.sendMessage("Gewonnen! Du hast den Dealer geschlagen!").queue();
            } else {
                channel.sendMessage("Du hast leider verloren!").queue();
            }
            channel.removeReactionById(game.messageId, "\u261D").queue();
            channel.removeReactionById(game.messageId, "\u270B").queue();
            games.remove(event.getUser());
            return true;
        }

        return false;
    }

    static class Game extends Blackjack {

        private User player;
        private String messageId;
        private int userScore;
        private int dealerScore;

        private int bet;

        private final ArrayList<Cards> dealerCards;
        private final ArrayList<Cards> playerCards;
        private final Stack<Cards> kartenstapel;

        public Game() {
            this.dealerCards = new ArrayList<>();
            this.playerCards = new ArrayList<>();
            this.kartenstapel = new Stack<>();

            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            kartenstapel.addAll(Arrays.asList(Cards.values()));
            Collections.shuffle(kartenstapel);
            dealerCards.add(0, kartenstapel.pop());
            playerCards.add(0, kartenstapel.pop());
            dealerCards.add(1, kartenstapel.pop());
            playerCards.add(1, kartenstapel.pop());
        }

        private MessageEmbed hit(Boolean newCard) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Blackjack");
            builder.addField("Cards of dealer:", "secret card\n" + dealerCards.get(1).getName()
                    + "\nDealer score: " + dealerCards.get(1).getValue(), false);
            builder.addField("Your cards:", getCards(playerCards, newCard)
                    + "\nYour score: " + calculateScore(true), false);
            builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
            userScore = calculateScore(true);
            return builder.build();
        }

        private MessageEmbed stand() {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Blackjack");
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
            return builder.build();
        }

        private String getCards(ArrayList<Cards> list, Boolean nextCard) {
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
    }

}
