/*
 * Copyright (c) 2002-2009 "Neo Technology," Network Engine for Objects in Lund
 * AB [http://neotechnology.com] This file is part of Neo4j. Neo4j is free
 * software: you can redistribute it and/or modify it under the terms of the GNU
 * Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details. You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.onlinebackup;

import java.io.File;

import org.neo4j.api.core.EmbeddedNeo;

/**
 * Start an EmbeddedNeo from a directory location and wrap it as XA data source.
 */
public class LocalNeoResource extends EmbeddedNeoResource
{
    private LocalNeoResource( final EmbeddedNeo neo )
    {
        super( neo );
    }

    public static LocalNeoResource getInstance( final String storeDir )
    {
        String separator = System.getProperty( "file.separator" );
        String store = storeDir + separator + "neostore";
        if ( !new File( store ).exists() )
        {
            throw new RuntimeException( "Unable to locate local neo store in["
                + storeDir + "]" );
        }
        return new LocalNeoResource( new EmbeddedNeo( storeDir ) );
    }
}
