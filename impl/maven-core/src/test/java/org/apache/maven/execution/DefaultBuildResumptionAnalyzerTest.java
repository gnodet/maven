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
package org.apache.maven.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.api.Project;
import org.apache.maven.api.exec.BuildResumptionData;
import org.apache.maven.graph.ProjectStub;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class DefaultBuildResumptionAnalyzerTest {
    private final DefaultBuildResumptionAnalyzer analyzer = new DefaultBuildResumptionAnalyzer();

    private InternalMavenSession session;
    private MavenExecutionResult executionResult;
    private DefaultMavenResult result;

    @BeforeEach
    void before() {
        session = Mockito.mock(InternalMavenSession.class);
        when(session.getProjects(anyList())).thenAnswer(invocation -> {
            List<MavenProject> projects = invocation.getArgument(0);
            List<Project> prjs = new ArrayList<>(projects.size());
            for (MavenProject project : projects) {
                prjs.add(session.getProject(project));
            }
            return prjs;
        });
        executionResult = new DefaultMavenExecutionResult();
        result = new DefaultMavenResult(session, executionResult);
    }

    @Test
    void resumeFromGetsDetermined() {
        MavenProject projectA = createSucceededMavenProject("A");
        MavenProject projectB = createFailedMavenProject("B");
        executionResult.setTopologicallySortedProjects(asList(projectA, projectB));

        Optional<BuildResumptionData> result = analyzer.determineBuildResumptionData(this.result);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getRemainingProjects(), is(asList("test:B")));
    }

    @Test
    void resumeFromIsIgnoredWhenFirstProjectFails() {
        MavenProject projectA = createFailedMavenProject("A");
        MavenProject projectB = createMavenProject("B");
        executionResult.setTopologicallySortedProjects(asList(projectA, projectB));

        Optional<BuildResumptionData> result = analyzer.determineBuildResumptionData(this.result);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    void projectsSucceedingAfterFailedProjectsAreExcluded() {
        MavenProject projectA = createSucceededMavenProject("A");
        MavenProject projectB = createFailedMavenProject("B");
        MavenProject projectC = createSucceededMavenProject("C");
        executionResult.setTopologicallySortedProjects(asList(projectA, projectB, projectC));

        Optional<BuildResumptionData> result = analyzer.determineBuildResumptionData(this.result);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getRemainingProjects(), is(asList("test:B")));
    }

    @Test
    void projectsDependingOnFailedProjectsAreNotExcluded() {
        MavenProject projectA = createSucceededMavenProject("A");
        MavenProject projectB = createFailedMavenProject("B");
        MavenProject projectC = createSkippedMavenProject("C");
        projectC.setDependencies(singletonList(toDependency(projectB)));
        executionResult.setTopologicallySortedProjects(asList(projectA, projectB, projectC));

        Optional<BuildResumptionData> result = analyzer.determineBuildResumptionData(this.result);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getRemainingProjects(), is(asList("test:B", "test:C")));
    }

    @Test
    void projectsFailingAfterAnotherFailedProjectAreNotExcluded() {
        MavenProject projectA = createSucceededMavenProject("A");
        MavenProject projectB = createFailedMavenProject("B");
        MavenProject projectC = createSucceededMavenProject("C");
        MavenProject projectD = createFailedMavenProject("D");
        executionResult.setTopologicallySortedProjects(asList(projectA, projectB, projectC, projectD));

        Optional<BuildResumptionData> result = analyzer.determineBuildResumptionData(this.result);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getRemainingProjects(), is(asList("test:B", "test:D")));
    }

    private MavenProject createMavenProject(String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId(artifactId);
        ProjectStub stub = new ProjectStub().setMavenModel(project.getModel());
        when(session.getProject(project)).thenReturn(stub);
        when(session.getMavenProject(stub)).thenReturn(project);
        return project;
    }

    private Dependency toDependency(MavenProject mavenProject) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(mavenProject.getGroupId());
        dependency.setArtifactId(mavenProject.getArtifactId());
        dependency.setVersion(mavenProject.getVersion());
        return dependency;
    }

    private MavenProject createSkippedMavenProject(String artifactId) {
        return createMavenProject(artifactId);
    }

    private MavenProject createSucceededMavenProject(String artifactId) {
        MavenProject project = createMavenProject(artifactId);
        executionResult.addBuildSummary(new BuildSuccess(project, 0));
        return project;
    }

    private MavenProject createFailedMavenProject(String artifactId) {
        MavenProject project = createMavenProject(artifactId);
        executionResult.addBuildSummary(new BuildFailure(project, 0, new Exception()));
        executionResult.addException(new LifecycleExecutionException("", project));
        return project;
    }
}
