package de.penguins.pingubot.plugins;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public abstract class Plugin {

    private String name;
    private String description;

    private final List<String> commands;

    public Plugin() {
        this.commands = new ArrayList<>();
        this.description = "";
    }

    public List<String> commands() {
        return commands;
    }

    protected void addCommands(String... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }
}
