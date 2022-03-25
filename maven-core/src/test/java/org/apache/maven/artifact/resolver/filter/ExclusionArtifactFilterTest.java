package org.apache.maven.artifact.resolver.filter;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExclusionArtifactFilterTest
{
    private Artifact artifact;

    @BeforeEach
    public void setup()
    {
        artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "org.apache.maven" );
        when( artifact.getArtifactId() ).thenReturn( "maven-core" );
    }

    @Test
    public void testExcludeExact()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "org.apache.maven" ).artifactId( "maven-core" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testExcludeNoMatch()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "org.apache.maven" ).artifactId( "maven-model" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeGroupIdWildcard()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "*" ).artifactId( "maven-core" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }


    @Test
    public void testExcludeGroupIdWildcardNoMatch()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "*" ).artifactId( "maven-compat" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeArtifactIdWildcard()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "org.apache.maven" ).artifactId( "*" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testExcludeArtifactIdWildcardNoMatch()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "org.apache.groovy" ).artifactId( "*" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( true ) );
    }

    @Test
    public void testExcludeAllWildcard()
    {
        Exclusion exclusion = Exclusion.newBuilder().groupId( "*" ).artifactId( "*" ).build();
        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Collections.singletonList( exclusion ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testMultipleExclusionsExcludeArtifactIdWildcard()
    {
        Exclusion exclusion1 = Exclusion.newBuilder().groupId( "org.apache.groovy" ).artifactId( "*" ).build();
        Exclusion exclusion2 = Exclusion.newBuilder().groupId( "org.apache.maven" ).artifactId( "maven-core" ).build();

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Arrays.asList( exclusion1, exclusion2 ) );

        assertThat( filter.include( artifact ), is( false ) );
    }

    @Test
    public void testMultipleExclusionsExcludeGroupIdWildcard()
    {
        Exclusion exclusion1 = Exclusion.newBuilder().groupId( "*" ).artifactId( "maven-model" ).build();
        Exclusion exclusion2 = Exclusion.newBuilder().groupId( "org.apache.maven" ).artifactId( "maven-core" ).build();

        ExclusionArtifactFilter filter = new ExclusionArtifactFilter( Arrays.asList( exclusion1, exclusion2 ) );

        assertThat( filter.include( artifact ), is( false ) );
    }
}