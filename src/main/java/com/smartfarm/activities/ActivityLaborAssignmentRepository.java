package com.smartfarm.activities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLaborAssignmentRepository extends JpaRepository<ActivityLaborAssignment, Long> {

	List<ActivityLaborAssignment> findByActivityId(String activityId);

	List<ActivityLaborAssignment> findByEmployeeId(String employeeId);
}
