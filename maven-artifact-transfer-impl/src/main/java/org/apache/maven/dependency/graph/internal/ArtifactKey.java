package org.apache.maven.dependency.graph.internal;

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

import java.util.Objects;

import org.apache.maven.project.MavenProject;

/**
 * Uniquely defines an artifact by groupId, artifactId and version.
 */
final class ArtifactKey
{
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final int hashCode;

    ArtifactKey( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hashCode = Objects.hash( groupId, artifactId, version );
    }

    ArtifactKey( MavenProject project )
    {
        this( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof ArtifactKey ) )
        {
            return false;
        }

        ArtifactKey that = (ArtifactKey) o;

        return artifactId.equals( that.artifactId ) && groupId.equals( that.groupId ) && version.equals( that.version );
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
