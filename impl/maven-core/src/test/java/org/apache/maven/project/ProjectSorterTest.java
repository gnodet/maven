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
package org.apache.maven.project;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.api.Project;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.graph.ProjectStub;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test sorting projects by dependencies.
 *
 */
class ProjectSorterTest {
    @Test
    void testShouldNotFailWhenPluginDepReferencesCurrentProject() throws Exception {
        ProjectStub project = createProject("group", "artifact", "1.0");
        Dependency dep = createDependency("group", "artifact", "1.0");
        Plugin plugin = createPlugin("other.group", "other-artifact", "1.0")
                .withDependencies(List.of(dep));
        project.setModel(project.getModel().withBuild(project.getModel().getBuild().withPlugins(List.of(plugin))));

        new ProjectSorter(Collections.singletonList(project));
    }

    @Test
    void testShouldNotFailWhenManagedPluginDepReferencesCurrentProject() throws Exception {
        Dependency dep = createDependency("group", "artifact", "1.0");
        Plugin plugin = createPlugin("other.group", "other-artifact", "1.0").withDependencies(List.of(dep));

        ProjectStub project = createProject("group", "artifact", "1.0");
        project.setModel(project.getModel().withBuild(project.getModel().getBuild().withPluginManagement(
                PluginManagement.newBuilder().plugins(List.of(plugin)).build())));

        new ProjectSorter(Collections.singletonList(project));
    }

    @Test
    void testShouldNotFailWhenProjectReferencesNonExistentProject() throws Exception {
        Extension extension = createExtension("other.group", "other-artifact", "1.0");

        ProjectStub project = createProject("group", "artifact", "1.0");
        project.setModel(project.getModel().withBuild(project.getModel().getBuild().withExtensions(List.of(extension))));

        new ProjectSorter(Collections.singletonList(project));
    }

    @Test
    void testMatchingArtifactIdsDifferentGroupIds() throws Exception {
        List<Project> projects = new ArrayList<>();
        ProjectStub project1 = createProject("groupId1", "artifactId", "1.0");
        projects.add(project1);
        Project project2 = createProject("groupId2", "artifactId", "1.0");
        projects.add(project2);
        addDependency(project1, createDependency(project2));

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(project2, projects.get(0));
        assertEquals(project1, projects.get(1));
    }

    @Test
    void testMatchingGroupIdsDifferentArtifactIds() throws Exception {
        List<Project> projects = new ArrayList<>();
        ProjectStub project1 = createProject("groupId", "artifactId1", "1.0");
        projects.add(project1);
        Project project2 = createProject("groupId", "artifactId2", "1.0");
        projects.add(project2);
        addDependency(project1, createDependency(project2));

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(project2, projects.get(0));
        assertEquals(project1, projects.get(1));
    }

    @Test
    void testMatchingIdsAndVersions() throws Exception {
        List<Project> projects = new ArrayList<>();
        Project project1 = createProject("groupId", "artifactId", "1.0");
        projects.add(project1);
        Project project2 = createProject("groupId", "artifactId", "1.0");
        projects.add(project2);

        assertThrows(
                DuplicateProjectException.class,
                () -> new ProjectSorter(projects).getSortedProjects(),
                "Duplicate projects should fail");
    }

    @Test
    void testMatchingIdsAndDifferentVersions() throws Exception {
        List<Project> projects = new ArrayList<>();
        Project project1 = createProject("groupId", "artifactId", "1.0");
        projects.add(project1);
        Project project2 = createProject("groupId", "artifactId", "2.0");
        projects.add(project2);

        projects = new ProjectSorter(projects).getSortedProjects();
        assertEquals(project1, projects.get(0));
        assertEquals(project2, projects.get(1));
    }

    @Test
    void testPluginDependenciesInfluenceSorting() throws Exception {
        List<Project> projects = new ArrayList<>();

        ProjectStub parentProject = createProject("groupId", "parent", "1.0");
        projects.add(parentProject);

        ProjectStub declaringProject = createProject("groupId", "declarer", "1.0", parentProject);
        projects.add(declaringProject);

        ProjectStub pluginLevelDepProject = createProject("groupId", "plugin-level-dep", "1.0", parentProject);
        projects.add(pluginLevelDepProject);

        ProjectStub pluginProject = createProject("groupId", "plugin", "1.0", parentProject);
        projects.add(pluginProject);

        Plugin plugin = createPlugin(pluginProject).withDependencies(List.of(createDependency(pluginLevelDepProject)));
        addPlugin(declaringProject, plugin);

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(parentProject, projects.get(0));

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertThat(projects, hasItem(pluginProject));
        assertThat(projects, hasItem(pluginLevelDepProject));

        // the declaring project MUST be listed after the plugin and its plugin-level dep, though.
        assertEquals(declaringProject, projects.get(3));
    }

