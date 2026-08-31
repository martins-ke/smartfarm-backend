package com.smartfarm.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, String> {

	@Query("SELECT DISTINCT p.category FROM Project p WHERE p.supervisor.id = :supervisorId")
	List<Category> findCategoriesForSupervisor(@Param("supervisorId") String supervisorId);

	@Query("SELECT DISTINCT c FROM Category c WHERE c.id IN (SELECT ac.id FROM User u JOIN u.assignedCategories ac WHERE u.id = :managerId) OR c.id IN (SELECT p.category.id FROM Project p WHERE p.manager.id = :managerId)")
	List<Category> findCategoriesForManager(@Param("managerId") String managerId);
}
