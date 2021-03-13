package com.example.demo.plugins;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public abstract class Plugin {

    private String name;

    private final List<String> commands;
    private final List<String> allowedRoles;
    private final List<Permission> allowedPermissions;

    public Plugin() {
        this.commands = new ArrayList<>();
        this.allowedRoles = new ArrayList<>();
        this.allowedPermissions = new ArrayList<>();
    }

    public List<String> commands() {
        return commands;
    }

    protected void addCommands(String... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    protected void addAllowedRoles(String... allowedRoles) {
        this.allowedRoles.addAll(Arrays.asList(allowedRoles));
    }

    protected void addAllowedPermissions(Permission... permissions) {
        this.allowedPermissions.addAll(Arrays.asList(permissions));
    }

    protected boolean notAllowed(Member member) {

        boolean allowed = true;

        if (!allowedRoles.isEmpty()) {
            allowed = !Collections.disjoint(member.getRoles().stream().map(Role::getName).collect(Collectors.toList()), allowedRoles);
        }

        if (!allowedPermissions.isEmpty()) {
            allowed = allowed || !Collections.disjoint(member.getPermissions(), allowedPermissions);
        }

        return !allowed;
    }

    public MessageEmbed help(String prefix) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Helper page for " + name);
        StringBuilder sb = new StringBuilder();
        for (String command : commands) {
            sb.append(prefix).append(command).append("\n");
        }
        builder.setDescription(sb.toString());
        return builder.build();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
