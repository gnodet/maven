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

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Session;
import org.apache.maven.api.repository.Repository;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Default RepositorySession.
 */
public class DefaultSession implements Session
{
    private final RepositorySystemSession repositorySystemSession;

    private final Path localRepository;

    private final List<Repository> remoteRepositories;

    public DefaultSession(RepositorySystemSession repositorySystemSession,
                          Path localRepository,
                          List<Repository> remoteRepositories )
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
    public Session withLocalRepository(Path localRepository )
    {
        return new DefaultSession( repositorySystemSession, localRepository, remoteRepositories );
    }

    @Override
    public List<Repository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    @Override
    public Session withRemoteRepositories( List<Repository> remoteRepositories )
    {
        return new DefaultSession( repositorySystemSession, localRepository, remoteRepositories );
    }

    public static RepositorySystemSession getSession( Session session )
    {
        return ( (DefaultSession) session ).getRepositorySystemSession();
    }

}
