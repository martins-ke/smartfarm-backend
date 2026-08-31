package com.smartfarm.activities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateActivityRequest(
		@NotBlank(message = "activity title required!") 
		String title,
		@NotBlank(message = "activity type required!") 
		String type,
		@NotBlank(message = "please add a few notes for the activity!") 
		String notes,
		@NotNull(message = "project id missing!")
		String project_id
		) {

}
