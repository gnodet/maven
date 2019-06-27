package org.apache.maven.model.profile;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;

/**
 * Handles profile injection into the model.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
@SuppressWarnings( { "checkstyle:methodname" } )
public class DefaultProfileInjector
    implements ProfileInjector
{

    private ProfileModelMerger merger = new ProfileModelMerger();

    @Override
    public void injectProfile( Model model, Profile profile, ModelBuildingRequest request,
                               ModelProblemCollector problems )
    {
        if ( profile != null )
        {
            merger.mergeModelBase( model, profile );

            if ( profile.getBuild() != null )
            {
                if ( model.getBuild() == null )
                {
                    model.setBuild( new Build() );
                }
                merger.mergeBuildBase( model, profile );
            }
        }
    }

    /**
     * ProfileModelMerger
     */
    protected static class ProfileModelMerger
        extends MavenModelMerger
    {

        public void mergeModelBase( ModelBase target, ModelBase source )
        {
            Map<Object, Object> context = new HashMap<>();
            context.put( "org.apache.maven.model.target", target );
            context.put( "org.apache.maven.model.source", source );
            context.put( "org.apache.maven.model.target.path", new ArrayDeque<>( Arrays.asList( "project" ) ) );
            context.put( "org.apache.maven.model.source.path", new ArrayDeque<>( Arrays.asList( "profile" ) ) );

            mergeModelBase( target, source, true, context );
        }

        public void mergeBuildBase( Model target, Profile source )
        {
            Map<Object, Object> context = new HashMap<>();
            context.put( "org.apache.maven.model.target", target );
            context.put( "org.apache.maven.model.source", source );
            context.put( "org.apache.maven.model.target.path", new ArrayDeque<>( Arrays.asList( "project/build" ) ) );
            context.put( "org.apache.maven.model.source.path", new ArrayDeque<>( Arrays.asList( "profile/build" ) ) );

            mergeBuildBase( target.getBuild(), source.getBuild(), true, context );
        }

        @Override
        protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();
                Map<Object, Plugin> master = new LinkedHashMap<>( tgt.size() * 2 );

                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey( element );
                    master.put( key, element );
                }

                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<>();
                List<Plugin> pending = new ArrayList<>();
                for ( Plugin element : src )
                {
                    Object key = getPluginKey( element );
                    Plugin existing = master.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( existing, element, sourceDominant, context );

                        if ( !pending.isEmpty() )
                        {
                            predecessors.put( key, pending );
                            pending = new ArrayList<>();
                        }
                    }
                    else
                    {
                        pending.add( element );
                    }
                }

                List<Plugin> result = new ArrayList<>( src.size() + tgt.size() );
                for ( Map.Entry<Object, Plugin> entry : master.entrySet() )
                {
                    List<Plugin> pre = predecessors.get( entry.getKey() );
                    if ( pre != null )
                    {
                        result.addAll( pre );
                    }
                    result.add( entry.getValue() );
                }
                result.addAll( pending );

                target.setPlugins( result );

                Map<Integer, Integer> tgtInd = new HashMap<>();
                Map<Integer, Integer> srcInd = new HashMap<>();
                for ( int dstIdx = 0; dstIdx < result.size(); dstIdx++ )
                {
                    Object key = getPluginKey( result.get( dstIdx ) );
                    for ( int srcIdx = 0; srcIdx < src.size(); srcIdx++ )
                    {
                        if ( Objects.equals( getPluginKey( src.get( srcIdx ) ), key ) )
                        {
                            srcInd.put( srcIdx, dstIdx );
                        }
                    }
                    for ( int tgtIdx = 0; tgtIdx < tgt.size(); tgtIdx++ )
                    {
                        if ( Objects.equals( getPluginKey( tgt.get( tgtIdx ) ), key ) )
                        {
                            tgtInd.put( tgtIdx, dstIdx );
                        }
                    }
                }
                move( context, "plugins", tgtInd, srcInd );
            }
        }

        @Override
        protected void mergePlugin_Executions( Plugin target, Plugin source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<PluginExecution> src = source.getExecutions();
            if ( !src.isEmpty() )
            {
                List<PluginExecution> tgt = target.getExecutions();
                Map<Object, PluginExecution> merged =
                    new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( PluginExecution element : tgt )
                {
                    Object key = getPluginExecutionKey( element );
                    merged.put( key, element );
                }

                for ( PluginExecution element : src )
                {
                    Object key = getPluginExecutionKey( element );
                    PluginExecution existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergePluginExecution( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        merged.put( key, element );
                    }
                }

                target.setExecutions( new ArrayList<>( merged.values() ) );
            }
        }

        @Override
        protected void mergeReporting_Plugins( Reporting target, Reporting source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<ReportPlugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged =
                    new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( ReportPlugin element : tgt )
                {
                    Object key = getReportPluginKey( element );
                    merged.put( key, element );
                }

                for ( ReportPlugin element : src )
                {
                    Object key = getReportPluginKey( element );
                    ReportPlugin existing = merged.get( key );
                    if ( existing == null )
                    {
                        merged.put( key, element );
                    }
                    else
                    {
                        mergeReportPlugin( existing, element, sourceDominant, context );
                    }
                }

                target.setPlugins( new ArrayList<>( merged.values() ) );
            }
        }

        @Override
        protected void mergeReportPlugin_ReportSets( ReportPlugin target, ReportPlugin source, boolean sourceDominant,
                                                     Map<Object, Object> context )
        {
            List<ReportSet> src = source.getReportSets();
            if ( !src.isEmpty() )
            {
                List<ReportSet> tgt = target.getReportSets();
                Map<Object, ReportSet> merged = new LinkedHashMap<>( ( src.size() + tgt.size() ) * 2 );

                for ( ReportSet element : tgt )
                {
                    Object key = getReportSetKey( element );
                    merged.put( key, element );
                }

                for ( ReportSet element : src )
                {
                    Object key = getReportSetKey( element );
                    ReportSet existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergeReportSet( existing, element, sourceDominant, context );
                    }
                    else
                    {
                        merged.put( key, element );
                    }
                }

                target.setReportSets( new ArrayList<>( merged.values() ) );
            }
        }

    }

}
