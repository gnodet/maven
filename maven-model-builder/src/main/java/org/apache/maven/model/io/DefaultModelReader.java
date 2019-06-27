package org.apache.maven.model.io;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Handles deserialization of a model from some kind of textual format like XML.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelReader
    implements ModelReader
{

    @Override
    public Model read( File input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        Model model = read( new FileInputStream( input ), options );

        model.setPomFile( input );

        return model;
    }

    @Override
    public Model read( Reader input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        try ( final Reader in = input )
        {
            return read( in, isStrict( options ), getSource( options ) );
        }
    }

    @Override
    public Model read( InputStream input, Map<String, ?> options )
        throws IOException
    {
        Objects.requireNonNull( input, "input cannot be null" );

        try ( final XmlStreamReader in = ReaderFactory.newXmlReader( input ) )
        {
            return read( in, isStrict( options ), getSource( options ) );
        }
    }

    private boolean isStrict( Map<String, ?> options )
    {
        Object value = ( options != null ) ? options.get( IS_STRICT ) : null;
        return value == null || Boolean.parseBoolean( value.toString() );
    }

    private InputSource getSource( Map<String, ?> options )
    {
        Object value = ( options != null ) ? options.get( INPUT_SOURCE ) : null;
        return (InputSource) value;
    }

    private static class AbstractTransformer
    {

        final Set<String> locations;

        AbstractTransformer( Set<String> locations )
        {
            this.locations = locations;
        }

        public String transform( String source, String fieldName )
        {
            doTransform( source, fieldName );
            return source;
        }

        private void doTransform( String source, String fieldName )
        {
            if ( source.contains( "${" ) )
            {
                locations.add( fieldName );
            }
        }

        public Xpp3Dom transform( Xpp3Dom source, String fieldName )
        {
            doTransform( source, fieldName );
            return source;
        }

        private void doTransform( Xpp3Dom source, String fieldName )
        {
            if ( source.getValue() != null )
            {
                doTransform( source.getValue(), fieldName );
            }
            for ( String attr : source.getAttributeNames() )
            {
                String value = source.getAttribute( attr );
                doTransform( value, fieldName + "/@" + attr );
            }
            for ( Xpp3Dom child : source.getChildren() )
            {
                int idx = 0, nb = 0;
                for ( Xpp3Dom c : source.getChildren() )
                {
                    if ( c.getName().equals( child.getName() ) )
                    {
                        if ( c == child )
                        {
                            idx = nb;
                        }
                        nb++;
                    }
                }
                if ( nb > 1 )
                {
                    doTransform( child, fieldName + "/" + child.getName() + "[" + idx + "]" );
                }
                else
                {
                    doTransform( child, fieldName + "/" + child.getName() );
                }
            }
        }
    }

    private Model read( Reader reader, boolean strict, InputSource source )
        throws IOException
    {
        try
        {
            if ( source != null )
            {
                final SortedSet<String> locations = new TreeSet<>();
                class Transformer extends AbstractTransformer implements MavenXpp3ReaderEx.ContentTransformer
                {
                    private Transformer( Set<String> locations )
                    {
                        super( locations );
                    }
                }
                Model model = new MavenXpp3ReaderEx( new Transformer( locations ) ).read( reader, strict, source );
                for ( Profile profile : model.getProfiles() )
                {
                    profile.setInterpolationLocations( new TreeSet<String>() );
                }
                for ( String location : locations )
                {
                    if ( location.startsWith( "project/profiles[" ) )
                    {
                        for ( int idx = 0; idx < model.getProfiles().size(); idx++ )
                        {
                            String key = "project/profiles[" + idx + "]";
                            if ( location.startsWith( key ) )
                            {
                                model.getProfiles().get( idx ).getInterpolationLocations()
                                        .add( location.replace( key, "profile" ) );
                            }
                        }
                    }
                }
                model.setInterpolationLocations( locations );
                return model;
            }
            else
            {
                final SortedSet<String> locations = new TreeSet<>();
                class Transformer extends AbstractTransformer implements MavenXpp3Reader.ContentTransformer
                {
                    private Transformer( Set<String> locations )
                    {
                        super( locations );
                    }
                }
                Model model = new MavenXpp3Reader( new Transformer( locations ) ).read( reader, strict );
                for ( Profile profile : model.getProfiles() )
                {
                    profile.setInterpolationLocations( new TreeSet<String>() );
                }
                for ( String location : locations )
                {
                    if ( location.startsWith( "project/profiles[" ) )
                    {
                        for ( int idx = 0; idx < model.getProfiles().size(); idx++ )
                        {
                            String key = "project/profiles[" + idx + "]";
                            if ( location.startsWith( key ) )
                            {
                                model.getProfiles().get( idx ).getInterpolationLocations()
                                        .add( location.replace( key, "profile" ) );
                            }
                        }
                    }
                }
                model.setInterpolationLocations( locations );
                return model;
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new ModelParseException( e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e );
        }
    }

}
