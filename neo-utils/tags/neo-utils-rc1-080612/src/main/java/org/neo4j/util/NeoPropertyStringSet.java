package org.neo4j.util;

import org.neo4j.api.core.Node;

/**
 * A {@link NeoPropertySet} where the objects are strings.
 */
public class NeoPropertyStringSet extends NeoPropertySet<String>
{
	/**
	 * @param node the collection node.
	 * @param propertyKey the property key for storing values.
	 * @param delimiter custom delimiter between values.
	 */
	public NeoPropertyStringSet( Node node, String propertyKey,
		String delimiter )
	{
		super( node, propertyKey, delimiter );
	}

	/**
	 * @param node the collection node.
	 * @param propertyKey the property key for storing values.
	 */
	public NeoPropertyStringSet( Node node, String propertyKey )
	{
		super( node, propertyKey );
	}

	@Override
	protected String itemToString( Object item )
	{
		return item.toString();
	}

	@Override
	protected String stringToItem( String string )
	{
		return string;
	}
}