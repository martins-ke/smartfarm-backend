package com.smartfarm.projects;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartfarm.activities.Activity;
import com.smartfarm.harvest.Harvest;
import com.smartfarm.sales.Sale;

public interface ProjectRepository extends JpaRepository<Project, String> {

	List<Project> findByCategoryId(String category_id);
	Page<Project> findByCategoryId(String category_id, Pageable pageable);

	List<Project> findBySupervisorId(String supervisorId);

	@Query("SELECT p FROM Project p WHERE p.manager.id = :userId OR p.category.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :userId)")
	List<Project> findProjectsListForManager(@Param("userId") String userId);

	@Query("SELECT p FROM Project p WHERE p.category.id = :categoryId AND p.supervisor.id = :supervisorId")
	Page<Project> findProjectsForSupervisor(@Param("categoryId") String categoryId, @Param("supervisorId") String supervisorId, Pageable pageable);

	@Query("SELECT p FROM Project p WHERE p.category.id = :categoryId AND (p.manager.id = :userId OR p.category.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :userId))")
	Page<Project> findProjectsForManager(@Param("categoryId") String categoryId, @Param("userId") String userId, Pageable pageable);

	@Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.expenses WHERE p.id = :project_id")
	Optional<Project> findProjectWithRecords(@Param("project_id") String project_id);
	
	@Query("SELECT s FROM Sale s WHERE s.project.id = :project_id")
	List<Sale> findProjectSales(@Param("project_id") String project_id);
	
	@Query("SELECT h FROM Harvest h WHERE h.project.id = :project_id")
	List<Harvest> findProjectHarvest(@Param("project_id") String project_id); 
	
	@Query("SELECT a FROM Activity a WHERE a.project.id = :project_id")
	List<Activity> findProjectActivities(@Param("project_id") String project_id); 
	
	@Query("SELECT COALESCE(SUM(p.budget), 0) FROM Project p")
	BigDecimal totalProjectsBudget();
	
	@Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status")
	Long totaActiveProjects(@Param("status") String status);

	// Scoped metrics for supervisor
	@Query("SELECT COUNT(p) FROM Project p WHERE p.supervisor.id = :supervisorId")
	Long countBySupervisorId(@Param("supervisorId") String supervisorId);

	@Query("SELECT COUNT(p) FROM Project p WHERE p.supervisor.id = :supervisorId AND p.status = :status")
	Long countActiveBySupervisorId(@Param("supervisorId") String supervisorId, @Param("status") String status);

	@Query("SELECT COALESCE(SUM(p.budget), 0) FROM Project p WHERE p.supervisor.id = :supervisorId")
	BigDecimal totalBudgetBySupervisorId(@Param("supervisorId") String supervisorId);

	// Scoped metrics for manager
	@Query("SELECT COUNT(p) FROM Project p WHERE p.manager.id = :managerId OR p.category.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :managerId)")
	Long countForManager(@Param("managerId") String managerId);

	@Query("SELECT COUNT(p) FROM Project p WHERE (p.manager.id = :managerId OR p.category.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :managerId)) AND p.status = :status")
	Long countActiveForManager(@Param("managerId") String managerId, @Param("status") String status);

	@Query("SELECT COALESCE(SUM(p.budget), 0) FROM Project p WHERE p.manager.id = :managerId OR p.category.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :managerId)")
	BigDecimal totalBudgetForManager(@Param("managerId") String managerId);
}
