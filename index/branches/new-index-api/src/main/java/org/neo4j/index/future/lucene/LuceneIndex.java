package org.neo4j.index.future.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.neo4j.commons.iterator.CombiningIterator;
import org.neo4j.commons.iterator.IteratorAsIterable;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.ReadOnlyIndexException;
import org.neo4j.index.impl.IdToEntityIterator;
import org.neo4j.index.impl.SimpleIndexHits;

abstract class LuceneIndex<T extends PropertyContainer> implements Index<T>
{
    static final String KEY_DOC_ID = "_id_";
    
    final LuceneIndexProvider service;
    final IndexIdentifier identifier;

    LuceneIndex( LuceneIndexProvider service, IndexIdentifier identifier )
    {
        this.service = service;
        this.identifier = identifier;
    }
    
    LuceneXaConnection getConnection()
    {
        if ( service.broker == null )
        {
            throw new ReadOnlyIndexException();
        }
        return service.broker.acquireResourceConnection();
    }
    
    LuceneXaConnection getReadOnlyConnection()
    {
        return service.broker == null ? null :
                service.broker.acquireReadOnlyResourceConnection();
    }
    
    public void add( T entity, String key, Object value )
    {
        getConnection().add( this, entity, key, value );
    }

    public void remove( T entity, String key, Object value )
    {
        getConnection().remove( this, entity, key, value );
    }
    
    IndexType getIndexType()
    {
        return identifier.getType( service.dataSource.config );
    }
    
    public IndexHits<T> get( String key, Object value )
    {
        return query( getIndexType().get( key, value ) );
    }

    public IndexHits<T> query( String key, Object queryOrQueryObject )
    {
        Query query = queryOrQueryObject instanceof Query ? (Query) queryOrQueryObject :
                getIndexType().query( key, queryOrQueryObject );
        return query( query );
    }

    public IndexHits<T> query( Object queryOrQueryObject )
    {
        Query query = queryOrQueryObject instanceof Query ? (Query) queryOrQueryObject :
                getIndexType().query( null, queryOrQueryObject );
        return query( query );
    }
    
    private IndexHits<T> query( Query query )
    {
        List<Long> ids = new ArrayList<Long>();
        LuceneXaConnection con = getReadOnlyConnection();
        LuceneTransaction luceneTx = null;
        if ( con != null )
        {
            luceneTx = getReadOnlyConnection().getLuceneTx();
        }
        Set<Long> addedIds = Collections.emptySet();
        Set<Long> removedIds = Collections.emptySet();
        if ( luceneTx != null )
        {
            addedIds = luceneTx.getAddedIds( this, query );
            ids.addAll( addedIds );
            removedIds = luceneTx.getRemovedIds( this, query );
        }
        service.dataSource.getReadLock();
        Iterator<Long> idIterator = null;
        Integer idIteratorSize = null;
        IndexSearcherRef searcher = null;
        boolean isLazy = false;
        try
        {
            searcher = service.dataSource.getIndexSearcher( identifier );
            if ( searcher != null )
            {
                DocToIdIterator searchedNodeIds = search( searcher,
                        query, removedIds );
                if ( searchedNodeIds.size() >= service.lazynessThreshold )
                {
                    // Instantiate a lazy iterator
                    isLazy = true;
                    Collection<Iterator<Long>> iterators = new ArrayList<Iterator<Long>>();
                    iterators.add( ids.iterator() );
                    iterators.add( searchedNodeIds );
                    idIterator = new CombiningIterator<Long>( iterators );
                    idIteratorSize = ids.size() + searchedNodeIds.size();
                }
                else
                {
                    // Loop through result here (and cache it if possible)
                    readNodesFromHits( searchedNodeIds, ids );
                }
            }
        }
        finally
        {
            // The DocToIdIterator closes the IndexSearchRef instance anyways,
            // or the LazyIterator if it's a lazy one. So no need here.
            service.dataSource.releaseReadLock();
        }

        if ( idIterator == null )
        {
            idIterator = ids.iterator();
            idIteratorSize = ids.size();
        }

        IndexHits<T> hits = new SimpleIndexHits<T>(
                new IteratorAsIterable<T>( new IdToEntityIterator<T>( idIterator )
                {
                    @Override
                    protected T getEntity( long id )
                    {
                        return getById( id );
                    }
                } ), idIteratorSize );
        if ( isLazy )
        {
            hits = new LazyIndexHits<T>( hits, searcher );
        }
        return hits;
    }

    private DocToIdIterator search( IndexSearcherRef searcher,
            Query query, Set<Long> deletedNodes )
    {
        try
        {
            searcher.incRef();
            Hits hits = searcher.getSearcher().search( query );
            return new DocToIdIterator( new HitsIterator( hits ), deletedNodes,
                    searcher );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to query " + this + " with "
                                        + query, e );
        }
    }

    private void readNodesFromHits( DocToIdIterator searchedIds,
            Collection<Long> ids )
    {
        ArrayList<Long> readNodeIds = new ArrayList<Long>();
        while ( searchedIds.hasNext() )
        {
            Long readNodeId = searchedIds.next();
            ids.add( readNodeId );
            readNodeIds.add( readNodeId );
        }
    }
    
    protected abstract T getById( long id );
    
    static class NodeIndex extends LuceneIndex<Node>
    {
        NodeIndex( LuceneIndexProvider service,
                IndexIdentifier identifier )
        {
            super( service, identifier );
        }

        @Override
        protected Node getById( long id )
        {
            return service.graphDb.getNodeById( id );
        }
    }
    
    static class RelationshipIndex extends LuceneIndex<Relationship>
    {
        RelationshipIndex( LuceneIndexProvider service,
                IndexIdentifier identifier )
        {
            super( service, identifier );
        }

        @Override
        protected Relationship getById( long id )
        {
            return service.graphDb.getRelationshipById( id );
        }
    }
}
