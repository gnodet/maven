package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.junit.jupiter.api.Test;

/**
 * Test sorting projects by dependencies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ProjectSorterTest
{
    private Parent createParent( MavenProject project )
    {
        return createParent( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Parent createParent( String groupId, String artifactId, String version )
    {
        return Parent.newBuilder()
                .groupId( groupId ).artifactId( artifactId ).version( version ).build();
    }

    private Dependency createDependency( MavenProject project )
    {
        return createDependency( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Dependency createDependency( String groupId, String artifactId, String version )
    {
        return Dependency.newBuilder()
                .groupId( groupId ).artifactId( artifactId ).version( version ).build();
    }

    private Plugin createPlugin( MavenProject project )
    {
        return createPlugin( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private Plugin createPlugin( String groupId, String artifactId, String version )
    {
        return Plugin.newBuilder()
                .groupId( groupId ).artifactId( artifactId ).version( version ).build();
    }

    private Extension createExtension( String groupId, String artifactId, String version )
    {
        return Extension.newBuilder()
                .groupId( groupId ).artifactId( artifactId ).version( version ).build();
    }

    private static MavenProject createProject( String groupId, String artifactId, String version )
    {
        return new MavenProject( Model.newBuilder()
                .groupId( groupId ).artifactId( artifactId ).version( version )
                .build( Build.newInstance() )
                .build() );
    }

    @Test
    public void testShouldNotFailWhenPluginDepReferencesCurrentProject()
        throws Exception
    {
        MavenProject project = createProject( "group", "artifact", "1.0" );

        Build build = project.getBuild();

        Plugin plugin = createPlugin( "other.group", "other-artifact", "1.0" );

        Dependency dep = createDependency( "group", "artifact", "1.0" );

        plugin = plugin.withDependencies( Collections.singletonList( dep ) );

        project.setBuild( build.withPlugins( Collections.singletonList( plugin ) ) );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    @Test
    public void testShouldNotFailWhenManagedPluginDepReferencesCurrentProject()
        throws Exception
    {
        Dependency dep = createDependency( "group", "artifact", "1.0" );

        Plugin plugin = createPlugin( "other.group", "other-artifact", "1.0" )
                .withDependencies( Collections.singletonList( dep ) );

        PluginManagement pMgt = PluginManagement.newBuilder()
                        .plugins( Collections.singletonList( plugin ) ).build();

        MavenProject project = createProject( "group", "artifact", "1.0" );
        project.setBuild( project.getBuild().withPluginManagement( pMgt ) );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    @Test
    public void testShouldNotFailWhenProjectReferencesNonExistentProject()
        throws Exception
    {
        Extension extension = createExtension( "other.group", "other-artifact", "1.0" );

        MavenProject project = createProject( "group", "artifact", "1.0" );
        project.setBuild( project.getBuild().withExtensions( Collections.singletonList( extension ) ) );

        new ProjectSorter( Collections.singletonList( project ) );
    }

    @Test
    public void testMatchingArtifactIdsDifferentGroupIds()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();
        MavenProject project1 = createProject( "groupId1", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId2", "artifactId", "1.0" );
        projects.add( project2 );
        project1.setDependencies( Collections.singletonList( createDependency( project2 ) ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    @Test
    public void testMatchingGroupIdsDifferentArtifactIds()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();
        MavenProject project1 = createProject( "groupId", "artifactId1", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId2", "1.0" );
        projects.add( project2 );
        project1.setDependencies( Collections.singletonList( createDependency( project2 ) ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( project2, projects.get( 0 ) );
        assertEquals( project1, projects.get( 1 ) );
    }

    @Test
    public void testMatchingIdsAndVersions()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();
        MavenProject project1 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project2 );

        assertThrows(
                DuplicateProjectException.class,
                () -> new ProjectSorter( projects ).getSortedProjects(),
                "Duplicate projects should fail" );
    }

    @Test
    public void testMatchingIdsAndDifferentVersions()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();
        MavenProject project1 = createProject( "groupId", "artifactId", "1.0" );
        projects.add( project1 );
        MavenProject project2 = createProject( "groupId", "artifactId", "2.0" );
        projects.add( project2 );

        projects = new ProjectSorter( projects ).getSortedProjects();
        assertEquals( project1, projects.get( 0 ) );
        assertEquals( project2, projects.get( 1 ) );
    }

    @Test
    public void testPluginDependenciesInfluenceSorting()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject parentProject = createProject( "groupId", "parent", "1.0" );
        projects.add( parentProject );

        MavenProject declaringProject = createProject( "groupId", "declarer", "1.0" );
        declaringProject.setParent( parentProject );
        declaringProject.setModel( declaringProject.getModel().withParent( createParent( parentProject ) ) );
        projects.add( declaringProject );

        MavenProject pluginLevelDepProject = createProject( "groupId", "plugin-level-dep", "1.0" );
        pluginLevelDepProject.setParent( parentProject );
        pluginLevelDepProject.setModel( pluginLevelDepProject.getModel().withParent( createParent( parentProject ) ) );
        projects.add( pluginLevelDepProject );

        MavenProject pluginProject = createProject( "groupId", "plugin", "1.0" );
        pluginProject.setParent( parentProject );
        pluginProject.setModel( pluginProject.getModel().withParent( createParent( parentProject ) ) );
        projects.add( pluginProject );

        Plugin plugin = createPlugin( pluginProject )
                .withDependencies( Collections.singletonList( createDependency( pluginLevelDepProject ) ) );

        declaringProject.setBuild( declaringProject.getBuild().withPlugins( Collections.singletonList( plugin ) ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( parentProject, projects.get( 0 ) );

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertThat( projects, hasItem( pluginProject ) );
        assertThat( projects, hasItem( pluginLevelDepProject ) );

        // the declaring project MUST be listed after the plugin and its plugin-level dep, though.
        assertEquals( declaringProject, projects.get( 3 ) );
    }

    @Test
    public void testPluginDependenciesInfluenceSorting_DeclarationInParent()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject parentProject = createProject( "groupId", "parent-declarer", "1.0" );
        projects.add( parentProject );

        MavenProject pluginProject = createProject( "groupId", "plugin", "1.0" );
        pluginProject.setParent( parentProject );
        pluginProject.setModel( pluginProject.getModel().withParent( createParent( parentProject ) ) );
        projects.add( pluginProject );

        MavenProject pluginLevelDepProject = createProject( "groupId", "plugin-level-dep", "1.0" );
        pluginLevelDepProject.setParent( parentProject );
        pluginLevelDepProject.setModel( pluginLevelDepProject.getModel().withParent( createParent( parentProject ) ) );
        projects.add( pluginLevelDepProject );

        Plugin plugin = createPlugin( pluginProject )
                .withDependencies( Collections.singletonList( createDependency( pluginLevelDepProject ) ) );

        parentProject.setBuild( parentProject.getBuild().withPlugins( Collections.singletonList( plugin ) ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( parentProject, projects.get( 0 ) );

        // the order of these two is non-deterministic, based on when they're added to the reactor.
        assertThat( projects, hasItem( pluginProject ) );
        assertThat( projects, hasItem( pluginLevelDepProject ) );
    }

    @Test
    public void testPluginVersionsAreConsidered()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject pluginProjectA = createProject( "group", "plugin-a", "2.0-SNAPSHOT" );
        projects.add( pluginProjectA );
        pluginProjectA.setBuild( pluginProjectA.getBuild()
                .withPlugins( Collections.singletonList( createPlugin( "group", "plugin-b", "1.0" ) ) ) );

        MavenProject pluginProjectB = createProject( "group", "plugin-b", "2.0-SNAPSHOT" );
        projects.add( pluginProjectB );
        pluginProjectB.setBuild( pluginProjectB.getBuild()
                .withPlugins( Collections.singletonList( createPlugin( "group", "plugin-a", "1.0" ) ) ) );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertThat( projects, hasItem( pluginProjectA ) );
        assertThat( projects, hasItem( pluginProjectB ) );
    }

    @Test
    public void testDependencyPrecedesProjectThatUsesSpecificDependencyVersion()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject usingProject = createProject( "group", "project", "1.0" );
        projects.add( usingProject );
        usingProject.setModel( usingProject.getModel()
                .withDependencies( Collections.singletonList( createDependency( "group", "dependency", "1.0" ) ) ) );

        MavenProject pluginProject = createProject( "group", "dependency", "1.0" );
        projects.add( pluginProject );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( pluginProject, projects.get( 0 ) );
        assertEquals( usingProject, projects.get( 1 ) );
    }

    @Test
    public void testDependencyPrecedesProjectThatUsesUnresolvedDependencyVersion()
        throws Exception
    {
        List<MavenProject> projects = new ArrayList<>();

        MavenProject usingProject = createProject( "group", "project", "1.0" );
        projects.add( usingProject );
        usingProject.setModel( usingProject.getModel()
                .withDependencies( Collections.singletonList( createDependency( "group", "dependency", "[1.0,)" ) ) ) );

        MavenProject pluginProject = createProject( "group", "dependency", "1.0" );
        projects.add( pluginProject );

        projects = new ProjectSorter( projects ).getSortedProjects();

        assertEquals( pluginProject, projects.get( 0 ) );
        assertEquals( usingProject, projects.get( 1 ) );
    }

}
