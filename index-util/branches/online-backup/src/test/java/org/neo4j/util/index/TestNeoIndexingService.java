package org.neo4j.util.index;

import java.util.Iterator;

import org.neo4j.api.core.Node;
import org.neo4j.util.NeoTestCase;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.Isolation;
import org.neo4j.util.index.NeoIndexService;

public class TestNeoIndexingService extends NeoTestCase
{
	private IndexService indexService;
	
	@Override
	public void setUp() throws Exception
	{
	    super.setUp();
        indexService = new NeoIndexService( neo() );
	}
	
	@Override
	protected void beforeNeoShutdown()
	{
        indexService.shutdown();
	}
    
    public void testSimple()
    {
        Node node1 = neo().createNode();
        
        assertTrue( !indexService.getNodes( "a_property", 
            1 ).iterator().hasNext() );

        indexService.index( node1, "a_property", 1 );
        
        Iterator<Node> itr = indexService.getNodes( "a_property", 
            1 ).iterator();
        assertEquals( node1, itr.next() );
        assertTrue( !itr.hasNext() );
        
        indexService.removeIndex( node1, "a_property", 1 );
        assertTrue( !indexService.getNodes( "a_property", 
            1 ).iterator().hasNext() );

        indexService.index( node1, "a_property", 1 );
        Node node2 = neo().createNode();
        indexService.index( node2, "a_property", 1 );
        
        itr = indexService.getNodes( "a_property", 1 ).iterator();
        assertTrue( itr.next() != null );
        assertTrue( itr.next() != null );
        assertTrue( !itr.hasNext() );
        assertTrue( !itr.hasNext() );       
        
        indexService.removeIndex( node1, "a_property", 1 );
        indexService.removeIndex( node2, "a_property", 1 );
        assertTrue( !indexService.getNodes( "a_property", 
            1 ).iterator().hasNext() );
        itr = indexService.getNodes( "a_property", 1 ).iterator();
        assertTrue( !itr.hasNext() );
        restartTx();
        
        indexService.setIsolation( Isolation.ASYNC_OTHER_TX );
        indexService.index( node1, "a_property", 1 );
        itr = indexService.getNodes( "a_property", 1 ).iterator();
        assertTrue( !itr.hasNext() );
        try
        {
            Thread.sleep( 1000 );
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted();
        }
        itr = indexService.getNodes( "a_property", 1 ).iterator();
        assertTrue( itr.hasNext() );
        indexService.setIsolation( Isolation.SYNC_OTHER_TX );
        indexService.removeIndex( node1, "a_property", 1 );
        itr = indexService.getNodes( "a_property", 1 ).iterator();
        assertTrue( !itr.hasNext() );
        node1.delete();
        node2.delete();
    }
	
/*	public void testIllegalStuff()
	{
		Node node1 = neo.createNode();
		try 
		{ 
			new MultiValueIndex( "blabla", null, neo );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		try 
		{ 
			new MultiValueIndex( "blabla", node1, null );
			fail( "Null parameter should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		Index sIndex = new SingleValueIndex( "multi", node1, neo );
		try 
		{ 
			new MultiValueIndex( "blabla", node1, neo );
			fail( "Wrong index type should throw exception" );
		} 
		catch ( IllegalArgumentException e ) { // good
		}
		sIndex.drop();
		tx.success();
	}	

    public void testValues()
    {
        Set<Node> nodes = new HashSet<Node>();
        for ( int i = 0; i < 100; i++ )
        {
            Node node1 = neo.createNode();
            Node node2 = neo.createNode();
            nodes.add( node1 );
            nodes.add( node2 );
            index.index( node1, i );
            index.index( node2, i );
        }
        for ( Node node : index.values() )
        {
            assertTrue( nodes.remove( node ) );
        }
        assertTrue( nodes.isEmpty() );
    }*/
}
