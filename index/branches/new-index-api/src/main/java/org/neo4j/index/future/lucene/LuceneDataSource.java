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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
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
public class LuceneDataSource extends XaDataSource
{
    public static final byte[] DEFAULT_BRANCH_ID = "lucene".getBytes();
    
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
    private final String storeDir;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); 
    private final Analyzer fieldAnalyzer = LOWER_CASE_WHITESPACE_ANALYZER;
    private final LuceneIndexStore store;
    final Map<Object, Object> config;

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
        this.storeDir = getStoreDir( params );
        this.store = new LuceneIndexStore( storeDir + "/lucene-store.db" );
        this.config = params;
        XaCommandFactory cf = new LuceneCommandFactory();
        XaTransactionFactory tf = new LuceneTransactionFactory( store );
        xaContainer = XaContainer.create( this.storeDir + "/lucene.log", cf, tf, params );
        try
        {
            xaContainer.openLogicalLog();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to open lucene log in " +
                    this.storeDir, e );
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
    }

    @Override
    public XaConnection getXaConnection()
    {
        return new LuceneXaConnection( storeDir, xaContainer
            .getResourceManager(), getBranchId() );
    }
    
    protected Analyzer getAnalyzer()
    {
        return this.fieldAnalyzer;
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
    
    private Directory getDirectory( IndexIdentifier identifier ) throws IOException
    {
        String path = "lcn";
        if ( identifier.itemsClass.equals( Node.class ) )
        {
            path += "n";
        }
        else if ( identifier.itemsClass.equals( Relationship.class ) )
        {
            path += "r";
        }
        else
        {
            throw new RuntimeException( identifier.itemsClass.getName() );
        }
        
        File parent = new File( storeDir, path );
        return FSDirectory.open( new File( parent, identifier.indexName ) );
    }
    
    IndexSearcherRef getIndexSearcher( IndexIdentifier identifier )
    {
        try
        {
            IndexSearcherRef searcher = indexSearchers.get( identifier );
            if ( searcher == null )
            {
                Directory dir = getDirectory( identifier );
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
            Directory dir = getDirectory( identifier );
            IndexWriter writer = new IndexWriter( dir, getAnalyzer(),
                    MaxFieldLength.UNLIMITED );
            
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
    
    protected void deleteDocuments( IndexWriter writer, IndexIdentifier identifier,
            long entityId, String key, Object value )
    {
        try
        {
            IndexType type = identifier.getType( this.config );
            writer.deleteDocuments( type.deletionQuery( entityId, key, value ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to delete for " + entityId + ","
                + "," + value + " using" + writer, e );
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

//    LruCache<String,Collection<Long>> getFromCache( String key )
//    {
//        return caching.get( key );
//    }
//
//    void enableCache( String key, int maxNumberOfCachedEntries )
//    {
//        this.caching.put( key, new LruCache<String,Collection<Long>>( key,
//            maxNumberOfCachedEntries, null ) );
//    }
//    
//    /**
//     * Returns the enabled cache size or {@code null} if not enabled
//     * for {@code key}.
//     * @param key the key to get the cache size for.
//     * @return the cache size for {@code key} or {@code null}.
//     */
//    Integer getEnabledCacheSize( String key )
//    {
//        LruCache<String, Collection<Long>> cache = this.caching.get( key );
//        return cache != null ? cache.maxSize() : null;
//    }
//
//    void invalidateCache( String key, Object value )
//    {
//        LruCache<String,Collection<Long>> cache = caching.get( key );
//        if ( cache != null )
//        {
//            cache.remove( value.toString() );
//        }
//    }
//    
//    void invalidateCache( String key )
//    {
//        caching.remove( key );
//    }
//    
//    void invalidateCache()
//    {
//        caching.clear();
//    }

//    protected void fillDocument( Document document, long nodeId, String key,
//        Object value )
//    {
//        document.add( new Field( LuceneIndex.KEY_DOC_ID,
//            String.valueOf( nodeId ), Field.Store.YES,
//            Field.Index.NOT_ANALYZED ) );
//        document.add( new Field( key, value.toString(), Field.Store.NO,
//            getIndexStrategy( key, value ) ) );
//    }
//
//    protected Index getIndexStrategy( String key, Object value )
//    {
//        return Field.Index.NOT_ANALYZED;
//    }

    @Override
    public void keepLogicalLogs( boolean keep )
    {
        xaContainer.getLogicalLog().setKeepLogs( keep );
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
    
    @Override
    public void applyLog( ReadableByteChannel byteChannel ) throws IOException
    {
        xaContainer.getLogicalLog().applyLog( byteChannel );
    }
    
    @Override
    public void rotateLogicalLog() throws IOException
    {
        // flush done inside rotate
        xaContainer.getLogicalLog().rotate();
    }
    
    @Override
    public ReadableByteChannel getLogicalLog( long version ) throws IOException
    {
        return xaContainer.getLogicalLog().getLogicalLog( version );
    }
    
    @Override
    public boolean hasLogicalLog( long version )
    {
        return xaContainer.getLogicalLog().hasLogicalLog( version );
    }
    
    @Override
    public boolean deleteLogicalLog( long version )
    {
        return xaContainer.getLogicalLog().deleteLogicalLog( version );
    }
    
    @Override
    public void setAutoRotate( boolean rotate )
    {
        xaContainer.getLogicalLog().setAutoRotateLogs( rotate );
    }
    
    @Override
    public void setLogicalLogTargetSize( long size )
    {
        xaContainer.getLogicalLog().setLogicalLogTargetSize( size );
    }
    
    @Override
    public void makeBackupSlave()
    {
        xaContainer.getLogicalLog().makeBackupSlave();
    }

    public String getFileName( long version )
    {
        return xaContainer.getLogicalLog().getFileName( version );
    }

    public long getLogicalLogLength( long version )
    {
        return xaContainer.getLogicalLog().getLogicalLogLength( version );
    }
}