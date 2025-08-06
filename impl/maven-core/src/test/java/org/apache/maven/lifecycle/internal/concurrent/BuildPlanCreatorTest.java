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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.maven.api.Project;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.graph.ProjectStub;
import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.impl.DefaultMojoExecution;
import org.apache.maven.internal.impl.DefaultProjectManager;
import org.apache.maven.plugin.MojoExecution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPlanCreatorTest {

    @Test
    void testMulti() {
        ProjectStub project = new ProjectStub();
        project.setActiveSubprojects(List.of());
        Map<Project, List<Project>> projects = Map.of(project, List.of());

        BuildPlan plan = calculateLifecycleMappings(projects, "package");

        new BuildPlanLogger(null).writePlan(System.out::println, plan);
    }

    @Test
    void testCondense() {
        ProjectStub p1 = new ProjectStub();
        p1.setActiveSubprojects(List.of());
        p1.setArtifactId("p1");
        ProjectStub p2 = new ProjectStub();
        p2.setActiveSubprojects(List.of());
        p2.setArtifactId("p2");
        Map<Project, List<Project>> projects = Map.of(p1, List.of(), p2, List.of(p1));

        BuildPlan plan = calculateLifecycleMappings(projects, "verify");
        plan.then(calculateLifecycleMappings(projects, "install"));

        Stream.of(p1, p2).forEach(project -> {
            plan.requiredStep(project, "after:resources").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "after:test-resources").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "compile").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "test-compile").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "test").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "package").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
            plan.requiredStep(project, "install").addMojo(new DefaultMojoExecution(null, new MojoExecution(null)), 0);
        });

        ProjectManager manager = new DefaultProjectManager(null, null);
        new BuildPlanLogger(manager) {
            @Override
            protected void mojo(Consumer<String> writer, org.apache.maven.api.MojoExecution mojoExecution) {}
        }.writePlan(System.out::println, plan);

        plan.allSteps().forEach(phase -> {
            phase.predecessors.forEach(
                    pred -> assertTrue(plan.step(pred.project, pred.name).isPresent(), "Phase not present: " + pred));
        });
    }

    @Test
    void testAlias() {
        ProjectStub p1 = new ProjectStub();
        p1.setArtifactId("p1");
        p1.setActiveSubprojects(List.of());
        Map<Project, List<Project>> projects = Map.of(p1, List.of());

        BuildPlan plan = calculateLifecycleMappings(projects, "generate-resources");
        assertNotNull(plan);
    }

    @Test
    void testAllPhase() {
        ProjectStub c1 = new ProjectStub();
        c1.setArtifactId("c1");
        c1.setActiveSubprojects(List.of());
        ProjectStub c2 = new ProjectStub();
        c2.setArtifactId("c2");
        c2.setActiveSubprojects(List.of());
        ProjectStub p = new ProjectStub();
        p.setArtifactId("p");
        p.setActiveSubprojects(List.of(c1, c2));
        Map<Project, List<Project>> projects = Map.of(p, List.of(), c1, List.of(), c2, List.of());

        BuildPlan plan = calculateLifecycleMappings(projects, "all");
        assertNotNull(plan);
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(p, "before:each"));
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(c1, "before:all"));
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(c2, "before:all"));
        assertIsSuccessor(plan.requiredStep(c1, "after:all"), plan.requiredStep(p, "after:all"));
        assertIsSuccessor(plan.requiredStep(c2, "after:all"), plan.requiredStep(p, "after:all"));
    }

    private void assertIsSuccessor(BuildStep predecessor, BuildStep successor) {
        assertTrue(
                successor.isSuccessorOf(predecessor),
                String.format("Expected '%s' to be a successor of '%s'", successor.toString(), predecessor.toString()));
    }

    @SuppressWarnings("checkstyle:UnusedLocalVariable")
    private BuildPlan calculateLifecycleMappings(Map<Project, List<Project>> projects, String phase) {
        DefaultLifecycleRegistry lifecycles = new DefaultLifecycleRegistry(List.of());
        BuildPlanExecutor builder = new BuildPlanExecutor(null, null, null, null, null, null, null, null, lifecycles);
        BuildPlanExecutor.BuildContext context = builder.new BuildContext();
        return context.calculateLifecycleMappings(projects, phase);
    }

    /*
    @Test
    void testPlugins() {
        DefaultLifecycleRegistry lifecycles =
                new DefaultLifecycleRegistry(Collections.emptyList(), Collections.emptyMap());
        BuildPlanCreator builder = new BuildPlanCreator(null, null, null, null, null, lifecycles);
        ProjectStub p1 = new ProjectStub();
        p1.setGroupId("g");
        p1.setArtifactId("p1");
        p1.getBuild().getPlugins().add(new Plugin(org.apache.maven.api.model.Plugin.newBuilder()
                .groupId("g").artifactId("p2")
                .
                .build()))
        ProjectStub p2 = new ProjectStub();
        p2.setGroupId("g");
        p2.setArtifactId("p2");

        Map<ProjectStub, List<ProjectStub>> projects = new HashMap<>();
        projects.put(p1, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p1));
        Lifecycle lifecycle = lifecycles.require("default");
        BuildPlan plan = builder.calculateLifecycleMappings(null, projects, lifecycle, "verify");
        plan.then(builder.calculateLifecycleMappings(null, projects, lifecycle, "install"));

        Stream.of(p1, p2).forEach(project -> {
            plan.requiredStep(project, "post:resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "post:test-resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test-compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "package").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "install").addMojo(new MojoExecution(null), 0);
        });

        plan.condense();

        new BuildPlanLogger() {
            @Override
            protected void mojo(Consumer<String> writer, MojoExecution mojoExecution) {}
        }.writePlan(System.out::println, plan);

        plan.allSteps().forEach(phase -> {
            phase.predecessors.forEach(
                    pred -> assertTrue(plan.step(pred.project, pred.name).isPresent(), "Phase not present: " + pred));
        });
    }
     */
}
