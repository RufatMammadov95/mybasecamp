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

		String hashedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(hashedPassword);
		user.setRole(User.Role.USER);
		return userRepository.save(user);
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
