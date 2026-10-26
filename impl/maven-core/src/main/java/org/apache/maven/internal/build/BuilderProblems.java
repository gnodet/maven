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
package org.apache.maven.internal.build;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.model.building.ModelProblem;

/**
 * Shared conversion utilities between legacy model-building types and the
 * structured {@link BuilderProblem} API.
 *
 * @since 4.1.0
 */
public final class BuilderProblems {

    private BuilderProblems() {}

    /**
     * Converts a legacy {@link ModelProblem} into a structured {@link BuilderProblem},
     * preserving source file, line/column numbers, exception, message and severity.
     * <p>
     * The deduplication key is {@code "model:<source>::<message>"}, which avoids
     * the hash-collision risk of {@code String.hashCode()} while keeping the key
     * stable across repeated builds of the same project.
     *
     * @param problem the model problem to convert, must not be {@code null}
     * @return the equivalent structured problem, never {@code null}
     */
    public static BuilderProblem fromModelProblem(ModelProblem problem) {
        BuilderProblem.Severity severity =
                switch (problem.getSeverity()) {
                    case FATAL -> BuilderProblem.Severity.FATAL;
                    case ERROR -> BuilderProblem.Severity.ERROR;
                    default -> BuilderProblem.Severity.WARNING;
                };
        return BuilderProblem.builder()
                .source(problem.getSource())
                .lineNumber(problem.getLineNumber())
                .columnNumber(problem.getColumnNumber())
                .exception(problem.getException())
                .message(problem.getMessage())
                .severity(severity)
                .key("model:" + problem.getSource() + "::" + problem.getMessage())
                .build();
    }
}
