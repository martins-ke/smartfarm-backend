package com.smartfarm.employees;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

	boolean existsByIdNumber(String idNumber);

	boolean existsByIdNumberAndIsDeletedFalse(String idNumber);

	Optional<Employee> findByIdNumber(String idNumber);

	Optional<Employee> findByIdNumberAndIsDeletedFalse(String idNumber);

	List<Employee> findByStatusIgnoreCase(String status);

	List<Employee> findByIsDeletedFalse();

	List<Employee> findByStatusIgnoreCaseAndIsDeletedFalse(String status);

	Optional<Employee> findByIdAndIsDeletedFalse(String id);
}
