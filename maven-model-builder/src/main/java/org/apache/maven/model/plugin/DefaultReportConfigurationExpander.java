package org.apache.maven.model.plugin;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.xml.Dom;
import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles expansion of general report plugin configuration into individual report sets.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultReportConfigurationExpander
    implements ReportConfigurationExpander
{

    @Override
    public void expandPluginConfiguration( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            for ( ReportPlugin reportPlugin : reporting.getPlugins() )
            {
                Dom parentDom = reportPlugin.getConfiguration();

                if ( parentDom != null )
                {
                    for ( ReportSet execution : reportPlugin.getReportSets() )
                    {
                        Dom childDom = execution.getConfiguration();
                        if ( childDom != null )
                        {
                            childDom.merge( parentDom );
                        }
                        else
                        {
                            childDom = parentDom.clone();
                        }
                        execution.setConfiguration( childDom );
                    }
                }
            }
        }
    }

}
