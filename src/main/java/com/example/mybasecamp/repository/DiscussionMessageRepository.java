package com.example.mybasecamp.repository;

import com.example.mybasecamp.model.DiscussionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionMessageRepository extends JpaRepository<DiscussionMessage, Long> {
}