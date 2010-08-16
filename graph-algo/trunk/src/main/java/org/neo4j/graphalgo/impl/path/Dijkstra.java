package org.neo4j.graphalgo.impl.path;

import java.util.Iterator;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.BestFirstSelectorFactory;
import org.neo4j.graphalgo.impl.util.StopAfterWeightIterator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

/**
 * @author Tobias Ivarsson
 * @author Martin Neumann
 * @author Mattias Persson
 */
public class Dijkstra implements PathFinder<WeightedPath>
{
    private static final TraversalDescription TRAVERSAL =
            Traversal.description().uniqueness(
                    Uniqueness.NONE );

    private final RelationshipExpander expander;
    private final CostEvaluator<Double> costEvaluator;

    public Dijkstra( RelationshipExpander expander, CostEvaluator<Double> costEvaluator )
    {
        this.expander = expander;
        this.costEvaluator = costEvaluator;
    }

    public Iterable<WeightedPath> findAllPaths( Node start, final Node end )
    {
        Predicate<Path> filter = new Predicate<Path>()
        {
            public boolean accept( Path position )
            {
                return position.endNode().equals( end );
            }
        };

        final Traverser traverser = TRAVERSAL.expand( expander ).order(
                new SelectorFactory( costEvaluator ) ).filter( filter ).traverse( start );
        return new Iterable<WeightedPath>()
        {
            public Iterator<WeightedPath> iterator()
            {
                return new StopAfterWeightIterator( traverser.iterator(),
                        costEvaluator );
            }
        };
    }

    public WeightedPath findSinglePath( Node start, Node end )
    {
        Iterator<WeightedPath> result = findAllPaths( start, end ).iterator();
        return result.hasNext() ? result.next() : null;
    }

    private static class SelectorFactory extends BestFirstSelectorFactory<Double, Double>
    {
        private final CostEvaluator<Double> evaluator;

        SelectorFactory( CostEvaluator<Double> evaluator )
        {
            this.evaluator = evaluator;
        }

        @Override
        protected Double calculateValue( TraversalBranch next )
        {
            return next.depth() == 0 ? 0d : evaluator.getCost(
                    next.relationship(), Direction.OUTGOING );
        }

        @Override
        protected Double addPriority( TraversalBranch source,
                Double currentAggregatedValue, Double value )
        {
            return withDefault( currentAggregatedValue, 0d ) + withDefault( value, 0d );
        }

        private <T> T withDefault( T valueOrNull, T valueIfNull )
        {
            return valueOrNull != null ? valueOrNull : valueIfNull;
        }

        @Override
        protected Double getStartData()
        {
            return 0d;
        }
    }
}
