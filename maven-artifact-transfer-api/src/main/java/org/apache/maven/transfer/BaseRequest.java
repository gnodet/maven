package org.apache.maven.transfer;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Base class for requests.
 *
 * @param <T> the actual request type
 */
public class BaseRequest<T extends BaseRequest>
{

    private RepositorySession session;
    private Path localRepository;
    private List<ArtifactRepository> repositories = Collections.emptyList();

    public RepositorySession getSession()
    {
        return session;
    }

    public T setSession( RepositorySession session )
    {
        this.session = session;
        return me();
    }

    public Path getLocalRepository()
    {
        return localRepository;
    }

    public T setLocalRepository( Path localRepository )
    {
        this.localRepository = localRepository;
        return me();
    }

    /**
     * Gets the repositories to use for this request.
     *
     * @return The repositories to use for this request, never {@code null}.
     */
    public List<ArtifactRepository> getRepositories()
    {
        return repositories;
    }

    /**
     * Sets the repositories to use for this request.
     *
     * @param repositories The repositories to use for the request, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public T setRepositories( List<ArtifactRepository> repositories )
    {
        if ( repositories == null )
        {
            this.repositories = Collections.emptyList();
        }
        else
        {
            this.repositories = repositories;
        }
        return me();
    }

    /**
     * Adds the specified repository for this request.
     *
     * @param repository The repository to collect dependency information from, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    public T addRepository( ArtifactRepository repository )
    {
        if ( repository != null )
        {
            if ( repositories.isEmpty() )
            {
                repositories = new ArrayList<>();
            }
            repositories.add( repository );
        }
        return me();
    }

    @SuppressWarnings( "unchecked" )
    private T me()
    {
        return (T) this;
    }

}
