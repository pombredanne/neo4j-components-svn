package org.neo4j.graphalgo.path;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.Traversal;

import common.Neo4jAlgoTestCase;

public class TestDijkstra extends Neo4jAlgoTestCase
{
    @Test
    public void testSmallGraph()
    {
        /* Layout:
         *                       (y)    
         *                        ^     
         *                        [2]  _____[1]___
         *                          \ v           |
         * (start)--[1]->(a)--[9]-->(x)<-        (e)--[2]->(f)
         *                |         ^ ^^  \       ^
         *               [1]  ---[7][5][3] -[3]  [1]
         *                v  /       | /      \  /
         *               (b)--[1]-->(c)--[1]->(d)
         */
        graph.makeEdge( "start", "a", "cost", (double) 1 );
        graph.makeEdge( "a", "x", "cost", (double) 9 );
        graph.makeEdge( "a", "b", "cost", (double) 1 );
        graph.makeEdge( "b", "x", "cost", (double) 7 );
        graph.makeEdge( "b", "c", "cost", (double) 1 );
        graph.makeEdge( "c", "x", "cost", (double) 5 );
        Relationship shortCTOXRelationship = graph.makeEdge( "c", "x", "cost", (double) 3 );
        graph.makeEdge( "c", "d", "cost", (double) 1 );
        graph.makeEdge( "d", "x", "cost", (double) 3 );
        graph.makeEdge( "d", "e", "cost", (double) 1 );
        graph.makeEdge( "e", "x", "cost", (double) 1 );
        graph.makeEdge( "e", "f", "cost", (double) 2 );
        graph.makeEdge( "x", "y", "cost", (double) 2 );
        
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(
                Traversal.expanderForTypes( MyRelTypes.R1, Direction.OUTGOING ),
                CommonEvaluators.doubleCostEvaluator( "cost" ) );
        
        // Assert that there are two matching paths
        assertPaths( finder.findAllPaths( graph.getNode( "start" ), graph.getNode( "x" ) ),
                "start,a,b,c,x", "start,a,b,c,d,e,x" );
        
        // Assert that for the shorter one it picked the correct relationship
        // of the two from (c) --> (x)
        for ( WeightedPath path :
                finder.findAllPaths( graph.getNode( "start" ), graph.getNode( "x" ) ) )
        {
            if ( getPathDef( path ).equals( "start,a,b,c,x" ) )
            {
                assertContainsRelationship( path, shortCTOXRelationship );
            }
        }
    }

    private void assertContainsRelationship( WeightedPath path,
            Relationship relationship )
    {
        for ( Relationship rel : path.relationships() )
        {
            if ( rel.equals( relationship ) )
            {
                return;
            }
        }
        fail( path + " should've contained " + relationship );
    }
}
