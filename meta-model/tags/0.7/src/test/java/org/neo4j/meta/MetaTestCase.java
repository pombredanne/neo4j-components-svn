package org.neo4j.meta;

import java.util.Collection;

import junit.framework.TestCase;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.meta.model.MetaModelRelTypes;
import org.neo4j.util.EntireGraphDeletor;

/**
 * Base class for the meta model tests.
 */
public abstract class MetaTestCase extends TestCase
{
	private static GraphDatabaseService graphDb;
	
	private Transaction tx;
	
	@Override
	protected void setUp() throws Exception
	{
		if ( graphDb == null )
		{
			graphDb = new EmbeddedGraphDatabase( "target/var/neo4j" );
			Runtime.getRuntime().addShutdownHook( new Thread()
			{
				@Override
				public void run()
				{
					graphDb.shutdown();
				}
			} );
		}
		tx = graphDb().beginTx();
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		tx.success();
		tx.finish();
		super.tearDown();
	}
	
	protected GraphDatabaseService graphDb()
	{
		return graphDb;
	}

	protected <T> void assertCollection( Collection<T> collection, T... items )
	{
		String collectionString = join( ", ", collection.toArray() );
		assertEquals( collectionString, items.length, collection.size() );
		for ( T item : items )
		{
			assertTrue( collection.contains( item ) );
		}
	}
	
	protected void deleteMetaModel()
	{
		Relationship rel = graphDb().getReferenceNode().getSingleRelationship(
			MetaModelRelTypes.REF_TO_META_SUBREF, Direction.OUTGOING );
		Node node = rel.getEndNode();
		rel.delete();
		new EntireGraphDeletor().delete( node );
	}

	protected <T> String join( String delimiter, T... items )
	{
		StringBuffer buffer = new StringBuffer();
		for ( T item : items )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( delimiter );
			}
			buffer.append( item.toString() );
		}
		return buffer.toString();
	}
}
