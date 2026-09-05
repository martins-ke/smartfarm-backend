package com.smartfarm.suppliers;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

	boolean existsByNameIgnoreCase(String name);

	Optional<Supplier> findByNameIgnoreCase(String name);

	List<Supplier> findByIsActiveTrue();
}
