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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.artifact.Artifact;
import org.apache.maven.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.artifact.filter.resolve.transform.EclipseAetherFilterTransformer;
import org.apache.maven.api.repository.Repository;
import org.apache.maven.api.graph.DependencyNode;
import org.apache.maven.api.graph.DependencyNodeVisitor;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * DependencyCollectorNode wrapper around {@link org.eclipse.aether.graph.DependencyNode}
 *
 * @author Pim Moerenhout
 *
 */
public class DefaultDependencyNodeAdapter implements DependencyNode
{

    private org.eclipse.aether.graph.DependencyNode dependencyNode;

    /**
     * @param dependencyNode {@link org.eclipse.aether.graph.DependencyNode}
     */
    public DefaultDependencyNodeAdapter( org.eclipse.aether.graph.DependencyNode dependencyNode )
    {
        this.dependencyNode = dependencyNode;
    }

    @Override
    public Artifact getArtifact()
    {
        return getArtifact( dependencyNode.getArtifact() );
    }

    @Override
    public List<DependencyNode> getChildren()
    {
        List<org.eclipse.aether.graph.DependencyNode> aetherChildren = dependencyNode.getChildren();
        List<DependencyNode> children = new ArrayList<>( aetherChildren.size() );
        for ( org.eclipse.aether.graph.DependencyNode aetherChild : aetherChildren )
        {
            children.add( new DefaultDependencyNodeAdapter( aetherChild ) );
        }
        return children;
    }

    @Override
    public List<Repository> getRemoteRepositories()
    {
        List<RemoteRepository> aetherRepositories = dependencyNode.getRepositories();
        List<Repository> mavenRepositories = new ArrayList<>( aetherRepositories.size() );

        for ( RemoteRepository aetherRepository : aetherRepositories )
        {
            mavenRepositories.add( new DefaultRepositoryAdapter( aetherRepository ) );
        }

        return mavenRepositories;
    }

    @Override
    public String getScope()
    {
        return dependencyNode.getDependency().getScope();
    }

    @Override
    public Boolean getOptional()
    {
        return dependencyNode.getDependency().getOptional();
    }

    @Override
    public boolean accept( DependencyNodeVisitor visitor )
    {
        if ( visitor.enter( this ) )
        {
            for ( org.eclipse.aether.graph.DependencyNode aetherNode : dependencyNode.getChildren() )
            {
                DependencyNode child = new DefaultDependencyNodeAdapter( aetherNode );
                if ( !child.accept( visitor ) )
                {
                    break;
                }
            }
        }

        return visitor.leave( this );
    }

    @Override
    public DependencyNode filter( TransformableFilter filter )
    {
        DependencyFilter depFilter = filter.transform( new EclipseAetherFilterTransformer() );

        List<org.eclipse.aether.graph.DependencyNode> parents = new ArrayList<>();
        DefaultDependencyNode newNode = doFilter( depFilter, parents, dependencyNode );
        return new DefaultDependencyNodeAdapter( newNode );
    }

    private DefaultDependencyNode doFilter( DependencyFilter depFilter,
                                            List<org.eclipse.aether.graph.DependencyNode> parents,
                                            org.eclipse.aether.graph.DependencyNode dependencyNode )
    {
        DefaultDependencyNode newNode = new DefaultDependencyNode( dependencyNode );
        parents.add( dependencyNode );
        for ( org.eclipse.aether.graph.DependencyNode child : dependencyNode.getChildren() )
        {
            if ( depFilter.accept( child, parents) )
            {
                newNode.getChildren().add( doFilter( depFilter, parents, child ) );
            }
        }
        parents.remove( parents.size() - 1 );
        return newNode;
    }

    @Override
    public int hashCode()
    {
        return dependencyNode.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        DefaultDependencyNodeAdapter other = (DefaultDependencyNodeAdapter) obj;
        if ( dependencyNode == null )
        {
            if ( other.dependencyNode != null )
            {
                return false;
            }
        }
        else if ( !dependencyNode.equals( other.dependencyNode ) )
        {
            return false;
        }
        return true;
    }

    private Artifact getArtifact( org.eclipse.aether.artifact.Artifact aetherArtifact )
    {
        return RepositoryUtils.toArtifact( aetherArtifact );
    }
}
