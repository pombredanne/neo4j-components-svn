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
package org.neo4j.index.lucene;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.impl.cache.LruCache;
import org.neo4j.kernel.impl.transaction.xaframework.LogBackedXaDataSource;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommand;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommandFactory;
import org.neo4j.kernel.impl.transaction.xaframework.XaConnection;
import org.neo4j.kernel.impl.transaction.xaframework.XaContainer;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;
import org.neo4j.kernel.impl.transaction.xaframework.XaLogicalLog;
import org.neo4j.kernel.impl.transaction.xaframework.XaTransaction;
import org.neo4j.kernel.impl.transaction.xaframework.XaTransactionFactory;
import org.neo4j.kernel.impl.util.ArrayMap;

/**
 * An {@link XaDataSource} optimized for the {@link LuceneIndexProvider}.
 * This class is public because the XA framework requires it.
 */
public class LuceneDataSource extends LogBackedXaDataSource
{
    public static final byte[] DEFAULT_BRANCH_ID = "162373".getBytes();
    
    /**
     * Default {@link Analyzer} for fulltext parsing.
     */
    public static final Analyzer LOWER_CASE_WHITESPACE_ANALYZER =
        new Analyzer()
    {
        @Override
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            return new LowerCaseFilter( new WhitespaceTokenizer( reader ) );
        }
    };

    public static final Analyzer WHITESPACE_ANALYZER = new Analyzer()
    {
        @Override
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            return new WhitespaceTokenizer( reader );
        }
    };
    
    public static final Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();
    
    private final ArrayMap<IndexIdentifier,IndexSearcherRef> indexSearchers = 
        new ArrayMap<IndexIdentifier,IndexSearcherRef>( 6, true, true );

    private final XaContainer xaContainer;
    private final String baseStorePath;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); 
    final LuceneIndexStore store;
    private boolean closed;
    private final Cache caching;

    /**
     * Constructs this data source.
     * 
     * @param params XA parameters.
     * @throws InstantiationException if the data source couldn't be
     * instantiated
     */
    public LuceneDataSource( Map<Object,Object> params ) 
        throws InstantiationException
    {
        super( params );
        this.baseStorePath = getStoreDir( params );
        cleanWriteLocks( baseStorePath );
        this.store = new LuceneIndexStore( baseStorePath + "/lucene-store.db" );
        XaCommandFactory cf = new LuceneCommandFactory();
        XaTransactionFactory tf = new LuceneTransactionFactory( store );
        xaContainer = XaContainer.create( this.baseStorePath + "/lucene.log", cf, tf, params );
        try
        {
            xaContainer.openLogicalLog();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to open lucene log in " +
                    this.baseStorePath, e );
        }
        
        xaContainer.getLogicalLog().setKeepLogs(
                shouldKeepLog( (String) params.get( "keep_logical_logs" ), getName() ) );
        setLogicalLogAtCreationTime( xaContainer.getLogicalLog() );
        caching = new Cache();
    }
    
    private void cleanWriteLocks( String directory )
    {
        File dir = new File( directory );
        if ( !dir.isDirectory() )
        {
            return;
        }
        for ( File file : dir.listFiles() )
        {
            if ( file.isDirectory() )
            {
                cleanWriteLocks( file.getAbsolutePath() );
            }
            else if ( file.getName().equals( "write.lock" ) )
            {
                boolean success = file.delete();
                assert success;
            }
        }
    }
    
    private String getStoreDir( Map<Object, Object> params )
    {
        String kernelStoreDir = (String) params.get( "store_dir" );
        File dir = new File( new File( kernelStoreDir ), "index" );
        if ( !dir.exists() )
        {
            if ( !dir.mkdirs() )
            {
                throw new RuntimeException( "Unable to create directory path["
                    + dir.getAbsolutePath() + "] for Neo4j store." );
            }
        }
        return dir.getAbsolutePath();
    }

    @Override
    public void close()
    {
        if ( closed )
        {
            return;
        }
        
        for ( IndexSearcherRef searcher : indexSearchers.values() )
        {
            try
            {
                searcher.dispose();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
        indexSearchers.clear();
        xaContainer.close();
        store.close();
        closed = true;
    }

    @Override
    public XaConnection getXaConnection()
    {
        return new LuceneXaConnection( baseStorePath, xaContainer
            .getResourceManager(), getBranchId() );
    }
    
    private class LuceneCommandFactory extends XaCommandFactory
    {
        LuceneCommandFactory()
        {
            super();
        }

        @Override
        public XaCommand readCommand( ReadableByteChannel channel, 
            ByteBuffer buffer ) throws IOException
        {
            return LuceneCommand.readCommand( channel, buffer );
        }
    }
    
    private class LuceneTransactionFactory extends XaTransactionFactory
    {
        private final LuceneIndexStore store;
        
        LuceneTransactionFactory( LuceneIndexStore store )
        {
            this.store = store;
        }
        
        @Override
        public XaTransaction create( int identifier )
        {
            return createTransaction( identifier, this.getLogicalLog() );
        }

        @Override
        public void flushAll()
        {
            // Not much we can do...
        }

        @Override
        public long getCurrentVersion()
        {
            return store.getVersion();
        }
        
        @Override
        public long getAndSetNewVersion()
        {
            return store.incrementVersion();
        }
    }
    
    void getReadLock()
    {
        lock.readLock().lock();
    }
    
    void releaseReadLock()
    {
        lock.readLock().unlock();
    }
    
    void getWriteLock()
    {
        lock.writeLock().lock();
    }
    
    void releaseWriteLock()
    {
        lock.writeLock().unlock();
    }
    
    /**
     * If nothing has changed underneath (since the searcher was last created
     * or refreshed) {@code null} is returned. But if something has changed a
     * refreshed searcher is returned. It makes use if the
     * {@link IndexReader#reopen()} which faster than opening an index from
     * scratch.
     * 
     * @param searcher the {@link IndexSearcher} to refresh.
     * @return a refreshed version of the searcher or, if nothing has changed,
     * {@code null}.
     * @throws IOException if there's a problem with the index.
     */
    private IndexSearcherRef refreshSearcher( IndexSearcherRef searcher )
    {
        try
        {
            IndexReader reader = searcher.getSearcher().getIndexReader();
            IndexReader reopened = reader.reopen();
            if ( reopened != reader )
            {
                IndexSearcher newSearcher = new IndexSearcher( reopened );
                searcher.detachOrClose();
                return new IndexSearcherRef( searcher.getIdentifier(), newSearcher );
            }
            return null;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    static Directory getDirectory( String storeDir,
            IndexIdentifier identifier ) throws IOException
    {
        File path = new File( storeDir, "lucene" );
        String extra = null;
        if ( identifier.itemsClass.equals( Node.class ) )
        {
            extra = "node";
        }
        else if ( identifier.itemsClass.equals( Relationship.class ) )
        {
            extra = "relationship";
        }
        else
        {
            throw new RuntimeException( identifier.itemsClass.getName() );
        }
        
        path = new File( path, extra );
        return FSDirectory.open( new File( path, identifier.indexName ) );
    }
    
    IndexSearcherRef getIndexSearcher( IndexIdentifier identifier )
    {
        try
        {
            IndexSearcherRef searcher = indexSearchers.get( identifier );
            if ( searcher == null )
            {
                Directory dir = getDirectory( baseStorePath, identifier );
                try
                {
                    String[] files = dir.listAll();
                    if ( files == null || files.length == 0 )
                    {
                        return null;
                    }
                }
                catch ( IOException e )
                {
                    return null;
                }
                IndexReader indexReader = IndexReader.open( dir, false );
                IndexSearcher indexSearcher = new IndexSearcher( indexReader );
                searcher = new IndexSearcherRef( identifier, indexSearcher );
                indexSearchers.put( identifier, searcher );
            }
            return searcher;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    XaTransaction createTransaction( int identifier,
        XaLogicalLog logicalLog )
    {
        return new LuceneTransaction( identifier, logicalLog, this );
    }

    void invalidateIndexSearcher( IndexIdentifier identifier )
    {
        IndexSearcherRef searcher = indexSearchers.get( identifier );
        if ( searcher != null )
        {
            IndexSearcherRef refreshedSearcher = refreshSearcher( searcher );
            if ( refreshedSearcher != null )
            {
                indexSearchers.put( identifier, refreshedSearcher );
            }
        }
    }

    synchronized IndexWriter getIndexWriter( IndexIdentifier identifier )
    {
        try
        {
            Directory dir = getDirectory( baseStorePath, identifier );
            directoryExists( dir );
            IndexWriter writer = new IndexWriter( dir, identifier.getType(
                    store.indexConfig ).analyzer, MaxFieldLength.UNLIMITED );
            
            // TODO We should tamper with this value and see how it affects the
            // general performance. Lucene docs says rather <10 for mixed
            // reads/writes 
//            writer.setMergeFactor( 8 );
            
            return writer;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private boolean directoryExists( Directory dir )
    {
        try
        {
            String[] files = dir.listAll();
            return files != null && files.length > 0;
        }
        catch ( IOException e )
        {
            return false;
        }
    }
    
    protected void addDocument( IndexWriter writer, IndexIdentifier identifier,
            long entityId, String key, Object value )
    {
        Document document = new Document();
        identifier.getType( store.indexConfig ).fillDocument(
                document, entityId, key, value );
        try
        {
            writer.addDocument( document );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    protected void deleteDocuments( IndexWriter writer, IndexIdentifier identifier,
            String queryAsString )
    {
        deleteDocuments( writer, identifier.getType( store.indexConfig ).query(
                null, queryAsString ) );
    }

    protected void deleteDocuments( IndexWriter writer, IndexIdentifier identifier,
            long entityId, String key, Object value )
    {
        deleteDocuments( writer, identifier.getType( store.indexConfig ).deletionQuery(
                entityId, key, value ) );
    }
    
    protected void deleteDocuments( IndexWriter writer, Query query )
    {
        try
        {
            // TODO
            writer.deleteDocuments( query );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to delete for " + query + " using" + writer, e );
        }
    }
    
    void removeWriter( IndexWriter writer )
    {
        try
        {
            writer.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to close lucene writer "
                + writer, e );
        }
    }

    LruCache<String,Collection<Long>> getFromCache( IndexIdentifier identifier,
            String key )
    {
        return caching.get( identifier, key );
    }

    void setCacheCapacity( IndexIdentifier identifier, String key, int maxNumberOfCachedEntries )
    {
        this.caching.setCapacity( identifier, key, maxNumberOfCachedEntries );
    }
    
    Integer getCacheCapacity( IndexIdentifier identifier, String key )
    {
        LruCache<String, Collection<Long>> cache = this.caching.get( identifier, key );
        return cache != null ? cache.maxSize() : null;
    }
    
    void invalidateCache( IndexIdentifier identifier, String key, Object value )
    {
        LruCache<String,Collection<Long>> cache = caching.get( identifier, key );
        if ( cache != null )
        {
            cache.remove( value.toString() );
        }
    }
    
    void invalidateCache( IndexIdentifier identifier )
    {
        this.caching.disable( identifier );
    }
    
    @Override
    public long getCreationTime()
    {
        return store.getCreationTime();
    }
    
    @Override
    public long getRandomIdentifier()
    {
        return store.getRandomNumber();
    }
    
    @Override
    public long getCurrentLogVersion()
    {
        return store.getVersion();
    }
}
