package com.example.demo.plugins.impl.baccarat;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReactionAddPlugin;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import com.example.demo.plugins.impl.blackjack.Cards;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.springframework.stereotype.Service;

import java.nio.channels.Channel;
import java.util.*;

@Service
public class Baccarat extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;

    public Baccarat(UserRepository userRepository) {
        setName("Baccarat");
        setDescription("The best game for stupid people.");
        addCommands("bc", "baccarat", "bac");
        this.userRepository = userRepository;
    }

    public static MessageEmbed help() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Baccarat help");
        embedBuilder.setDescription("""
                Baccarat ist the perfect game
                for when you are walking into a casino
                and want to look like Daniel Craig,
                but are already 7 Cosmopolitans deep into the evening.""");
        embedBuilder.addField("baccarat <bet> <betAmount>", "start a new Baccarat game, betting on the selected outcome\nand with the selected amount.", false);
        embedBuilder.addField("(player|bank|tie)", "possible options for the <bet>.\n" +
                "Bets on either the player/bank winning or a tie occurring.", false);
        embedBuilder.setFooter("""
                shortcuts:
                baccarat -> bac, bc
                player -> p
                bank -> b
                tie -> t""");
        return embedBuilder.build();
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {
        TextChannel channel = event.getChannel();
        User user = event.getAuthor();
        UserEntity userEntity = UserEntity.getUserByIdLong(null, user, userRepository);
        Member member = event.getMember();
        PlayerBet bet;

        //create shuffled Stack of 6 decks
        Stack<Cards> stack = new Stack<>();
        for (int i=0;i<6;i++) stack.addAll(Arrays.asList(Cards.values()));
        Collections.shuffle(stack);

        try {
            bet = switch (param.trim().split(" ")[0]) {
                case "player","p" -> PlayerBet.Player;
                case "bank", "b" -> PlayerBet.Bank;
                case "tie", "t" -> PlayerBet.Tie;
                case "rules" -> printRules(channel);
                default -> null;
            };
            if(bet == null) {
                channel.sendMessage(help()).queue();
                return true;
            }

            ArrayList<Cards> pHand = new ArrayList<>();
            ArrayList<Cards> bHand = new ArrayList<>();
            int pValue;
            int bValue;
            boolean natural;
            PlayerBet winner;

            long betAmount = Long.parseLong(param.trim().split(" ")[1]);

            if (userEntity.getFish() < betAmount || betAmount < 0) {
                channel.sendMessage("Don't bet money you don't have!").queue();
                throw new Exception();
            }
            userEntity.subFish(betAmount);

            //draw cards and evaluate hands
            pHand.add(stack.pop());
            pHand.add(stack.pop());
            bHand.add(stack.pop());
            bHand.add(stack.pop());

            pValue = calcValue(pHand);
            bValue = calcValue(bHand);

            natural = pValue >= 8 || bValue >= 8;

            //bank third card
            if(!natural) {
                //player third card
                if(pValue <= 5) pHand.add(stack.pop());
                pValue = calcValue(pHand);

                //bank third card
                if(bValue <= 2) bHand.add(stack.pop());
                else if(bValue <= 6) {
                    if(bValue > pValue && bet == PlayerBet.Bank) bHand.add(stack.pop());
                    else if(bValue == pValue && bet == PlayerBet.Tie) bHand.add(stack.pop());
                    else if(bValue < pValue && bet == PlayerBet.Player) bHand.add(stack.pop());
                    bValue = calcValue(bHand);
                }
            }

            //calculate winner
            winner = (pValue == bValue) ? PlayerBet.Tie : (pValue < bValue) ? PlayerBet.Bank : PlayerBet.Player;
            //Assign "winnings" to betAmount
            betAmount = (winner == bet) ? (long) (betAmount * bet.getRATE()) : 0;
            userEntity.addFish(betAmount);
            userRepository.saveAndFlush(userEntity);

            //print result
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Baccarat result");
            embedBuilder.setDescription(betAmount > 0 ? "You won!" : "You lost.");
            embedBuilder.addField("Player Cards", formatHand(pHand), false);
            embedBuilder.addField("Bank Cards", formatHand(bHand), false);
            embedBuilder.addField("Scores", formatScores(pValue, bValue), false);
            embedBuilder.addField("Bets", formatBets(bet, winner), false);
            channel.sendMessage(embedBuilder.build()).queue();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessage("Congratulations, you are officially too stupid to play the simplest casino game.\nMay I direct you to the exit?").queue();
            return true;
        }


    }

    private int calcValue(ArrayList<Cards> hand) {
        return hand.stream().mapToInt(c ->
                (c.getName().equals("10 of Clubs") || c.getName().equals("10 of Hearts") || c.getName().equals("10 of Spades") || c.getName().equals("10 of Diamonds")) ? 10 : c.getValue()%10
        ).sum();
    }

    private String formatHand(ArrayList<Cards> hand) {
        StringBuilder stringBuilder = new StringBuilder();
        for(Cards card : hand) {
            stringBuilder.append(card.getName());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private String formatScores(long pScore, long bScore) {
        return "Player Score: " +
                pScore +
                "\n" +
                "Bank Score: " +
                bScore +
                "\n";
    }

    private String formatBets(PlayerBet player, PlayerBet winner) {
        return "Your bet: " +
                player.getTYPE() +
                "\n" +
                "Winning bet: " +
                winner.getTYPE() +
                "\n";
    }

    private PlayerBet printRules(TextChannel channel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Baccarat Rules");
        embedBuilder.addField("Rules", "https://www.onlinecasinoselite.org/getting-started/gambling-rules/baccarat", false);
        embedBuilder.addField("Payout Rates", """
               Player Bet = 2:1
               Bank Bet = 1,95:1
               Tie Bet = 8:1
               """, false);
        channel.sendMessage(embedBuilder.build()).queue();
        return null;
    }
}
