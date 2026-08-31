package com.smartfarm.harvest;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HarvestRepository extends JpaRepository<Harvest, String>{
	boolean existsByProjectId(String projectId);

	@Query("SELECT COALESCE(SUM(h.quantity), 0) FROM Harvest h WHERE h.project.id = :projectId")
	Float totalHarvestQuantityByProjectId(@Param("projectId") String projectId);
}
