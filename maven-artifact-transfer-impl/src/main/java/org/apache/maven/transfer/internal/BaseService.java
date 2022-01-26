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

import java.util.List;
import java.util.Objects;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.transfer.BaseRequest;
import org.apache.maven.transfer.RepositorySession;
import org.apache.maven.transfer.repository.RepositoryManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Base class for services.
 */
public class BaseService
{

    protected final RepositorySystem repositorySystem;

    protected final RepositoryManager repositoryManager;

    public BaseService( RepositorySystem repositorySystem, RepositoryManager repositoryManager )
    {
        this.repositorySystem = Objects.requireNonNull( repositorySystem );
        this.repositoryManager = Objects.requireNonNull( repositoryManager );
    }

    protected RepositorySystemSession session( BaseRequest<?> request )
    {
        RepositorySession session = request.getSession();
        if ( request.getLocalRepository() != null )
        {
            session = repositoryManager.withLocalRepository( session, request.getLocalRepository() );
        }
        else if ( session.getLocalRepository() != null )
        {
            session = repositoryManager.withLocalRepository( session, session.getLocalRepository() );
        }
        return DefaultRepositorySession.getSession( session );
    }

    protected List<RemoteRepository> repositories( BaseRequest<?> request )
    {
        List<RemoteRepository> repositories = RepositoryUtils.toRepos( request.getRepositories() );
        if ( repositories == null || repositories.isEmpty() )
        {
            repositories = RepositoryUtils.toRepos( request.getSession().getRemoteRepositories() );
        }
        return repositories;
    }
}
