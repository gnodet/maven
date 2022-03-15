package org.apache.maven.toolchain.builder;

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

import org.apache.maven.api.xml.Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

public class DomBuilder
{

    public static Dom build( XmlPullParser parser, boolean strict )
    {
        // TODO
        return null;
    }

    public static class Xpp3Dom
            extends org.codehaus.plexus.util.xml.Xpp3Dom
            implements Dom
    {
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

    }
}
