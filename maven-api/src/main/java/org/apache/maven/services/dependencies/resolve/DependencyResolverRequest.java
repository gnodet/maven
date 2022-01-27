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
import org.apache.maven.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.api.dependencies.DependableCoordinate;
import org.apache.maven.services.dependencies.collect.DependencyCollectorRequest;

/**
 * A request to collect and resolve a dependency graph.
 */
public class DependencyResolverRequest extends DependencyCollectorRequest
{

    private TransformableFilter filter;

    public DependencyResolverRequest()
    {
    }

    public DependencyResolverRequest( Session session,
                                      DependableCoordinate coordinate,
                                      TransformableFilter filter )
    {
        super( session, coordinate );
        this.filter = filter;
    }

    public DependencyResolverRequest( Session session,
                                      Model model,
                                      TransformableFilter filter )
    {
        super( session, model );
        this.filter = filter;
    }

    public DependencyResolverRequest( Session session,
                                      Dependency rootDependency,
                                      List<Dependency> dependencies,
                                      TransformableFilter filter )
    {
        super( session, rootDependency, dependencies );
        this.filter = filter;
    }

    public DependencyResolverRequest( Session session,
                                      List<Dependency> dependencies,
                                      List<Dependency> managedDependencies,
                                      TransformableFilter filter )
    {
        super( session, dependencies, managedDependencies );
        this.filter = filter;
    }

    public DependencyResolverRequest setCoordinate( DependableCoordinate coordinate )
    {
        setCoordinate( coordinate );
        return this;
    }

    public TransformableFilter getFilter()
    {
        return filter;
    }

    public DependencyResolverRequest setFilter( TransformableFilter filter )
    {
        this.filter = filter;
        return this;
    }
}
