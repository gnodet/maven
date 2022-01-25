package org.apache.maven.artifact.filter.collection;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test case for ClassifierFilter
 */
public class TestClassifierFilter
        extends AbstractArtifactFeatureFilterTest
{

    @Before
    public void setUp()
        throws Exception
    {
        filterClass = ClassifierFilter.class;
        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        artifacts = factory.getClassifiedArtifacts();

    }

    @Test
    public void testParsing()
        throws Exception
    {
        parsing();

    }

    @Test
    public void testFiltering()
        throws Exception
    {
        Set<Artifact> result = filtering();
        for ( Artifact artifact : result )
        {
            assertTrue( artifact.getClassifier().equals( "one" ) || artifact.getClassifier().equals( "two" ) );
        }
    }

    @Test
    public void testFiltering2()
        throws Exception
    {
        Set<Artifact> result = filtering2();
        for ( Artifact artifact : result )
        {
            assertTrue( artifact.getClassifier().equals( "two" ) || artifact.getClassifier().equals( "four" ) );
        }
    }

    @Test
    public void testFiltering3()
        throws Exception
    {
        filtering3();
    }
}
