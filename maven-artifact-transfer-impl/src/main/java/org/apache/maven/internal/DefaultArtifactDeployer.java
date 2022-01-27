package org.apache.maven.internal;

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

import org.apache.maven.api.artifact.ArtifactMetadata;
import org.apache.maven.services.artifact.deploy.ArtifactDeployer;
import org.apache.maven.services.artifact.deploy.ArtifactDeployerException;
import org.apache.maven.services.artifact.deploy.ArtifactDeployerRequest;
import org.apache.maven.api.repository.Repository;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

/**
 *
 */
@Named
class DefaultArtifactDeployer extends BaseService implements ArtifactDeployer
{

    @Inject
    DefaultArtifactDeployer( RepositorySystem repositorySystem )
    {
        super( repositorySystem );
    }

    @Override
    public void deploy( ArtifactDeployerRequest request )
            throws ArtifactDeployerException
    {
        nonNull( request, "request" );
        nonNull( request.getSession(), "session" );
        nonNull( request.getRepository(), "repository" );

        RepositorySystemSession session = session( request );

        // prepare request
        DeployRequest deployRequest = new DeployRequest();

        Repository remoteRepository = request.getRepository();
        RemoteRepository aetherRepository = getRemoteRepository( session, remoteRepository );
        deployRequest.setRepository( aetherRepository );

        // transform artifacts
        Collection<org.apache.maven.api.artifact.Artifact> mavenArtifacts = request.getArtifacts();
        for ( org.apache.maven.api.artifact.Artifact mavenArtifact : mavenArtifacts )
        {
            Artifact aetherArtifact = RepositoryUtils.toArtifact( mavenArtifact );
            deployRequest.addArtifact( aetherArtifact );


            for ( ArtifactMetadata metadata : mavenArtifact.getMetadatas() )
            {
                if ( metadata instanceof ProjectArtifactMetadata )
                {
                    Artifact pomArtifact = new SubArtifact( aetherArtifact, "", "pom" );
                    pomArtifact = pomArtifact.setFile( ( (ProjectArtifactMetadata) metadata ).getFile() );
                    deployRequest.addArtifact( pomArtifact );
//                }
//                else if ( // metadata instanceof SnapshotArtifactRepositoryMetadata ||
//                        metadata instanceof ArtifactRepositoryMetadata )
//                {
                    // eaten, handled by repo system
                }
                else if ( metadata instanceof org.apache.maven.api.metadata.ArtifactMetadata )
                {
                    org.apache.maven.api.metadata.ArtifactMetadata transferMetadata =
                            (org.apache.maven.api.metadata.ArtifactMetadata) metadata;

                    deployRequest.addMetadata(
                            new DefaultMetadataBridge( metadata ).setFile( transferMetadata.getFile() ) );
                }
            }
        }

        // deploy
        try
        {
            repositorySystem.deploy( session, deployRequest );
        }
        catch ( DeploymentException e )
        {
            throw new ArtifactDeployerException( e.getMessage(), e );
        }
    }

    private RemoteRepository getRemoteRepository( RepositorySystemSession session,
                                                  Repository remoteRepository )
    {
        RemoteRepository aetherRepo = RepositoryUtils.toRepo( remoteRepository );

        if ( aetherRepo.getAuthentication() == null || aetherRepo.getProxy() == null )
        {
            RemoteRepository.Builder builder = new RemoteRepository.Builder( aetherRepo );

            if ( aetherRepo.getAuthentication() == null )
            {
                builder.setAuthentication( session.getAuthenticationSelector().getAuthentication( aetherRepo ) );
            }

            if ( aetherRepo.getProxy() == null )
            {
                builder.setProxy( session.getProxySelector().getProxy( aetherRepo ) );
            }

            aetherRepo = builder.build();
        }

        return aetherRepo;
    }
}
