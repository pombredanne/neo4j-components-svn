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

import java.util.concurrent.CountDownLatch;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.AbstractNeo4jTestCase;

public class TestIsolation extends AbstractNeo4jTestCase
{
    public TestIsolation( String testName )
    {
        super( testName );
    }

    public void setUp()
    {
        super.setUp();
    }

    public void tearDown()
    {
        super.tearDown();
    }

    public void testSimpleTransactionIsolation()
    {
        commit();
        final CountDownLatch latch1 = new CountDownLatch( 1 );
        final CountDownLatch latch2 = new CountDownLatch( 1 );
        Transaction tx = getGraphDb().beginTx();
        Node n1, n2;
        Relationship r1;
        try
        {
            n1 = getGraphDb().createNode();
            n2 = getGraphDb().createNode();
            r1 = n1.createRelationshipTo( n2, 
                DynamicRelationshipType.withName( "TEST" ) );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        final Node node1 = n1;
        final Node node2 = n2;
        final Relationship rel1 = r1;
        tx = getGraphDb().beginTx();
        try
        {
            node1.setProperty( "key", "old" );
            rel1.setProperty( "key", "old" );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        assertPropertyEqual( node1, "key", "old" );
        assertPropertyEqual( rel1, "key", "old" );
        assertRelationshipCount( node1, 1 );
        assertRelationshipCount( node2, 1 );
        Thread t1 = new Thread( new Runnable()
        {
            public void run()
            {
                Transaction tx = getGraphDb().beginTx();
                try
                {
                    node1.setProperty( "key", "new" );
                    rel1.setProperty( "key", "new" );
                    node1.createRelationshipTo( node2, 
                        DynamicRelationshipType.withName( "TEST" ) );
                    assertPropertyEqual( node1, "key", "new" );
                    assertPropertyEqual( rel1, "key", "new" );
                    assertRelationshipCount( node1, 2 );
                    assertRelationshipCount( node2, 2 );
                    latch1.countDown();
                    latch2.await();
                    assertPropertyEqual( node1, "key", "new" );
                    assertPropertyEqual( rel1, "key", "new" );
                    assertRelationshipCount( node1, 2 );
                    assertRelationshipCount( node2, 2 );
                    // no tx.success();
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                    Thread.interrupted();
                }
                finally
                {
                    tx.finish();
                    assertPropertyEqual( node1, "key", "old" );
                    assertPropertyEqual( rel1, "key", "old" );
                    assertRelationshipCount( node1, 1 );
                    assertRelationshipCount( node2, 1 );
                }
            }
        } );
        t1.start();
        try
        {
            latch1.await();
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted();
        }

        assertPropertyEqual( node1, "key", "old" );
        assertPropertyEqual( rel1, "key", "old" );
        assertRelationshipCount( node1, 1 );
        assertRelationshipCount( node2, 1 );

        latch2.countDown();
        try
        {
            t1.join();
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted();
        }
        assertPropertyEqual( node1, "key", "old" );
        assertPropertyEqual( rel1, "key", "old" );
        assertRelationshipCount( node1, 1 );
        assertRelationshipCount( node2, 1 );

        tx = getGraphDb().beginTx();
        try
        {
            for ( Relationship rel : node1.getRelationships() )
            {
                rel.delete();
            }
            node1.delete();
            node2.delete();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
    
    private void assertPropertyEqual( PropertyContainer primitive, String key, 
        String value )
    {
        Transaction tx = getGraphDb().beginTx();
        try
        {
            assertEquals( value, primitive.getProperty( key ) );
        }
        finally
        {
            tx.finish();
        }
    }
    
    private void assertRelationshipCount( Node node, int count )
    {
        Transaction tx = getGraphDb().beginTx();
        try
        {
            int actualCount = 0;
            for ( Relationship rel : node.getRelationships() )
            {
                actualCount++;
            }
            assertEquals( count, actualCount );
        }
        finally
        {
            tx.finish();
        }
    }
}
