package com.smartfarm.projects;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
		 @NotNull @NotBlank(message = "category id required!")
		 String category_id,
		 @NotNull @NotBlank(message = "name required!")
		 String name,
		 @NotNull @NotBlank(message = "season name required!")
		 String season,
		 String status,
		 @NotNull(message = "start date required!")
		 LocalDate startDate, 
		 LocalDate endDate,
		 @NotNull(message = "please fill estimated project amount!")
		 BigDecimal budget,
		 @NotNull @NotBlank(message = "please add a brief description about the project!")
		 String description
		) {

}
