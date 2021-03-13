package com.example.demo.database.entities;

import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "descriptions")
public class DescriptionEntity {

    @Id
    private Long userId;

    @NotNull
    private String description;

    public DescriptionEntity(long userId, @NotNull String description) {
        this.userId = userId;
        this.description = description;
    }

    public DescriptionEntity() {
    }

    public @NotNull String getDescription() {
        return description;
    }
}
