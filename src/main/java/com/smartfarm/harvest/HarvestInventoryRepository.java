package com.smartfarm.harvest;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HarvestInventoryRepository extends JpaRepository<HarvestInventory, Long> {

    /** Find inventory entry by both project and item name */
    Optional<HarvestInventory> findByProjectNameAndItemName(String projectName, String itemName);

    /** Find all entries for a project */
    List<HarvestInventory> findByProjectName(String projectName, Sort sort);

    /** Legacy single-item lookup */
    Optional<HarvestInventory> findByProjectName(String projectName);
}
