package com.smartfarm.user;

import java.util.List;

public record AssignCategoriesRequest(
    List<String> categoryIds
) {}
