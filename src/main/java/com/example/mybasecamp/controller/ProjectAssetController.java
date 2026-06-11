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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class ProjectAssetController {

	private final ProjectService projectService;
	private final UserRepository userRepository;
	private final AttachmentRepository attachmentRepository;

	public ProjectAssetController(ProjectService projectService, UserRepository userRepository,
			AttachmentRepository attachmentRepository) {
		this.projectService = projectService;
		this.userRepository = userRepository;
		this.attachmentRepository = attachmentRepository;
	}

	@PostMapping("/projects/upload/{id}")
	public String uploadFile(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			Project project = projectService.getProjectById(id);
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isMember = project.getMembers() != null
					&& project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()));

			if (!isOwner && !isMember) {
				redirectAttributes.addFlashAttribute("error",
						"You must be a member of this project to upload attachments!");
				redirectAttributes.addFlashAttribute("activePanel", "attachments");
				return "redirect:/projects/view/" + id;
			}

			if (!file.isEmpty()) {
				Attachment attachment = new Attachment();
				String fileName = StringUtils
						.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
				attachment.setFileName(fileName);
				attachment.setContentType(resolveContentType(file.getContentType(), fileName));
				attachment.setData(file.getBytes());
				attachment.setProject(project);

				attachmentRepository.save(attachment);
				redirectAttributes.addFlashAttribute("success", "File uploaded successfully");
			} else {
				redirectAttributes.addFlashAttribute("error", "No file selected for upload!");
			}
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("error", "Internal error while saving file: " + e.getMessage());
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

	@PostMapping("/projects/{projectId}/attachments/{attachmentId}/delete")
	public String deleteAttachment(@PathVariable Long projectId, @PathVariable Long attachmentId,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			Project project = projectService.getProjectById(projectId);
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isMember = project.getMembers() != null
					&& project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()));

			if (!isOwner && !isMember) {
				redirectAttributes.addFlashAttribute("error",
						"You do not have permission to delete files from this project!");
				redirectAttributes.addFlashAttribute("activePanel", "attachments");
				return "redirect:/projects/view/" + projectId;
			}

			attachmentRepository.deleteById(attachmentId);
			redirectAttributes.addFlashAttribute("success", "File deleted successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "An error occurred while deleting the file");
		}
		redirectAttributes.addFlashAttribute("activePanel", "attachments");
		return "redirect:/projects/view/" + projectId;
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

	@PostMapping("/projects/{projectId}/members/{memberId}/remove")
	public String removeMember(@PathVariable("projectId") Long projectId, @PathVariable("memberId") Long memberId,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
			Project project = projectService.getProjectById(projectId);

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId());
			boolean isAdmin = project.getMembers() != null && project.getMembers().stream()
					.anyMatch(m -> m.getId().equals(currentUser.getId()) && m.getRole() == User.Role.ADMIN);

			if (!isOwner && !isAdmin) {
				redirectAttributes.addFlashAttribute("error", "Only administrators can remove members!");
				redirectAttributes.addFlashAttribute("activePanel", "members");
				return "redirect:/projects/view/" + projectId;
			}

			User memberToRemove = userRepository.findById(memberId).orElseThrow();
			project.getMembers().remove(memberToRemove);
			projectService.createProject(project);

			redirectAttributes.addFlashAttribute("success", "Member removed successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error removing member");
		}
		redirectAttributes.addFlashAttribute("activePanel", "members");
		return "redirect:/projects/view/" + projectId;
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
		if (lowerName.endsWith(".pdf"))
			return "application/pdf";
		return "application/octet-stream";
	}
}