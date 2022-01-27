package org.apache.maven.services.dependencies.collect;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.services.BaseRequest;
import org.apache.maven.api.Session;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.api.dependencies.DependableCoordinate;

/**
 * A request to collect the transitive dependencies and to build a dependency graph from them. There are three ways to
 * create a dependency graph. First, only the root dependency can be given. Second, a root dependency and direct
 * dependencies can be specified in which case the specified direct dependencies are merged with the direct dependencies
 * retrieved from the artifact descriptor of the root dependency. And last, only direct dependencies can be specified in
 * which case the root node of the resulting graph has no associated dependency.
 *
 * @see DependencyCollector#collectDependencies(DependencyCollectorRequest)
 */
public class DependencyCollectorRequest extends BaseRequest<DependencyCollectorRequest>
{

    private Artifact rootArtifact;

    private Dependency rootDependency;

    private DependableCoordinate rootCoordinate;

    private Model rootModel;

    private List<Dependency> dependencies = Collections.emptyList();

    private List<Dependency> managedDependencies = Collections.emptyList();


    /**
     * Creates an uninitialized request.
     */
    public DependencyCollectorRequest()
    {
        // enables default constructor
    }

    /**
     * Creates a request with the specified properties.
     *
     * @param session {@link Session}
     * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code null}.
     */
    public DependencyCollectorRequest(Session session, Artifact rootArtifact )
    {
        setSession( session );
        setRootArtifact( rootArtifact );
    }

    /**
     * Creates a request with the specified properties.
     *
     * @param session the {@link Session} to use
     * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code null}.
     */
    public DependencyCollectorRequest(Session session, DependableCoordinate rootCoordinate )
    {
        setSession( session );
        setRootCoordinate( rootCoordinate );
    }

    /**
     * Creates a request with the specified properties.
     *
     * @param session the {@link Session} to use
     * @param rootDependency The root dependency whose transitive dependencies should be collected, may be {@code null}.
     */
    public DependencyCollectorRequest(Session session, Dependency rootDependency )
    {
        setSession( session );
        setRootDependency( rootDependency );
    }

    /**
     * Creates a request with the specified properties.
     *
     * @param session the {@link Session} to use
     * @param rootModel The root dependency whose transitive dependencies should be collected, may be {@code null}.
     * @param repositories The repositories to use for the collection, may be {@code null}.
     */
    public DependencyCollectorRequest(Session session, Model rootModel )
    {
        setSession( session );
        setRootModel( rootModel );
    }

    /**
     * Creates a new request with the specified properties.
     *
     * @param session the {@link Session} to use
     * @param rootDependency The root dependency whose transitive dependencies should be collected, may be {@code null}.
     * @param dependencies The direct dependencies to merge with the direct dependencies from the root dependency's
     *            artifact descriptor.
     */
    public DependencyCollectorRequest( Session session,
                                       Dependency rootDependency,
                                       List<Dependency> dependencies )
    {
        setSession( session );
        setRootDependency( rootDependency );
        setDependencies( dependencies );
    }

    /**
     * Creates a new request with the specified properties.
     *
     * @param session the {@link Session} to use
     * @param dependencies The direct dependencies of some imaginary root, may be {@code null}.
     * @param managedDependencies The dependency management information to apply to the transitive dependencies, may be
     *            {@code null}.
     */
    public DependencyCollectorRequest( Session session,
                                       List<Dependency> dependencies,
                                       List<Dependency> managedDependencies )
    {
        setSession( session );
        setDependencies( dependencies );
        setManagedDependencies( managedDependencies );
    }

    /**
     * Gets the root artifact for the dependency graph.
     * 
     * @return The root artifact for the dependency graph or {@code null} if none.
     */
    public Artifact getRootArtifact()
    {
        return rootArtifact;
    }

    /**
     * Sets the root artifact for the dependency graph. This must not be confused with
     * {@link #setRootDependency(Dependency)}: The root <em>dependency</em>, like any other specified dependency, will
     * be subject to dependency collection/resolution, i.e. should have an artifact descriptor and a corresponding
     * artifact file. The root <em>artifact</em> on the other hand is only used as a label for the root node of the
     * graph in case no root dependency was specified. As such, the configured root artifact is ignored if
     * {@link #getRootDependency()} does not return {@code null}.
     * 
     * @param rootArtifact The root artifact for the dependency graph, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest setRootArtifact( Artifact rootArtifact )
    {
        this.rootArtifact = rootArtifact;
        return this;
    }

    /**
     * Gets the root dependency of the graph.
     * 
     * @return The root dependency of the graph or {@code null} if none.
     */
    public Dependency getRootDependency()
    {
        return rootDependency;
    }

    /**
     * Sets the root dependency of the graph.
     * 
     * @param rootDependency The root dependency of the graph, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest setRootDependency( Dependency rootDependency )
    {
        this.rootDependency = rootDependency;
        return this;
    }

    public DependableCoordinate getRootCoordinate()
    {
        return rootCoordinate;
    }

    public DependencyCollectorRequest setRootCoordinate( DependableCoordinate rootCoordinate )
    {
        this.rootCoordinate = rootCoordinate;
        return this;
    }

    public Model getRootModel()
    {
        return rootModel;
    }

    public DependencyCollectorRequest setRootModel( Model rootModel )
    {
        this.rootModel = rootModel;
        return this;
    }

    /**
     * Gets the direct dependencies.
     * 
     * @return The direct dependencies, never {@code null}.
     */
    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    /**
     * Sets the direct dependencies. If both a root dependency and direct dependencies are given in the request, the
     * direct dependencies from the request will be merged with the direct dependencies from the root dependency's
     * artifact descriptor, giving higher priority to the dependencies from the request.
     * 
     * @param dependencies The direct dependencies, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest setDependencies( List<Dependency> dependencies )
    {
        if ( dependencies == null )
        {
            this.dependencies = Collections.emptyList();
        }
        else
        {
            this.dependencies = dependencies;
        }
        return this;
    }

    /**
     * Adds the specified direct dependency.
     * 
     * @param dependency The dependency to add, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest addDependency( Dependency dependency )
    {
        if ( dependency != null )
        {
            if ( this.dependencies.isEmpty() )
            {
                this.dependencies = new ArrayList<>();
            }
            this.dependencies.add( dependency );
        }
        return this;
    }

    /**
     * Gets the dependency management to apply to transitive dependencies.
     * 
     * @return The dependency management to apply to transitive dependencies, never {@code null}.
     */
    public List<Dependency> getManagedDependencies()
    {
        return managedDependencies;
    }

    /**
     * Sets the dependency management to apply to transitive dependencies. To clarify, this management does not apply to
     * the direct dependencies of the root node.
     * 
     * @param managedDependencies The dependency management, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest setManagedDependencies( List<Dependency> managedDependencies )
    {
        if ( managedDependencies == null )
        {
            this.managedDependencies = Collections.emptyList();
        }
        else
        {
            this.managedDependencies = managedDependencies;
        }
        return this;
    }

    /**
     * Adds the specified managed dependency.
     * 
     * @param managedDependency The managed dependency to add, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public DependencyCollectorRequest addManagedDependency( Dependency managedDependency )
    {
        if ( managedDependency != null )
        {
            if ( this.managedDependencies.isEmpty() )
            {
                this.managedDependencies = new ArrayList<>();
            }
            this.managedDependencies.add( managedDependency );
        }
        return this;
    }

    @Override
    public String toString()
    {
        return getRootDependency() + " -> " + getDependencies() + " < " + getRepositories();
    }

}
