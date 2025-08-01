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
package org.apache.maven.graph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.exec.ActivationSettings;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.exec.ProjectActivation;
import org.apache.maven.api.exec.ProjectDependencyGraph;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.BuildResumptionData;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.CycleDetectedException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.collector.MultiModuleCollectionStrategy;
import org.apache.maven.project.collector.PomlessCollectionStrategy;
import org.apache.maven.project.collector.RequestPomCollectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;

/**
 * Builds the {@link ProjectDependencyGraph inter-dependencies graph} between projects in the reactor.
 */
@Named(GraphBuilder.HINT)
@Singleton
public class DefaultGraphBuilder implements GraphBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGraphBuilder.class);

    private final BuildResumptionDataRepository buildResumptionDataRepository;
    private final PomlessCollectionStrategy pomlessCollectionStrategy;
    private final MultiModuleCollectionStrategy multiModuleCollectionStrategy;
    private final RequestPomCollectionStrategy requestPomCollectionStrategy;
    private final ProjectSelector projectSelector;

    @Inject
    public DefaultGraphBuilder(
            BuildResumptionDataRepository buildResumptionDataRepository,
            PomlessCollectionStrategy pomlessCollectionStrategy,
            MultiModuleCollectionStrategy multiModuleCollectionStrategy,
            RequestPomCollectionStrategy requestPomCollectionStrategy) {
        this.buildResumptionDataRepository = buildResumptionDataRepository;
        this.pomlessCollectionStrategy = pomlessCollectionStrategy;
        this.multiModuleCollectionStrategy = multiModuleCollectionStrategy;
        this.requestPomCollectionStrategy = requestPomCollectionStrategy;
        this.projectSelector = new ProjectSelector(); // if necessary switch to DI
    }

    @Override
    public Result<ProjectDependencyGraph> build(MavenRequest request, ProjectActivation projectActivation) {
        try {
            Result<ProjectDependencyGraph> result = sessionDependencyGraph(request, projectActivation);

            if (result == null) {
                final List<Project> projects = getProjectsForMavenReactor(request, projectActivation);
                validateProjects(projects, request);
                // processPackagingAttribute(projects, request);
                projectActivation = enrichRequestFromResumptionData(projects, request, projectActivation);
                result = reactorDependencyGraph(request, projectActivation, projects);
            }

            return result;
        } catch (final ProjectBuildingException | DuplicateProjectException | MavenExecutionException e) {
            return Result.error(Collections.singletonList(new DefaultModelProblem(null, null, null, null, 0, 0, e)));
        } catch (final CycleDetectedException e) {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();
            ProjectCycleException error = new ProjectCycleException(message, e);
            return Result.error(
                    Collections.singletonList(new DefaultModelProblem(null, null, null, null, 0, 0, error)));
        }
    }

    private Result<ProjectDependencyGraph> sessionDependencyGraph(MavenRequest  request, ProjectActivation projectActivation)
            throws CycleDetectedException, DuplicateProjectException {
        Result<ProjectDependencyGraph> result = null;

        Session session = request.getSession();
        if (session.getProjectDependencyGraph() != null || session.getProjects() != null) {
            List<Project> allProjects = session.getProjectDependencyGraph().getAllProjects();
            ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(projectActivation, allProjects, allProjects);
            if (session.getProjects() != null) {
                graph = new FilteredProjectDependencyGraph(
                        graph, session.getProjects());
            }

            result = Result.success(graph);
        }

        return result;
    }

    private Result<ProjectDependencyGraph> reactorDependencyGraph(MavenRequest request, ProjectActivation projectActivation, List<Project> projects)
            throws CycleDetectedException, DuplicateProjectException, MavenExecutionException {
        ProjectDependencyGraph projectDependencyGraph = new DefaultProjectDependencyGraph(projectActivation, projects, projects);
        List<Project> activeProjects = projectDependencyGraph.getSortedProjects();
        List<Project> allSortedProjects = projectDependencyGraph.getSortedProjects();
        activeProjects = trimProjectsToRequest(activeProjects, projectDependencyGraph, request);
        activeProjects =
                trimSelectedProjects(projectActivation, activeProjects, allSortedProjects, projectDependencyGraph, request);
        activeProjects = trimResumedProjects(activeProjects, projectDependencyGraph, request);
        activeProjects = trimExcludedProjects(projectActivation, activeProjects, projectDependencyGraph, request);

        if (activeProjects.size() != projectDependencyGraph.getSortedProjects().size()) {
            projectDependencyGraph = new FilteredProjectDependencyGraph(
                    projectDependencyGraph, activeProjects);
        }

        return Result.success(projectDependencyGraph);
    }

    private List<Project> trimProjectsToRequest(
            List<Project> activeProjects, ProjectDependencyGraph graph, MavenRequest request)
            throws MavenExecutionException {
        List<Project> result = activeProjects;

        if (request.getPom() != null) {
            result = getProjectsInRequestScope(request, activeProjects);

            List<Project> sortedProjects = graph.getSortedProjects();
            result.sort(comparing(sortedProjects::indexOf));

            result = includeAlsoMakeTransitively(result, request, graph);
        }

        return result;
    }

    private List<Project> trimSelectedProjects(
            ProjectActivation projectActivation,
            List<Project> projects,
            List<Project> allSortedProjects,
            ProjectDependencyGraph graph,
            MavenRequest request)
            throws MavenExecutionException {
        List<Project> result = projects;

        boolean hasActiveProjectSelectors = projectActivation.getActivations().stream()
                .anyMatch(pas -> pas.activationSettings().active());
        if (hasActiveProjectSelectors) {
            Set<Project> selectedProjects =
                    projectSelector.getActiveProjects(request, allSortedProjects, projectActivation);

            // it can be empty when an optional project is missing from the reactor, fallback to returning all projects
            if (!selectedProjects.isEmpty()) {
                result = new ArrayList<>(selectedProjects);

                result = includeAlsoMakeTransitively(result, request, graph);

                // Order the new list in the original order
                List<Project> sortedProjects = graph.getSortedProjects();
                result.sort(comparing(sortedProjects::indexOf));
            }
        }

        return result;
    }

    private List<Project> trimResumedProjects(
            List<Project> projects, ProjectDependencyGraph graph, MavenRequest request)
            throws MavenExecutionException {
        List<Project> result = projects;

        if (request.getResumeFrom() != null && !request.getResumeFrom().isEmpty()) {
            Path reactorDirectory = projectSelector.getBaseDirectoryFromRequest(request);

            String selector = request.getResumeFrom();

            Project resumingFromProject = projects.stream()
                    .filter(project -> projectSelector.isMatchingProject(project, selector, reactorDirectory))
                    .findFirst()
                    .orElseThrow(() -> new MavenExecutionException(
                            "Could not find project to resume reactor build from: " + selector + " vs "
                                    + formatProjects(projects),
                            request.getPom()));
            int resumeFromProjectIndex = projects.indexOf(resumingFromProject);
            List<Project> retainingProjects = result.subList(resumeFromProjectIndex, projects.size());

            result = includeAlsoMakeTransitively(retainingProjects, request, graph);
        }

        return result;
    }

    private List<Project> trimExcludedProjects(
            ProjectActivation projectActivation,
            List<Project> projects, ProjectDependencyGraph graph, MavenRequest request)
            throws MavenExecutionException {
        List<Project> result = projects;

        projectActivation.getActivations().stream()
                .filter(pa -> pa.activationSettings().active())
                .forEach(pas -> {});

        Set<String> requiredSelectors = projectActivation.getRequiredInactiveProjectSelectors();
        Set<String> optionalSelectors = projectActivation.getOptionalInactiveProjectSelectors();
        if (!requiredSelectors.isEmpty() || !optionalSelectors.isEmpty()) {
            Set<Project> excludedProjects = new HashSet<>(requiredSelectors.size() + optionalSelectors.size());
            List<Project> allProjects = graph.getAllProjects();
            excludedProjects.addAll(
                    projectSelector.getRequiredProjectsBySelectors(request, allProjects, requiredSelectors));
            excludedProjects.addAll(
                    projectSelector.getOptionalProjectsBySelectors(request, allProjects, optionalSelectors));

            result = new ArrayList<>(projects);
            result.removeAll(excludedProjects);

            if (result.isEmpty()) {
                boolean isPlural = excludedProjects.size() > 1;
                String message = String.format(
                        "The project exclusion%s in --projects/-pl resulted in an "
                                + "empty reactor, please correct %s.",
                        isPlural ? "s" : "", isPlural ? "them" : "it");
                throw new MavenExecutionException(message, request.getPom());
            }
        }

        return result;
    }

    private List<Project> includeAlsoMakeTransitively(
            List<Project> projects, MavenRequest request, ProjectDependencyGraph graph)
            throws MavenExecutionException {
        List<Project> result = projects;

        MavenRequest.MakeBehavior makeBehavior = request.getMakeBehavior();
        boolean makeUpstream = makeBehavior == MavenRequest.MakeBehavior.BOTH || makeBehavior == MavenRequest.MakeBehavior.UPSTREAM;
        boolean makeDownstream = makeBehavior == MavenRequest.MakeBehavior.BOTH || makeBehavior == MavenRequest.MakeBehavior.DOWNSTREAM;

        // if ((makeBehavior != null && !makeBehavior.isEmpty()) && !makeUpstream && !makeDownstream) {
        //     throw new MavenExecutionException("Invalid reactor make behavior: " + makeBehavior, request.getPom());
        // }

        if (makeUpstream || makeDownstream) {
            Set<Project> projectsSet = new HashSet<>(projects);

            for (Project project : projects) {
                if (makeUpstream) {
                    projectsSet.addAll(graph.getUpstreamProjects(project, true));
                }
                if (makeDownstream) {
                    projectsSet.addAll(graph.getDownstreamProjects(project, true));
                }
            }

            result = new ArrayList<>(projectsSet);

            // Order the new list in the original order
            List<Project> sortedProjects = graph.getSortedProjects();
            result.sort(comparing(sortedProjects::indexOf));
        }

        return result;
    }

    private ProjectActivation enrichRequestFromResumptionData(List<Project> projects, MavenRequest request, ProjectActivation projectActivation) {
        if (request.isResume()) {
            Project topProject = projects.stream()
                    .filter(Project::isTopProject)
                    .findFirst().orElse(null);
            if (topProject != null) {
                BuildResumptionData resumptionData = buildResumptionDataRepository.loadResumptionData(topProject);
                List<String> remainingProjects = resumptionData.getRemainingProjects();
                if (!remainingProjects.isEmpty()) {
                    LOGGER.info("Resuming from {} due to the --resume / -r feature.", String.join(", ", remainingProjects));
                    return new ProjectActivation(Stream.concat(
                            projectActivation.getActivations().stream(),
                            remainingProjects.stream().map(
                                    s -> new ProjectActivation.ProjectActivationSettings(s, ActivationSettings.activatedOptNoRecurse()))
                    ).toList());
                }
            }
        }
        return projectActivation;
    }

    private List<Project> getProjectsInRequestScope(MavenRequest request, List<Project> projects)
            throws MavenExecutionException {
        if (request.getPom() == null) {
            return projects;
        }

        Project requestPomProject = projects.stream()
                .filter(project -> request.getPom().equals(project.getPomPath()))
                .findFirst()
                .orElseThrow(() -> new MavenExecutionException(
                        "Could not find a project in reactor matching the request POM", request.getPom().toFile()));

        List<Project> subprojects = requestPomProject.getActiveSubprojects() != null
                ? requestPomProject.getActiveSubprojects()
                : Collections.emptyList();

        List<Project> result = new ArrayList<>(subprojects);
        result.add(requestPomProject);
        return result;
    }

    private String formatProjects(List<Project> projects) {
        StringBuilder projectNames = new StringBuilder();
        Iterator<Project> iterator = projects.iterator();
        while (iterator.hasNext()) {
            Project project = iterator.next();
            projectNames.append(project.getGroupId()).append(":").append(project.getArtifactId());
            if (iterator.hasNext()) {
                projectNames.append(", ");
            }
        }
        return projectNames.toString();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Project collection
    //
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<Project> getProjectsForMavenReactor(MavenRequest request, ProjectActivation projectActivation) throws ProjectBuildingException {
        // 1. Collect project for invocation without a POM.
        if (request.getPom() == null) {
            return pomlessCollectionStrategy.collectProjects(request);
        }

        // 2. Collect projects for all modules in the multi-module project.
        if (request.getMakeBehavior() != MavenRequest.MakeBehavior.DEFAULT || !projectActivation.isEmpty()) {
            List<Project> projects = multiModuleCollectionStrategy.collectProjects(request);
            if (!projects.isEmpty()) {
                return projects;
            }
        }

        // 3. Collect projects for explicitly requested POM.
        return requestPomCollectionStrategy.collectProjects(request);
    }

    private void validateProjects(List<Project> projects, MavenRequest request)
            throws MavenExecutionException {
        Map<String, Project> projectsMap = new HashMap<>();

        List<Project> projectsInRequestScope = getProjectsInRequestScope(request, projects);
        for (Project p : projectsInRequestScope) {
            String projectKey = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());

            projectsMap.put(projectKey, p);
        }

        for (Project project : projects) {
            // MNG-1911 / MNG-5572: Building plugins with extensions cannot be part of reactor
            for (Plugin plugin : project.getBuild().getPlugins()) {
                if (plugin.isExtensions()) {
                    String pluginKey =
                            ArtifactUtils.key(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());

                    if (projectsMap.containsKey(pluginKey)) {
                        LOGGER.warn(
                                "'{}' uses '{}' as extension which is not possible within the same reactor build. "
                                        + "This plugin was pulled from the local repository!",
                                project.getId(),
                                plugin.getKey());
                    }
                }
            }
        }
    }

    // private void processPackagingAttribute(List<Project> projects, MavenRequest request)
    //         throws MavenExecutionException {
    //     List<Project> projectsInRequestScope = getProjectsInRequestScope(request, projects);
    //     for (Project p : projectsInRequestScope) {
    //         if (Type.BOM.equals(p.getPackaging().id())) {
    //             LOGGER.info(
    //                     "The packaging attribute of the '{}' project is configured as 'bom' and changed to 'pom'",
    //                     p.getId());
    //             p.setPackaging("pom");
    //         }
    //     }
    // }
}
