package com.example.mybasecamp.controller;

import com.example.mybasecamp.model.User;
import com.example.mybasecamp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
	public String registerUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
		try {
			userService.registerUser(user);
			return "redirect:/login?success";
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("emailError", e.getMessage());
			return "redirect:/register";
		} catch (Exception e) {
			return "redirect:/register?error";
		}
	}

	@GetMapping("/login")
	public String showLoginForm() {
		return "login";
	}
}