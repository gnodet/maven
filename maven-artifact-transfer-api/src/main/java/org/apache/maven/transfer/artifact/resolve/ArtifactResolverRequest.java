package org.apache.maven.transfer.artifact.resolve;

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

import org.apache.maven.transfer.BaseRequest;
import org.apache.maven.transfer.RepositorySession;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.transfer.artifact.ArtifactCoordinate;

/**
 * A request for resolving an artifact.
 */
public class ArtifactResolverRequest extends BaseRequest<ArtifactResolverRequest>
{

    private Artifact artifact;
    private ArtifactCoordinate coordinate;

    public ArtifactResolverRequest()
    {
    }

    public ArtifactResolverRequest( RepositorySession session, Artifact artifact )
    {
        setSession( session );
        this.artifact = artifact;
    }

    public ArtifactResolverRequest( RepositorySession session, ArtifactCoordinate coordinate )
    {
        setSession( session );
        this.coordinate = coordinate;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public ArtifactResolverRequest setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
        return this;
    }

    public ArtifactCoordinate getCoordinate()
    {
        return coordinate;
    }

    public ArtifactResolverRequest setCoordinate( ArtifactCoordinate coordinate )
    {
        this.coordinate = coordinate;
        return this;
    }
}
