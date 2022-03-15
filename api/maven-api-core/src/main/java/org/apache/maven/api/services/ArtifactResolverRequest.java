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

import org.apache.maven.api.annotations.Nonnull;

import java.util.Objects;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;

/**
 * A request for resolving an artifact.
 */
public interface ArtifactResolverRequest
{
    @Nonnull
    Session getSession();

    @Nonnull
    Artifact getArtifact();

    @Nonnull
    static ArtifactResolverRequestBuilder builder()
    {
        return new ArtifactResolverRequestBuilder();
    }

    @Nonnull
    static ArtifactResolverRequest build( Session session, Artifact artifact )
    {
        return builder()
                .session( session )
                .artifact( artifact )
                .build();
    }

    class ArtifactResolverRequestBuilder
    {
        Session session;
        Artifact artifact;

        @Nonnull
        public ArtifactResolverRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequestBuilder artifact( Artifact artifact )
        {
            this.artifact = artifact;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequest build()
        {
            return new DefaultArtifactResolverRequest( session, artifact );
        }

        private static class DefaultArtifactResolverRequest extends BaseRequest implements ArtifactResolverRequest
        {
            @Nonnull
            private final Artifact artifact;

            DefaultArtifactResolverRequest( @Nonnull Session session,
                                            @Nonnull Artifact artifact )
            {
                super( session );
                this.artifact = Objects.requireNonNull( artifact );
            }

            @Nonnull
            @Override
            public Artifact getArtifact()
            {
                return artifact;
            }
        }
    }

}
