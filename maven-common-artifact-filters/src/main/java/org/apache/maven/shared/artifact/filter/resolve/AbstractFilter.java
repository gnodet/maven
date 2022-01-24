package org.apache.maven.shared.artifact.filter.resolve;

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

/**
 * Abstract filter for custom implementations
 *
 * @author Robert Scholte
 * @since 3.0
 */
public abstract class AbstractFilter implements TransformableFilter
{

    /** {@inheritDoc} */
    @Override
    public final <T> T transform( FilterTransformer<T> transformer )
    {
        return transformer.transform( this );
    }

    /**
     * <p>accept.</p>
     *
     * @param node {@link org.apache.maven.shared.artifact.filter.resolve.Node}
     * @param parents {@link org.apache.maven.shared.artifact.filter.resolve.Node}s.
     * @return {@code true} / {@code false}
     */
    public abstract boolean accept( Node node, List<Node> parents );
}
