package org.apache.maven.project;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;

/**
 * A stub implementation that assumes an empty lifecycle to bypass interaction with the plugin manager and to avoid
 * plugin artifact resolution from repositories.
 *
 * @author Benjamin Bentmann
 */
public class EmptyLifecycleExecutor
    implements LifecycleExecutor
{

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
    {
        return new MavenExecutionPlan( null, null );
    }

    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, boolean setup, String... tasks )
    {
        return new MavenExecutionPlan( null, null );
    }

    public void execute( MavenSession session )
    {
    }

    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        // NOTE: The upper-case packaging name is intentional, that's a special hinting mode used for certain tests
        if ( "JAR".equals( packaging ) )
        {
            return new LinkedHashSet<>( Arrays.asList(
                    newPlugin( "maven-compiler-plugin", "compile", "testCompile" ),
                    newPlugin( "maven-resources-plugin", "resources", "testResources" ),
                    newPlugin( "maven-surefire-plugin", "test" ),
                    newPlugin( "maven-jar-plugin", "jar" ),
                    newPlugin( "maven-install-plugin", "install" ),
                    newPlugin( "maven-deploy-plugin", "deploy" )
            ) );
        }
        else
        {
            return Collections.emptySet();
        }
    }

    private Plugin newPlugin( String artifactId, String... goals )
    {
        return Plugin.newBuilder()
                .groupId( "org.apache.maven.plugins" )
                .artifactId( artifactId )
                .executions( Arrays.stream( goals )
                        .map( this::newPluginExecution )
                        .collect( Collectors.toList() ) )
                .build();
    }

    private PluginExecution newPluginExecution( String goal )
    {
        return PluginExecution.newBuilder()
                .id( "default-" + goal )
                .goals( Collections.singletonList( goal ) )
                .build();
    }

    public void calculateForkedExecutions( MojoExecution mojoExecution, MavenSession session )
    {
    }

    public List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session )
    {
        return Collections.emptyList();
    }

}
