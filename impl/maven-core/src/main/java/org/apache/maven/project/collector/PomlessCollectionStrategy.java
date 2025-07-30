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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.DefaultMaven;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.Source;

/**
 * Strategy to collect projects for building when the Maven invocation is not in a directory that contains a pom.xml.
 */
@Named("PomlessCollectionStrategy")
@Singleton
public class PomlessCollectionStrategy extends AbstractProjectCollectionStrategy {
    private final ProjectBuilder projectBuilder;

    @Inject
    public PomlessCollectionStrategy(ProjectBuilder projectBuilder) {
        this.projectBuilder = projectBuilder;
    }

    @Override
    public List<Project> collectProjects(MavenRequest request) throws ProjectBuilderException {
        // Get the Session from the repository session
        Session session = request.getSession();

        // Convert UrlModelSource to UrlSource
        URL standalone = DefaultMaven.class.getResource("project/standalone.xml");
        ProjectBuilderRequest pbr = ProjectBuilderRequest.builder()
                .session(session)
                .recursive(request.isRecursive())
                .processPlugins(true)
                .source(new UrlSource(standalone))
                .build();

        ProjectBuilderResult result = projectBuilder.build(pbr);

        return projects(result).toList();
    }

    static Stream<Project> projects(ProjectBuilderResult result) {
        return Stream.concat(
                Stream.of(result.getProject()).filter(Optional::isPresent).map(Optional::get),
                result.getChildren().stream().flatMap(PomlessCollectionStrategy::projects));
    }

    static class UrlSource implements Source {
        private final URL url;

        UrlSource(URL url) {
            this.url = url;
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public InputStream openStream() throws IOException {
            return url.openStream();
        }

        @Override
        public String getLocation() {
            return url.toString();
        }

        @Override
        public Source resolve(String relative) {
            return null;
        }
    }
}
