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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.project.MavenProject;

/**
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold (Extract class)
 */
@Named
@Singleton
public class LifecyclePluginResolver
{
    private final PluginVersionResolver pluginVersionResolver;

    @Inject
    public LifecyclePluginResolver( PluginVersionResolver pluginVersionResolver )
    {
        this.pluginVersionResolver = pluginVersionResolver;
    }

    public void resolveMissingPluginVersions( MavenProject project, MavenSession session )
        throws PluginVersionResolutionException
    {
        Map<String, String> versions = new HashMap<>( 64 );

        List<Plugin> newPlugins = null;
        Build build = project.getBuild();
        List<Plugin> plugins = build.getPlugins();
        for ( int i = 0; i < plugins.size(); i++ )
        {
            Plugin plugin = plugins.get( i );
            if ( plugin.getVersion() == null )
            {
                PluginVersionRequest request = new DefaultPluginVersionRequest( plugin, session.getRepositorySession(),
                                                                                project.getRemotePluginRepositories() );
                String version = pluginVersionResolver.resolve( request ).getVersion();
                if ( newPlugins == null )
                {
                    newPlugins = new ArrayList<>( plugins );
                }
                plugin = plugin.withVersion( version );
                newPlugins.set( i, plugin );
            }
            versions.put( plugin.getKey(), plugin.getVersion() );
        }
        if ( newPlugins != null )
        {
            build = build.withPlugins( newPlugins );
            project.setBuild( build );
        }

        PluginManagement pluginManagement = project.getPluginManagement();
        if ( pluginManagement != null )
        {
            newPlugins = null;
            plugins = pluginManagement.getPlugins();
            for ( int i = 0; i < plugins.size(); i++ )
            {
                Plugin plugin = plugins.get( i );
                if ( plugin.getVersion() == null )
                {
                    String version = versions.get( plugin.getKey() );
                    if ( version == null )
                    {
                        PluginVersionRequest request =
                            new DefaultPluginVersionRequest( plugin, session.getRepositorySession(),
                                                             project.getRemotePluginRepositories() );
                        version = pluginVersionResolver.resolve( request ).getVersion();
                    }
                    if ( newPlugins == null )
                    {
                        newPlugins = new ArrayList<>( plugins );
                    }
                    plugin = plugin.withVersion( version );
                    newPlugins.set( i, plugin );
                }
            }
            if ( newPlugins != null )
            {
                project.setBuild( build.withPluginManagement(
                        build.getPluginManagement().withPlugins( newPlugins ) ) );
            }
        }
    }
}