    @Test
    void testPluginDependenciesInfluenceSortingDeclarationInParent() throws Exception {
        List<Project> projects = new ArrayList<>();

        ProjectStub parentProject = createProject("groupId", "parent-declarer", "1.0");
        projects.add(parentProject);

        ProjectStub pluginProject = createProject("groupId", "plugin", "1.0", parentProject);
        projects.add(pluginProject);

        ProjectStub pluginLevelDepProject = createProject("groupId", "plugin-level-dep", "1.0", parentProject);
        projects.add(pluginLevelDepProject);

        Plugin plugin = createPlugin(pluginProject).withDependencies(List.of(createDependency(pluginLevelDepProject)));
        addPlugin(parentProject, plugin);

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(parentProject, projects.get(0));

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertThat(projects, hasItem(pluginProject));
        assertThat(projects, hasItem(pluginLevelDepProject));
    }

    @Test
    void testPluginVersionsAreConsidered() throws Exception {
        List<Project> projects = new ArrayList<>();

        ProjectStub pluginProjectA = createProject("group", "plugin-a", "2.0-SNAPSHOT");
        projects.add(pluginProjectA);
        addPlugin(pluginProjectA, createPlugin("group", "plugin-b", "1.0"));

        ProjectStub pluginProjectB = createProject("group", "plugin-b", "2.0-SNAPSHOT");
        projects.add(pluginProjectB);
        addPlugin(pluginProjectB, createPlugin("group", "plugin-a", "1.0"));

        projects = new ProjectSorter(projects).getSortedProjects();

        assertThat(projects, hasItem(pluginProjectA));
        assertThat(projects, hasItem(pluginProjectB));
    }

    @Test
    void testDependencyPrecedesProjectThatUsesSpecificDependencyVersion() throws Exception {
        List<Project> projects = new ArrayList<>();

        ProjectStub usingProject = createProject("group", "project", "1.0");
        projects.add(usingProject);
        addDependency(usingProject, createDependency("group", "dependency", "1.0"));

        Project pluginProject = createProject("group", "dependency", "1.0");
        projects.add(pluginProject);

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(pluginProject, projects.get(0));
        assertEquals(usingProject, projects.get(1));
    }

    @Test
    void testDependencyPrecedesProjectThatUsesUnresolvedDependencyVersion() throws Exception {
        List<Project> projects = new ArrayList<>();

        ProjectStub usingProject = createProject("group", "project", "1.0");
        projects.add(usingProject);
        addDependency(usingProject, createDependency("group", "dependency", "[1.0,)"));

        Project pluginProject = createProject("group", "dependency", "1.0");
        projects.add(pluginProject);

        projects = new ProjectSorter(projects).getSortedProjects();

        assertEquals(pluginProject, projects.get(0));
        assertEquals(usingProject, projects.get(1));
    }

    private void addDependency(ProjectStub project, Dependency dependency) {
        project.setModel(project.getModel().withDependencies(
                Stream.concat(project.getModel().getDependencies().stream(), Stream.of(dependency)).toList()));
    }

    private void addPlugin(ProjectStub project, Plugin plugin) {
        project.setModel(project.getModel().withBuild(
                project.getModel().getBuild().withPlugins(
                Stream.concat(project.getModel().getBuild().getPlugins().stream(), Stream.of(plugin)).toList())));
    }

    private Parent createParent(Project project) {
        return createParent(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private Parent createParent(String groupId, String artifactId, String version) {
        return Parent.newBuilder().groupId(groupId).artifactId(artifactId).version(version).build();
    }

    private Dependency createDependency(Project project) {
        return createDependency(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private Dependency createDependency(String groupId, String artifactId, String version) {
        return Dependency
                .newBuilder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version).build();
    }

    private Plugin createPlugin(Project project) {
        return createPlugin(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private Plugin createPlugin(String groupId, String artifactId, String version) {
        return Plugin.newBuilder()
                .groupId(groupId)
                .artifactId(artifactId).version(version).build();
    }

    private Extension createExtension(String groupId, String artifactId, String version) {
        return Extension.newBuilder().groupId(groupId).artifactId(artifactId).version(version).build();
    }

    private ProjectStub createProject(String groupId, String artifactId, String version) {
        ProjectStub project = new ProjectStub();
        project.setModel(Model.newBuilder().groupId(groupId).artifactId(artifactId).version(version).build(Build.newInstance()).build());
        project.setPomPath(Paths.get(artifactId, "pom.xml"));
        return project;
    }

    private ProjectStub createProject(String groupId, String artifactId, String version, Project parent) {
        ProjectStub project = new ProjectStub();
        project.setModel(Model.newBuilder()
                        .parent(createParent(parent))
                .groupId(groupId).artifactId(artifactId).version(version).build(Build.newInstance()).build());
        return project;
    }
}
