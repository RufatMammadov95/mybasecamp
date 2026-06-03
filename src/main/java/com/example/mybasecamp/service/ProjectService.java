package com.example.mybasecamp.service;

import com.example.mybasecamp.model.Project;
import com.example.mybasecamp.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public Project createProject(Project project) {
		Objects.requireNonNull(project, "The project object to be created cannot be empty!");
		return projectRepository.save(project);
	}

	public Project getProjectById(Long id) {
		return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found!"));
	}

	public List<Project> getAllProjects() {
		return projectRepository.findAll();
	}

	public Project updateProject(Long id, Project updatedProjectDetails) {
		Objects.requireNonNull(updatedProjectDetails, "The project data to be updated cannot be empty!");

		Project existingProject = getProjectById(id);
		existingProject.setName(updatedProjectDetails.getName());
		existingProject.setDescription(updatedProjectDetails.getDescription());

		return projectRepository.save(existingProject);
	}

	public void deleteProject(Long id) {
		projectRepository.deleteById(id);
	}
}