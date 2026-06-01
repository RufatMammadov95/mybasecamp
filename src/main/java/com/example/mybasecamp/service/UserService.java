package com.example.mybasecamp.service;

import com.example.mybasecamp.model.User;
import com.example.mybasecamp.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User registerUser(User user) {
		user.setRole(User.Role.USER);
		return userRepository.save(user);
	}

	public User getUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found!"));
	}
	

}
