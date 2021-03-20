package com.example.pingubot.database.repositories;

import com.example.pingubot.database.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u.userId FROM UserEntity u WHERE u.rank = 'dynamic' ORDER BY u.mateability DESC, u.fish DESC ")
    List<Long> findAllByRankDynamic();

}
