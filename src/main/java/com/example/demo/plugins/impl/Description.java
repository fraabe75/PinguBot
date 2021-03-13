package com.example.demo.plugins.impl;

import com.example.demo.plugins.GuildMessageReceivedPlugin;
import com.example.demo.database.entities.DescriptionEntity;
import com.example.demo.database.repositories.DescriptionRepository;
import com.example.demo.plugins.Plugin;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Description extends Plugin implements GuildMessageReceivedPlugin {

    private final DescriptionRepository descriptionRepository;

    public Description(DescriptionRepository descriptionRepository) {
        setName("Description");

        addCommands("set", "get");

        this.descriptionRepository = descriptionRepository;
    }

    @Override
    public boolean guildMessageReceived(GuildMessageReceivedEvent event, String command, String param, String prefix) {

        if (!commands().contains(command))
            return false;

        TextChannel channel = event.getChannel();

        switch (command) {
            case "set" -> {
                if (param.isEmpty()) {
                    channel.sendMessage("Please provide a description").queue();
                    return true;
                }

                descriptionRepository.save(new DescriptionEntity(event.getAuthor().getIdLong(), param));
            }
            case "get" -> {
                DescriptionEntity descriptionEntity = descriptionRepository
                        .findById(event.getAuthor().getIdLong())
                        .orElse(null);

                if (descriptionEntity == null) {
                    channel.sendMessage("You haven't set a description yet").queue();
                    return true;
                }

                channel.sendMessage(descriptionEntity.getDescription()).queue();
            }
        }

        return true;
    }
}
