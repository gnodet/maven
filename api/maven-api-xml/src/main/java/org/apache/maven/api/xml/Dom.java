package org.apache.maven.api.xml;

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

import java.util.Collection;
import java.util.Map;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

public interface Dom
{

    String CHILDREN_COMBINATION_MODE_ATTRIBUTE = "combine.children";

    String CHILDREN_COMBINATION_MERGE = "merge";

    String CHILDREN_COMBINATION_APPEND = "append";

    /**
     * This default mode for combining children DOMs during merge means that where element names match, the process will
     * try to merge the element data, rather than putting the dominant and recessive elements (which share the same
     * element name) as siblings in the resulting DOM.
     */
    String DEFAULT_CHILDREN_COMBINATION_MODE = CHILDREN_COMBINATION_MERGE;

    String SELF_COMBINATION_MODE_ATTRIBUTE = "combine.self";

    String SELF_COMBINATION_OVERRIDE = "override";

    String SELF_COMBINATION_MERGE = "merge";

    String SELF_COMBINATION_REMOVE = "remove";

    /**
     * This default mode for combining a DOM node during merge means that where element names match, the process will
     * try to merge the element attributes and values, rather than overriding the recessive element completely with the
     * dominant one. This means that wherever the dominant element doesn't provide the value or a particular attribute,
     * that value or attribute will be set from the recessive DOM node.
     */
    String DEFAULT_SELF_COMBINATION_MODE = SELF_COMBINATION_MERGE;

    @Nonnull
    String getName();

    @Nullable
    String getValue();

    @Nonnull
    Map<String, String> getAttributes();

    @Nullable
    String getAttribute( @Nonnull String name );

    @Nonnull
    Collection<? extends Dom> getChildren();

    @Nullable
    Dom getChild( String name );

    @Nullable
    Object getInputLocation();

    @Nonnull
    Dom clone();

    default void merge( @Nullable Dom source )
    {
        merge( source, (Boolean) null );
    }

    void merge( @Nullable Dom source, @Nullable Boolean childMergeOverride );

    /**
     * Merge recessive into dominant and return either {@code dominant}
     * with merged information or a clone of {@code recessive} if
     * {@code dominant} is {@code null}.
     *
     * @param dominant the node
     * @param recessive if {@code null}, nothing will happen
     * @return the merged node
     */
    @Nullable
    static Dom merge( @Nullable Dom dominant, @Nullable Dom recessive )
    {
        return merge( dominant, recessive, null );
    }

    @Nullable
    static Dom merge( @Nullable Dom dominant, @Nullable Dom recessive, @Nullable Boolean childMergeOverride )
    {
        if ( recessive == null )
        {
            return dominant;
        }
        else if ( dominant == null )
        {
            return recessive.clone();
        }
        else
        {
            dominant.merge( recessive, childMergeOverride );
            return dominant;
        }
    }

}
