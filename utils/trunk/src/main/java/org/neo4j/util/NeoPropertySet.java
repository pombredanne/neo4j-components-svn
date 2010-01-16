package org.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

/**
 * A neo collection implemented with one property where the values are separated
 * with a delimiter.
 * @author mattias
 *
 * @param <T> the type of objects in the collection.
 */
public abstract class NeoPropertySet<T> extends AbstractNeoSet<T>
	implements Set<T>
{
	/**
	 * The delimiter used for separating values.
	 */
	public static final String DEFAULT_DELIMITER = "|";
	
	private Node node;
	private String key;
	private String delimiter;
	
	/**
	 * @param node the node to act as the collection.
	 * @param propertyKey the property key to use for the collection node to
	 * store the values.
	 */
	public NeoPropertySet( GraphDatabaseService neo, Node node, String propertyKey )
	{
		this( neo, node, propertyKey, DEFAULT_DELIMITER );
	}
	
	/**
	 * @param node the node to act as the collection.
	 * @param propertyKey the property key to use for the collection node to
	 * store the values.
	 * @param delimiter custom delimiter instead of {@link #DEFAULT_DELIMITER}.
	 */
	public NeoPropertySet( GraphDatabaseService neo, Node node, String propertyKey,
		String delimiter )
	{
		super( neo );
		this.node = node;
		this.key = propertyKey;
		this.delimiter = delimiter;
	}
	
	protected abstract String itemToString( Object item );
	
	protected abstract T stringToItem( String string );
	
	private Set<String> tokenize()
	{
		Transaction tx = neo().beginTx();
		try
		{
			Set<String> set = new HashSet<String>();
			if ( this.node.hasProperty( this.key ) )
			{
				String value = ( String ) this.node.getProperty( this.key );
				if ( value.length() > 0 )
				{
					for ( String token :
						value.split( Pattern.quote( this.delimiter ) ) )
					{
						set.add( token );
					}
				}
			}
			tx.success();
			return set;
		}
		finally
		{
			tx.finish();
		}
	}
	
	private String glue( Set<String> set )
	{
		StringBuffer buffer = new StringBuffer();
		for ( String token : set )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( this.delimiter );
			}
			buffer.append( token );
		}
		return buffer.toString();
	}
	
	private void store( String value, boolean changed )
	{
		if ( !changed )
		{
			return;
		}
		
		Transaction tx = neo().beginTx();
		try
		{
			this.node.setProperty( this.key, value );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	public boolean add( T item )
	{
		Transaction tx = neo().beginTx();
		try
		{
			Set<String> set = tokenize();
			boolean changed = set.add( itemToString( item ) );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public void clear()
	{
		Transaction tx = neo().beginTx();
		try
		{
			if ( this.node.hasProperty( this.key ) )
			{
				this.node.removeProperty( this.key );
			}
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean contains( Object item )
	{
		return this.tokenize().contains( itemToString( item ) );
	}

	public boolean isEmpty()
	{
		return this.tokenize().size() == 0;
	}

	public Iterator<T> iterator()
	{
		return new ItemIterator( this.tokenize().iterator() );
	}

	public boolean remove( Object item )
	{
		Transaction tx = neo().beginTx();
		try
		{
			Set<String> set = tokenize();
			boolean changed = set.remove( itemToString( item ) );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public boolean retainAll( Collection<?> realItems )
	{
		Transaction tx = neo().beginTx();
		try
		{
			Collection<String> items = new ArrayList<String>();
			for ( Object item : realItems )
			{
				items.add( itemToString( item ) );
			}
			
			Set<String> set = tokenize();
			boolean changed = set.retainAll( items );
			store( glue( set ), changed );
			tx.success();
			return changed;
		}
		finally
		{
			tx.finish();
		}
	}

	public int size()
	{
		return this.tokenize().size();
	}

	public Object[] toArray()
	{
		return this.tokenize().toArray();
	}

	public <R> R[] toArray( R[] array )
	{
		Object[] source = this.tokenize().toArray();
		for ( int i = 0; i < source.length; i++ )
		{
			array[ i ] = ( R ) stringToItem( ( String ) source[ i ] );
		}
		return array;
	}

	private class ItemIterator implements Iterator<T>
	{
		private Iterator<String> iterator;
		
		ItemIterator( Iterator<String> iterator )
		{
			this.iterator = iterator;
		}
		
		public boolean hasNext()
		{
			return iterator.hasNext();
		}

		public T next()
		{
			return stringToItem( iterator.next() );
		}

		public void remove()
		{
			this.iterator.remove();
		}
	}
}