package com.example.mybasecamp.controller;

import com.example.mybasecamp.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping("/projects")
	public String showProjectsPage(Model model) {
		model.addAttribute("projects", projectService.getAllProjects());
		return "projects";
	}
}