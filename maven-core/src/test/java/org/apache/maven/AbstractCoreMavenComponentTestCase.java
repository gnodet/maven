package org.apache.maven;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

import javax.inject.Inject;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;

@PlexusTest
public abstract class AbstractCoreMavenComponentTestCase
{

    @Inject
    protected PlexusContainer container;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected org.apache.maven.project.ProjectBuilder projectBuilder;

    abstract protected String getProjectsDirectory();

    protected PlexusContainer getContainer() {
        return container;
    }

    protected File getProject(String name )
        throws Exception
    {
        File source = new File( new File( getBasedir(), getProjectsDirectory() ), name );
        File target = new File( new File( getBasedir(), "target" ), name );
        FileUtils.copyDirectoryStructureIfModified( source, target );
        return new File( target, "pom.xml" );
    }

    protected MavenExecutionRequest createMavenExecutionRequest( File pom )
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPom( pom )
            .setProjectPresent( true )
            .setShowErrors( true )
            .setPluginGroups( Arrays.asList( "org.apache.maven.plugins" ) )
            .setLocalRepository( getLocalRepository() )
            .setRemoteRepositories( getRemoteRepositories() )
            .setPluginArtifactRepositories( getPluginArtifactRepositories() )
            .setGoals( Arrays.asList( "package" ) );

        if ( pom != null )
        {
            request.setMultiModuleProjectDirectory( pom.getParentFile() );
        }

        return request;
    }

    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession( File pom )
        throws Exception
    {
        return createMavenSession( pom, new Properties() );
    }

    protected MavenSession createMavenSession( File pom, Properties executionProperties )
                    throws Exception
    {
        return createMavenSession( pom, executionProperties, false );
    }

    protected MavenSession createMavenSession( File pom, Properties executionProperties, boolean includeModules )
        throws Exception
    {
        MavenExecutionRequest request = createMavenExecutionRequest( pom );

        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest()
            .setLocalRepository( request.getLocalRepository() )
            .setRemoteRepositories( request.getRemoteRepositories() )
            .setPluginArtifactRepositories( request.getPluginArtifactRepositories() )
            .setSystemProperties( executionProperties )
            .setUserProperties( new Properties() );

        List<MavenProject> projects = new ArrayList<>();

        if ( pom != null )
        {
            MavenProject project = projectBuilder.build( pom, configuration ).getProject();

            projects.add( project );
            if ( includeModules )
            {
                for( String module : project.getModules() )
                {
                    File modulePom = new File( pom.getParentFile(), module );
                    if( modulePom.isDirectory() )
                    {
                        modulePom = new File( modulePom, "pom.xml" );
                    }
                    projects.add( projectBuilder.build( modulePom, configuration ).getProject() );
                }
            }
        }
        else
        {
            MavenProject project = createStubMavenProject();
            project.setRemoteArtifactRepositories( request.getRemoteRepositories() );
            project.setPluginArtifactRepositories( request.getPluginArtifactRepositories() );
            projects.add( project );
        }

        initRepoSession( configuration );

        MavenSession session =
            new MavenSession( getContainer(), configuration.getRepositorySession(), request,
                              new DefaultMavenExecutionResult() );
        session.setProjects( projects );
        session.setAllProjects( session.getProjects() );

        return session;
    }

    protected void initRepoSession( ProjectBuildingRequest request )
        throws Exception
    {
        File localRepoDir = new File( request.getLocalRepository().getBasedir() );
        LocalRepository localRepo = new LocalRepository( localRepoDir );
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager( new SimpleLocalRepositoryManagerFactory().newInstance( session, localRepo ) );
        request.setRepositorySession( session );
    }

    protected MavenProject createStubMavenProject()
    {
        Model model = Model.newBuilder()
                .groupId( "org.apache.maven.test" )
                .artifactId( "maven-test" )
                .version( "1.0" )
                .build();
        return new MavenProject( model );
    }

    protected List<ArtifactRepository> getRemoteRepositories()
        throws InvalidRepositoryException
    {
        File repoDir = new File( getBasedir(), "src/test/remote-repo" ).getAbsoluteFile();

        RepositoryPolicy policy = RepositoryPolicy.newBuilder()
                        .enabled( "true" )
                        .checksumPolicy( "ignore" )
                        .updatePolicy( "always" )
                        .build();

        Repository repository = Repository.newBuilder()
                        .id( RepositorySystem.DEFAULT_REMOTE_REPO_ID )
                        .url( "file://" + repoDir.toURI().getPath() )
                        .releases( policy )
                        .snapshots( policy )
                        .build();

        return Arrays.asList( repositorySystem.buildArtifactRepository( repository ) );
    }

    protected List<ArtifactRepository> getPluginArtifactRepositories()
        throws InvalidRepositoryException
    {
        return getRemoteRepositories();
    }

    protected ArtifactRepository getLocalRepository()
        throws InvalidRepositoryException
    {
        File repoDir = new File( getBasedir(), "target/local-repo" ).getAbsoluteFile();

        return repositorySystem.createLocalRepository( repoDir );
    }

    protected class ProjectBuilder
    {
        private MavenProject project;

        public ProjectBuilder( MavenProject project )
        {
            this.project = project;
        }

        public ProjectBuilder( String groupId, String artifactId, String version )
        {
            Model model = Model.newBuilder()
                            .modelVersion( "4.0.0" )
                            .groupId( groupId )
                            .artifactId( artifactId )
                            .version( version )
                            .build( Build.newInstance() )
                            .build();
            project = new MavenProject( model );
        }

        public ProjectBuilder setGroupId( String groupId )
        {
            project.setGroupId( groupId );
            return this;
        }

        public ProjectBuilder setArtifactId( String artifactId )
        {
            project.setArtifactId( artifactId );
            return this;
        }

        public ProjectBuilder setVersion( String version )
        {
            project.setVersion( version );
            return this;
        }

        // Dependencies
        //
        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope )
        {
            return addDependency( groupId, artifactId, version, scope, (Exclusion)null );
        }

        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, Exclusion exclusion )
        {
            return addDependency( groupId, artifactId, version, scope, null, exclusion );
        }

        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath )
        {
            return addDependency( groupId, artifactId, version, scope, systemPath, null );
        }

        public ProjectBuilder addDependency( String groupId, String artifactId, String version, String scope, String systemPath, Exclusion exclusion )
        {
            Dependency d = Dependency.newBuilder()
                            .groupId( groupId )
                            .artifactId( artifactId )
                            .version( version )
                            .scope( scope )
                            .systemPath( scope.equals(  Artifact.SCOPE_SYSTEM ) ? systemPath : null )
                            .exclusions( exclusion != null ? Collections.singletonList( exclusion ) : null )
                            .build();

            List<Dependency> dependencies = new ArrayList<>( project.getDependencies() );
            dependencies.add( d );
            project.setDependencies( dependencies );

            return this;
        }

        // Plugins
        //
        public ProjectBuilder addPlugin( Plugin plugin )
        {
            project.getBuildPlugins().add( plugin );
            return this;
        }

        public MavenProject get()
        {
            return project;
        }
    }
}
