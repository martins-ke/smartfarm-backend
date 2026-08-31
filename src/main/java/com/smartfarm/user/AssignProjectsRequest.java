package com.smartfarm.user;

import java.util.List;

public record AssignProjectsRequest(
    List<String> projectIds
) {}
