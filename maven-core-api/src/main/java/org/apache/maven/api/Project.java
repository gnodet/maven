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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Model;

/**
 * Interface representing a Maven project.
 */
public interface Project
{

    @Nonnull
    String getGroupId();

    @Nonnull
    String getArtifactId();

    @Nonnull
    String getVersion();

    @Nonnull
    String getPackaging();

    @Nonnull
    Artifact getArtifact();

    @Nonnull
    Model getModel();

    /**
     * Returns the path to the pom file for this project.
     * A project is usually read from the file system and this will point to
     * the file.  In some cases, a transient project can be created which
     * will not point to an actual pom file.
     * @return the path of the pom
     */
    Path getPomPath();

    default Path getBasedir()
    {
        Path pomPath = getPomPath();
        return pomPath != null ? pomPath.getParent() : null;
    }

    @Nonnull
    List<Dependency> getDependencies();

    @Nonnull
    List<Dependency> getManagedDependencies();

    default String getId()
    {
        return getModel().getId();
    }

    boolean isExecutionRoot();
}
