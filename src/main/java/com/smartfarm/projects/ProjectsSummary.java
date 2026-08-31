package com.smartfarm.projects;

import java.math.BigDecimal;

public record ProjectsSummary(
		Long allCount,
		Long activeCount,
		BigDecimal totalBudget
		) {

}
