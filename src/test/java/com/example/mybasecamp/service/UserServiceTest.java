package com.example.mybasecamp.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.mybasecamp.model.User;
import com.example.mybasecamp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	public void testGetUserById_Success() {
		Long userId = 1L;
		User mockUser = new User();
		mockUser.setId(userId);
		mockUser.setEmail("testuser@example.com");

		when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

		User result = userService.getUserById(userId);

		assertNotNull(result);
		assertEquals("testuser@example.com", result.getEmail());
		assertEquals(userId, result.getId());

		verify(userRepository, times(1)).findById(userId);
	}
}