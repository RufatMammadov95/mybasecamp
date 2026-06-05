package com.example.mybasecamp.repository;
import com.example.mybasecamp.model.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    List<Discussion> findByProjectIdOrderByCreatedAtAsc(Long projectId);
}