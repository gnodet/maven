package org.apache.maven.transfer.artifact.install.internal;

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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.apache.maven.transfer.artifact.install.ArtifactInstaller;
import org.apache.maven.transfer.artifact.install.ArtifactInstallerException;
import org.apache.maven.transfer.artifact.install.ArtifactInstallerRequest;
import org.apache.maven.transfer.internal.BaseService;
import org.apache.maven.transfer.metadata.internal.DefaultMetadataBridge;
import org.apache.maven.transfer.repository.RepositoryManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;

/**
 *
 */
@Singleton
@Named
public class DefaultArtifactInstaller
        extends BaseService
        implements ArtifactInstaller
{
    @Inject
    public DefaultArtifactInstaller( RepositorySystem repositorySystem,
                                     RepositoryManager repositoryManager )
    {
        super( repositorySystem, repositoryManager );
    }

    @Override
    public void install( ArtifactInstallerRequest installerRequest )
            throws ArtifactInstallerException
    {
        Collection<org.apache.maven.artifact.Artifact> mavenArtifacts = installerRequest.getArtifacts();

        // prepare installRequest
        InstallRequest request = new InstallRequest();

        // transform artifacts
        for ( org.apache.maven.artifact.Artifact mavenArtifact : mavenArtifacts )
        {
            Artifact mainArtifact = RepositoryUtils.toArtifact( mavenArtifact );
            request.addArtifact( mainArtifact );

            for ( ArtifactMetadata metadata : mavenArtifact.getMetadataList() )
            {
                if ( metadata instanceof ProjectArtifactMetadata )
                {
                    Artifact pomArtifact = new SubArtifact( mainArtifact, "", "pom" );
                    pomArtifact = pomArtifact.setFile( ( (ProjectArtifactMetadata) metadata ).getFile() );
                    request.addArtifact( pomArtifact );
                }
                else if ( // metadata instanceof SnapshotArtifactRepositoryMetadata ||
                        metadata instanceof ArtifactRepositoryMetadata )
                {
                    // eaten, handled by repo system
                }
                else if ( metadata instanceof org.apache.maven.transfer.metadata.ArtifactMetadata )
                {
                    org.apache.maven.transfer.metadata.ArtifactMetadata transferMetadata =
                            (org.apache.maven.transfer.metadata.ArtifactMetadata) metadata;

                    request.addMetadata( new DefaultMetadataBridge( metadata ).setFile( transferMetadata.getFile() ) );
                }
            }
        }

        // install
        try
        {
            RepositorySystemSession session = session( installerRequest );

            repositorySystem.install( session, request );
        }
        catch ( InstallationException e )
        {
            throw new ArtifactInstallerException( e.getMessage(), e );
        }
    }
}
