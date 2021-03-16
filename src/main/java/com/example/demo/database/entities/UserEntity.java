package com.example.demo.database.entities;

import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private Long userId;

    @NotNull
    private String userName;

    private long fish;
    private long mateability;

    public UserEntity(long userId, @NotNull String userName) {
        this.userId = userId;
        this.userName = userName.toLowerCase();
        this.fish = 0;
        this.mateability = 0;
    }

    public UserEntity() {
    }

    public Long getUserId() {
        return userId;
    }

    public @NotNull String getUserName() {
        return userName;
    }

    public long getFish() {
        return fish;
    }

    public synchronized void addFish(long toAdd) {
        this.fish += toAdd;
    }

    public synchronized void subFish(long toSub) {
        this.fish = this.fish - toSub <= 0 ? 0 : this.fish - toSub;
    }

    public long getMateability() {
        return mateability;
    }

    public synchronized void addMateability(long toAdd) {
        this.mateability += toAdd;
    }

    public synchronized void subMateability(long toSub) {
        if (this.mateability - toSub >= 0) {
            this.mateability -= toSub;
        }
    }
}
