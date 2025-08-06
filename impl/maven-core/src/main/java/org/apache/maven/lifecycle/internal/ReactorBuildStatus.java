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
package org.apache.maven.lifecycle.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.maven.api.Project;
import org.apache.maven.api.exec.ProjectDependencyGraph;

/**
 * Contains status information that is global to an entire reactor build.
 *
 * @since 3.0
 */
public class ReactorBuildStatus {
    private final ProjectDependencyGraph projectDependencyGraph;

    private final Collection<String> blackListedProjects = Collections.synchronizedSet(new HashSet<>());

    private volatile boolean halted = false;

    public ReactorBuildStatus(ProjectDependencyGraph projectDependencyGraph) {
        this.projectDependencyGraph = projectDependencyGraph;
    }

    public boolean isBlackListed(Project project) {
        return blackListedProjects.contains(project.gav());
    }

    public void blackList(Project project) {
        if (blackListedProjects.add(project.gav()) && projectDependencyGraph != null) {
            for (Project downstreamProject : projectDependencyGraph.getDownstreamProjects(project, true)) {
                blackListedProjects.add(downstreamProject.gav());
            }
        }
    }

    public void halt() {
        halted = true;
    }

    public boolean isHalted() {
        return halted;
    }

    public boolean isHaltedOrBlacklisted(Project project) {
        return isBlackListed(project) || isHalted();
    }
}
