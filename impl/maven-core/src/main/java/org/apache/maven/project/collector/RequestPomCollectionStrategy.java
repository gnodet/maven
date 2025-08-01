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
package org.apache.maven.project.collector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.collector.ProjectsSelector;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.services.ProjectBuilderException;

/**
 * Strategy to collect projects based on the <code>-f</code> CLI parameter or the pom.xml in the working directory.
 */
@Named("RequestPomCollectionStrategy")
@Singleton
public class RequestPomCollectionStrategy extends AbstractProjectCollectionStrategy {
    private final ProjectsSelector projectsSelector;

    @Inject
    public RequestPomCollectionStrategy(ProjectsSelector projectsSelector) {
        this.projectsSelector = projectsSelector;
    }

    @Override
    public List<Project> collectProjects(MavenRequest request) throws ProjectBuilderException {
        return projectsSelector.selectProjects(List.of(request.getPom().toAbsolutePath()), request);
    }
}
