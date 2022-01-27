package org.apache.maven.services.artifact.resolve;

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

import org.apache.maven.services.BaseRequest;
import org.apache.maven.api.Session;
import org.apache.maven.api.artifact.ArtifactCoordinates;

/**
 * A request for resolving an artifact.
 */
public class ArtifactResolverRequest extends BaseRequest<ArtifactResolverRequest>
{

    private ArtifactCoordinates coordinates;

    public ArtifactResolverRequest()
    {
    }

    public ArtifactResolverRequest( Session session, ArtifactCoordinates coordinates )
    {
        setSession( session );
        this.coordinates = coordinates;
    }

    public ArtifactCoordinates getCoordinates()
    {
        return coordinates;
    }

    public ArtifactResolverRequest setCoordinates( ArtifactCoordinates coordinates )
    {
        this.coordinates = coordinates;
        return this;
    }
}
