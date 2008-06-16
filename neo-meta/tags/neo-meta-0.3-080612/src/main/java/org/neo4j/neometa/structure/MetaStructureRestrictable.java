package org.neo4j.neometa.structure;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.neo4j.api.core.Node;

/**
 * Common functionality for restrictions (f.ex. cardinality and values).
 */
public interface MetaStructureRestrictable
{
	/**
	 * @return the underlying neo {@link Node}.
	 */
	Node node();
	
	/**
	 * @return the underlying {@link MetaStructure}.
	 */
	MetaStructure meta();
	
	/**
	 * @return the name of the object.
	 */
	String getName();
	
	/**
	 * @return the mimimum cardinality set for this property. Can return
	 * {@code null} which means no restriction.
	 */
	Integer getMinCardinality();
	
	/**
	 * Sets the minimum cardinality of this property. {@code null} means
	 * no restriction.
	 * @param minCardinality the minimum cardinality to set.
	 */
	void setMinCardinality( Integer minCardinality );

	/**
	 * @return the maximum cardinality set for this property. Can return
	 * {@code null} which means no restriction.
	 */
	Integer getMaxCardinality();
	
	/**
	 * Sets the maximum cardinality of this property. {@code null} means
	 * no restriction.
	 * @param maxCardinality the maximum cardinality to set.
	 */
	void setMaxCardinality( Integer maxCardinality );
	
	/**
	 * Convenience method for setting both min and max cardinality.
	 * @param cardinality the min and max cardinality to set.
	 */
	void setCardinality( Integer cardinality );
	
	/**
	 * Sets the range of the expected value(s) for this restriction. F.ex.
	 * a string, a number or a an instance of a {@link MetaStructureClass}.
	 * @param range the property range.
	 */
	void setRange( PropertyRange range );
	
	/**
	 * @return the {@link PropertyRange} set with
	 * {@link #setRange(PropertyRange)} or {@code null} if no range is
	 * specifically set for this object.
	 */
	PropertyRange getRange();
	
	/**
	 * If cardinality is >1 then this will decide the rules of the collection.
	 * F.ex {@link Set} doesn't allow duplicates whereas {@link List} will.
	 * @param collectionClass the collection class type.
	 */
	void setCollectionBehaviourClass(
		Class<? extends Collection> collectionClass );

	/**
	 * @return the collection behaviour set with
	 * {@link #setCollectionBehaviourClass(Class)}.
	 */
	Class<? extends Collection<?>> getCollectionBehaviourClass();
}