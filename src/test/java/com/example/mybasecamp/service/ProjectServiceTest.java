package com.example.mybasecamp.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.mybasecamp.model.Project;
import com.example.mybasecamp.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    public void testGetProjectById_Success() {
        Long projectId = 1L;
        Project mockProject = new Project();
        mockProject.setId(projectId);
        mockProject.setName("Test project 2");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));

        Project result = projectService.getProjectById(projectId);

        assertNotNull(result);
        assertEquals("Test project 2", result.getName());
        assertEquals(projectId, result.getId());

        verify(projectRepository, times(1)).findById(projectId);
    }
}