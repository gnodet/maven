package org.apache.maven.lifecycle.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifeCyclePluginAnalyzer;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author jdcasey
 * @author Kristian Rosenvold (extracted class only)
 */
@Singleton
@Named
public class DefaultLifecyclePluginAnalyzer
    implements LifeCyclePluginAnalyzer
{
    public static final String DEFAULTLIFECYCLEBINDINGS_MODELID = "org.apache.maven:maven-core:"
        + DefaultLifecyclePluginAnalyzer.class.getPackage().getImplementationVersion() + ":default-lifecycle-bindings";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final PlexusContainer plexusContainer;

    private final DefaultLifecycles defaultLifeCycles;

    @Inject
    public DefaultLifecyclePluginAnalyzer( final PlexusContainer plexusContainer,
                                           final DefaultLifecycles defaultLifeCycles )
    {
        this.plexusContainer = requireNonNull( plexusContainer );
        this.defaultLifeCycles = requireNonNull( defaultLifeCycles );
    }

    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.

    // We are going to take the project packaging and find all plugins in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //

    @Override
    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Looking up lifecycle mappings for packaging " + packaging + " from "
                + Thread.currentThread().getContextClassLoader() );
        }

        LifecycleMapping lifecycleMappingForPackaging = lookupLifecycleMapping( packaging );

        if ( lifecycleMappingForPackaging == null )
        {
            return null;
        }

        Map<String, Plugin> plugins = new LinkedHashMap<>();

        for ( Lifecycle lifecycle : defaultLifeCycles.getLifeCycles() )
        {
            org.apache.maven.lifecycle.mapping.Lifecycle lifecycleConfiguration =
                lifecycleMappingForPackaging.getLifecycles().get( lifecycle.getId() );

            Map<String, LifecyclePhase> phaseToGoalMapping = null;

            if ( lifecycleConfiguration != null )
            {
                phaseToGoalMapping = lifecycleConfiguration.getLifecyclePhases();
            }
            else if ( lifecycle.getDefaultLifecyclePhases() != null )
            {
                phaseToGoalMapping = lifecycle.getDefaultLifecyclePhases();
            }

            if ( phaseToGoalMapping != null )
            {
                for ( Map.Entry<String, LifecyclePhase> goalsForLifecyclePhase : phaseToGoalMapping.entrySet() )
                {
                    String phase = goalsForLifecyclePhase.getKey();
                    LifecyclePhase goals = goalsForLifecyclePhase.getValue();
                    if ( goals != null )
                    {
                        parseLifecyclePhaseDefinitions( plugins, phase, goals );
                    }
                }
            }
        }

        return new HashSet<>( plugins.values() );
    }

    /**
     * Performs a lookup using Plexus API to make sure we can look up only "visible" (see Maven classloading) components
     * from current module and for example not extensions coming from other modules.
     */
    private LifecycleMapping lookupLifecycleMapping( final String packaging )
    {
        try
        {
            return plexusContainer.lookup( LifecycleMapping.class, packaging );
        }
        catch ( ComponentLookupException e )
        {
            if ( e.getCause() instanceof NoSuchElementException )
            {
                return null;
            }
            throw new RuntimeException( e );
        }
    }

    private void parseLifecyclePhaseDefinitions( Map<String, Plugin> plugins, String phase, LifecyclePhase goals )
    {
        InputSource inputSource = new InputSource( DEFAULTLIFECYCLEBINDINGS_MODELID, null );
        InputLocation location = new InputLocation( inputSource );

        List<LifecycleMojo> mojos = goals.getMojos();
        if ( mojos != null )
        {

            for ( int i = 0; i < mojos.size(); i++ )
            {
                LifecycleMojo mojo = mojos.get( i );

                GoalSpec gs = parseGoalSpec( mojo.getGoal() );

                if ( gs == null )
                {
                    logger.warn( "Ignored invalid goal specification '" + mojo.getGoal()
                            + "' from lifecycle mapping for phase " + phase );
                    continue;
                }

                Plugin plugin = Plugin.newBuilder()
                        .groupId( gs.groupId )
                        .artifactId( gs.artifactId )
                        .version( gs.version )
                        .location( "", location )
                        .location( "groupId", location )
                        .location( "artifactId", location )
                        .location( "version", location )
                        .build();

                Plugin existing = plugins.get( plugin.getKey() );
                if ( existing != null )
                {
                    if ( existing.getVersion() == null )
                    {
                        plugin = Plugin.newBuilder( existing )
                                        .version( plugin.getVersion() )
                                        .location( "version", location )
                                        .build();
                    }
                    else
                    {
                        plugin = existing;
                    }
                }

                PluginExecution execution = PluginExecution.newBuilder()
                                .id( getExecutionId( plugin, gs.goal ) )
                                .phase( phase )
                                .priority( i - mojos.size() )
                                .goals( Collections.singletonList( gs.goal ) )
                                .location( "", location )
                                .location( "id", location )
                                .location( "phase", location )
                                .location( "goals", location )
                                .configuration( mojo.getConfiguration() )
                                .build();

                plugin = Plugin.newBuilder( plugin )
                        .dependencies( mojo.getDependencies() )
                        .executions( concat( plugin.getExecutions(), execution ) )
                        .build();

                plugins.put( plugin.getKey(), plugin );
            }
        }
    }

    private <T> List<T> concat( List<T> list, T t )
    {
        List<T> newList = new ArrayList<>( ( list != null ? list.size() : 0 ) + 1 );
        if ( list != null )
        {
            newList.addAll( list );
        }
        newList.add( t );
        return newList;
    }

    private GoalSpec parseGoalSpec( String goalSpec )
    {
        GoalSpec gs = new GoalSpec();

        String[] p = StringUtils.split( goalSpec.trim(), ":" );

        if ( p.length == 3 )
        {
            // <groupId>:<artifactId>:<goal>
            gs.groupId = p[0];
            gs.artifactId = p[1];
            gs.goal = p[2];
        }
        else if ( p.length == 4 )
        {
            // <groupId>:<artifactId>:<version>:<goal>
            gs.groupId = p[0];
            gs.artifactId = p[1];
            gs.version = p[2];
            gs.goal = p[3];
        }
        else
        {
            // invalid
            gs = null;
        }

        return gs;
    }

    private String getExecutionId( Plugin plugin, String goal )
    {
        Set<String> existingIds = new HashSet<>();
        for ( PluginExecution execution : plugin.getExecutions() )
        {
            existingIds.add( execution.getId() );
        }

        String base = "default-" + goal;
        String id = base;

        for ( int index = 1; existingIds.contains( id ); index++ )
        {
            id = base + '-' + index;
        }

        return id;
    }

    static class GoalSpec
    {

        String groupId;

        String artifactId;

        String version;

        String goal;

    }

}
