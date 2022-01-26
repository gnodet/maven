package org.apache.maven.transfer.internal;

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

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.transfer.RepositorySession;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Default RepositorySession.
 */
public class DefaultRepositorySession implements RepositorySession
{
    private final RepositorySystemSession repositorySystemSession;

    private final Path localRepository;

    private final List<ArtifactRepository> remoteRepositories;

    public DefaultRepositorySession( RepositorySystemSession repositorySystemSession,
                                     Path localRepository,
                                     List<ArtifactRepository> remoteRepositories )
    {
        this.repositorySystemSession = repositorySystemSession;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    public RepositorySystemSession getRepositorySystemSession()
    {
        return repositorySystemSession;
    }

    @Override
    public Path getLocalRepository()
    {
        return localRepository;
    }

    @Override
    public RepositorySession withLocalRepository( Path localRepository )
    {
        return new DefaultRepositorySession( repositorySystemSession, localRepository, remoteRepositories );
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    @Override
    public RepositorySession withRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        return new DefaultRepositorySession( repositorySystemSession, localRepository, remoteRepositories );
    }

    public static RepositorySystemSession getSession( RepositorySession session )
    {
        return ( ( DefaultRepositorySession ) session ).getRepositorySystemSession();
    }

}
