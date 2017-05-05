package com.infoclinika.mssharing.skyline.web.controller.response;

import com.infoclinika.mssharing.model.read.DashboardReader;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public class ProjectsStructureResponse {
    public final Set<DashboardReader.ProjectStructure> projects;

    public ProjectsStructureResponse(Set<DashboardReader.ProjectStructure> projects) {
        this.projects = projects;
    }
}
