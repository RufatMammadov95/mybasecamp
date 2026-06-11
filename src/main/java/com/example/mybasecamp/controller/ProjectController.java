package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.*;
import com.example.mybasecamp.repository.*;
import com.example.mybasecamp.service.ProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
public class ProjectController {

	private final ProjectService projectService;
	private final UserRepository userRepository;
	private final DiscussionRepository discussionRepository;
	private final AttachmentRepository attachmentRepository;

	public ProjectController(ProjectService projectService, UserRepository userRepository,
			DiscussionRepository discussionRepository, AttachmentRepository attachmentRepository) {
		this.projectService = projectService;
		this.userRepository = userRepository;
		this.discussionRepository = discussionRepository;
		this.attachmentRepository = attachmentRepository;
	}

	@GetMapping("/projects")
	public String showProjectsPage(@RequestParam(value = "filter", required = false) String filter,
			@AuthenticationPrincipal UserDetails userDetails, Model model) {

		String currentUserEmail = userDetails.getUsername();
		List<Project> allProjects = projectService.getAllProjects();
		List<Project> filteredProjects = new ArrayList<>();

		if ("created".equals(filter)) {
			for (Project p : allProjects) {
				if (p.getOwner() != null && currentUserEmail.equalsIgnoreCase(p.getOwner().getEmail())) {
					filteredProjects.add(p);
				}
			}
			model.addAttribute("currentFilter", "created");
		} else if ("shared".equals(filter)) {
			for (Project p : allProjects) {
				boolean isMember = false;
				if (p.getMembers() != null) {
					for (User member : p.getMembers()) {
						if (currentUserEmail.equalsIgnoreCase(member.getEmail())) {
							isMember = true;
							break;
						}
					}
				}
				if (isMember) {
					filteredProjects.add(p);
				}
			}
			model.addAttribute("currentFilter", "shared");
		} else {
			for (Project p : allProjects) {
				boolean isMember = false;
				if (p.getMembers() != null) {
					for (User member : p.getMembers()) {
						if (currentUserEmail.equalsIgnoreCase(member.getEmail())) {
							isMember = true;
							break;
						}
					}
				}
				boolean isOwner = p.getOwner() != null && currentUserEmail.equalsIgnoreCase(p.getOwner().getEmail());

				if (isOwner || isMember) {
					filteredProjects.add(p);
				}
			}
			model.addAttribute("currentFilter", "all");
		}

		model.addAttribute("projects", filteredProjects);
		model.addAttribute("newProjectObj", new Project());
		return "projects";
	}

	@PostMapping("/projects/new")
	public String createProject(@ModelAttribute("newProjectObj") Project project,
			@AuthenticationPrincipal UserDetails userDetails) {
		User owner = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
		project.setOwner(owner);
		projectService.createProject(project);
		return "redirect:/projects";
	}

	@GetMapping("/projects/view/{id}")
	public String viewProject(@PathVariable("id") Long id,
			@RequestParam(value = "discussionId", required = false) Long discussionId, Model model) {
		Project project = projectService.getProjectById(id);
		model.addAttribute("project", project);

		model.addAttribute("description", project.getDescription());

		List<Discussion> discussions = discussionRepository.findByProjectIdOrderByCreatedAtAsc(id);
		model.addAttribute("discussions", discussions);

		Discussion activeDiscussion = null;
		if (discussionId != null) {
			activeDiscussion = discussionRepository.findById(discussionId).orElse(null);
		} else if (!discussions.isEmpty()) {
			activeDiscussion = discussions.get(0);
		}
		model.addAttribute("activeDiscussion", activeDiscussion);

		if (!model.containsAttribute("activePanel")) {
			if (discussionId != null) {
				model.addAttribute("activePanel", "topics");
			} else {
				model.addAttribute("activePanel", "overview");
			}
		}

		List<Attachment> rawAttachments = attachmentRepository.findByProjectId(id);
		List<Map<String, Object>> processedAttachments = new ArrayList<>();
		boolean hasImages = false;

		for (Attachment att : rawAttachments) {
			Map<String, Object> fileMap = new HashMap<>();
			fileMap.put("id", att.getId());
			fileMap.put("fileName", att.getFileName());

			if (att.getData() != null) {
				String base64Image = Base64.getEncoder().encodeToString(att.getData());
				fileMap.put("base64Data", base64Image);

				String contentType = resolveContentType(att.getContentType(), att.getFileName());
				fileMap.put("contentType", contentType);
				boolean isImage = contentType.startsWith("image/");
				fileMap.put("isImage", isImage);
				hasImages = hasImages || isImage;
			} else {
				fileMap.put("isImage", false);
			}

			processedAttachments.add(fileMap);
		}

		model.addAttribute("attachments", processedAttachments);
		model.addAttribute("hasImages", hasImages);
		return "project-detail";
	}

	@PostMapping("/projects/edit-name/{id}")
	public String updateName(@PathVariable("id") Long id, @RequestParam("name") String name,
			RedirectAttributes redirectAttributes) {
		Project project = projectService.getProjectById(id);
		project.setName(name);
		projectService.createProject(project);

		redirectAttributes.addFlashAttribute("activePanel", "overview");
		return "redirect:/projects/view/" + id;
	}

	@PostMapping("/projects/edit-desc/{id}")
	public String updateDescription(@PathVariable("id") Long id, @RequestParam("description") String desc,
			RedirectAttributes redirectAttributes) {
		Project project = projectService.getProjectById(id);
		project.setDescription(desc);
		projectService.createProject(project);

		redirectAttributes.addFlashAttribute("activePanel", "overview");
		return "redirect:/projects/view/" + id;
	}

	@PostMapping("/projects/delete/{id}")
	public String deleteProject(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		try {
			Project project = projectService.getProjectById(id);
			User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			if (project.getOwner() == null || !project.getOwner().getId().equals(currentUser.getId())) {
				redirectAttributes.addFlashAttribute("error",
						"You do not have permission to delete this project! Only the owner can delete it.");
				return "redirect:/projects/view/" + id;
			}

			projectService.deleteProject(id);
			redirectAttributes.addFlashAttribute("success", "Project deleted successfully.");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error",
					"An error occurred while deleting the project: " + e.getMessage());
			return "redirect:/projects";
		}

		return "redirect:/projects";
	}

	private String resolveContentType(String contentType, String fileName) {
		if (contentType != null && !contentType.isBlank()
				&& !"application/octet-stream".equalsIgnoreCase(contentType)) {
			return contentType;
		}
		if (fileName == null)
			return "application/octet-stream";
		String lowerName = fileName.toLowerCase();
		if (lowerName.endsWith(".png"))
			return "image/png";
		if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg"))
			return "image/jpeg";
		if (lowerName.endsWith(".gif"))
			return "image/gif";
		if (lowerName.endsWith(".webp"))
			return "image/webp";
		if (lowerName.endsWith(".svg"))
			return "image/svg+xml";
		if (lowerName.endsWith(".pdf"))
			return "application/pdf";
		return "application/octet-stream";
	}
}