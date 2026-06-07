package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.*;
import com.example.mybasecamp.repository.*;
import com.example.mybasecamp.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

@Controller
public class ProjectController {

	private final ProjectService projectService;
	private final UserRepository userRepository;
	private final DiscussionRepository discussionRepository;
	private final AttachmentRepository attachmentRepository;
	private final DiscussionMessageRepository discussionMessageRepository;

	public ProjectController(ProjectService projectService, UserRepository userRepository,
			DiscussionRepository discussionRepository, AttachmentRepository attachmentRepository,
			DiscussionMessageRepository discussionMessageRepository) {
		this.projectService = projectService;
		this.userRepository = userRepository;
		this.discussionRepository = discussionRepository;
		this.attachmentRepository = attachmentRepository;
		this.discussionMessageRepository = discussionMessageRepository;
	}

	@GetMapping("/projects/view/{id}")
	public String viewProject(@PathVariable("id") Long id,
			@RequestParam(value = "discussionId", required = false) Long discussionId, Model model) {
		Project project = projectService.getProjectById(id);
		model.addAttribute("project", project);

		List<Discussion> discussions = discussionRepository.findByProjectIdOrderByCreatedAtAsc(id);
		model.addAttribute("discussions", discussions);

		Discussion activeDiscussion = null;
		if (discussionId != null) {
			activeDiscussion = discussionRepository.findById(discussionId).orElse(null);
		} else if (!discussions.isEmpty()) {
			activeDiscussion = discussions.get(0);
		}
		model.addAttribute("activeDiscussion", activeDiscussion);
		if (discussionId != null) {
			model.addAttribute("activePanel", "topics");
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
	public String updateName(@PathVariable("id") Long id, @RequestParam("name") String name) {
		Project project = projectService.getProjectById(id);
		project.setName(name);
		projectService.createProject(project);
		return "redirect:/projects/view/" + id;
	}

	@PostMapping("/projects/edit-desc/{id}")
	public String updateDescription(@PathVariable("id") Long id, @RequestParam("description") String desc) {
		Project project = projectService.getProjectById(id);
		project.setDescription(desc);
		projectService.createProject(project);
		return "redirect:/projects/view/" + id;
	}

	@PostMapping("/projects/add-discussion/{id}")
	public String addDiscussion(@PathVariable("id") Long id, @RequestParam("title") String title,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		Project project = projectService.getProjectById(id);
		User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

		Discussion discussion = new Discussion();
		discussion.setTitle(title);
		discussion.setProject(project);
		discussion.setUser(user);

		discussionRepository.save(discussion);

		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + id + "?discussionId=" + discussion.getId();
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/add-message")
	public String addDiscussionMessage(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @RequestParam("content") String content,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {

		Discussion discussion = discussionRepository.findById(discussionId).orElseThrow();
		User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

		DiscussionMessage message = new DiscussionMessage();
		message.setContent(content);
		message.setDiscussion(discussion);
		message.setUser(user);

		discussionMessageRepository.save(message);

		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/delete")
	public String deleteDiscussion(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, RedirectAttributes redirectAttributes) {

		discussionRepository.deleteById(discussionId);
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/edit")
	public String editDiscussion(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @RequestParam("title") String newTitle,
			RedirectAttributes redirectAttributes) {

		Discussion discussion = discussionRepository.findById(discussionId).orElseThrow();
		discussion.setTitle(newTitle);
		discussionRepository.save(discussion);

		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}

	@PostMapping("/projects/upload/{id}")
	public String uploadFile(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) throws IOException {
		if (!file.isEmpty()) {
			Project project = projectService.getProjectById(id);
			Attachment attachment = new Attachment();
			String fileName = StringUtils
					.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
			attachment.setFileName(fileName);
			attachment.setContentType(resolveContentType(file.getContentType(), fileName));
			attachment.setData(file.getBytes());
			attachment.setProject(project);
			attachmentRepository.save(attachment);
		}

		redirectAttributes.addFlashAttribute("activePanel", "attachments");
		return "redirect:/projects/view/" + id;
	}

	@GetMapping("/projects/attachments/{attachmentId}/download")
	public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
		Attachment attachment = attachmentRepository.findById(attachmentId)
				.orElseThrow(() -> new RuntimeException("Attachment not found!"));
		String contentType = resolveContentType(attachment.getContentType(), attachment.getFileName());

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
				.body(attachment.getData());
	}

	private String resolveContentType(String contentType, String fileName) {
		if (contentType != null && !contentType.isBlank()
				&& !"application/octet-stream".equalsIgnoreCase(contentType)) {
			return contentType;
		}

		if (fileName == null) {
			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}

		String lowerName = fileName.toLowerCase();
		if (lowerName.endsWith(".png")) {
			return MediaType.IMAGE_PNG_VALUE;
		}
		if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG_VALUE;
		}
		if (lowerName.endsWith(".gif")) {
			return "image/gif";
		}
		if (lowerName.endsWith(".webp")) {
			return "image/webp";
		}
		if (lowerName.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (lowerName.endsWith(".pdf")) {
			return MediaType.APPLICATION_PDF_VALUE;
		}

		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	@PostMapping("/projects/{projectId}/attachments/{attachmentId}/delete")
	public String deleteAttachment(@PathVariable Long projectId, @PathVariable Long attachmentId,
			RedirectAttributes redirectAttributes) {
		try {
			attachmentRepository.deleteById(attachmentId);

			redirectAttributes.addFlashAttribute("success", "Fayl uğurla silindi.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Faylı silərkən xəta baş verdi.");
		}

		return "redirect:/projects/view/" + projectId + "?activePanel=attachments";
	}

	@PostMapping("/projects/add-member/{id}")
	public String addMember(@PathVariable("id") Long id, @RequestParam("email") String email,
			@RequestParam(value = "isAdmin", defaultValue = "false") boolean isAdmin,
			RedirectAttributes redirectAttributes) {

		redirectAttributes.addFlashAttribute("activePanel", "members");

		try {
			Project project = projectService.getProjectById(id);
			User member = userRepository.findByEmail(email)
					.orElseThrow(() -> new IllegalArgumentException("User not found"));

			if (project.getMembers().contains(member) || project.getOwner().equals(member)) {
				redirectAttributes.addFlashAttribute("error", "This user is already included in the project!");
				return "redirect:/projects/view/" + id;
			}

			if (isAdmin) {
				member.setRole(User.Role.ADMIN);
			} else {
				member.setRole(User.Role.USER);
			}
			userRepository.save(member);

			project.getMembers().add(member);
			projectService.createProject(project);

			String successMsg = isAdmin ? "Admin added successfully!" : "Member added successfully!";
			redirectAttributes.addFlashAttribute("success", successMsg);

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "No user matching the email you entered was found!");
		}

		return "redirect:/projects/view/" + id;
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

	@PostMapping("/projects/delete/{id}")
	public String deleteProject(@PathVariable("id") Long id) {
		projectService.deleteProject(id);
		return "redirect:/projects";
	}
}