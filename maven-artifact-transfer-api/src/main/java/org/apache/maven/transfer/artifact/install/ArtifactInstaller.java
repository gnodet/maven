package org.apache.maven.transfer.artifact.install;

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

import java.util.Collection;
import java.util.Collections;

import org.apache.maven.transfer.RepositorySession;
import org.apache.maven.artifact.Artifact;

/**
 * @author Robert Scholte
 */
public interface ArtifactInstaller
{
    /**
     * @param request {@link ArtifactInstallerRequest}
     * @throws ArtifactInstallerException in case of an error.
     * @throws IllegalArgumentException in case <code>request</code> is <code>null</code>, <code>mavenArtifacts</code>
     *             is <code>null</code> or <code>mavenArtifacts</code> is empty (<code>mavenArtifacts.isEmpty()</code>
     *             == <code>true</code>).
     */
    void install( ArtifactInstallerRequest request )
        throws ArtifactInstallerException, IllegalArgumentException;

    /**
     * @param session the repository session
     * @param artifact the {@link Artifact} to install
     * @throws ArtifactInstallerException In case of an error which can be the a given artifact can not be found or the
     *             installation has failed.
     * @throws IllegalArgumentException in case of parameter <code>request</code> is <code>null</code> or parameter
     *             <code>localRepository</code> is <code>null</code> or <code>localRepository</code> is not a directory
     *             or parameter <code>mavenArtifacts</code> is <code>null</code> or
     *             <code>mavenArtifacts.isEmpty()</code> is <code>true</code>.
     */
    default void install( RepositorySession session, Artifact artifact )
            throws ArtifactInstallerException
    {
        install( session, Collections.singletonList( artifact ) );
    }

    /**
     * @param session the repository session
     * @param artifacts Collection of {@link Artifact MavenArtifacts}
     * @throws ArtifactInstallerException In case of an error which can be the a given artifact can not be found or the
     *             installation has failed.
     * @throws IllegalArgumentException in case of parameter <code>request</code> is <code>null</code> or parameter
     *             <code>localRepository</code> is <code>null</code> or <code>localRepository</code> is not a directory
     *             or parameter <code>mavenArtifacts</code> is <code>null</code> or
     *             <code>mavenArtifacts.isEmpty()</code> is <code>true</code>.
     */
    default void install( RepositorySession session, Collection<Artifact> artifacts )
            throws ArtifactInstallerException
    {
        install( new ArtifactInstallerRequest( session, artifacts ) );
    }

}
