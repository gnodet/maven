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
package org.apache.maven.repository.internal.relocation;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenArtifactRelocationSource;
import org.apache.maven.repository.internal.RelocatedArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relocation source from user properties.
 *
 * @since 4.0.0
 */
@Singleton
@Named
@Priority(50)
@SuppressWarnings("checkstyle:MagicNumber")
public final class UserPropertiesArtifactRelocationSource implements MavenArtifactRelocationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPropertiesArtifactRelocationSource.class);

    private static final String CONFIG_PROP_RELOCATIONS_ENTRIES = "maven.relocations.entries";

    @Override
    public Artifact relocatedTarget(RepositorySystemSession session, ArtifactDescriptorRequest request, Model model) {
        Relocations relocations = (Relocations) session.getData()
                .computeIfAbsent(getClass().getName() + ".relocations", () -> readRelocations(session));
        if (relocations != null) {
            Relocation relocation = relocations.getRelocation(request.getArtifact());
            if (relocation != null && (isProjectContext(request.getRequestContext()) || relocation.global)) {
                LOGGER.info(
                        "{} applied: {}",
                        relocation.global ? "User global relocation" : "User project relocation",
                        relocation);
                return new RelocatedArtifact(
                        request.getArtifact(),
                        isAny(relocation.target.getGroupId()) ? null : relocation.target.getGroupId(),
                        isAny(relocation.target.getArtifactId()) ? null : relocation.target.getArtifactId(),
                        isAny(relocation.target.getVersion()) ? null : relocation.target.getVersion(),
                        relocation.global ? "User global relocation" : "User project relocation");
            }
        }
        return null;
    }

    private boolean isProjectContext(String context) {
        return context != null && context.startsWith("project");
    }

    private static boolean isAny(String str) {
        return "*".equals(str);
    }

    private static boolean matches(String pattern, String str) {
        if (isAny(pattern)) {
            return true;
        } else if (pattern.endsWith("*")) {
            return str.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return Objects.equals(pattern, str);
        }
    }

    private static Predicate<Artifact> artifactPredicate(Artifact artifact) {
        return a -> matches(artifact.getGroupId(), a.getGroupId())
                && matches(artifact.getArtifactId(), a.getArtifactId())
                && matches(artifact.getBaseVersion(), a.getBaseVersion());
    }

    private static class Relocation {
        private final Predicate<Artifact> predicate;
        private final boolean global;
        private final Artifact source;
        private final Artifact target;

        private Relocation(boolean global, Artifact source, Artifact target) {
            this.predicate = artifactPredicate(source);
            this.global = global;
            this.source = source;
            this.target = target;
        }

        @Override
        public String toString() {
            return source + (global ? " >> " : " > ") + target;
        }
    }

    private static class Relocations {
        private final List<Relocation> relocations;

        private Relocations(List<Relocation> relocations) {
            this.relocations = relocations;
        }

        private Relocation getRelocation(Artifact artifact) {
            return relocations.stream()
                    .filter(r -> r.predicate.test(artifact))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Relocations readRelocations(RepositorySystemSession session) {
        String relocationsEntries = (String) session.getConfigProperties().get(CONFIG_PROP_RELOCATIONS_ENTRIES);
        if (relocationsEntries == null) {
            return null;
        }
        String[] entries = relocationsEntries.split(",");
        try (Stream<String> lines = Arrays.stream(entries)) {
            List<Relocation> relocationList = lines.filter(
                            l -> l != null && !l.trim().isEmpty())
                    .map(l -> {
                        boolean global;
                        String splitExpr;
                        if (l.contains(">>")) {
                            global = true;
                            splitExpr = ">>";
                        } else if (l.contains(">")) {
                            global = false;
                            splitExpr = ">";
                        } else {
                            throw new IllegalArgumentException("Unrecognized entry: " + l);
                        }
                        String[] parts = l.split(splitExpr);
                        if (parts.length < 1) {
                            throw new IllegalArgumentException("Unrecognized entry: " + l);
                        }
                        Artifact s = new DefaultArtifact(parts[0]);
                        Artifact t;
                        if (parts.length > 1) {
                            t = new DefaultArtifact(parts[1]);
                        } else {
                            t = new DefaultArtifact("org.apache.maven.banned:user-relocation:1.0");
                        }
                        return new Relocation(global, s, t);
                    })
                    .collect(Collectors.toList());
            LOGGER.info("Parsed {} user relocations", relocationList.size());
            return new Relocations(relocationList);
        }
    }
}