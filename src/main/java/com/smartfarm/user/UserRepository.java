package com.smartfarm.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	boolean existsByUsername(String username); 
	Optional<User> findByUsername(String username);
	Optional<User> findByEmail(String email);
	
	long countByRoleIgnoreCase(String role);
	boolean existsByRoleIgnoreCase(String role);
	List<User> findByRoleIgnoreCase(String role);
	List<User> findByCreatedById(String createdById);
	List<User> findByManagerId(String managerId);
	long countByManagerId(String managerId);
}
