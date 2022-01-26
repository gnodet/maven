package org.apache.maven.transfer.dependencies.internal;

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
import org.apache.maven.transfer.dependencies.DependableCoordinate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;

/**
 * Utility class
 */
public class DependencyUtils
{

    /**
     * Based on RepositoryUtils#toDependency(org.apache.maven.model.Dependency, ArtifactTypeRegistry)
     *
     * @param coordinate  {@link DependableCoordinate}
     * @param stereotypes {@link ArtifactTypeRegistry
     * @return as Aether Dependency
     */
    public static org.eclipse.aether.graph.Dependency toDependency( DependableCoordinate coordinate,
                                                                    ArtifactTypeRegistry stereotypes )
    {
        ArtifactType stereotype = stereotypes.get( coordinate.getType() );
        if ( stereotype == null )
        {
            stereotype = new DefaultArtifactType( coordinate.getType() );
        }

        Artifact artifact = new DefaultArtifact( coordinate.getGroupId(), coordinate.getArtifactId(),
                coordinate.getClassifier(), null, coordinate.getVersion(), null, stereotype );

        return new org.eclipse.aether.graph.Dependency( artifact, null );
    }

    public static org.eclipse.aether.graph.Dependency toDependency( org.apache.maven.model.Dependency root,
                                                                    ArtifactTypeRegistry typeRegistry )
    {
        return RepositoryUtils.toDependency( root, typeRegistry );
    }

}
