package com.smartfarm.projects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Used for PUT /projects/{id} – all fields are optional so callers
 * can send only the fields they want to change.
 */
public record UpdateProjectRequest(
        String name,
        String season,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String description
) {
}
