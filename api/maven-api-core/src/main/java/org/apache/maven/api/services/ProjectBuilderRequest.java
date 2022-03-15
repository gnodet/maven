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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;

public interface ProjectBuilderRequest
{

    @Nonnull
    Session getSession();

    @Nonnull
    Optional<Path> getPath();

    @Nonnull
    Optional<ProjectBuilderSource> getSource();

    @Nonnull
    Optional<Artifact> getArtifact();

    boolean isAllowStubModel();

    boolean isRecursive();

    boolean isProcessPlugins();

    boolean isResolveDependencies();

    @Nonnull
    static ProjectBuilderRequest build( @Nonnull Session session, @Nonnull ProjectBuilderSource source )
    {
        return builder()
                .session( session )
                .source( source )
                .build();
    }

    @Nonnull
    static ProjectBuilderRequest build( @Nonnull Session session, @Nonnull Path path )
    {
        return builder()
                .session( session )
                .path( path )
                .build();
    }

    @Nonnull
    static ProjectBuilderRequest build( @Nonnull Session session, @Nonnull Artifact artifact )
    {
        return builder()
                .session( session )
                .artifact( artifact )
                .build();
    }

    @Nonnull
    static ProjectBuilderRequestBuilder builder()
    {
        return new ProjectBuilderRequestBuilder();
    }

    class ProjectBuilderRequestBuilder
    {
        Session session;
        Path path;
        ProjectBuilderSource source;
        Artifact artifact;
        boolean allowStubModel;
        boolean recursive;
        boolean processPlugins = true;
        boolean resolveDependencies = true;

        public ProjectBuilderRequestBuilder session( Session session )
        {
            this.session = session;
            return this;
        }

        public ProjectBuilderRequestBuilder path( Path path )
        {
            this.path = path;
            return this;
        }

        public ProjectBuilderRequestBuilder source( ProjectBuilderSource source )
        {
            this.source = source;
            return this;
        }

        public ProjectBuilderRequestBuilder artifact( Artifact artifact )
        {
            this.artifact = artifact;
            return this;
        }

        public ProjectBuilderRequestBuilder processPlugins( boolean processPlugins )
        {
            this.processPlugins = processPlugins;
            return this;
        }

        public ProjectBuilderRequestBuilder resolveDependencies( boolean resolveDependencies )
        {
            this.resolveDependencies = resolveDependencies;
            return this;
        }

        public ProjectBuilderRequest build()
        {
            return new DefaultProjectBuilderRequest( session, path, source, artifact,
                    allowStubModel, recursive, processPlugins, resolveDependencies );
        }

        private static class DefaultProjectBuilderRequest extends BaseRequest
            implements ProjectBuilderRequest
        {
            private final Path path;
            private final ProjectBuilderSource source;
            private final Artifact artifact;
            private final boolean allowStubModel;
            private final boolean recursive;
            private final boolean processPlugins;
            private final boolean resolveDependencies;

            @SuppressWarnings( "checkstyle:ParameterNumber" )
            DefaultProjectBuilderRequest( @Nonnull Session session,
                                          @Nullable Path path,
                                          @Nullable ProjectBuilderSource source,
                                          @Nullable Artifact artifact,
                                          boolean allowStubModel,
                                          boolean recursive,
                                          boolean processPlugins,
                                          boolean resolveDependencies )
            {
                super( session );
                this.path = path;
                this.source = source;
                this.artifact = artifact;
                this.allowStubModel = allowStubModel;
                this.recursive = recursive;
                this.processPlugins = processPlugins;
                this.resolveDependencies = resolveDependencies;
            }

            @Nonnull
            @Override
            public Optional<Path> getPath()
            {
                return Optional.ofNullable( path );
            }

            @Nonnull
            @Override
            public Optional<ProjectBuilderSource> getSource()
            {
                return Optional.ofNullable( source );
            }

            @Nonnull
            @Override
            public Optional<Artifact> getArtifact()
            {
                return Optional.ofNullable( artifact );
            }

            @Override
            public boolean isAllowStubModel()
            {
                return allowStubModel;
            }

            @Override
            public boolean isRecursive()
            {
                return recursive;
            }

            @Override
            public boolean isProcessPlugins()
            {
                return processPlugins;
            }

            @Override
            public boolean isResolveDependencies()
            {
                return resolveDependencies;
            }
        }

    }
}
