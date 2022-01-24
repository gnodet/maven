package org.apache.maven.shared.artifact.filter.collection;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

/**
 * <p>ArtifactFilterException class.</p>
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class ArtifactFilterException
    extends Exception
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create an instance.
     */
    public ArtifactFilterException()
    {
        super();
    }

    /**
     * <p>Constructor for ArtifactFilterException.</p>
     *
     * @param theMessage The message which describes what happends.
     * @param theCause The cause.
     */
    public ArtifactFilterException( String theMessage, Throwable theCause )
    {
        super( theMessage, theCause );
    }

    /**
     * <p>Constructor for ArtifactFilterException.</p>
     *
     * @param theMessage The message which describes what happends.
     */
    public ArtifactFilterException( String theMessage )
    {
        super( theMessage );
    }

    /**
     * <p>Constructor for ArtifactFilterException.</p>
     *
     * @param theCause {@link java.lang.Throwable} cause of the problem.
     */
    public ArtifactFilterException( Throwable theCause )
    {
        super( theCause );
    }

}
