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

import java.util.List;
import org.apache.maven.api.Project;
import org.apache.maven.api.exec.ProjectDependencyGraph;
import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilteredProjectDependencyGraphTest {

    @Mock
    private ProjectDependencyGraph projectDependencyGraph;

    private final Project aProject = createProject("A");

    private final Project bProject = createProject("B");

    private final Project cProject = createProject("C");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downstreamProjectsShouldBeCached(boolean transitive) {
        FilteredProjectDependencyGraph graph =
                new FilteredProjectDependencyGraph(projectDependencyGraph, List.of(aProject));

        when(projectDependencyGraph.getDownstreamProjects(bProject, transitive)).thenReturn(List.of(cProject));

        graph.getDownstreamProjects(bProject, transitive);
        graph.getDownstreamProjects(bProject, transitive);

        verify(projectDependencyGraph).getDownstreamProjects(bProject, transitive);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void upstreamProjectsShouldBeCached(boolean transitive) {
        FilteredProjectDependencyGraph graph =
                new FilteredProjectDependencyGraph(projectDependencyGraph, List.of(aProject));

        when(projectDependencyGraph.getUpstreamProjects(bProject, transitive)).thenReturn(List.of(cProject));

        graph.getUpstreamProjects(bProject, transitive);
        graph.getUpstreamProjects(bProject, transitive);

        verify(projectDependencyGraph).getUpstreamProjects(bProject, transitive);
    }

    private static Project createProject(String artifactId) {
        ProjectStub result = new ProjectStub();
        result.setModel(Model.newBuilder()
                .groupId("org.apache")
                .artifactId(artifactId)
                .version("1.2")
                .build());
        return result;
    }
}
