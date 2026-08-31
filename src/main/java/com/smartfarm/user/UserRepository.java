package com.smartfarm.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	boolean existsByUsername(String username); 
	User findByUsername(String username);
	Optional<User> findOptionalByUsername(String username);
	
	long countByRoleIgnoreCase(String role);
	boolean existsByRoleIgnoreCase(String role);
	List<User> findByRoleIgnoreCase(String role);
	List<User> findByCreatedById(String createdById);
}

