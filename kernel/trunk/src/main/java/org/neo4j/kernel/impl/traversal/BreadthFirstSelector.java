package org.neo4j.kernel.impl.traversal;

import java.util.LinkedList;
import java.util.Queue;

import org.neo4j.graphdb.traversal.ExpansionSource;
import org.neo4j.graphdb.traversal.SourceSelector;
import org.neo4j.graphdb.traversal.TraversalRules;

class BreadthFirstSelector implements SourceSelector
{
    private final Queue<ExpansionSource> queue = new LinkedList<ExpansionSource>();
    private ExpansionSource current;
    
    BreadthFirstSelector( ExpansionSource startSource )
    {
        this.current = startSource;
    }

    public ExpansionSource nextPosition( TraversalRules rules )
    {
        ExpansionSource result = null;
        while ( result == null )
        {
            ExpansionSource next = current.next( rules );
            if ( next != null )
            {
                queue.add( next );
                if ( rules.okToReturn( next ) )
                {
                    result = next;
                }
            }
            else
            {
                current = queue.poll();
                if ( current == null )
                {
                    return null;
                }
            }
        }
        return result;
    }
}