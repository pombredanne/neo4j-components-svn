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
package org.neo4j.index.future.lucene;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.lucene.search.Query;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.kernel.impl.transaction.xaframework.XaConnectionHelpImpl;
import org.neo4j.kernel.impl.transaction.xaframework.XaResourceHelpImpl;
import org.neo4j.kernel.impl.transaction.xaframework.XaResourceManager;

/**
 * An XA connection used with {@link LuceneDataSource}.
 * This class is public because the XA framework requires it.
 */
class LuceneXaConnection extends XaConnectionHelpImpl
{
    private final LuceneXaResource xaResource;

    LuceneXaConnection( Object identifier, XaResourceManager xaRm, 
        byte[] branchId )
    {
        super( xaRm );
        xaResource = new LuceneXaResource( identifier, xaRm, branchId );
    }
    
    @Override
    public XAResource getXaResource()
    {
        return xaResource;
    }
    
    private static class LuceneXaResource extends XaResourceHelpImpl
    {
        private final Object identifier;
        
        LuceneXaResource( Object identifier, XaResourceManager xaRm, 
            byte[] branchId )
        {
            super( xaRm, branchId );
            this.identifier = identifier;
        }
        
        @Override
        public boolean isSameRM( XAResource xares )
        {
            if ( xares instanceof LuceneXaResource )
            {
                return identifier.equals( 
                    ((LuceneXaResource) xares).identifier );
            }
            return false;
        }
    }

    private LuceneTransaction luceneTx;
    
    LuceneTransaction getLuceneTx()
    {
        if ( luceneTx == null )
        {
            try
            {
                luceneTx = ( LuceneTransaction ) getTransaction();
            }
            catch ( XAException e )
            {
                throw new RuntimeException( "Unable to get lucene tx", e );
            }
        }
        return luceneTx;
    }
    
    <T extends PropertyContainer> void add( LuceneIndex<T> index,
            T entity, String key, Object value )
    {
        getLuceneTx().add( index, entity, key, value );
    }
    
    <T extends PropertyContainer> void remove( LuceneIndex<T> index,
            T entity, String key, Object value )
    {
        getLuceneTx().remove( index, entity, key, value );
    }
    
    <T extends PropertyContainer> void remove( LuceneIndex<T> index,
            Query query )
    {
        getLuceneTx().remove( index, query );
    }
}
