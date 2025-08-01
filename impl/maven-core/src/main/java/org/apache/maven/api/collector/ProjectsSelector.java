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
package org.apache.maven.api.collector;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.services.ProjectBuilderException;

/**
 * Facade to select projects for a given set of pom.xml files.
 */
public interface ProjectsSelector {
    /**
     * Select Maven projects from a list of POM files.
     * @param files List of POM files.
     * @param request The {@link MavenRequest}
     * @return A list of projects that have been found in the specified POM files.
     * @throws ProjectBuilderException In case the POMs are not used.
     */
    List<Project> selectProjects(@Nonnull List<Path> files, @Nonnull MavenRequest request)
            throws ProjectBuilderException;
}
