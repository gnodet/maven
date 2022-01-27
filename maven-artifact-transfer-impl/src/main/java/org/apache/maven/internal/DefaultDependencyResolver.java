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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Model;
import org.apache.maven.artifact.filter.resolve.transform.EclipseAetherFilterTransformer;
import org.apache.maven.api.dependencies.DependableCoordinate;
import org.apache.maven.services.dependencies.resolve.DependencyResolver;
import org.apache.maven.services.dependencies.resolve.DependencyResolverException;
import org.apache.maven.services.dependencies.resolve.DependencyResolverRequest;
import org.apache.maven.services.dependencies.resolve.DependencyResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;

/**
 *
 */
@Named
class DefaultDependencyResolver extends BaseService
        implements DependencyResolver
{

    private final ArtifactHandlerManager artifactHandlerManager;

    @Inject
    DefaultDependencyResolver( RepositorySystem repositorySystem,
                               ArtifactHandlerManager artifactHandlerManager )
    {
        super( repositorySystem );
        this.artifactHandlerManager = Objects.requireNonNull( artifactHandlerManager );
    }

    @Override
    public DependencyResolverResult resolveDependencies(
                    DependencyResolverRequest request ) throws DependencyResolverException
    {
        ArtifactTypeRegistry typeRegistry = RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager );

        CollectRequest collectRequest = new CollectRequest();

        if ( request.getRootArtifact() != null )
        {
            collectRequest.setRootArtifact( RepositoryUtils.toArtifact( request.getRootArtifact() ) );
        }
        else if ( request.getRootDependency() != null )
        {
            collectRequest.setRoot( RepositoryUtils.toDependency( request.getRootDependency(), typeRegistry ) );
        }
        else if ( request.getRootCoordinate() != null )
        {
            DependableCoordinate root = request.getRootCoordinate();
            ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( root.getType() );
            String extension = artifactHandler != null ? artifactHandler.getExtension() : null;
            Artifact aetherArtifact = new DefaultArtifact( root.getGroupId(), root.getArtifactId(),
                    root.getClassifier(), extension, root.getVersion() );
            collectRequest.setRootArtifact( aetherArtifact );
        }
        else if ( request.getRootModel() != null )
        {
            Model root = request.getRootModel();
            ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( root.getPackaging() );
            String extension = artifactHandler != null ? artifactHandler.getExtension() : null;
            Artifact aetherArtifact = new DefaultArtifact( root.getGroupId(), root.getArtifactId(),
                    extension, "", root.getVersion() );
            collectRequest.setRootArtifact( aetherArtifact );
            for ( org.apache.maven.model.Dependency mavenDependency : root.getDependencies() )
            {
                collectRequest.addDependency( RepositoryUtils.toDependency( mavenDependency, typeRegistry ) );
            }
            if ( root.getDependencyManagement() != null )
            {
                for ( org.apache.maven.model.Dependency mavenDependency
                            : root.getDependencyManagement().getDependencies() )
                {
                    collectRequest.addManagedDependency(
                            RepositoryUtils.toDependency( mavenDependency, typeRegistry ) );
                }
            }
        }
        else
        {
            throw new IllegalArgumentException();
        }
        for ( org.apache.maven.model.Dependency mavenDependency : request.getDependencies() )
        {
            collectRequest.addDependency( RepositoryUtils.toDependency( mavenDependency, typeRegistry ) );
        }
        for ( org.apache.maven.model.Dependency mavenDependency : request.getManagedDependencies() )
        {
            collectRequest.addManagedDependency( RepositoryUtils.toDependency( mavenDependency, typeRegistry ) );
        }
        // Repositories to use
        collectRequest.setRepositories( repositories( request ) );
        // Filter
        DependencyFilter depFilter = null;
        if ( request.getFilter() != null )
        {
            depFilter = request.getFilter().transform( new EclipseAetherFilterTransformer() );
        }
        DependencyRequest depRequest = new DependencyRequest( collectRequest, depFilter );

        try
        {
            RepositorySystemSession session = session( request );

            DependencyResult dependencyResults = repositorySystem.resolveDependencies( session, depRequest );

            return new DefaultDependencyResolverResult( dependencyResults );
        }
        catch ( DependencyResolutionException e )
        {
            throw new DefaultDependencyResolverException( e );
        }
    }
}
