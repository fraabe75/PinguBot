package com.example.demo.mangos.impl;

import com.example.javatemplatespringboot.database.entities.DescriptionEntity;
import com.example.javatemplatespringboot.database.repositories.DescriptionRepository;
import com.example.javatemplatespringboot.mangos.GuildMessageReceivedMango;
import com.example.javatemplatespringboot.mangos.Mango;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.stereotype.Service;

@Service
public class Description extends Mango implements GuildMessageReceivedMango {

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
