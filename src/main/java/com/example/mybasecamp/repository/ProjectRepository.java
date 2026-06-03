package com.example.mybasecamp.repository;

import com.example.mybasecamp.model.Project;
import com.example.mybasecamp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByOwner(User owner);
}