package org.neo4j.meta.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.neo4j.util.OneOfRelTypesReturnableEvaluator;

/**
 * Represents a class in the meta model.
 */
public class MetaModelClass extends MetaModelThing
{
	/**
	 * @param model the {@link MetaModel} instance.
	 * @param node the {@link Node} to wrap.
	 */
	public MetaModelClass( MetaModel model, Node node )
	{
		super( model, node );
	}
	
	private Collection<MetaModelClass> hierarchyCollection(
		Direction direction )
	{
		return new ObjectCollection<MetaModelClass>( graphDb(),
			node(), MetaModelRelTypes.META_IS_SUBCLASS_OF, direction,
			model(), MetaModelClass.class );
	}
	
	@Override
	public Collection<MetaModelClass> getDirectSubs()
	{
		return hierarchyCollection( Direction.INCOMING );
	}
	
	@Override
	public Collection<MetaModelClass> getDirectSupers()
	{
		return hierarchyCollection( Direction.OUTGOING );
	}
	
	@Override
	protected RelationshipType subRelationshipType()
	{
		return MetaModelRelTypes.META_IS_SUBCLASS_OF;
	}
	
	/**
	 * @return a modifiable collection of properties directly related to
	 * this class.
	 */
	public Collection<MetaModelProperty> getDirectProperties()
	{
		return new ObjectCollection<MetaModelProperty>( graphDb(),
			node(), MetaModelRelTypes.META_CLASS_HAS_PROPERTY,
			Direction.OUTGOING, model(), MetaModelProperty.class );
	}
	
	/**
	 * @return an unmodifiable collection of all properties related to this
	 * class.
	 */
	public Collection<MetaModelProperty> getAllProperties()
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			HashSet<MetaModelProperty> properties =
				new HashSet<MetaModelProperty>();
			for ( Node node : node().traverse( Traverser.Order.BREADTH_FIRST,
				StopEvaluator.END_OF_GRAPH,
				
				// Maybe remove these three lines? They go for subproperties too
				new AllPropertiesRE(),
				MetaModelRelTypes.META_IS_SUBPROPERTY_OF,
					Direction.INCOMING,
					
				MetaModelRelTypes.META_CLASS_HAS_PROPERTY,
					Direction.OUTGOING,
				MetaModelRelTypes.META_IS_SUBCLASS_OF,
					Direction.OUTGOING ) )
			{
				properties.add( new MetaModelProperty( model(), node ) );
			}
			return Collections.unmodifiableSet( properties );
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * @param property the {@link MetaModelProperty} to associate with.
	 * @param allowCreate whether to allow creation of the restriction if
	 * it doesn't exist.
	 * @return the restriction for {@code property} or creates a new if
	 * {@code allowCreate} is {@code true}.
	 */
	public MetaModelRestriction getRestriction(
		MetaModelProperty property, boolean allowCreate )
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			Collection<MetaModelRestriction> restrictions =
				getDirectRestrictions();
			for ( MetaModelRestriction restriction : restrictions )
			{
				if ( restriction.getMetaProperty().equals( property ) )
				{
					return restriction;
				}
			}
			if ( !allowCreate )
			{
				return null;
			}
			
//			if ( !getAllProperties().contains( property ) )
//			{
//				throw new RuntimeException( this + " isn't in the domain of " +
//					property + " add it first" );
//			}
			Node node = graphDb().createNode();
			MetaModelRestriction result = new MetaModelRestriction(
				model(), node );
			restrictions.add( result );
			node.createRelationshipTo( property.node(),
				MetaModelRelTypes.META_RESTRICTION_TO_PROPERTY );
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * @return the restrictions for this class.
	 */
	public Collection<MetaModelRestriction> getDirectRestrictions()
	{
		return new ObjectCollection<MetaModelRestriction>(
			graphDb(), node(), MetaModelRelTypes.META_RESTRICTION_TO_CLASS,
			Direction.INCOMING, model(), MetaModelRestriction.class );
	}
	
	/**
	 * @return an unmodifiable collection of all direct restrictions as well
	 * as restrictions for super classes.
	 */
	public Collection<MetaModelRestriction> getAllRestrictions()
	{
		Transaction tx = graphDb().beginTx();
		try
		{
			HashSet<MetaModelRestriction> restrictions =
				new HashSet<MetaModelRestriction>();
			for ( Node node : node().traverse( Traverser.Order.BREADTH_FIRST,
				StopEvaluator.END_OF_GRAPH,
				new OneOfRelTypesReturnableEvaluator(
					MetaModelRelTypes.META_RESTRICTION_TO_CLASS ),
				MetaModelRelTypes.META_RESTRICTION_TO_CLASS,
					Direction.INCOMING,
				MetaModelRelTypes.META_IS_SUBCLASS_OF,
					Direction.OUTGOING ) )
			{
				restrictions.add(
					new MetaModelRestriction( model(), node ) );
			}
			return Collections.unmodifiableSet( restrictions );
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * @return a modifiable collection of instances of this class.
	 */
	public Collection<Node> getInstances()
	{
		return new InstanceCollection( graphDb(), node(), model() );
	}
	
	private class AllPropertiesRE implements ReturnableEvaluator
	{
		private boolean same( RelationshipType r1,
			RelationshipType r2 )
		{
			return r1.name().equals( r2.name() );
		}
		
		public boolean isReturnableNode( TraversalPosition currentPos )
		{
			Relationship lastRel =
				currentPos.lastRelationshipTraversed();
			if ( lastRel == null || same( lastRel.getType(),
				MetaModelRelTypes.META_IS_SUBCLASS_OF ) )
			{
				return false;
			}
			if ( same( lastRel.getType(),
				MetaModelRelTypes.META_IS_SUBPROPERTY_OF ) )
			{
				if ( currentPos.currentNode().hasRelationship(
					MetaModelRelTypes.META_CLASS_HAS_PROPERTY ) )
				{
					return false;
				}
			}
			return true;
		}
	}
}
