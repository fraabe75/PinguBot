package com.example.demo.database.entities;

import com.example.demo.database.repositories.UserRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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

    private String rank;

    public UserEntity(long userId, @NotNull String userName) {
        this.userId = userId;
        this.userName = userName.toLowerCase();
        this.fish = 50;
        this.mateability = 0;
        this.rank = "fairy";
    }

    public UserEntity() {

    }

    public static UserEntity getUserByIdLong(Member member, User user, UserRepository userRep) {
        UserEntity userEntity;
        long id = (member == null ? user.getIdLong() : member.getIdLong());
        String memberName = (member == null ?
                user.getName() :
                (member.getNickname() == null ? member.getEffectiveName() : member.getNickname())
        );

        if (!userRep.existsById(id)) {
            userEntity = new UserEntity(id, memberName);
        } else {
            userEntity = userRep.getOne(id);
            if (!userEntity.getUserName().equals(memberName)
            ) {
                userEntity.setUserName(memberName);
            }
        }
        userRep.saveAndFlush(userEntity);
        return userEntity;
    }

    public Long getUserId() {
        return userId;
    }

    public @NotNull String getUserName() {
        return userName;
    }

    public void setUserName(@NotNull String userName) {
        this.userName = userName;
    }

    public long getFish() {
        return fish;
    }

    public synchronized void addFish(long toAdd) {
        this.fish += toAdd;
    }

    public synchronized void subFish(long toSub) {
        this.fish = (this.fish - toSub) < 0 ? 0 : this.fish - toSub;
    }

    public long getMateability() {
        return mateability;
    }

    public synchronized void addMateability(long toAdd) {
        this.mateability += toAdd;
    }

    public synchronized void subMateability(long toSub) {
        this.mateability = (this.mateability - toSub) < 0 ? 0 : this.mateability - toSub;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }
}
