package org.neo4j.onlinebackup;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
//import org.neo4j.index.IndexService;
//import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.XaDataSource;

/**
 * Try to backup Neo4j and a Lucene data source to another
 * running Neo4j+Lucene.
 */
public class MultiRunningTest
{
    private static enum MyRels implements RelationshipType
    {
        TEST
    }

    private static final String FILE_SEP = System
        .getProperty( "file.separator" );
    private static final String TARGET_DIR = "target";
    private static final String VAR = TARGET_DIR + FILE_SEP + "var";
    private static final String STORE_LOCATION_DIR = VAR + FILE_SEP + "neo-db";
    private static final String BACKUP_LOCATION_DIR = VAR + FILE_SEP
        + "neo-backup";

    @Before
    public void setup()
    {
        Util.deleteDir( new File( VAR ) );

        System.out
            .println( "setting up database and backup-copy including Lucene" );

        EmbeddedGraphDatabase graphDb = Util.startGraphDbInstance( STORE_LOCATION_DIR );
        XaDataSource neoStoreXaDataSource = graphDb.getConfig()
            .getPersistenceModule().getPersistenceManager()
            .getPersistenceSource().getXaDataSource();
        neoStoreXaDataSource.keepLogicalLogs( true );

//        IndexService indexService = new LuceneIndexService( graphDb );
        XaDataSourceManager xaDsm = graphDb.getConfig().getTxModule()
            .getXaDataSourceManager();
        XaDataSource ds = xaDsm.getXaDataSource( "lucene" );
        // ((LuceneDataSource) ds).keepLogicalLogs( true );

        Transaction tx = graphDb.beginTx();
        try
        {
//            indexService.index( addNode( graphDb ), "number", 1 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        Util.stopGraphDb( graphDb ); // , indexService );

        Util.copyDir( STORE_LOCATION_DIR, BACKUP_LOCATION_DIR );
    }

    @Test
    public void backup() throws IOException
    {
        System.out.println( "starting tests" );
        EmbeddedGraphDatabase graphDb = Util.startGraphDbInstance( STORE_LOCATION_DIR );
        ((NeoStoreXaDataSource) graphDb.getConfig().getPersistenceModule()
            .getPersistenceManager().getPersistenceSource()
            .getXaDataSource()).keepLogicalLogs( true );
//        IndexService indexService = new LuceneIndexService( graphDb );
        XaDataSourceManager xaDsm = graphDb.getConfig().getTxModule()
            .getXaDataSourceManager();
        // XaDataSource ds = xaDsm.getXaDataSource( "lucene" );
        // ((LuceneDataSource) ds).keepLogicalLogs( true );

        System.out.println( "backing up original db without any changes" );
        tryBackup( graphDb, BACKUP_LOCATION_DIR, 0 );

        Transaction tx = graphDb.beginTx();
        try
        {
            addNode( graphDb );
//            indexService.index( addNode( graphDb ), "number", 2 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        System.out.println( "one node added" );
        tryBackup( graphDb, BACKUP_LOCATION_DIR, 1 );

        tx = graphDb.beginTx();
        try
        {
            addNode( graphDb );
//            indexService.index( addNode( graphDb ), "number", 3 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        System.out.println( "one node added" );

        tx = graphDb.beginTx();
        try
        {
//            indexService.index( addNode( graphDb ), "number", 4 );
            addNode( graphDb );
            System.out.println( "one node added, not commited" );
            tryBackup( graphDb, BACKUP_LOCATION_DIR, 2 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        System.out.println( "previous add commited" );
        tryBackup( graphDb, BACKUP_LOCATION_DIR, 3 );

        Util.stopGraphDb( graphDb ); // , indexService );
    }

    protected void tryBackup( EmbeddedGraphDatabase graphDb, String location, int relCount )
        throws IOException
    {
        setupBackup( graphDb, location );

        EmbeddedGraphDatabase bDb = Util.startGraphDbInstance( location );
//        IndexService bIndexService = new LuceneIndexService( bDb );
        Transaction bTx = bDb.beginTx();
        try
        {
            List<Relationship> rels = new ArrayList<Relationship>();
            for ( Relationship rel : bDb.getReferenceNode().getRelationships() )
            {
                rels.add( rel );
            }
            assertEquals( relCount, rels.size() );
            // Node node = bIndexService.getSingleNode( "number", relCount );
            // assertEquals( true, node != null );
            // assertEquals( node.getId(), (long) (Long) node.getProperty(
            //     "theId", -1L ) );
            bTx.success();
        }
        finally
        {
            bTx.finish();
        }
        Util.stopGraphDb( bDb ); // , bIndexService );
    }

    @SuppressWarnings( "serial" )
    protected void setupBackup( EmbeddedGraphDatabase graphDb, String location )
        throws IOException
    {
        EmbeddedGraphDatabase bDb = Util.startGraphDbInstance( location );
//        IndexService bIndexService = new LuceneIndexService( bDb );
        Backup backupComp = new Neo4jBackup( graphDb, bDb, new ArrayList<String>()
        {
            {
                add( "nioneodb" );
//                add( "lucene" );
            }
        } );
        backupComp.enableFileLogger();
        backupComp.doBackup();
        Util.stopGraphDb( bDb ); //, bIndexService );
    }

    private Node addNode( EmbeddedGraphDatabase graphDb )
    {
        Node referenceNode = graphDb.getReferenceNode();
        Node node = graphDb.createNode();
        node.setProperty( "theId", node.getId() );
        referenceNode.createRelationshipTo( node, MyRels.TEST );
        return node;
    }
}
