package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

/**
 * Migration unit with a simple default implementation using reflection to
 * find the migrators and a normal sub reference node as the config node.
 */
public abstract class SimpleMigration extends Migration
{
	private String versionClassPrefix;
	
	/**
	 * @param graphDB the {@link GraphDatabaseService} instance to store migration info in.
	 * @param subReferenceType the {@link RelationshipType} to use a sub
	 * reference type.
	 */
	public SimpleMigration( GraphDatabaseService graphDB,
	        RelationshipType subReferenceType )
	{
		super( graphDB, getConfigNodeFromType( graphDB, subReferenceType ) );
		this.versionClassPrefix = this.getMigratorPrefix();
	}
	
	public SimpleMigration( GraphDatabaseService graphDB )
	{
		this( graphDB, MigrationRelationshipTypes.REF_MIGRATION );
	}
	
	private static Node getConfigNodeFromType( GraphDatabaseService graphDb,
		RelationshipType type )
	{
		return new GraphDatabaseUtil( graphDb ).getOrCreateSubReferenceNode( type );
	}
	
	protected String getMigratorPrefix()
	{
		String className = this.getClass().getName();
		int dotIndex = className.lastIndexOf( '.' );
		String result = className.substring( 0, dotIndex + 1 ) + "Migrator";
		return result;
	}
	
	@Override
	protected Migrator findMigrator( int version )
	{
		String className = this.versionClassPrefix + version;
		try
		{
			Class<? extends Migrator> cls =
				Class.forName( className ).asSubclass( Migrator.class );
			return cls.newInstance();
		}
		catch ( RuntimeException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}
	
	public static enum MigrationRelationshipTypes implements RelationshipType
	{
		REF_MIGRATION,
	}
}
