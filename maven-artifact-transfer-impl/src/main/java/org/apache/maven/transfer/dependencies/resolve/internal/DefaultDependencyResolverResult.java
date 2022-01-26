package org.apache.maven.transfer.dependencies.resolve.internal;

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

import java.util.AbstractList;
import java.util.List;

import org.apache.maven.transfer.artifact.resolve.ArtifactResolverResult;
import org.apache.maven.transfer.artifact.resolve.internal.DefaultArtifactResolverResult;
import org.apache.maven.transfer.dependencies.collect.internal.DefaultDependencyNodeAdapter;
import org.apache.maven.transfer.dependencies.resolve.DependencyResolverResult;
import org.apache.maven.transfer.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * {@link ArtifactResolverResult} wrapper for {@link ArtifactResult}
 * 
 * @author Robert Scholte
 * @since 3.0
 */
class DefaultDependencyResolverResult
    implements DependencyResolverResult
{
    private final DependencyResult dependencyResult;

    /**
     * @param dependencyResult {@link DependencyResult}
     */
    DefaultDependencyResolverResult( DependencyResult dependencyResult )
    {
        this.dependencyResult = dependencyResult;
    }

    @Override
    public List<Exception> getCollectorExceptions()
    {
        return dependencyResult.getCollectExceptions();
    }

    @Override
    public DependencyNode getRoot()
    {
        return new DefaultDependencyNodeAdapter( dependencyResult.getRoot() );
    }

    @Override
    public List<ArtifactResolverResult> getArtifactResults()
    {
        List<ArtifactResult> list = dependencyResult.getArtifactResults();
        return new AbstractList<ArtifactResolverResult>()
        {
            @Override
            public ArtifactResolverResult get( int index )
            {
                return new DefaultArtifactResolverResult( list.get( index ) );
            }
            @Override
            public int size()
            {
                return list.size();
            }
        };
    }
}
