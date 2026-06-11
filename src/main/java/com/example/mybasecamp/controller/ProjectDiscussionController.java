package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.*;
import com.example.mybasecamp.repository.*;
import com.example.mybasecamp.service.ProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
public class ProjectDiscussionController {

	private final ProjectService projectService;
	private final UserRepository userRepository;
	private final DiscussionRepository discussionRepository;
	private final DiscussionMessageRepository discussionMessageRepository;

	public ProjectDiscussionController(ProjectService projectService, UserRepository userRepository,
			DiscussionRepository discussionRepository, DiscussionMessageRepository discussionMessageRepository) {
		this.projectService = projectService;
		this.userRepository = userRepository;
		this.discussionRepository = discussionRepository;
		this.discussionMessageRepository = discussionMessageRepository;
	}

	@PostMapping("/projects/add-discussion/{id}")
	public String addDiscussion(@PathVariable("id") Long id, @RequestParam("title") String title,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			if (title == null || title.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Topic title cannot be empty!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + id;
			}

			Project project = projectService.getProjectById(id);
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isAdminMember = false;
			if (project.getMembers() != null) {
				for (User member : project.getMembers()) {
					if (member.getId().equals(user.getId()) && member.getRole() == User.Role.ADMIN) {
						isAdminMember = true;
						break;
					}
				}
			}
			if (!isOwner && !isAdminMember) {
				redirectAttributes.addFlashAttribute("error",
						"Only project administrators can create a discussion thread!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + id;
			}

			Discussion discussion = new Discussion();
			discussion.setTitle(title.trim());
			discussion.setProject(project);
			discussion.setUser(user);

			if (project.getDiscussions() == null) {
				project.setDiscussions(new ArrayList<>());
			}
			project.getDiscussions().add(discussion);
			discussionRepository.save(discussion);

			redirectAttributes.addFlashAttribute("success", "Topic added successfully");
			redirectAttributes.addFlashAttribute("activePanel", "topics");
			return "redirect:/projects/view/" + id + "?discussionId=" + discussion.getId();

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error",
					"An error occurred while creating the topic: " + e.getMessage());
			return "redirect:/projects/view/" + id;
		}
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/add-message")
	public String addDiscussionMessage(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @RequestParam("content") String content,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			if (content == null || content.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Message content cannot be empty!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			Project project = projectService.getProjectById(projectId);
			Discussion discussion = discussionRepository.findById(discussionId).orElseThrow();
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isMember = project.getMembers() != null
					&& project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()));

			if (!isOwner && !isMember) {
				redirectAttributes.addFlashAttribute("error", "You must be a member of this project to post messages!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			DiscussionMessage message = new DiscussionMessage();
			message.setContent(content.trim());
			message.setDiscussion(discussion);
			message.setUser(user);

			discussionMessageRepository.save(message);
			redirectAttributes.addFlashAttribute("success", "Your message has been sent");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error sending message: " + e.getMessage());
		}
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/delete")
	public String deleteDiscussion(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		try {
			Project project = projectService.getProjectById(projectId);
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isAdminMember = project.getMembers() != null && project.getMembers().stream()
					.anyMatch(m -> m.getId().equals(user.getId()) && m.getRole() == User.Role.ADMIN);

			if (!isOwner && !isAdminMember) {
				redirectAttributes.addFlashAttribute("error",
						"Only project administrators can delete this discussion!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + projectId;
			}

			discussionRepository.deleteById(discussionId);
			redirectAttributes.addFlashAttribute("success", "Discussion deleted successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error deleting discussion: " + e.getMessage());
		}
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/edit")
	public String editDiscussion(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @RequestParam("title") String newTitle,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			if (newTitle == null || newTitle.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Discussion title cannot be empty!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			Project project = projectService.getProjectById(projectId);
			User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

			boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());
			boolean isAdminMember = project.getMembers() != null && project.getMembers().stream()
					.anyMatch(m -> m.getId().equals(user.getId()) && m.getRole() == User.Role.ADMIN);

			if (!isOwner && !isAdminMember) {
				redirectAttributes.addFlashAttribute("error", "Only project administrators can edit this discussion!");
				redirectAttributes.addFlashAttribute("activePanel", "topics");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			Discussion discussion = discussionRepository.findById(discussionId).orElseThrow();
			discussion.setTitle(newTitle.trim());
			discussionRepository.save(discussion);
			redirectAttributes.addFlashAttribute("success", "Discussion updated successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error updating discussion: " + e.getMessage());
		}
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/messages/{messageId}/edit")
	public String editMessage(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @PathVariable("messageId") Long messageId,
			@RequestParam("content") String newContent, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		try {
			User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
			DiscussionMessage message = discussionMessageRepository.findById(messageId).orElseThrow();

			if (!message.getUser().getId().equals(currentUser.getId())) {
				redirectAttributes.addFlashAttribute("error", "You can only edit your own messages!");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			if (newContent == null || newContent.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Message content cannot be empty!");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			message.setContent(newContent.trim());
			discussionMessageRepository.save(message);
			redirectAttributes.addFlashAttribute("success", "Message updated successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error updating message");
		}
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}

	@PostMapping("/projects/{projectId}/discussions/{discussionId}/messages/{messageId}/delete")
	public String deleteMessage(@PathVariable("projectId") Long projectId,
			@PathVariable("discussionId") Long discussionId, @PathVariable("messageId") Long messageId,
			@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
		try {
			User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
			DiscussionMessage message = discussionMessageRepository.findById(messageId).orElseThrow();
			Project project = projectService.getProjectById(projectId);

			boolean isMessageOwner = message.getUser().getId().equals(currentUser.getId());
			boolean isProjectOwner = project.getOwner() != null
					&& project.getOwner().getId().equals(currentUser.getId());
			boolean isProjectAdmin = project.getMembers() != null && project.getMembers().stream()
					.anyMatch(m -> m.getId().equals(currentUser.getId()) && m.getRole() == User.Role.ADMIN);

			if (!isMessageOwner && !isProjectOwner && !isProjectAdmin) {
				redirectAttributes.addFlashAttribute("error", "You do not have permission to delete this message!");
				return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
			}

			discussionMessageRepository.deleteById(messageId);
			redirectAttributes.addFlashAttribute("success", "Message deleted successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error deleting message");
		}
		redirectAttributes.addFlashAttribute("activePanel", "topics");
		return "redirect:/projects/view/" + projectId + "?discussionId=" + discussionId;
	}
}