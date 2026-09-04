package com.smartfarm.projects;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.smartfarm.ApiResponse;
import com.smartfarm.category.Category;
import com.smartfarm.category.CategoryRepository;
import com.smartfarm.expenses.ExpenseRepository;
import com.smartfarm.expenses.ExpenseResponse;
import com.smartfarm.sales.SalesRepository;
import com.smartfarm.user.User;
import com.smartfarm.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import com.smartfarm.util.IdGenarator;

@Service
public class ProjectsService {

	private final ProjectRepository projectRepo;
	private final CategoryRepository categoryRepo;
	private final ExpenseRepository expenseRepo;
	private final SalesRepository salesRepo;
	private final UserRepository userRepo;
	
	public ProjectsService(ProjectRepository projectRepo, CategoryRepository categoryRepo, ExpenseRepository expenseRepo, SalesRepository salesRepo, UserRepository userRepo) {
		this.projectRepo = projectRepo;
		this.categoryRepo = categoryRepo;
		this.expenseRepo = expenseRepo;
		this.salesRepo = salesRepo;
		this.userRepo = userRepo;
	}
	
	public ResponseEntity<ApiResponse<Project>> createProject(CreateProjectRequest request){
		if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Start date cannot be greater than end date!", false, Instant.now()));
		}

		if (request.name() != null && projectRepo.existsByNameIgnoreCase(request.name().trim())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "A project with the name '" + request.name().trim() + "' already exists! Please choose a unique name.", false, Instant.now()));
		}

		Category category = categoryRepo.findById(request.category_id()).orElseThrow(()-> new EntityNotFoundException("Category not in the system!"));
		long count = projectRepo.count();
		
		String id = IdGenarator.generateId(request.name().trim(), count);
		while (projectRepo.existsById(id)) {
			count++;
			id = IdGenarator.generateId(request.name().trim(), count);
		}

		Project p = new Project(id, request.name().trim(), request.season(), request.status(), request.startDate(), 
						request.endDate(), request.budget(), request.description(), category);
		return ResponseEntity.status(201).body(new ApiResponse<>(projectRepo.save(p), "Project created successfully", true, Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<Page<Project>>> getProjectsByCategoryId(String category_id, int page, int size, String userId, String userRole){
		Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		
		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			// Supervisor ONLY sees projects assigned to them
			Page<Project> supProjects = projectRepo.findProjectsForSupervisor(category_id, userId.trim(), pageable);
			return ResponseEntity.status(200).body(new ApiResponse<>(supProjects, null, true, Instant.now()));
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			// Manager sees projects assigned to them or in their assigned categories
			Page<Project> mgrProjects = projectRepo.findProjectsForManager(category_id, userId.trim(), pageable);
			return ResponseEntity.status(200).body(new ApiResponse<>(mgrProjects, null, true, Instant.now()));
		}

		// Admin sees all
		return ResponseEntity.status(200).body(new ApiResponse<>(projectRepo.findByCategoryId(category_id, pageable), null, true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(String id, String userId, String userRole){
		Project project = projectRepo.findProjectWithRecords(id).orElseThrow(()-> new EntityNotFoundException("Project not found with id: " + id)); 
		
		// Guard: If supervisor, verify assigned to this project
		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			boolean isAssigned = project.getSupervisor() != null && userId.trim().equals(project.getSupervisor().getId());
			if (!isAssigned) {
				return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You are not assigned to supervise this project.", false, Instant.now()));
			}
		}

		boolean isSupervisor = "SUPERVISOR".equalsIgnoreCase(userRole);
		BigDecimal totalExpenses = isSupervisor ? BigDecimal.ZERO : expenseRepo.totalExpensesByProjectId(id);
		BigDecimal totalSales = isSupervisor ? BigDecimal.ZERO : salesRepo.totalSalesByProjectId(id);
		BigDecimal netValue = isSupervisor ? BigDecimal.ZERO : totalSales.subtract(totalExpenses);
		BigDecimal displayBudget = isSupervisor ? BigDecimal.ZERO : project.getBudget();

		List<ExpenseResponse> expenses = isSupervisor ? java.util.Collections.emptyList() : project.getExpenses().stream().map(e -> new ExpenseResponse(
				e.getId(), e.getTitle(), e.getAmount(), e.getUnitPrice(), e.getQuantity(), e.getAdded_on(), e.getNotes())).toList();
		
		List<com.smartfarm.sales.Sale> projectSales = isSupervisor ? java.util.Collections.emptyList() : projectRepo.findProjectSales(id);

		ProjectResponse p = new ProjectResponse(project.getId(), project.getName(), project.getSeason(), displayBudget,
				project.getStatus(), project.getStartDate(), project.getEndDate(), project.getDescription(), 
				totalSales, totalExpenses, netValue,
				expenses, 
				projectSales, 
				projectRepo.findProjectHarvest(id),
				projectRepo.findProjectActivities(id));
		
		 return ResponseEntity.status(200).body(new ApiResponse<>( p, null, true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<List<Project>>> getAllProjects(String userId, String userRole) {
		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			return ResponseEntity.ok(new ApiResponse<>(projectRepo.findBySupervisorId(userId.trim()), null, true, Instant.now()));
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			return ResponseEntity.ok(new ApiResponse<>(projectRepo.findProjectsListForManager(userId.trim()), null, true, Instant.now()));
		}
		return ResponseEntity.ok(new ApiResponse<>(projectRepo.findAll(), null, true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Long>> getAllProjectsCount(){
		 return ResponseEntity.status(200).body(new ApiResponse<>( projectRepo.count(), "Total projects tracked across every category.", true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<BigDecimal>> getAllProjectsBudget(){ 
		 return ResponseEntity.status(200).body(new ApiResponse<>( projectRepo.totalProjectsBudget(), "Combined projects budget across all categories.", true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<Long>> getActiveProjectsCount(){
		 return ResponseEntity.status(200).body(new ApiResponse<>( projectRepo.totaActiveProjects("active"), "Projects currently marked as active.", true, Instant.now()));
	}
	
	public ResponseEntity<ApiResponse<ProjectsSummary>> projectsSummary(String userId, String userRole){ 
		Long allCount;
		Long activeCount;
		BigDecimal totalBudget;

		if ("SUPERVISOR".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			allCount = projectRepo.countBySupervisorId(userId.trim());
			activeCount = projectRepo.countActiveBySupervisorId(userId.trim(), "active");
			totalBudget = BigDecimal.ZERO; // Financial Shield for Supervisor
		} else if ("MANAGER".equalsIgnoreCase(userRole) && userId != null && !userId.trim().isEmpty()) {
			allCount = projectRepo.countForManager(userId.trim());
			activeCount = projectRepo.countActiveForManager(userId.trim(), "active");
			totalBudget = projectRepo.totalBudgetForManager(userId.trim());
		} else {
			allCount = projectRepo.count();
			activeCount = projectRepo.totaActiveProjects("active");
			totalBudget = projectRepo.totalProjectsBudget();
		}

		ProjectsSummary summary = new ProjectsSummary(allCount != null ? allCount : 0L, activeCount != null ? activeCount : 0L, totalBudget != null ? totalBudget : BigDecimal.ZERO);
		return ResponseEntity.status(200).body(new ApiResponse<>(summary, "Projects summary", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Project>> updateProjectStatus(String id, UpdateStatusRequest request) {
		Project project = projectRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

		String newStatus = request.status() != null ? request.status().trim().toLowerCase() : "active";
		project.setStatus(newStatus);
		Project saved = projectRepo.save(project);

		return ResponseEntity.ok(new ApiResponse<>(saved, "Status updated to " + newStatus, true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Project>> updateProject(String id, UpdateProjectRequest request) {
		Project project = projectRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

		LocalDate effectiveStart = request.startDate() != null ? request.startDate() : project.getStartDate();
		LocalDate effectiveEnd = request.endDate() != null ? request.endDate() : project.getEndDate();
		if (effectiveStart != null && effectiveEnd != null && effectiveStart.isAfter(effectiveEnd)) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Start date cannot be greater than end date!", false, Instant.now()));
		}

		if (request.name() != null && !request.name().trim().equalsIgnoreCase(project.getName())) {
			if (projectRepo.existsByNameIgnoreCase(request.name().trim())) {
				return ResponseEntity.status(400).body(new ApiResponse<>(null, "A project with the name '" + request.name().trim() + "' already exists! Please choose a unique name.", false, Instant.now()));
			}
			project.setName(request.name().trim());
		}
		if (request.season() != null)      project.setSeason(request.season());
		if (request.status() != null)      project.setStatus(request.status());
		if (request.startDate() != null)   project.setStartDate(request.startDate());
		if (request.endDate() != null)     project.setEndDate(request.endDate());
		if (request.budget() != null)      project.setBudget(request.budget());
		if (request.description() != null) project.setDescription(request.description());

		Project saved = projectRepo.save(project);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Project updated", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<Project>> assignSupervisor(String id, AssignSupervisorRequest request) {
		Project project = projectRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

		if (request.supervisorId() != null && !request.supervisorId().trim().isEmpty()) {
			User supervisor = userRepo.findById(request.supervisorId())
					.orElseThrow(() -> new EntityNotFoundException("Supervisor not found with ID: " + request.supervisorId()));
			project.setSupervisor(supervisor);
		} else {
			project.setSupervisor(null);
		}

		Project saved = projectRepo.save(project);
		return ResponseEntity.ok(new ApiResponse<>(saved, "Supervisor assigned to project successfully.", true, Instant.now()));
	}
}
