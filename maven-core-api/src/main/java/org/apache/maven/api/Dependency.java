package org.apache.maven.api;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.Collection;

@Immutable
public interface Dependency
{
    @Nonnull
    Artifact getArtifact();

    // TODO: make that en enum ?
    @Nonnull
    String getScope();

    @Nullable
    Boolean getOptional();

    @Nonnull
    Collection<Exclusion> getExclusions();

    @Nonnull
    default String getGroupId()
    {
        return getArtifact().getGroupId();
    }

    @Nonnull
    default String getArtifactId()
    {
        return getArtifact().getArtifactId();
    }

    @Nonnull
    default String getVersion()
    {
        return getArtifact().getVersion();
    }

    @Nonnull
    default String getType()
    {
        // TODO
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Nonnull
    default String getClassifier()
    {
        return getArtifact().getClassifier();
    }

}
