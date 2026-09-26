package com.smartfarm.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

	@Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.quantityInStock <= i.minStockLevel")
	long countLowStockItems();
}
