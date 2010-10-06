/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

package common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * Base class for test cases working on a NeoService. It sets up a NeoService
 * and a transaction.
 * @author Patrik Larsson
 */
public abstract class Neo4jAlgoTestCase
{
    protected static GraphDatabaseService graphDb;
    protected static SimpleGraphBuilder graph = null;
    protected Transaction tx;

    protected static enum MyRelTypes implements RelationshipType
    {
        R1, R2, R3
    }

    @BeforeClass
    public static void setUpGraphDb() throws Exception
    {
        graphDb = new EmbeddedGraphDatabase( "target/var/algotest" );
        graph = new SimpleGraphBuilder( graphDb, MyRelTypes.R1 );
    }
    
    @Before
    public void setUpTransaction()
    {
        tx = graphDb.beginTx();
    }

    @AfterClass
    public static void tearDownGraphDb() throws Exception
    {
        graphDb.shutdown();
    }
    
    @After
    public void tearDownTransactionAndGraph()
    {
        graph.clear();
        tx.success();
        tx.finish();
    }
    
    protected void restartTx()
    {
        tx.success();
        tx.finish();
        tx = graphDb.beginTx();
    }

    protected void assertPathDef( Path path, String... names )
    {
        int i = 0;
        for ( Node node : path.nodes() )
        {
            assertEquals( "Wrong node " + i + " in " + getPathDef( path ),
                    names[i++], node.getProperty( SimpleGraphBuilder.KEY_ID ) );
        }
        assertEquals( names.length, i );
    }
    
    protected void assertPath( Path path, Node... nodes )
    {
        int i = 0;
        for ( Node node : path.nodes() )
        {
            assertEquals( "Wrong node " + i + " in " + getPathDef( path ),
                    nodes[i++], node );
        }
        assertEquals( nodes.length, i );
    }

    protected <E> void assertContains( Iterable<E> actual, E... expected )
    {
        Set<E> expectation = new HashSet<E>( Arrays.asList( expected ) );
        for ( E element : actual )
        {
            if ( !expectation.remove( element ) )
            {
                fail( "unexpected element <" + element + ">" );
            }
        }
        if ( !expectation.isEmpty() )
        {
            fail( "the expected elements <" + expectation
                  + "> were not contained" );
        }
    }

    public String getPathDef( Path path )
    {
        StringBuilder builder = new StringBuilder();
        for ( Node node : path.nodes() )
        {
            if ( builder.length() > 0 )
            {
                builder.append( "," );
            }
            builder.append( node.getProperty( SimpleGraphBuilder.KEY_ID ) );
        }
        return builder.toString();
    }
    
    public void assertPaths( Iterable<? extends Path> paths, String... pathDefinitions )
    {
        List<String> pathDefs = new ArrayList<String>( Arrays.asList( pathDefinitions ) );
        for ( Path path : paths )
        {
            String pathDef = getPathDef( path );
            int index = pathDefs.indexOf( pathDef );
            if ( index != -1 )
            {
                pathDefs.remove( index );
            }
            else
            {
                fail( "Unexpected path " + pathDef );
            }
        }
        assertTrue( "Should be empty: " + pathDefs.toString(), pathDefs.isEmpty() );
    }
}
