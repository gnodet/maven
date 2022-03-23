package org.apache.maven.model.merge;


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

import java.util.Collections;

import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MavenModelMergerTest
{
    private MavenModelMerger modelMerger = new MavenModelMerger();

    // modelVersion is neither inherited nor injected
    @Test
    public void testMergeModel_ModelVersion()
    {
        Model parent = new Model.Builder().modelVersion( "4.0.0" ).build();
        Model model = new Model.Builder().build();
        Model.Builder builder = new Model.Builder( model );
        modelMerger.mergeModel_ModelVersion( builder, model, parent, false, null );
        assertNull( builder.build().getModelVersion() );

        model = new Model.Builder().modelVersion( "5.0.0" ).build();
        builder = new Model.Builder( model );
        modelMerger.mergeModel_ModelVersion( builder, model, parent, false, null );
        assertEquals( "5.0.0", builder.build().getModelVersion() );
    }

    // ArtifactId is neither inherited nor injected
    @Test
    public void testMergeModel_ArtifactId()
    {
        Model parent = new Model.Builder().artifactId( "PARENT" ).build();
        Model model = new Model.Builder().build();
        Model.Builder builder = new Model.Builder( model );
        modelMerger.mergeModel_ArtifactId( builder, model, parent, false, null );
        assertNull( model.getArtifactId() );

        model = new Model.Builder().artifactId( "MODEL" ).build();
        builder = new Model.Builder( model );
        modelMerger.mergeModel_ArtifactId( builder, model, parent, false, null );
        assertEquals( "MODEL", builder.build().getArtifactId() );
    }

    // Prerequisites are neither inherited nor injected
    @Test
    public void testMergeModel_Prerequisites()
    {
        Model parent = new Model.Builder().prerequisites( new Prerequisites.Builder().build() ).build();
        Model model = new Model.Builder().build();
        Model.Builder builder = new Model.Builder( model );
        modelMerger.mergeModel_Prerequisites( builder, model, parent, false, null );
        assertNull( builder.build().getPrerequisites() );

        Prerequisites modelPrerequisites = new Prerequisites.Builder().maven( "3.0" ).build();
        model = new Model.Builder().prerequisites( modelPrerequisites ).build();
        builder = new Model.Builder( model );
        modelMerger.mergeModel_Prerequisites( builder, model, parent, false, null );
        assertEquals( modelPrerequisites, builder.build().getPrerequisites() );
    }

    // Profiles are neither inherited nor injected
    @Test
    public void testMergeModel_Profiles()
    {
        Model parent = new Model.Builder().profiles( Collections.singletonList( new Profile.Builder().build() ) ).build();
        Model model = new Model.Builder().build();
        Model.Builder builder = new Model.Builder( model );
        modelMerger.mergeModel_Profiles( builder, model, parent, false, null );
        assertEquals( 0, builder.build().getProfiles().size() );

        Profile modelProfile = new Profile.Builder().id( "MODEL" ).build();
        model = new Model.Builder().profiles( Collections.singletonList( modelProfile ) ).build();
        builder = new Model.Builder( model );
        modelMerger.mergeModel_Prerequisites( builder, model, parent, false, null );
        assertEquals( Collections.singletonList( modelProfile ), builder.build().getProfiles() );
    }

}
