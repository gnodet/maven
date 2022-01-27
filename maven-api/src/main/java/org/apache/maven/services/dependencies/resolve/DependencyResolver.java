package org.apache.maven.services.dependencies.resolve;

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

import java.util.List;

import org.apache.maven.api.Session;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.api.dependencies.DependableCoordinate;

/**
 * The DependencyResolver service can be used to collect the dependencies
 * and download the artifacts.
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 */
public interface DependencyResolver
{
    /**
     * Collect dependencies and resolve the artifacts.
     *
     * @param request {@link DependencyResolverRequest}
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    DependencyResolverResult resolveDependencies( DependencyResolverRequest request )
            throws DependencyResolverException;

    /**
     * This will resolve the dependencies of the coordinate, not resolving the artifact of the coordinate itself. If
     * the coordinate needs to be resolved too, use
     * {@link #resolveDependencies(Session, List, List, TransformableFilter)} passing
     * {@code Collections.singletonList(coordinate)}
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param rootCoordinate {@link DependableCoordinate}
     * @param filter {@link TransformableFilter} used to eventually filter out some dependencies
     *               when downloading (can be {@code null}).
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    default DependencyResolverResult resolveDependencies( Session session,
                                                          DependableCoordinate rootCoordinate,
                                                          TransformableFilter filter )
        throws DependencyResolverException
    {
        return resolveDependencies( new DependencyResolverRequest( session, rootCoordinate, filter ) );
    }

    /**
     * This will resolve the dependencies of the coordinate, not resolving the the artifact of the coordinate itself. If
     * the coordinate needs to be resolved too, use
     * {@link #resolveDependencies(Session, List, List, TransformableFilter)} passing
     * {@code Collections.singletonList(coordinate)}
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param rootModel {@link Model}
     * @param filter {@link TransformableFilter} (can be {@code null}).
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    default DependencyResolverResult resolveDependencies( Session session,
                                                          Model rootModel,
                                                          TransformableFilter filter )
        throws DependencyResolverException
    {
        return resolveDependencies( new DependencyResolverRequest( session, rootModel, filter ) );
    }

    /**
     * @param session The {@link Session}, must not be {@code null}.
     * @param dependencies the dependencies to resolve, can be {@code null}
     * @param managedDependencies managed dependencies, can be {@code null}
     * @param filter a filter, can be {@code null}
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    default DependencyResolverResult resolveDependencies( Session session,
                                                          List<Dependency> dependencies,
                                                          List<Dependency> managedDependencies,
                                                          TransformableFilter filter )
        throws DependencyResolverException
    {
        return resolveDependencies(
                new DependencyResolverRequest( session, dependencies, managedDependencies, filter ) );
    }
}
