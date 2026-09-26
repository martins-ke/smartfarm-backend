package com.smartfarm.projects;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;
import com.smartfarm.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectsController {
	private final ProjectsService projectService;
	
	public ProjectsController(ProjectsService projectService) { 
		this.projectService = projectService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Project>> createProject(
			@Valid @RequestBody CreateProjectRequest request,
			@AuthenticationPrincipal User currentUser) {
		return projectService.createProject(request, currentUser);
	}
	
	@GetMapping("/{category_id}/{category}")
	public ResponseEntity<ApiResponse<Page<Project>>> getProjectsByCategoryId(
			@PathVariable String category_id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal User currentUser) {
		return projectService.getProjectsByCategoryId(category_id, page, size, currentUser); 
	}
	
	@GetMapping("/{projectId}")
	public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
			@PathVariable String projectId,
			@AuthenticationPrincipal User currentUser) {
		return projectService.getProjectById(projectId, currentUser); 
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Project>>> getAllProjects(
			@AuthenticationPrincipal User currentUser) {
		return projectService.getAllProjects(currentUser);
	}

	@GetMapping("/all/count")
	public ResponseEntity<ApiResponse<Long>> getAllProjectsCount(
			@AuthenticationPrincipal User currentUser) {
		return projectService.getAllProjectsCount();
	}
	
	@GetMapping("/all/budget")
	public ResponseEntity<ApiResponse<BigDecimal>> getAllProjectsBudget(
			@AuthenticationPrincipal User currentUser) {
		return projectService.getAllProjectsBudget(); 
	}
	
	@GetMapping("/active/count")
	public ResponseEntity<ApiResponse<Long>> getActiveProjectsCount(
			@AuthenticationPrincipal User currentUser) {
		return projectService.getActiveProjectsCount();
	}
	
	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<ProjectsSummary>> getProjectsSummary(
			@AuthenticationPrincipal User currentUser) {
		return projectService.projectsSummary(currentUser);  
	}

	/** PATCH /projects/{id}/status  –  update status only */
	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<Project>> updateProjectStatus(
			@PathVariable String id,
			@RequestBody UpdateStatusRequest request,
			@AuthenticationPrincipal User currentUser) {
		return projectService.updateProjectStatus(id, request, currentUser);
	}

	/** PUT /projects/{id}  –  update any project fields */
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Project>> updateProject(
			@PathVariable String id,
			@RequestBody UpdateProjectRequest request,
			@AuthenticationPrincipal User currentUser) {
		return projectService.updateProject(id, request, currentUser);
	}

	/** PATCH /projects/{id}/assign-supervisor */
	@PatchMapping("/{id}/assign-supervisor")
	public ResponseEntity<ApiResponse<Project>> assignSupervisor(
			@PathVariable String id,
			@RequestBody AssignSupervisorRequest request,
			@AuthenticationPrincipal User currentUser) {
		return projectService.assignSupervisor(id, request, currentUser);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteProject(
			@PathVariable String id,
			@AuthenticationPrincipal User currentUser) {
		return projectService.deleteProject(id, currentUser);
	}
}
