package org.neo4j.neometa.structure;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;

public class MetaStructureObject
{
	private static final String KEY_NAME = "name";
	
	private MetaStructure meta;
	private Node node;
	
	MetaStructureObject( MetaStructure meta, Node node )
	{
		this.meta = meta;
		this.node = node;
	}
	
	protected MetaStructure meta()
	{
		return this.meta;
	}
	
	public NeoService neo()
	{
		return meta().neo();
	}
	
	public Node node()
	{
		return this.node;
	}
	
	protected void setProperty( String key, Object value )
	{
		meta().neoUtil().setProperty( node(), key, value );
	}
	
	protected Object getProperty( String key )
	{
		return meta().neoUtil().getProperty( node(), key );
	}

	protected Object getProperty( String key, Object defaultValue )
	{
		return meta().neoUtil().getProperty( node(), key, defaultValue );
	}
	
	void setName( String name )
	{
		setProperty( KEY_NAME, name );
	}
	
	public String getName()
	{
		return ( String ) getProperty( KEY_NAME, null );
	}
	
	@Override
	public int hashCode()
	{
		return node().hashCode();
	}
	
	@Override
	public boolean equals( Object o )
	{
		return o != null && getClass().equals( o.getClass() ) && node().equals(
			( ( MetaStructureObject ) o ).node() );
	}
}
