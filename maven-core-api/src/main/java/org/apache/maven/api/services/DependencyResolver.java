package org.apache.maven.api.services;

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
import java.util.function.Predicate;

import org.apache.maven.api.Session;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;

/**
 * The DependencyResolver service can be used to collect the dependencies
 * and download the artifacts.
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 */
public interface DependencyResolver extends Service
{
    /**
     * Collect dependencies and resolve the artifacts.
     *
     * @param request {@link DependencyResolverRequest}
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    DependencyResolverResult resolve( DependencyResolverRequest request )
            throws DependencyResolverException;

    /**
     * This will resolve the dependencies of the coordinate, not resolving the artifact of the coordinate itself. If
     * the coordinate needs to be resolved too, use
     * {@link #resolveDependencies(Session, List, List, Predicate< Node >)} passing
     * {@code Collections.singletonList(coordinate)}
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param root {@link Dependency}
     * @param filter {@link Predicate} used to eventually filter out some dependencies
     *               when downloading (can be {@code null}).
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    default DependencyResolverResult resolve( Session session,
                                              Dependency root,
                                              Predicate<Node> filter )
        throws DependencyResolverException
    {
        return resolve( DependencyResolverRequest.build( session, root, filter ) );
    }

    /**
     * This will resolve the dependencies of the coordinate, not resolving the artifact of the coordinate itself. If
     * the coordinate needs to be resolved too, use
     * {@link #resolveDependencies(Session, List, List, Predicate< Node >)} passing
     * {@code Collections.singletonList(coordinate)}
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param project {@link Project}
     * @param filter {@link Predicate< Node >} (can be {@code null}).
     * @return the resolved dependencies.
     * @throws DependencyResolverException in case of an error.
     */
    default DependencyResolverResult resolve( Session session,
                                              Project project,
                                              Predicate<Node> filter )
        throws DependencyResolverException
    {
        return resolve( DependencyResolverRequest.build( session, project, filter ) );
    }

}
