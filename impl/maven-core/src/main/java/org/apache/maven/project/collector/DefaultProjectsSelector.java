/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.project.collector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to select projects for a given set of pom.xml files.
 */
@Named
@Singleton
public class DefaultProjectsSelector implements ProjectsSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProjectsSelector.class);
    private final ProjectBuilder projectBuilder;

    @Inject
    public DefaultProjectsSelector(ProjectBuilder projectBuilder) {
        this.projectBuilder = projectBuilder;
    }

    @Override
    public List<Project> selectProjects(List<Path> files, MavenRequest request) throws ProjectBuilderException {

        Session session = request.getSession();

        boolean isRecursive = request.isRecursive();

        // Build projects using new API - need to iterate through files since new API doesn't have bulk build
        List<ProjectBuilderResult> results = new ArrayList<>();

        for (Path path : files) {
            try {
                ProjectBuilderRequest builderRequest = ProjectBuilderRequest.builder()
                        .session(session)
                        .path(path)
                        .recursive(isRecursive)
                        .build();
                results.add(projectBuilder.build(builderRequest));
            } catch (org.apache.maven.api.services.ProjectBuilderException e) {
                results.add(e.getResult());
            }
        }

        List<Project> projects = new ArrayList<>(results.size());

        long totalProblemsCount = 0;

        for (ProjectBuilderResult result : results) {
            result.getProject().ifPresent(projects::add);

            // Add child projects if this was a recursive build
            addChildProjects(result, projects);

            int problemsCount = result.getProblems().size();
            totalProblemsCount += problemsCount;
            if (problemsCount != 0 && LOGGER.isWarnEnabled()) {
                LOGGER.warn("");
                LOGGER.warn(
                        "{} {} encountered while building the effective model for '{}' (use -e to see details)",
                        problemsCount,
                        (problemsCount == 1) ? "problem was" : "problems were",
                        result.getProjectId());

                if (request.isShowErrors()) { // this means -e or -X (as -X enables -e as well)
                    for (BuilderProblem problem : result.getProblems()) {
                        String loc = formatLocation(problem, result.getProjectId());
                        LOGGER.warn("{}{}", problem.getMessage(), !loc.isEmpty() ? " @ " + loc : "");
                    }
                }
            }
        }

        if (totalProblemsCount > 0) {
            LOGGER.warn("");
            LOGGER.warn("Total model problems reported: {}", totalProblemsCount);
            LOGGER.warn("");
            LOGGER.warn("It is highly recommended to fix these problems"
                    + " because they threaten the stability of your build.");
            LOGGER.warn("");
            LOGGER.warn("For this reason, future Maven versions might no"
                    + " longer support building such malformed projects.");
            LOGGER.warn("");
        }

        return projects;
    }

    /**
     * Creates a string with all location details for the specified model problem. If the project identifier is
     * provided, the generated location will omit the model id and source information and only give line/column
     * information for problems originating directly from this POM.
     *
     * @param problem The problem whose location should be formatted, must not be {@code null}.
     * @param projectId The {@code <groupId>:<artifactId>:<version>} of the corresponding project, may be {@code null}
     *            to force output of model id and source.
     * @return The formatted problem location or an empty string if unknown, never {@code null}.
     */
    public static String formatLocation(BuilderProblem problem, String projectId) {
        StringBuilder buffer = new StringBuilder(256);

        if (problem instanceof ModelProblem modelProblem
                && !modelProblem.getModelId().equals(projectId)) {
            buffer.append(modelProblem.getModelId());
        }

        if (!problem.getSource().isEmpty()) {
            if (!buffer.isEmpty()) {
                buffer.append(", ");
            }
            buffer.append(problem.getSource());
        }

        if (problem.getLineNumber() > 0) {
            if (!buffer.isEmpty()) {
                buffer.append(", ");
            }
            buffer.append("line ").append(problem.getLineNumber());
        }

        if (problem.getColumnNumber() > 0) {
            if (!buffer.isEmpty()) {
                buffer.append(", ");
            }
            buffer.append("column ").append(problem.getColumnNumber());
        }

        return buffer.toString();
    }

    /**
     * Recursively adds child projects from a ProjectBuilderResult to the projects list.
     *
     * @param result the ProjectBuilderResult to process
     * @param projects the list to add projects to
     */
    private void addChildProjects(ProjectBuilderResult result, List<Project> projects) {
        for (ProjectBuilderResult child : result.getChildren()) {
            child.getProject().ifPresent(projects::add);
            // Recursively add children of children
            addChildProjects(child, projects);
        }
    }
}
