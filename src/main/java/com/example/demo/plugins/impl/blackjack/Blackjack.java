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
        builder.setDescription("They see me rollin'");
        builder.addField(
                "How to play:",
                """
                Lorem Ipsum
                """,
                false
        );
        return builder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        User user = event.getAuthor();

        if (param.equals("play")) {

            if(!games.containsKey(user)) {
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
            return true;
        }

        if(param.equals("bet")) {
            channel.sendMessage("Einsatz hier").queue();
            return true;
        }

        return false;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {

        if (event.getUser().isBot() || !games.get(event.getUser()).messageId.equals(messageId)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        Game game = games.get(event.getUser());

        if (event.getReactionEmote().getEmoji().equals("\u261D")) {
            channel.editMessageById(game.messageId, game.hit(true)).queue(message -> message.removeReaction("\u261D", game.player).queue());
            return true;
        }

        if (event.getReactionEmote().getEmoji().equals("\u270B")) {
            channel.editMessageById(game.messageId, game.stand()).queue(message -> message.removeReaction("\u270B", game.player).queue());
            return true;
        }

        return false;
    }

    static class Game extends Blackjack {

        private User player;
        private String messageId;
        private int numberOfCards;

        private final ArrayList<Cards> dealerCards;
        private final ArrayList<Cards> playerCards;
        private final Stack<Cards> kartenstapel;

        public Game() {
            this.numberOfCards = 2;
            this.dealerCards = new ArrayList<>();
            this.playerCards = new ArrayList<>();
            this.kartenstapel = new Stack<>();

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
            builder.addField("Cards of dealer:", "secret card\n" + dealerCards.get(1).getName(), false);
            builder.addField("Your cards:", getCards(playerCards, newCard), false);
            builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
            numberOfCards++;
            return builder.build();
        }

        private MessageEmbed stand() {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Blackjack");
            builder.addField("Cards of dealer:", getCards(dealerCards, true), false);
            builder.addField("Your cards:", getCards(playerCards, false), false);
            builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
            numberOfCards++;
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
            if(nextCard) {
                Cards card = kartenstapel.pop();
                list.add(i, card);
                sb.append(card.getName());
            }
            return sb.toString();
        }
    }

}
