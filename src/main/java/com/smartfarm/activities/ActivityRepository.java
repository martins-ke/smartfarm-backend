package com.smartfarm.activities;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, String> {

	List<Activity> findByProjectId(String projectId);

	List<Activity> findByProjectIdOrderByScheduledDateAsc(String projectId);

	List<Activity> findByStatusIgnoreCase(String status);

	@Query("SELECT a FROM Activity a WHERE a.scheduledDate BETWEEN :startDate AND :endDate AND LOWER(a.status) != 'completed' AND LOWER(a.status) != 'cancelled'")
	List<Activity> findUpcomingScheduledTasks(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	@Query("SELECT a FROM Activity a WHERE a.scheduledDate < :currentDate AND LOWER(a.status) != 'completed' AND LOWER(a.status) != 'cancelled'")
	List<Activity> findOverdueTasks(@Param("currentDate") LocalDate currentDate);
}
