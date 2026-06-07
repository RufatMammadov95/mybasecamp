package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.User;
import com.example.mybasecamp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

	private final UserService userService;

	public ProfileController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/edit")
	public String showEditProfileForm(Model model, Principal principal) {
		String email = principal.getName();
		User currentUser = userService.getUserByEmail(email);

		model.addAttribute("user", currentUser);
		return "edit-profile";
	}

	@PostMapping("/update")
	public String updateProfile(@RequestParam("username") String username,
			@RequestParam(value = "oldPassword", required = false) String oldPassword,
			@RequestParam(value = "newPassword", required = false) String newPassword, Principal principal,
			RedirectAttributes redirectAttributes) {
		String email = principal.getName();
		try {
			userService.updateProfile(email, username, oldPassword, newPassword);
			redirectAttributes.addFlashAttribute("successMessage", "Profil məlumatlarınız uğurla yeniləndi!");
			return "redirect:/profile/edit";
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/profile/edit";
		}
	}
}