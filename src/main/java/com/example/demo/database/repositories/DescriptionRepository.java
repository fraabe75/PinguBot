package com.example.demo.database.repositories;

import com.example.demo.database.entities.DescriptionEntity;
import com.example.javatemplatespringboot.database.entities.DescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescriptionRepository extends JpaRepository<DescriptionEntity, Long> {
}
