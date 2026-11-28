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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link JakartaEeApiMigrationStrategy} class.
 */
@DisplayName("JakartaEeApiMigrationStrategy")
class JakartaEeApiMigrationStrategyTest {

    private static final Path POM_PATH = Paths.get("/project/pom.xml");

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable when --all option is true")
        void applicableWithAll() {
            JakartaEeApiMigrationStrategy strategy = new JakartaEeApiMigrationStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithAll(true))));
        }

        @Test
        @DisplayName("should be applicable with default options (no flags)")
        void applicableWithDefaults() {
            JakartaEeApiMigrationStrategy strategy = new JakartaEeApiMigrationStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext()));
        }

        @Test
        @DisplayName("should be applicable when --model is true")
        void applicableWithModel() {
            JakartaEeApiMigrationStrategy strategy = new JakartaEeApiMigrationStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(true))));
        }

        @Test
        @DisplayName("should not be applicable when --model is false")
        void notApplicableWithModelFalse() {
            JakartaEeApiMigrationStrategy strategy = new JakartaEeApiMigrationStrategy();
            assertFalse(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(false))));
        }
    }

    @Nested
    @DisplayName("Single dependency migration")
    class SingleDependencyMigrationTests {

        private JakartaEeApiMigrationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new JakartaEeApiMigrationStrategy();
        }

        @Test
        @DisplayName("should migrate javax.xml.bind:jaxb-api to jakarta.xml.bind:jakarta.xml.bind-api")
        void migrateJaxbApi() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.bind</groupId>
                                <artifactId>jaxb-api</artifactId>
                                <version>2.3.1</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.xml.bind"), "groupId should be migrated: " + output);
            assertTrue(output.contains("jakarta.xml.bind-api"), "artifactId should be migrated: " + output);
            assertTrue(output.contains("2.3.3"), "version should be updated: " + output);
            assertFalse(output.contains("javax.xml.bind"), "old groupId should be gone: " + output);
        }

        @Test
        @DisplayName("should migrate javax.annotation:javax.annotation-api")
        void migrateAnnotationApi() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.annotation</groupId>
                                <artifactId>javax.annotation-api</artifactId>
                                <version>1.3.2</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.annotation"), "groupId should be migrated: " + output);
            assertTrue(output.contains("jakarta.annotation-api"), "artifactId should be migrated: " + output);
            assertTrue(output.contains("1.3.5"), "version should be updated: " + output);
        }

        @Test
        @DisplayName("should migrate javax.activation:activation")
        void migrateActivation() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.activation</groupId>
                                <artifactId>activation</artifactId>
                                <version>1.1.1</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.activation"), "groupId should be migrated: " + output);
            assertTrue(output.contains("jakarta.activation-api"), "artifactId should be migrated: " + output);
            assertTrue(output.contains("1.2.2"), "version should be updated: " + output);
        }

        @Test
        @DisplayName("should migrate javax.xml.ws:jaxws-api")
        void migrateJaxwsApi() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.ws</groupId>
                                <artifactId>jaxws-api</artifactId>
                                <version>2.3.1</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.xml.ws"), "groupId should be migrated: " + output);
            assertTrue(output.contains("jakarta.xml.ws-api"), "artifactId should be migrated: " + output);
        }

        @Test
        @DisplayName("should not modify POM with no legacy Java EE dependencies")
        void noLegacyDependencies() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>org.slf4j</groupId>
                                <artifactId>slf4j-api</artifactId>
                                <version>1.7.36</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should not modify POM with no dependencies section")
        void noDependenciesSection() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }
    }

    @Nested
    @DisplayName("Version handling")
    class VersionHandlingTests {

        private JakartaEeApiMigrationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new JakartaEeApiMigrationStrategy();
        }

        @Test
        @DisplayName("should update version when explicitly declared")
        void updatesExplicitVersion() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.bind</groupId>
                                <artifactId>jaxb-api</artifactId>
                                <version>2.2.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            strategy.doApply(context, Map.of(POM_PATH, doc));

            String output = doc.toXml();
            assertTrue(output.contains("2.3.3"), "version should be updated to Jakarta equivalent: " + output);
            assertFalse(output.contains("2.2.0"), "old version should be replaced: " + output);
        }

        @Test
        @DisplayName("should not add version when dependency has no version element (managed by depMgmt)")
        void preservesManagedDependencyWithoutVersion() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>javax.xml.bind</groupId>
                                    <artifactId>jaxb-api</artifactId>
                                    <version>2.3.1</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.bind</groupId>
                                <artifactId>jaxb-api</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            strategy.doApply(context, Map.of(POM_PATH, doc));

            String output = doc.toXml();
            // Both the depMgmt entry and the plain dep should have their groupId migrated
            assertTrue(output.contains("jakarta.xml.bind"), "groupId should be migrated: " + output);
            // The depMgmt entry has 2.3.3; the plain dep had no <version> so none should be added
            assertTrue(output.contains("2.3.3"), "dependencyManagement version should be updated: " + output);
            // Find the plain <dependencies> section (after </dependencyManagement>) and verify
            // it does NOT contain a <version> element for the migrated dependency
            int depMgmtClose = output.indexOf("</dependencyManagement>");
            assertTrue(depMgmtClose > 0, "dependencyManagement should be present: " + output);
            String plainDepsSection = output.substring(depMgmtClose);
            assertFalse(
                    plainDepsSection.contains("<version>2.3.3</version>"),
                    "Plain dependency (no version) should not have a version element added: " + plainDepsSection);
        }
    }

    @Nested
    @DisplayName("Dependency management migration")
    class DependencyManagementTests {

        private JakartaEeApiMigrationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new JakartaEeApiMigrationStrategy();
        }

        @Test
        @DisplayName("should migrate legacy dependencies in dependencyManagement")
        void migratesInDependencyManagement() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0</version>
                        <packaging>pom</packaging>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>javax.xml.bind</groupId>
                                    <artifactId>jaxb-api</artifactId>
                                    <version>2.3.1</version>
                                </dependency>
                                <dependency>
                                    <groupId>javax.annotation</groupId>
                                    <artifactId>javax.annotation-api</artifactId>
                                    <version>1.3.2</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.xml.bind"), "JAXB groupId should be migrated: " + output);
            assertTrue(output.contains("jakarta.annotation"), "annotation groupId should be migrated: " + output);
            assertFalse(output.contains("javax.xml.bind"), "old JAXB groupId should be gone: " + output);
            assertFalse(output.contains("javax.annotation"), "old annotation groupId should be gone: " + output);
        }
    }

    @Nested
    @DisplayName("Profile dependency migration")
    class ProfileDependencyTests {

        private JakartaEeApiMigrationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new JakartaEeApiMigrationStrategy();
        }

        @Test
        @DisplayName("should migrate legacy dependencies inside profiles")
        void migratesInProfiles() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <profiles>
                            <profile>
                                <id>legacy-jee</id>
                                <dependencies>
                                    <dependency>
                                        <groupId>javax.activation</groupId>
                                        <artifactId>javax.activation-api</artifactId>
                                        <version>1.2.0</version>
                                    </dependency>
                                </dependencies>
                            </profile>
                        </profiles>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String output = doc.toXml();
            assertTrue(output.contains("jakarta.activation"), "groupId should be migrated in profile: " + output);
            assertFalse(output.contains("javax.activation"), "old groupId should be gone in profile: " + output);
        }
    }

    @Nested
    @DisplayName("Single dependency unit tests")
    class SingleDependencyUnitTests {

        private JakartaEeApiMigrationStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new JakartaEeApiMigrationStrategy();
        }

        @Test
        @DisplayName("migrateDependency returns false for non-legacy groupId")
        void noMigrationForUnknownArtifact() {
            String depXml = """
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>my-lib</artifactId>
                        <version>1.0</version>
                    </dependency>
                    """;
            Document doc = Document.of("<deps>" + depXml + "</deps>");
            Element dep = doc.root().childElement("dependency").orElseThrow();
            UpgradeContext context = TestUtils.createMockContext();

            assertFalse(strategy.migrateDependency(dep, context));
        }

        @Test
        @DisplayName("migrateDependency returns true and updates groupId for jaxb-api")
        void migratesJaxbApiDependency() {
            String depXml = """
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    """;
            Document doc = Document.of("<deps>" + depXml + "</deps>");
            Element dep = doc.root().childElement("dependency").orElseThrow();
            UpgradeContext context = TestUtils.createMockContext();

            assertTrue(strategy.migrateDependency(dep, context));
            assertEquals("jakarta.xml.bind", dep.childTextTrimmed("groupId"));
            assertEquals("jakarta.xml.bind-api", dep.childTextTrimmed("artifactId"));
            assertEquals("2.3.3", dep.childTextTrimmed("version"));
        }
    }

    @Nested
    @DisplayName("Migration map completeness")
    class MigrationMapTests {

        @Test
        @DisplayName("migration map should contain all JEP-320 removed Java EE APIs")
        void migrationMapCompleteness() {
            Map<String, JakartaEeApiMigrationStrategy.Replacement> map =
                    JakartaEeApiMigrationStrategy.LEGACY_TO_JAKARTA;

            assertTrue(map.containsKey("javax.xml.bind:jaxb-api"), "JAXB API should be mapped");
            assertTrue(map.containsKey("javax.xml.ws:jaxws-api"), "JAX-WS API should be mapped");
            assertTrue(
                    map.containsKey("javax.annotation:javax.annotation-api"), "javax.annotation API should be mapped");
            assertTrue(map.containsKey("javax.activation:activation"), "activation API (old) should be mapped");
            assertTrue(
                    map.containsKey("javax.activation:javax.activation-api"), "activation API (new) should be mapped");
            assertTrue(map.containsKey("javax.jws:javax.jws-api"), "JWS API should be mapped");
        }

        @Test
        @DisplayName("all replacement versions should be non-null and non-empty")
        void replacementVersionsNonEmpty() {
            for (Map.Entry<String, JakartaEeApiMigrationStrategy.Replacement> entry :
                    JakartaEeApiMigrationStrategy.LEGACY_TO_JAKARTA.entrySet()) {
                JakartaEeApiMigrationStrategy.Replacement r = entry.getValue();
                assertFalse(r.groupId().isBlank(), entry.getKey() + " should have groupId");
                assertFalse(r.artifactId().isBlank(), entry.getKey() + " should have artifactId");
                assertFalse(r.version().isBlank(), entry.getKey() + " should have version");
            }
        }
    }
}
