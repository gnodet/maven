package org.apache.maven.model.building;

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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.building.Source;
import org.apache.maven.model.Model;

/**
 * 
 * @author Robert Scholte
 * @since 3.7.0
 */
class DefaultTransformerContext implements TransformerContext
{
    final Map<String, String> userProperties = new HashMap<>();
    
    final Map<Path, Model> modelByPath = new HashMap<>();

    final Map<GAKey, Model> modelByGA = new HashMap<>();

    final Map<GAKey, Set<Source>> mappedSources = new ConcurrentHashMap<>( 64 );

    @Override
    public String getUserProperty( String key )
    {
        return userProperties.get( key );
    }

    @Override
    public Model getRawModel( Path p )
    {
        return modelByPath.get( p );
    }

    @Override
    public Model getRawModel( String groupId, String artifactId )
    {
        return modelByGA.get( new GAKey( groupId, artifactId ) );
    }

    public Source getSource( String groupId, String artifactId )
    {
        Set<Source> sources = mappedSources.get( new DefaultTransformerContext.GAKey( groupId, artifactId ) );
        if ( sources == null )
        {
            return null;
        }
        return sources.stream().reduce( ( a, b ) ->
        {
            throw new IllegalStateException( "No unique Source for " + groupId + ':' + artifactId
                    + ": " + a.getLocation() + " and " + b.getLocation() );
        } ).orElse( null );
    }

    public void putSource( String groupId, String artifactId, Source source )
    {
        mappedSources.computeIfAbsent( new GAKey( groupId, artifactId ), k -> new HashSet<>() ).add( source );
    }
    
    static class GAKey
    {
        private final String groupId;
        private final String artifactId;
        private final int hashCode;
        
        GAKey( String groupId, String artifactId )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.hashCode = Objects.hash( groupId, artifactId );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            GAKey other = (GAKey) obj;
            return Objects.equals( artifactId, other.artifactId ) && Objects.equals( groupId, other.groupId );
        }
    }
}
