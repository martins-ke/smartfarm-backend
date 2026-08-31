package com.smartfarm.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartfarm.projects.Project;

@Repository
public interface InventoryUsageRepository extends JpaRepository<InventoryUsage, String> {
    List<InventoryUsage> findByProject(Project project);
}
