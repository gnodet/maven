package org.apache.maven.model.interpolation;

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

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * StringSearchModelInterpolator
 */
@Named
@Singleton
public class StringSearchModelInterpolator
    extends AbstractStringBasedModelInterpolator
{

    private static final Map<Class<?>, InterpolateObjectAction.CacheItem> CACHED_ENTRIES =
        new ConcurrentHashMap<>( 80, 0.75f, 2 );
    public static final int INT = 40;
    // Empirical data from 3.x, actual =40

    private static final Map<Class<?>, Map<String, Field>> FIELDS_CACHE = new HashMap<>();


    @Override
    public Model interpolateModel( Model model, File projectDir, ModelBuildingRequest config,
                                   ModelProblemCollector problems )
    {
        interpolateObject( model, model, projectDir, config, problems );

        return model;
    }

    protected void interpolateObject( Object obj, Model model, File projectDir, ModelBuildingRequest config,
                                      ModelProblemCollector problems )
    {
        try
        {
            List<? extends ValueSource> valueSources = createValueSources( model, projectDir, config, problems );
            List<? extends InterpolationPostProcessor> postProcessors =
                createPostProcessors( model, projectDir, config );

//            Model org = model.clone();
//            Model prev = model.clone();

            PrivilegedAction<Object> action;
            if ( obj == model && model.getInterpolationLocations() != null )
            {
//                new InterpolateObjectAction( prev, valueSources, postProcessors, this, problems ).run();
                action = new InterpolatedModelAction( model, valueSources, postProcessors, this, problems );
            }
            else
            {
                action = new InterpolateObjectAction( obj, valueSources, postProcessors, this, problems );
            }

            AccessController.doPrivileged( action );

            /*
            try
            {
                if ( obj == model && model.getInterpolationLocations() != null )
                {
                    StringWriter sw1 = new StringWriter();
                    new MavenXpp3Writer().write( sw1, prev );
                    StringWriter sw2 = new StringWriter();
                    new MavenXpp3Writer().write( sw2, model );
                    String s1 = sw1.toString();
                    String s2 = sw2.toString();

                    if ( !s1.equals( s2 ) )
                    {
                        char[] c1 = s1.toCharArray();
                        char[] c2 = s2.toCharArray();
                        for ( int i = 0; i < Math.min( c1.length, c2.length ); i++ )
                        {
                            if ( c1[i] != c2[i] )
                            {
                                System.out.println( "differ at " + i );
                                System.out.println( s1.substring( Math.max( i - INT, 0 ),
                                                                  Math.min( i + INT, s1.length() ) )
                                                      .replace( "\n", "" ) );
                                System.out.println( s2.substring( Math.max( i - INT, 0 ),
                                                                  Math.min( i + INT, s2.length() ) )
                                                      .replace( "\n", "" ) );
                                break;
                            }
                        }
                    }
                }
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            */
        }
        finally
        {
            getInterpolator().clearAnswers();
        }
    }

    @Override
    protected Interpolator createInterpolator()
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );

        return interpolator;
    }

    private static final class InterpolatedModelAction
        implements PrivilegedAction<Object>
    {

        private final Model project;

        private final StringSearchModelInterpolator modelInterpolator;

        private final List<? extends ValueSource> valueSources;

        private final List<? extends InterpolationPostProcessor> postProcessors;

        private final ModelProblemCollector problems;

        InterpolatedModelAction( Model target, List<? extends ValueSource> valueSources,
                                        List<? extends InterpolationPostProcessor> postProcessors,
                                        StringSearchModelInterpolator modelInterpolator,
                                        ModelProblemCollector problems )
        {
            this.project = target;
            this.modelInterpolator = modelInterpolator;
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;
            this.problems = problems;
        }

        @Override
        public Object run()
        {
            for ( String location : project.getInterpolationLocations() )
            {
                Object current = this;
                int cur = 0;
                while ( cur < location.length() )
                {
                    int n = getNextTokenEnd( location, cur );
                    String s = location.substring( cur, n );
                    if ( current instanceof Xpp3Dom )
                    {
                        if ( s.startsWith( "@" ) )
                        {
                            setXmlAttribute( (Xpp3Dom) current, s.substring( 1 ) );
                            cur = n + 1;
                            if ( cur < location.length() )
                            {
                                throw new IllegalStateException();
                            }
                        }
                        else
                        {
                            if ( n < location.length() && location.charAt( n ) == '[' )
                            {
                                String name = s;
                                cur = n;
                                n = location.indexOf( ']', cur );
                                s = location.substring( cur + 1, n );
                                int idx = Integer.parseInt( s );
                                int nb = -1;
                                for ( Xpp3Dom child : ( ( Xpp3Dom ) current ).getChildren() )
                                {
                                    if ( child.getName().equals( name ) )
                                    {
                                        if ( ++nb == idx )
                                        {
                                            current = child;
                                            break;
                                        }
                                    }
                                }
                                cur = n + 2;
                            }
                            else
                            {
                                Object next = ( ( Xpp3Dom ) current ).getChild( s );
                                if ( next == null )
                                {
                                    System.err.println( "Retrieved null for " + s + " in " + location );
                                    break;
                                }
                                current = next;
                                cur = n + 1;
                            }
                            if ( cur >= location.length() )
                            {
                                setXmlElementValue( (Xpp3Dom) current );
                            }
                        }
                    }
                    else
                    {
                        Map<String, Field> cache = FIELDS_CACHE.get( current.getClass() );
                        if ( cache == null )
                        {
                            cache = new HashMap<>();
                            FIELDS_CACHE.put( current.getClass(), cache );
                        }
                        Field field = cache.get( s );
                        if ( field == null )
                        {
                            Class<?> clazz = current.getClass();
                            while ( field == null && clazz != null )
                            {
                                try
                                {
                                    field = clazz.getDeclaredField( s );
                                }
                                catch ( NoSuchFieldException e )
                                {
                                    clazz = clazz.getSuperclass();
                                }
                            }
                            if ( field == null )
                            {
                                throw new RuntimeException( "Unable to find field " + s
                                        + " in expression " + location );
                            }
                            field.setAccessible( true );
                            cache.put( s, field );
                        }
                        String index = null;
                        if ( n < location.length() && location.charAt( n ) == '[' )
                        {
                            cur = n;
                            n = location.indexOf( ']', cur );
                            index = location.substring( cur + 1, n );
                            n++;
                        }
                        if ( n < location.length() || index != null )
                        {
                            try
                            {
                                Object next = field.get( current );
                                if ( next == null )
                                {
                                    System.err.println( "Retrieved null for " + s + " in " + location );
                                    break;
                                }
                                current = next;
                            }
                            catch ( Exception e )
                            {
                                throw new RuntimeException(
                                        "Unable to get field " + s + " in expression " + location );
                            }
                            if ( index != null )
                            {
                                if ( n >= location.length() )
                                {
                                    setIndexedValue( current, index );
                                }
                                else
                                {
                                    Object next = getIndexedValue( current, index );
                                    if ( next == null )
                                    {
                                        System.err.println( "Retrieved null for " + s + " in " + location );
                                        break;
                                    }
                                    current = next;
                                }
                            }
                        }
                        else
                        {
                            setFieldValue( location, current, field );
                        }
                        cur = n + 1;
                    }
                }
            }
            return null;
        }

        private void setFieldValue( String location, Object current, Field field )
        {
            try
            {
                String orgv = (String) field.get( current );
                String newv = interpolate( orgv );
                field.set( current, newv );
            }
            catch ( Exception e )
            {
                throw new RuntimeException(
                        "Unable to set field " + field.getName() + " in expression " + location );
            }
        }

        private Object getIndexedValue( Object current, String index )
        {
            if ( current instanceof List )
            {
                current = ( (List) current ).get( Integer.parseInt( index ) );
            }
            else if ( current instanceof Map )
            {
                current = ( (Map) current ).get( index );
            }
            else
            {
                throw new IllegalStateException();
            }
            return current;
        }

        private void setIndexedValue( Object current, String index )
        {
            if ( current instanceof List )
            {
                int idx = Integer.parseInt( index );
                setListElement( (List) current, idx );
            }
            else if ( current instanceof Map )
            {
                setMapElement( (Map) current, index );
            }
            else
            {
                throw new IllegalStateException();
            }
        }

        private void setMapElement( Map current, String index )
        {
            String orgv = (String) current.get( index );
            String newv = interpolate( orgv );
            current.put( index, newv );
        }

        private void setListElement( List current, int idx )
        {
            String orgv = (String) current.get( idx );
            String newv = interpolate( orgv );
            current.set( idx, newv );
        }

        private void setXmlElementValue( Xpp3Dom current )
        {
            String orgv = current.getValue();
            String newv = interpolate( orgv );
            current.setValue( newv );
        }

        private void setXmlAttribute( Xpp3Dom current, String attr )
        {
            String orgv = current.getAttribute( attr );
            String newv = interpolate( orgv );
            current.setAttribute( attr, newv );
        }

        private int getNextTokenEnd( String location, int cur )
        {
            int dotIdx = location.indexOf( '/', cur );
            int bktIdx = location.indexOf( '[', cur );
            return Math.min( dotIdx >= 0
                    ? dotIdx
                    : location.length(), bktIdx >= 0 ? bktIdx : location.length() );
        }

        private String interpolate( String value )
        {
            return modelInterpolator.interpolateInternal( value, valueSources, postProcessors, problems );
        }

    }

    private static final class InterpolateObjectAction
        implements PrivilegedAction<Object>
    {

        private final LinkedList<Object> interpolationTargets;

        private final StringSearchModelInterpolator modelInterpolator;

        private final List<? extends ValueSource> valueSources;

        private final List<? extends InterpolationPostProcessor> postProcessors;

        private final ModelProblemCollector problems;

        InterpolateObjectAction( Object target, List<? extends ValueSource> valueSources,
                                 List<? extends InterpolationPostProcessor> postProcessors,
                                 StringSearchModelInterpolator modelInterpolator, ModelProblemCollector problems )
        {
            this.valueSources = valueSources;
            this.postProcessors = postProcessors;

            this.interpolationTargets = new LinkedList<>();
            interpolationTargets.add( target );

            this.modelInterpolator = modelInterpolator;

            this.problems = problems;
        }

        @Override
        public Object run()
        {
            while ( !interpolationTargets.isEmpty() )
            {
                Object obj = interpolationTargets.removeFirst();

                traverseObjectWithParents( obj.getClass(), obj );
            }

            return null;
        }


        private String interpolate( String value )
        {
            return modelInterpolator.interpolateInternal( value, valueSources, postProcessors, problems );
        }

        private void traverseObjectWithParents( Class<?> cls, Object target )
        {
            if ( cls == null )
            {
                return;
            }

            CacheItem cacheEntry = getCacheEntry( cls );
            if ( cacheEntry.isArray() )
            {
                evaluateArray( target, this );
            }
            else if ( cacheEntry.isQualifiedForInterpolation )
            {
                cacheEntry.interpolate( target, this );

                traverseObjectWithParents( cls.getSuperclass(), target );
            }
        }


        private CacheItem getCacheEntry( Class<?> cls )
        {
            CacheItem cacheItem = CACHED_ENTRIES.get( cls );
            if ( cacheItem == null )
            {
                cacheItem = new CacheItem( cls );
                CACHED_ENTRIES.put( cls, cacheItem );
            }
            return cacheItem;
        }

        private static void evaluateArray( Object target, InterpolateObjectAction ctx )
        {
            int len = Array.getLength( target );
            for ( int i = 0; i < len; i++ )
            {
                Object value = Array.get( target, i );
                if ( value != null )
                {
                    if ( String.class == value.getClass() )
                    {
                        String interpolated = ctx.interpolate( (String) value );

                        if ( !interpolated.equals( value ) )
                        {
                            Array.set( target, i, interpolated );
                        }
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

        private static class CacheItem
        {
            private final boolean isArray;

            private final boolean isQualifiedForInterpolation;

            private final CacheField[] fields;

            private boolean isQualifiedForInterpolation( Class<?> cls )
            {
                return !cls.getName().startsWith( "java" );
            }

            private boolean isQualifiedForInterpolation( Field field, Class<?> fieldType )
            {
                if ( Map.class.equals( fieldType ) && "locations".equals( field.getName() ) )
                {
                    return false;
                }
                if ( Set.class.equals( fieldType ) && "interpolationLocations".equals( field.getName() ) )
                {
                    return false;
                }
                if ( InputLocation.class.equals( fieldType ) )
                {
                    return false;
                }

                //noinspection SimplifiableIfStatement
                if ( fieldType.isPrimitive() )
                {
                    return false;
                }

                return !"parent".equals( field.getName() );
            }

            CacheItem( Class clazz )
            {
                this.isQualifiedForInterpolation = isQualifiedForInterpolation( clazz );
                this.isArray = clazz.isArray();
                List<CacheField> fields = new ArrayList<>();
                for ( Field currentField : clazz.getDeclaredFields() )
                {
                    Class<?> type = currentField.getType();
                    if ( isQualifiedForInterpolation( currentField, type ) )
                    {
                        if ( String.class == type )
                        {
                            if ( !Modifier.isFinal( currentField.getModifiers() ) )
                            {
                                fields.add( new StringField( currentField ) );
                            }
                        }
                        else if ( List.class.isAssignableFrom( type ) )
                        {
                            fields.add( new ListField( currentField ) );
                        }
                        else if ( Collection.class.isAssignableFrom( type ) )
                        {
                            throw new RuntimeException( "We dont interpolate into collections, use a list instead" );
                        }
                        else if ( Map.class.isAssignableFrom( type ) )
                        {
                            fields.add( new MapField( currentField ) );
                        }
                        else
                        {
                            fields.add( new ObjectField( currentField ) );
                        }
                    }
                }
                this.fields = fields.toArray( new CacheField[0] );

            }

            public void interpolate( Object target, InterpolateObjectAction interpolateObjectAction )
            {
                for ( CacheField field : fields )
                {
                    field.interpolate( target, interpolateObjectAction );
                }
            }

            public boolean isArray()
            {
                return isArray;
            }
        }

        abstract static class CacheField
        {
            protected final Field field;

            CacheField( Field field )
            {
                this.field = field;
                field.setAccessible( true );
            }

            void interpolate( Object target, InterpolateObjectAction interpolateObjectAction )
            {
//                synchronized ( field )
//                {
//                    boolean isAccessible = field.isAccessible();
//                    field.setAccessible( true );
                    try
                    {
                        doInterpolate( target, interpolateObjectAction );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        interpolateObjectAction.problems.add(
                            new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage(
                                "Failed to interpolate field3: " + field + " on class: "
                                    + field.getType().getName() ).setException(
                                e ) ); // TODO Not entirely the same message
                    }
                    catch ( IllegalAccessException e )
                    {
                        interpolateObjectAction.problems.add(
                            new ModelProblemCollectorRequest( Severity.ERROR, Version.BASE ).setMessage(
                                "Failed to interpolate field4: " + field + " on class: "
                                    + field.getType().getName() ).setException( e ) );
                    }
//                    finally
//                    {
//                        field.setAccessible( isAccessible );
//                    }
//                }


            }

            abstract void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException;
        }

        static final class StringField
            extends CacheField
        {
            StringField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                String value = (String) field.get( target );
                if ( value == null )
                {
                    return;
                }

                String interpolated = ctx.interpolate( value );

                if ( !interpolated.equals( value ) )
                {
                    field.set( target, interpolated );
                }
            }
        }

        static final class ListField
            extends CacheField
        {
            ListField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                @SuppressWarnings( "unchecked" ) List<Object> c = (List<Object>) field.get( target );
                if ( c == null )
                {
                    return;
                }

                int size = c.size();
                Object value;
                for ( int i = 0; i < size; i++ )
                {

                    value = c.get( i );

                    if ( value != null )
                    {
                        if ( String.class == value.getClass() )
                        {
                            String interpolated = ctx.interpolate( (String) value );

                            if ( !interpolated.equals( value ) )
                            {
                                try
                                {
                                    c.set( i, interpolated );
                                }
                                catch ( UnsupportedOperationException e )
                                {
                                    return;
                                }
                            }
                        }
                        else
                        {
                            if ( value.getClass().isArray() )
                            {
                                evaluateArray( value, ctx );
                            }
                            else
                            {
                                ctx.interpolationTargets.add( value );
                            }
                        }
                    }
                }
            }
        }

        static final class MapField
            extends CacheField
        {
            MapField( Field field )
            {
                super( field );
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                @SuppressWarnings( "unchecked" ) Map<Object, Object> m = (Map<Object, Object>) field.get( target );
                if ( m == null || m.isEmpty() )
                {
                    return;
                }

                for ( Map.Entry<Object, Object> entry : m.entrySet() )
                {
                    Object value = entry.getValue();

                    if ( value == null )
                    {
                        continue;
                    }

                    if ( String.class == value.getClass() )
                    {
                        String interpolated = ctx.interpolate( (String) value );

                        if ( !interpolated.equals( value ) )
                        {
                            try
                            {
                                entry.setValue( interpolated );
                            }
                            catch ( UnsupportedOperationException ignore )
                            {
                                // nop
                            }
                        }
                    }
                    else if ( value.getClass().isArray() )
                    {
                        evaluateArray( value, ctx );
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

        static final class ObjectField
            extends CacheField
        {
            private final boolean isArray;

            ObjectField( Field field )
            {
                super( field );
                this.isArray = field.getType().isArray();
            }

            @Override
            void doInterpolate( Object target, InterpolateObjectAction ctx )
                throws IllegalAccessException
            {
                Object value = field.get( target );
                if ( value != null )
                {
                    if ( isArray )
                    {
                        evaluateArray( value, ctx );
                    }
                    else
                    {
                        ctx.interpolationTargets.add( value );
                    }
                }
            }
        }

    }

}
