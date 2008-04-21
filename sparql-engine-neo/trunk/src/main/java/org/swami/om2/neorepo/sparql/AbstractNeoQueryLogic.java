package org.swami.om2.neorepo.sparql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import name.levering.ryan.sparql.model.GroupConstraint;
import org.neo4j.util.matching.PatternMatch;
import org.neo4j.util.matching.PatternMatcher;
import org.neo4j.util.matching.PatternNode;

public abstract class AbstractNeoQueryLogic
{
	private List<NeoVariable> variableList = new LinkedList<NeoVariable>();
	private MetaModelProxy metaModel;

	AbstractNeoQueryLogic( MetaModelProxy metaModel )
	{
		this.metaModel = metaModel;
	}
	
	protected List<NeoVariable> getNeoVariables()
	{
		return this.variableList;
	}

	protected Iterable<PatternMatch> performMatches( QueryGraph graph )
	{
		ArrayList<Iterable<PatternMatch>> results =
			new ArrayList<Iterable<PatternMatch>>();
		PatternNode startNode = graph.getStartNode();
		String[] types =
			this.metaModel.getSubTypes( startNode.getLabel(), true );
		for ( String type : types )
		{
			results.add( PatternMatcher.getMatcher().match( startNode,
				this.metaModel.getClassNode( type ),
				graph.getOptionalGraphs() ) );
		}
		return new PatternMatchesWrapper( results );
	}
	
//	private Iterable<PatternNode> getStartNodes(
//		PatternNode startNode )
//	{
//		ArrayList<PatternNode> startNodes = new ArrayList<PatternNode>();
//		startNodes.add( startNode );
//		return startNodes;
//	}
	
	protected QueryGraph buildGraph( GroupConstraint groupConstraint )
	{
		QueryGraph graph = new QueryGraph( this.metaModel, this.variableList );
		graph.build( groupConstraint );
		graph.assertGraph();

		return graph;
	}

	private class PatternMatchesWrapper
		implements Iterable<PatternMatch>, Iterator<PatternMatch>
	{
		private Iterator<Iterable<PatternMatch>> matches;
		private Iterator<PatternMatch> current;
		
		PatternMatchesWrapper( Iterable<Iterable<PatternMatch>> matches )
		{
			this.matches = matches.iterator();
			if ( this.matches.hasNext() )
			{
				this.current = this.matches.next().iterator();
			}
		}
		
		public Iterator<PatternMatch> iterator()
		{
			return this;
		}
	
		public boolean hasNext()
		{
			if ( this.current == null )
			{
				return false;
			}
			else if ( this.current.hasNext() )
			{
				return true;
			}
			else if ( this.matches.hasNext() )
			{
				this.current = this.matches.next().iterator();
				// recursive call so we don't stop if there's an empty iterator
				// in the middle of this.matches.
				return this.hasNext();
			}
			
			return false;
		}
	
		public PatternMatch next()
		{
			if ( this.current != null && this.current.hasNext() )
			{
				return this.current.next();
			}
			else if ( this.matches.hasNext() )
			{
				this.current = this.matches.next().iterator();
				// recursive call so we don't return null if there's an empty
				// iterator in the middle of this.matches.
				return this.next();
			}
			else
			{
				return null;
			}
		}
	
		public void remove()
		{
			throw new RuntimeException( "Remove is not supported" );
		}
	}
}
