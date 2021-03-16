package com.example.demo.plugins.impl;

import com.example.demo.database.entities.UserEntity;
import com.example.demo.database.repositories.UserRepository;
import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Fish extends Plugin implements GuildMessageReceivedPlugin {

    private final UserRepository userRepository;

    public Fish(UserRepository userRepository) {
        setName("Fish");
        setDescription("Go fishing!");
        addCommands("fish");
        this.userRepository = userRepository;
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        TextChannel channel = event.getChannel();
        int numberOfFish = (int) (Math.random() * 10) + 1;
        String id = event.getMember().getId();
        UserEntity user = userRepository.findById(Long.parseLong(id)).get();
        user.addFish(numberOfFish);
        userRepository.saveAndFlush(user);
        channel.sendMessage(numberOfFish + " fish added to your repository!").queue();

        return true;
    }


}
