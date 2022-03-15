package org.apache.maven.model.builder;

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

import java.io.Reader;
import java.util.Collections;
import java.util.Optional;

import org.apache.maven.api.xml.Dom;
import org.apache.maven.model.InputLocation;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class DomBuilder
{

    public static Xpp3Dom build( Reader reader )
            throws XmlPullParserException
    {
        return build( reader, true );
    }

    public static Xpp3Dom build( Reader reader, boolean trim )
            throws XmlPullParserException
    {
        XmlPullParser parser = new MXParser();
        parser.setInput( reader );
        return build( parser, trim );
    }

    public static Xpp3Dom build( XmlPullParser parser )
    {
        return build( parser, true, null );
    }

    public static Xpp3Dom build( XmlPullParser parser, boolean trim )
    {
        return build( parser, trim, null );
    }

    public static Xpp3Dom build( XmlPullParser parser, boolean trim, LocationBuilder locationBuilder )
    {
        // TODO
        return null;
    }

    public static class LocationBuilder
    {

        private final InputLocation location;

        public LocationBuilder( InputLocation location )
        {
            this.location = location;
        }

        public InputLocation getLocation()
        {
            return location;
        }
    }

    public static class Xpp3Dom
            extends org.codehaus.plexus.util.xml.Xpp3Dom
            implements Dom
    {
        private static final Xpp3Dom[] EMPTY_DOM_ARRAY = new Xpp3Dom[0];

        public Xpp3Dom( String name )
        {
            super( name );
        }

        public Xpp3Dom( String name, Object inputLocation )
        {
            super( name, inputLocation );
        }

        public Xpp3Dom( org.codehaus.plexus.util.xml.Xpp3Dom src )
        {
            super( src );
        }

        public Xpp3Dom( org.codehaus.plexus.util.xml.Xpp3Dom src, String name )
        {
            super( src, name );
        }

        @Override
        public Xpp3Dom clone()
        {
            return new Xpp3Dom( this );
        }

        @Override
        public void merge( Dom source )
        {
            mergeXpp3Dom( this, (org.codehaus.plexus.util.xml.Xpp3Dom) source );
        }

        public Xpp3Dom getChild( String name )
        {
            return (Xpp3Dom) super.getChild( name );
        }

        public Xpp3Dom getChild( int i )
        {
            return (Xpp3Dom) super.getChild( i );
        }

        public Xpp3Dom[] getChildren()
        {
            return Optional.ofNullable( this.childList )
                    .orElse( Collections.emptyList() )
                    .stream()
                    .map( Xpp3Dom.class::cast )
                    .toArray( Xpp3Dom[]::new );
        }

        public Xpp3Dom[] getChildren( String name )
        {
            return Optional.ofNullable( this.childList )
                    .orElse( Collections.emptyList() )
                    .stream()
                    .filter( d -> name.equals( d.getName() ) )
                    .map( Xpp3Dom.class::cast )
                    .toArray( Xpp3Dom[]::new );
        }

    }
}
