/*
 * Copyright 2008 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.shortestpath;

import java.util.List;

import org.neo4j.graphalgo.shortestpath.std.IntegerAdder;
import org.neo4j.graphalgo.shortestpath.std.IntegerComparator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * FindPath class. This class can be used to perform shortest path computations
 * between two nodes on an unweighted network. Currently just wraps a
 * {@link Dijkstra}.
 * @author Patrik Larsson
 */
public class FindPath implements SingleSourceSingleSinkShortestPath<Integer>
{
    Dijkstra<Integer> dijkstra;

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#calculate()
     */
    public boolean calculate()
    {
        return dijkstra.calculate();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#calculateMultiple()
     */
    public boolean calculateMultiple()
    {
        return dijkstra.calculateMultiple();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getCost()
     */
    public Integer getCost()
    {
        return dijkstra.getCost();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getDirection()
     */
    public Direction getDirection()
    {
        return dijkstra.getDirection();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPath()
     */
    public List<PropertyContainer> getPath()
    {
        return dijkstra.getPath();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPathAsNodes()
     */
    public List<Node> getPathAsNodes()
    {
        return dijkstra.getPathAsNodes();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPathAsRelationships()
     */
    public List<Relationship> getPathAsRelationships()
    {
        return dijkstra.getPathAsRelationships();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPaths()
     */
    public List<List<PropertyContainer>> getPaths()
    {
        return dijkstra.getPaths();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPathsAsNodes()
     */
    public List<List<Node>> getPathsAsNodes()
    {
        return dijkstra.getPathsAsNodes();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getPathsAsRelationships()
     */
    public List<List<Relationship>> getPathsAsRelationships()
    {
        return dijkstra.getPathsAsRelationships();
    }

    /**
     * @return
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#getRelationshipTypes()
     */
    public RelationshipType[] getRelationshipTypes()
    {
        return dijkstra.getRelationshipTypes();
    }

    /**
     * @param maxNodesToTraverse
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#limitMaxNodesToTraverse(long)
     */
    public void limitMaxNodesToTraverse( long maxNodesToTraverse )
    {
        dijkstra.limitMaxNodesToTraverse( maxNodesToTraverse );
    }

    /**
     * @param maxRelationShipsToTraverse
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#limitMaxRelationShipsToTraverse(long)
     */
    public void limitMaxRelationShipsToTraverse( long maxRelationShipsToTraverse )
    {
        dijkstra.limitMaxRelationShipsToTraverse( maxRelationShipsToTraverse );
    }

    /**
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#reset()
     */
    public void reset()
    {
        dijkstra.reset();
    }

    /**
     * @param endNode
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#setEndNode(org.neo4j.api.core.Node)
     */
    public void setEndNode( Node endNode )
    {
        dijkstra.setEndNode( endNode );
    }

    /**
     * @param startNode
     * @see org.neo4j.graphalgo.shortestpath.Dijkstra#setStartNode(org.neo4j.api.core.Node)
     */
    public void setStartNode( Node startNode )
    {
        dijkstra.setStartNode( startNode );
    }

    public FindPath( Node startNode, Node endNode, Direction relationDirection,
        RelationshipType... costRelationTypes )
    {
        dijkstra = new Dijkstra<Integer>( 0, startNode, endNode,
            new CostEvaluator<Integer>()
            {
                public Integer getCost( Relationship relationship,
                    boolean backwards )
                {
                    return 1;
                }
            }, new IntegerAdder(), new IntegerComparator(), relationDirection,
            costRelationTypes );
    }
}
