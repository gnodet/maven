package org.apache.maven.transfer.dependencies.collect;

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

/**
 * Thrown in case of bad artifact descriptors, version ranges or other issues encountered during calculation of the
 * dependency graph.
 */
public class DependencyCollectorException
    extends Exception
{
    /**
     *
     */
    private static final long serialVersionUID = -3134726259840210686L;

    /**
     * @param message The message you would give for the exception.
     * @param cause The cause which is related to the message.
     */
    public DependencyCollectorException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
