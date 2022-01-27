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

import org.apache.maven.api.Session;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.api.dependencies.DependableCoordinate;

/**
 * The DependencyCollector service can be used to collect dependencies
 * for a given artifact and builds a graph of them.
 * The dependencies collection mechanism will not download any artifacts,
 * and only the pom files will be downloaded.
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 */
public interface DependencyCollector
{

    /**
     * Collects the transitive dependencies and builds a dependency graph.
     * Note that this operation is only concerned about determining the coordinates of the
     * transitive dependencies and does not actually resolve the artifact files.
     *
     * @param request The dependency collection request, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     *
     * @see DependencyCollector#collectDependencies(Session, Model)
     * @see DependencyCollector#collectDependencies(Session, Dependency)
     * @see DependencyCollector#collectDependencies(Session, DependableCoordinate)
     */
    DependencyCollectorResult collectDependencies( DependencyCollectorRequest request )
            throws DependencyCollectorException, IllegalArgumentException;

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param root The Maven Dependency, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @see #collectDependencies(DependencyCollectorRequest)
     */
    default DependencyCollectorResult collectDependencies( Session session,
                                                           Dependency root )
        throws DependencyCollectorException
    {
        return collectDependencies( new DependencyCollectorRequest( session, root ) );
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param root The Maven DependableCoordinate, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @see #collectDependencies(DependencyCollectorRequest)
     */
    default DependencyCollectorResult collectDependencies( Session session,
                                                           DependableCoordinate root )
        throws DependencyCollectorException
    {
        return collectDependencies( new DependencyCollectorRequest( session, root ) );
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param root The Maven model, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @see #collectDependencies(DependencyCollectorRequest)
     */
    default DependencyCollectorResult collectDependencies( Session session,
                                                           Model root )
        throws DependencyCollectorException
    {
        return collectDependencies( new DependencyCollectorRequest( session, root ) );
    }

}
