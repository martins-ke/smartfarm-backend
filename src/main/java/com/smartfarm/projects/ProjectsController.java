package com.smartfarm.projects;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartfarm.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ProjectsController {
	private final ProjectsService projectService;
	
	public ProjectsController(ProjectsService projectService) { 
		this.projectService = projectService;
	}
	
	@PostMapping("/create")
	public ResponseEntity<ApiResponse<Project>> createProject(
			@Valid @RequestBody CreateProjectRequest request,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return projectService.createProject(request, effectiveUserId, effectiveUserRole);
	}
	
	@GetMapping("/{category_id}/{category}")
	public ResponseEntity<ApiResponse<Page<Project>>> getProjectsByCategoryId(
			@PathVariable String category_id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;

		return projectService.getProjectsByCategoryId(category_id, page, size, effectiveUserId, effectiveUserRole); 
	}
	
	@GetMapping("/{projectId}")
	public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
			@PathVariable String projectId,
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;

		return projectService.getProjectById(projectId, effectiveUserId, effectiveUserRole); 
	}
	
	@GetMapping("/all")
	public ResponseEntity<ApiResponse<List<Project>>> getAllProjects(
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole) {
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;
		return projectService.getAllProjects(effectiveUserId, effectiveUserRole);
	}

	@GetMapping("/all/count")
	public ResponseEntity<ApiResponse<Long>> getAllProjectsCount(){
		return projectService.getAllProjectsCount();
	}
	
	@GetMapping("/all/budget")
	public ResponseEntity<ApiResponse<BigDecimal>> getAllProjectsBudget(){
		return projectService.getAllProjectsBudget(); 
	}
	
	@GetMapping("/active/count")
	public ResponseEntity<ApiResponse<Long>> getActiveProjectsCount(){
		return projectService.getActiveProjectsCount();
	}
	
	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<ProjectsSummary>> getProjectsSummary(
			@RequestHeader(value = "X-User-Id", required = false) String headerUserId,
			@RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) String userRole){
		
		String effectiveUserId = userId != null ? userId : headerUserId;
		String effectiveUserRole = userRole != null ? userRole : headerUserRole;

		return projectService.projectsSummary(effectiveUserId, effectiveUserRole);  
	}

	/** PATCH /projects/{id}/status  –  update status only */
	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<Project>> updateProjectStatus(
			@PathVariable String id,
			@RequestBody UpdateStatusRequest request) {
		return projectService.updateProjectStatus(id, request);
	}

	/** PUT /projects/{id}  –  update any project fields */
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Project>> updateProject(
			@PathVariable String id,
			@RequestBody UpdateProjectRequest request) {
		return projectService.updateProject(id, request);
	}

	/** PATCH /projects/{id}/assign-supervisor */
	@PatchMapping("/{id}/assign-supervisor")
	public ResponseEntity<ApiResponse<Project>> assignSupervisor(
			@PathVariable String id,
			@RequestBody AssignSupervisorRequest request) {
		return projectService.assignSupervisor(id, request);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable String id) {
		return projectService.deleteProject(id);
	}
}
