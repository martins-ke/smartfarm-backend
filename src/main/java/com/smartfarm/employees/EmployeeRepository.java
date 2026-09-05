package com.smartfarm.employees;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

	boolean existsByIdNumber(String idNumber);

	Optional<Employee> findByIdNumber(String idNumber);

	List<Employee> findByStatusIgnoreCase(String status);
}
