package org.neo4j.meta.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

/**
 * Represents a namespace in the meta model structure. A namespace is useful
 * when there would be conflicting names of properties or classes.
 */
public class MetaModelNamespace extends MetaModelObject
{
	private Map<String, MetaModelClass> classCache =
		Collections.synchronizedMap(
			new HashMap<String, MetaModelClass>() );
	private Map<String, MetaModelProperty> propertyCache =
		Collections.synchronizedMap(
			new HashMap<String, MetaModelProperty>() );
	
	/**
	 * @param model the {@link MetaModel} instance.
	 * @param node the {@link Node} to wrap.
	 */
	public MetaModelNamespace( MetaModel model, Node node )
	{
		super( model, node );
	}
	
	/**
	 * Returns (and optionally creates) a {@link MetaModelClass} instance
	 * (with underlying {@link Node}).
	 * @param name the name of the class.
	 * @param allowCreate if {@code true} and no class by the given {@code name}
	 * exists then it is created.
	 * @return the {@link MetaModelClass} in this namespace with the given
	 * {@code name}.
	 */
	public MetaModelClass getMetaClass( String name, boolean allowCreate )
	{
		return ( ( MetaModelImpl ) model() ).findOrCreateInCollection(
			getMetaClasses(), name, allowCreate, MetaModelClass.class,
			classCache );
	}
	
	/**
	 * @return a modifiable collection of all {@link MetaModelClass}
	 * instances for this namespace.
	 */
	public Collection<MetaModelClass> getMetaClasses()
	{
		return new ObjectCollection<MetaModelClass>( graphDb(),
			node(), MetaModelRelTypes.META_CLASS, Direction.OUTGOING,
			model(), MetaModelClass.class );
	}
	
	/**
	 * Returns (and optionally creates) a {@link MetaModelProperty} instance
	 * (with underlying {@link Node}).
	 * @param name the name of the property.
	 * @param allowCreate if {@code true} and no property by the given
	 * {@code name} exists then it is created.
	 * @return the {@link MetaModelProperty} in this namespace with the
	 * given {@code name}.
	 */
	public MetaModelProperty getMetaProperty( String name,
		boolean allowCreate )
	{
		return ( ( MetaModelImpl ) model() ).findOrCreateInCollection(
			getMetaProperties(), name, allowCreate,
			MetaModelProperty.class, propertyCache );
	}
	
	/**
	 * @return a modifiable collection of all {@link MetaModelProperty}
	 * instances for this namespace.
	 */
	public Collection<MetaModelProperty> getMetaProperties()
	{
		return new ObjectCollection<MetaModelProperty>( graphDb(),
			node(), MetaModelRelTypes.META_PROPERTY, Direction.OUTGOING,
			model(), MetaModelProperty.class );
	}
	
	@Override
	public String toString()
	{
		String name = ( String ) node().getProperty( KEY_NAME, "GLOBAL" );
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
