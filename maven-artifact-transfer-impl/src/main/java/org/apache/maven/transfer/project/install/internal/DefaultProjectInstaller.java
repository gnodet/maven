package org.apache.maven.transfer.project.install.internal;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.transfer.artifact.install.ArtifactInstaller;
import org.apache.maven.transfer.artifact.install.ArtifactInstallerException;
import org.apache.maven.transfer.artifact.install.ArtifactInstallerRequest;
import org.apache.maven.transfer.internal.BaseService;
import org.apache.maven.transfer.project.NoFileAssignedException;
import org.apache.maven.transfer.project.install.ProjectInstaller;
import org.apache.maven.transfer.project.install.ProjectInstallerRequest;
import org.apache.maven.transfer.repository.RepositoryManager;
import org.eclipse.aether.RepositorySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Named
public class DefaultProjectInstaller extends BaseService
        implements ProjectInstaller
{

    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultProjectInstaller.class );

    private final ArtifactInstaller installer;

    @Inject
    DefaultProjectInstaller( RepositorySystem repositorySystem, RepositoryManager repositoryManager,
                             ArtifactInstaller installer )
    {
        super( repositorySystem, repositoryManager );
        this.installer = Objects.requireNonNull( installer );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install( ProjectInstallerRequest installerRequest )
        throws ArtifactInstallerException, NoFileAssignedException, IllegalArgumentException
    {

        validateParameters( installerRequest );
        MavenProject project = installerRequest.getProject();

        Artifact artifact = project.getArtifact();
        String packaging = project.getPackaging();
        File pomFile = project.getFile();

        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        // TODO: push into transformation
        boolean isPomArtifact = "pom".equals( packaging );

        ProjectArtifactMetadata metadata;

        Collection<File> metadataFiles = new LinkedHashSet<>();

        ArtifactInstallerRequest request = new ArtifactInstallerRequest()
                .setSession( installerRequest.getSession() )
                .setLocalRepository( installerRequest.getLocalRepository() )
                .setRepositories( installerRequest.getRepositories() );

        if ( isPomArtifact )
        {
            if ( pomFile != null )
            {
                request.addArtifact( new ProjectArtifact( project ) );
            }
        }
        else
        {
            if ( pomFile != null )
            {
                metadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( metadata );
            }

            File file = artifact.getFile();

            // Here, we have a temporary solution to MINSTALL-3 (isDirectory() is true if it went through compile
            // but not package). We are designing in a proper solution for Maven 2.1
            if ( file != null && file.isFile() )
            {
                request.addArtifact( artifact );
            }
            else if ( !attachedArtifacts.isEmpty() )
            {
                throw new NoFileAssignedException( "The packaging plugin for this project did not assign "
                    + "a main file to the project but it has attachments. Change packaging to 'pom'." );
            }
            else
            {
                throw new NoFileAssignedException(
                        "The packaging for this project did not assign a file to the build artifact" );
            }
        }

        for ( Artifact attached : attachedArtifacts )
        {
            LOGGER.debug( "Installing artifact: {}", attached.getId() );
            request.addArtifact( attached );
        }

        installer.install( request );
    }

    private void validateParameters( ProjectInstallerRequest installerRequest )
    {
        if ( installerRequest == null )
        {
            throw new IllegalArgumentException( "The parameter installerRequest is not allowed to be null." );
        }
    }

}
