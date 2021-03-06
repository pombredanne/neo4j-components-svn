/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.core;

import java.util.NoSuchElementException;

import org.neo4j.kernel.impl.util.IntArray;

class FastRelTypeElement extends RelTypeElementIterator
{
    private final IntArray src;

    private int position = 0;
    private Integer nextElement = null;

    FastRelTypeElement( String type, NodeImpl node, IntArray src )
    {
        super( type, node );
        if ( src == null )
        {
            this.src = new IntArray();
        }
        else
        {
            this.src = src;
        }
    }

    @Override
    public boolean hasNext( NodeManager nodeManager )
    {
        if ( nextElement != null )
        {
            return true;
        }
        if ( position >= src.length() )
        {
            while ( getNode().getMoreRelationships( nodeManager ) &&
                position >= src.length() );
        }
        while ( position < src.length() )
        {
            nextElement = src.get(position++);
            return true;
        }
        return false;
    }

    @Override
    public int next( NodeManager nodeManager )
    {
        hasNext( nodeManager );
        if ( nextElement != null )
        {
            Integer elementToReturn = nextElement;
            nextElement = null;
            return elementToReturn;
        }
        throw new NoSuchElementException();
    }
}
