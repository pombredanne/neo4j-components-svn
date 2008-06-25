/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.neoclipse.view;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

/**
 * Resolves the properties for Neo nodes and relationships.
 * 
 * @author Peter H&auml;nsgen
 */
public class NeoGraphPropertySourceProvider implements IPropertySourceProvider
{
    public IPropertySource getPropertySource(Object source)
    {
        if (source instanceof Node)
        {
            return new NeoNodePropertySource((Node) source);
        }
        else if (source instanceof Relationship)
        {
            return new NeoRelationshipPropertySource((Relationship) source);
        }
        else
        {
            return null;
        }
    }
}
