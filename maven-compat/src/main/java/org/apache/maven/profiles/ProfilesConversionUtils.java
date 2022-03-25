package org.apache.maven.profiles;

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

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProfilesConversionUtils
 */
@Deprecated
public class ProfilesConversionUtils
{
    private ProfilesConversionUtils()
    {
    }

    public static org.apache.maven.model.Profile convertFromProfileXmlProfile( Profile profileXmlProfile )
    {
        org.apache.maven.model.Profile.Builder profile = org.apache.maven.model.Profile.newBuilder();

        profile.id( profileXmlProfile.getId() );

        profile.location( "", new org.apache.maven.model.InputLocation(
                new org.apache.maven.model.InputSource( "settings.xml", null ) ) );

        org.apache.maven.profiles.Activation settingsActivation = profileXmlProfile.getActivation();

        if ( settingsActivation != null )
        {
            org.apache.maven.model.Activation.Builder activation = org.apache.maven.model.Activation.newBuilder();

            activation.activeByDefault( settingsActivation.isActiveByDefault() );

            activation.jdk( settingsActivation.getJdk() );

            org.apache.maven.profiles.ActivationProperty settingsProp = settingsActivation.getProperty();
            if ( settingsProp != null )
            {
                activation.property( org.apache.maven.model.ActivationProperty.newBuilder()
                        .name( settingsProp.getName() )
                        .value( settingsProp.getValue() )
                        .build() );
            }

            org.apache.maven.profiles.ActivationOS settingsOs = settingsActivation.getOs();
            if ( settingsOs != null )
            {
                activation.os( org.apache.maven.model.ActivationOS.newBuilder()
                        .arch( settingsOs.getArch() )
                        .family( settingsOs.getFamily() )
                        .name( settingsOs.getName() )
                        .version( settingsOs.getVersion() )
                        .build() );
            }

            org.apache.maven.profiles.ActivationFile settingsFile = settingsActivation.getFile();
            if ( settingsFile != null )
            {
                activation.file( org.apache.maven.model.ActivationFile.newBuilder()
                        .exists( settingsFile.getExists() )
                        .missing( settingsFile.getMissing() )
                        .build() );
            }

            profile.activation( activation.build() );
        }

        profile.properties( profileXmlProfile.getProperties() );

        List<org.apache.maven.profiles.Repository> repos = profileXmlProfile.getRepositories();
        if ( repos != null )
        {
            profile.repositories( repos.stream()
                    .map( ProfilesConversionUtils::convertFromProfileXmlRepository )
                    .collect( Collectors.toList() ) );
        }

        List<org.apache.maven.profiles.Repository> pluginRepos = profileXmlProfile.getPluginRepositories();
        if ( pluginRepos != null )
        {
            profile.pluginRepositories( pluginRepos.stream()
                    .map( ProfilesConversionUtils::convertFromProfileXmlRepository )
                    .collect( Collectors.toList() ) );
        }

        return profile.build();
    }

    private static org.apache.maven.model.Repository convertFromProfileXmlRepository(
            org.apache.maven.profiles.Repository profileXmlRepo )
    {
        org.apache.maven.model.Repository.Builder repo = org.apache.maven.model.Repository.newBuilder();

        repo.id( profileXmlRepo.getId() );
        repo.layout( profileXmlRepo.getLayout() );
        repo.name( profileXmlRepo.getName() );
        repo.url( profileXmlRepo.getUrl() );

        if ( profileXmlRepo.getSnapshots() != null )
        {
            repo.snapshots( convertRepositoryPolicy( profileXmlRepo.getSnapshots() ) );
        }
        if ( profileXmlRepo.getReleases() != null )
        {
            repo.releases( convertRepositoryPolicy( profileXmlRepo.getReleases() ) );
        }

        return repo.build();
    }

    private static org.apache.maven.model.RepositoryPolicy convertRepositoryPolicy( RepositoryPolicy profileXmlPolicy )
    {
        org.apache.maven.model.RepositoryPolicy policy = org.apache.maven.model.RepositoryPolicy.newBuilder()
                .enabled( Boolean.toString( profileXmlPolicy.isEnabled() ) )
                .updatePolicy( profileXmlPolicy.getUpdatePolicy() )
                .checksumPolicy( profileXmlPolicy.getChecksumPolicy() )
                .build();
        return policy;
   }

}
