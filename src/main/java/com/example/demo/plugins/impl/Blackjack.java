package com.example.demo.plugins.impl;

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
import java.util.List;

@Service
public class Blackjack extends Plugin implements GuildMessageReceivedPlugin, GuildMessageReactionAddPlugin {

    private String myMessageId;
    private int numberOfCards = 2;
    private User player;

    private String[] dealerCards = new String[12];
    private String[] playerCards = new String[12];
    private Stack<String> kartenstapel = new Stack<>();

    public Blackjack() {
        setName("Blackjack");
        setDescription("Play a fun game of Blackjack!");
        addCommands("bj", "blackjack");
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command)) {
            return false;
        }

        TextChannel channel = event.getChannel();
        player = event.getAuthor();

        List<String> cards = Arrays.asList("2clubs", "2spades", "2hearts", "2diamonds",
                "3clubs", "3spades", "3hearts", "3diamonds", "4clubs", "4spades", "4hearts", "4diamonds",
                "5clubs", "5spades", "5hearts", "5diamonds", "6clubs", "6spades", "6hearts", "6diamonds",
                "7clubs", "7spades", "7hearts", "7diamonds", "8clubs", "8spades", "8hearts", "8diamonds",
                "9clubs", "9spades", "9hearts", "9diamonds", "10clubs", "10spades", "10hearts", "10diamonds",
                "jackclubs", "jackspades", "jackhearts", "jackdiamonds",
                "queenclubs", "queenspades", "queenhearts", "queendiamonds",
                "kingclubs", "kingspades", "kinghearts", "kingdiamonds",
                "aceclubs", "acespades", "acehearts", "acediamonds");

        kartenstapel.addAll(cards);
        Collections.shuffle(kartenstapel);

        dealerCards[0] = kartenstapel.pop();
        playerCards[0] = kartenstapel.pop();
        dealerCards[1] = kartenstapel.pop();
        playerCards[1] = kartenstapel.pop();

        channel.sendMessage(sendGame()).queue(message -> {
            message.addReaction("\u261D").queue();
            message.addReaction("\u270B").queue();
            myMessageId = message.getId();
        });

        System.out.println(dealerCards[0]);

        return true;
    }

    @Override
    public boolean guildMessageReactionAdd(GuildMessageReactionAddEvent event, String messageId) {

        if (event.getUser().isBot() || !event.getUser().equals(player)) {
            return false;
        }

        TextChannel channel = event.getChannel();

        if (event.getReactionEmote().getEmoji().equals("\u261D")) {
            channel.editMessageById(myMessageId, sendGame())
                   .queue(message -> message.removeReaction("\u261D", player).queue());
        }

        if (event.getReactionEmote().getEmoji().equals("\u270B")) {
            //TODO: Dealer ist dran
            channel.sendMessage("Dealer ist dran.").queue();

        }

        return true;
    }

    private MessageEmbed sendGame() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Blackjack");
        builder.addField("Cards of dealer:", "secret card\n" + dealerCards[1], false);
        StringBuilder sb = new StringBuilder();
        sb.append(playerCards[0]).append("\n").append(playerCards[1]).append("\n");
        int i = 2;
        while (playerCards[i] != null) {
            sb.append(playerCards[i]).append("\n");
            i++;
        }
        playerCards[i] = kartenstapel.pop();
        builder.addField("Your cards:", sb.toString(), false);
        builder.setFooter("\u261D" + ": hit, " + "\u270B" + ": stand");
        numberOfCards++;
        return builder.build();
    }

}
