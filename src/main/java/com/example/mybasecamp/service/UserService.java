package com.example.mybasecamp.service;

import com.example.mybasecamp.exception.UserNotFoundException;
import com.example.mybasecamp.model.User;
import com.example.mybasecamp.repository.UserRepository;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User registerUser(User user) {
		Objects.requireNonNull(user, "The User object cannot be empty!");

		if (userRepository.existsByEmail(user.getEmail())) {
			throw new RuntimeException("This email address is already registered!");
		}

		if (userRepository.existsByUsername(user.getUsername())) {
			throw new RuntimeException("This username (Name) is already taken! Try another name.");
		}

		String hashedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(hashedPassword);
		user.setRole(User.Role.USER);
		return userRepository.save(user);
	}

	public void updateProfile(String email, String newUsername, String oldPassword, String newPassword) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		if (newUsername != null && !newUsername.trim().isEmpty()) {
			if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
				throw new RuntimeException("This username is already taken!");
			}
			user.setUsername(newUsername);
		}

		if (newPassword != null && !newPassword.trim().isEmpty()) {
			if (oldPassword == null || oldPassword.trim().isEmpty()) {
				throw new RuntimeException("To change the password, you must enter the old password!");
			}

			if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
				throw new RuntimeException("The old password you entered is incorrect!");
			}

			if (newPassword.length() < 6) {
				throw new RuntimeException("The new password must be at least 6 characters long!");
			}

			user.setPassword(passwordEncoder.encode(newPassword));
		}

		userRepository.save(user);
	}

	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
	}

	public void deleteUser(Long id) {
		User user = getUserById(id);
		userRepository.delete(user);
	}

	public void setAdmin(Long id) {
		User user = getUserById(id);
		user.setRole(User.Role.ADMIN);
		userRepository.save(user);
	}

	public void removeAdmin(Long id) {
		User user = getUserById(id);
		user.setRole(User.Role.USER);
		userRepository.save(user);
	}
}