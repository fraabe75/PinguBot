package com.example.demo.database.repositories;

import com.example.demo.database.entities.DescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface DescriptionRepository extends JpaRepository<DescriptionEntity, Long> {
}
