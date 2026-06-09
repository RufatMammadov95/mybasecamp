package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.User;
import com.example.mybasecamp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/")
	public String showWelcomePage() {
		return "index";
	}

	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		model.addAttribute("user", new User());
		return "register";
	}

	@PostMapping("/register")
	public String registerUser(@ModelAttribute("user") User user,
			@RequestParam(value = "passwordConfirmation", required = false) String passwordConfirmation,
			RedirectAttributes redirectAttributes) {
		try {
			if (user.getUsername() == null || user.getUsername().trim().isEmpty() || user.getEmail() == null
					|| user.getEmail().trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("emailError", "Name and Email cannot be empty!");
				return "redirect:/register";
			}

			if (user.getPassword() == null || user.getPassword().trim().length() < 6) {
				redirectAttributes.addFlashAttribute("passwordError", "Password must be at least 6 characters long!");
				return "redirect:/register";
			}

			if (passwordConfirmation != null && !user.getPassword().equals(passwordConfirmation)) {
				redirectAttributes.addFlashAttribute("passwordError", "Passwords do not match!");
				return "redirect:/register";
			}

			user.setUsername(user.getUsername().trim());
			user.setEmail(user.getEmail().trim());

			userService.registerUser(user);
			return "redirect:/login?success";

		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("emailError", e.getMessage());
			return "redirect:/register";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("emailError", "An unexpected error occurred during registration.");
			return "redirect:/register";
		}
	}

	@GetMapping("/login")
	public String showLoginForm() {
		return "login";
	}
}