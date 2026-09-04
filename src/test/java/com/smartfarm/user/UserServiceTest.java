package com.smartfarm.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartfarm.ApiResponse;
import com.smartfarm.auth.EmailService;
import com.smartfarm.auth.PasswordResetTokenRepository;
import com.smartfarm.category.CategoryRepository;
import com.smartfarm.projects.Project;
import com.smartfarm.projects.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private ProjectRepository projectRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository tokenRepo;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User supervisor;
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        supervisor = new User("SUP002", "supervisor2", "sup2@smartfarm.com", "pass", "SUPERVISOR", "ACTIVE", "ADMIN01");
        supervisor.setMaxProjectCapacity(4);

        project1 = new Project();
        project1.setId("PROJ001");
        project1.setName("Tomato Greenhouse");

        project2 = new Project();
        project2.setId("PROJ002");
        project2.setName("Maize Plantation");
    }

    @Test
    void assignProjectsToSupervisor_successfullyAssignsProjects() {
        when(userRepo.findById("SUP002")).thenReturn(Optional.of(supervisor));
        when(projectRepo.findBySupervisorId("SUP002")).thenReturn(Collections.emptyList());
        when(projectRepo.findById("PROJ001")).thenReturn(Optional.of(project1));
        when(projectRepo.findById("PROJ002")).thenReturn(Optional.of(project2));

        AssignProjectsRequest request = new AssignProjectsRequest(List.of("PROJ001", "PROJ002"));
        ResponseEntity<ApiResponse<Void>> response = userService.assignProjectsToSupervisor("SUP002", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals(supervisor, project1.getSupervisor());
        assertEquals(supervisor, project2.getSupervisor());
        verify(projectRepo).save(project1);
        verify(projectRepo).save(project2);
    }

    @Test
    void assignProjectsToSupervisor_whenExceedingCapacity_returnsBadRequest() {
        supervisor.setMaxProjectCapacity(1);
        when(userRepo.findById("SUP002")).thenReturn(Optional.of(supervisor));

        AssignProjectsRequest request = new AssignProjectsRequest(List.of("PROJ001", "PROJ002"));
        ResponseEntity<ApiResponse<Void>> response = userService.assignProjectsToSupervisor("SUP002", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().message().contains("exceeds maximum active capacity"));
    }

    @Test
    void assignProjectsToSupervisor_whenDefaultCapacityZero_defaultsToFourAndSucceeds() {
        supervisor.setMaxProjectCapacity(0); // Should resolve to 4
        when(userRepo.findById("SUP002")).thenReturn(Optional.of(supervisor));
        when(projectRepo.findBySupervisorId("SUP002")).thenReturn(Collections.emptyList());
        when(projectRepo.findById("PROJ001")).thenReturn(Optional.of(project1));

        AssignProjectsRequest request = new AssignProjectsRequest(List.of("PROJ001"));
        ResponseEntity<ApiResponse<Void>> response = userService.assignProjectsToSupervisor("SUP002", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
    }
}
