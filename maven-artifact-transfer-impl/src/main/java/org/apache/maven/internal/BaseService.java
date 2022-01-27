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
import java.util.Objects;

import org.apache.maven.services.BaseRequest;
import org.apache.maven.api.Session;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Base class for services.
 */
class BaseService
{

    protected final RepositorySystem repositorySystem;

    BaseService( RepositorySystem repositorySystem )
    {
        this.repositorySystem = Objects.requireNonNull( repositorySystem );
    }

    protected void nonNull( Object obj, String name )
    {
        if ( obj == null )
        {
            throw new IllegalArgumentException( name + " should not be null" );
        }
    }

    protected RepositorySystemSession session( BaseRequest<?> request )
    {
        Session session = request.getSession();
        if ( request.getLocalRepository() != null )
        {
            session = withLocalRepository( session, request.getLocalRepository() );
        }
        else if ( session.getLocalRepository() != null )
        {
            session = withLocalRepository( session, session.getLocalRepository() );
        }
        return DefaultSession.getSession( session );
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

    protected Session withLocalRepository(Session repositorySession, Path localRepository )
    {
        if ( localRepository == null )
        {
            return repositorySession;
        }

        RepositorySystemSession session = session( repositorySession );

        // "clone" session and replace localRepository
        DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession( session );

        // Clear cache, since we're using a new local repository
        newSession.setCache( new DefaultRepositoryCache() );

        // keep same repositoryType
        String repositoryType = resolveRepositoryType( session.getLocalRepository() );

        LocalRepositoryManager localRepositoryManager = repositorySystem.newLocalRepositoryManager( newSession,
                new LocalRepository( localRepository.toFile(), repositoryType ) );

        newSession.setLocalRepositoryManager( localRepositoryManager );

        return new DefaultSession( newSession, null, null );
    }

    /**
     * @param localRepository {@link LocalRepository}
     * @return the resolved type.
     */
    protected String resolveRepositoryType( LocalRepository localRepository )
    {
        String repositoryType;
        if ( "enhanced".equals( localRepository.getContentType() ) )
        {
            repositoryType = "default";
        }
        else
        {
            // this should be "simple"
            repositoryType = localRepository.getContentType();
        }
        return repositoryType;
    }

    private RepositorySystemSession session( Session session )
    {
        return ( (DefaultSession) session ).getRepositorySystemSession();
    }

}
