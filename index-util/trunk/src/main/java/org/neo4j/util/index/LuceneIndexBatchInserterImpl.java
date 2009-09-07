package org.neo4j.util.index;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.neo4j.impl.batchinsert.BatchInserter;
import org.neo4j.impl.util.ArrayMap;
import org.neo4j.impl.util.FileUtils;

public class LuceneIndexBatchInserterImpl implements LuceneIndexBatchInserter
{
    private final String storeDir;

    private final ArrayMap<String,IndexWriter> indexWriters = 
        new ArrayMap<String,IndexWriter>( 6, false, false );

    private final Analyzer fieldAnalyzer = new Analyzer()
    {
        @Override
        public TokenStream tokenStream( String fieldName, Reader reader )
        {
            return new LowerCaseFilter( new WhitespaceTokenizer( reader ) );
        }
    };
    
    public LuceneIndexBatchInserterImpl( BatchInserter neo )
    {
        this.storeDir = fixPath( neo.getStore() + "/" + getDirName() );
    }
    
    protected String getDirName()
    {
        return LuceneIndexService.DIR_NAME;
    }
    
    private String fixPath( String dir )
    {
        String store = FileUtils.fixSeparatorsInPath( dir );
        File directories = new File( dir );
        if ( !directories.exists() )
        {
            if ( !directories.mkdirs() )
            {
                throw new RuntimeException( "Unable to create directory path["
                    + storeDir + "] for Lucene index store." );
            }
        }
        return store;
    }
    
    private Directory instantiateDirectory( String key ) throws IOException
    {
        return FSDirectory.getDirectory( new File( storeDir + "/" + key ) );
    }
    
    public void index( long node, String key, Object value )
    {
        IndexWriter writer = indexWriters.get( key );
        if ( writer == null )
        {
            try
            {
                Directory dir = instantiateDirectory( key );
                writer = new IndexWriter( dir, fieldAnalyzer,
                    MaxFieldLength.UNLIMITED );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
            indexWriters.put( key, writer );
        }
        Document document = new Document();
        fillDocument( document, node, key, value );
        try
        {
            writer.addDocument( document );
            if ( key.equals( cachedForKey ) )
            {
                if ( cachedIndexSearcher != null )
                {
                    cachedIndexSearcher.close();
                    cachedIndexSearcher = null;
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void fillDocument( Document document, long nodeId, String key,
        Object value )
    {
        document.add( new Field( LuceneIndexService.DOC_ID_KEY,
            String.valueOf( nodeId ), Field.Store.YES,
            Field.Index.NOT_ANALYZED ) );
        document.add( new Field( LuceneIndexService.DOC_INDEX_KEY,
            value.toString(), Field.Store.NO, getIndexStrategy() ) );
    }
    
    protected Field.Index getIndexStrategy()
    {
        return Field.Index.NOT_ANALYZED;
    }
    
    public void shutdown()
    {
        for ( IndexWriter writer : indexWriters.values() )
        {
            try
            {
                writer.close();
            }
            catch ( CorruptIndexException e )
            {
                throw new RuntimeException( e );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    private IndexSearcher cachedIndexSearcher = null;
    private String cachedForKey = null;
    
    public Iterable<Long> getNodes( String key, Object value )
    {
        IndexWriter writer = indexWriters.remove( key );
        if ( writer != null )
        {
            try
            {
                writer.close();
            }
            catch ( CorruptIndexException e )
            {
                throw new RuntimeException( e );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        Set<Long> nodeSet = new HashSet<Long>();
        if ( !key.equals( cachedForKey ) || cachedIndexSearcher == null )
        {
            try
            {
                Directory dir = instantiateDirectory( key );
                cachedForKey = key;
                if ( dir.list().length == 0 )
                {
                    return Collections.emptySet();
                }
                cachedIndexSearcher = new IndexSearcher( dir );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        Query query = formQuery( key, value );
        try
        {
            Hits hits = cachedIndexSearcher.search( query );
            for ( int i = 0; i < hits.length(); i++ )
            {
                Document document = hits.doc( i );
                long id = Long.parseLong( document.getField(
                    LuceneIndexService.DOC_ID_KEY ).stringValue() );
                nodeSet.add( id );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        return nodeSet;
    }
    
    protected Query formQuery( String key, Object value )
    {
        return new TermQuery( new Term( LuceneIndexService.DOC_INDEX_KEY, 
            value.toString() ) );
    }
    
    public void optimize()
    {
        try
        {
            List<IndexWriter> writers = new ArrayList<IndexWriter>();
            for ( IndexWriter writer : indexWriters.values() )
            {
                writer.optimize( true );
                writers.add( writer );
            }
            indexWriters.clear();
            for ( IndexWriter writer : writers )
            {
                writer.close();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    public long getSingleNode( String key, Object value )
    {
        Iterator<Long> nodes = getNodes( key, value ).iterator();
        long node = nodes.hasNext() ? nodes.next() : -1;
        while ( nodes.hasNext() )
        {
            if ( !nodes.next().equals( node ) )
            {
                throw new RuntimeException( "More than one node for " + key + "="
                    + value );
            }
        }
        return node;
    }
}
