package org.apache.maven.services.artifact.install;

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
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.services.BaseRequest;
import org.apache.maven.api.Session;
import org.apache.maven.api.artifact.Artifact;

/**
 * A request for installing one or more artifacts in the local repository.
 */
public class ArtifactInstallerRequest extends BaseRequest<ArtifactInstallerRequest>
{

    private Collection<Artifact> artifacts = Collections.emptyList();

    public ArtifactInstallerRequest()
    {
    }

    public ArtifactInstallerRequest(Session session, Collection<Artifact> artifacts )
    {
        setSession( session );
        this.artifacts = artifacts;
    }

    public Collection<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public ArtifactInstallerRequest setArtifacts( Collection<Artifact> artifacts )
    {
        if ( artifacts == null )
        {
            this.artifacts = Collections.emptyList();
        }
        else
        {
            this.artifacts = artifacts;
        }
        return this;
    }

    public ArtifactInstallerRequest addArtifact( Artifact artifact )
    {
        if ( artifact != null )
        {
            if ( artifacts.isEmpty() )
            {
                artifacts = new ArrayList<>();
            }
            artifacts.add( artifact );
        }
        return this;
    }
}
