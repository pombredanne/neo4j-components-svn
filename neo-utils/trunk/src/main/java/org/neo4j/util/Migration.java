package org.neo4j.util;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;

/**
 * Used for migrating data between one version of the code to a newer version.
 * Migration is done typically when the data structure needs changes and some
 * adaptation to work with the new version of the code.
 * 
 * Versions begin at zero and increases one per version. Version zero needs
 * no migration. So if the "code version" is zero then no migration is
 * beeing done. If "code version" is more than zero and "data version" is
 * less than "code version" then instances of {@link Migrator} are created
 * and executed for each differentiating version.
 */
public abstract class Migration
{
	private NeoService neo;
	private Node configNode;
	private boolean firstVersionIsAlwaysZero;
	private boolean pretending;
	
	/**
	 * Creates a new migration object with a reference to a configuration node.
	 * @param configNode the node to hold configuration data for migration.
	 * @param neo the {@link NeoService} to use.
	 * This node should be the same every time in a code base.
	 */
	public Migration( NeoService neo, Node configNode )
	{
		this.neo = neo;
		this.configNode = configNode;
	}
	
	private Node getConfigNode()
	{
		return this.configNode;
	}
	
	protected String getCurrentVersionPropertyKey()
	{
		return "current_version";
	}
	
	/**
	 * Used as a lookup to find a {@link Migrator} instance for a specific
	 * version. The first migrator has version one.
	 * @param version which version to find a {@link Migrator} for.
	 * @return the {@link Migrator} for <code>version</code>.
	 */
	protected abstract Migrator findMigrator( int version );
	
	/**
	 * Implemented by the client class to tell the migration unit which version
	 * the code is. If the returned value is zero then nothing is done.
	 * Actual migration starts at version one.
	 * @return the version of the code.
	 */
	protected abstract int getCodeVersion();

	/**
	 * Returns the version of the data. The first call to this method will
	 * return the same version as "code version", and increase as
	 * "code version" increases and calls to {@link #syncVersion()} are made.
	 * @return the version of the data.
	 */
	public int getDataVersion()
	{
		Transaction tx = neo.beginTx();
		try
		{
			int result = 0;
			String key = getCurrentVersionPropertyKey();
			if ( this.getConfigNode().hasProperty( key ) )
			{
				result = ( Integer ) this.getConfigNode().getProperty( key );
			}
			else
			{
				result = this.firstVersionIsAlwaysZero ?
					// This causes all the migrators to run each time a new
					// database is created, but it should be ok since it's
					// empty then.
					0 :
						
					// This is how it should be, but only if the Migration class
					// is used by the client from the beginning of the
					// development There is a problem if the migration class
					// starts getting used after a while, when the first
					// migrator is written.
					this.getCodeVersion();
				this.getConfigNode().setProperty( key, result );
			}
			if ( this.firstVersionIsAlwaysZero )
			{
				result = ( Integer ) this.getConfigNode().getProperty( key, 0 );
			}
			else
			{
				if ( this.getConfigNode().hasProperty( key ) )
				{
					result = ( Integer ) this.getConfigNode().getProperty(
						key );
				}
				else
				{
					result = this.getCodeVersion();
					this.getConfigNode().setProperty( key, result );
				}
			}
			tx.success();
			return result;
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Sets the data version to <code>version</code>. This is really only
	 * used internally to update the data version after each migrated version,
	 * but can be helpful if a problem should arise and some version will
	 * have to be migrated again.
	 * @param version the new data version.
	 */
	public void setDataVersion( int version )
	{
		Transaction tx = neo.beginTx();
		try
		{
			this.getConfigNode().setProperty( getCurrentVersionPropertyKey(),
				version );
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	
	/**
	 * Useful for testing purposes. If <code>pretend</code> is
	 * <code>true</code> all needed migration will be done, but not committed.
	 * @param pretend wether ot nor to pretend the acttual migration.
	 */
	public void setPretend( boolean pretend )
	{
		this.pretending = pretend;
	}
	
	/**
	 * Makes the "data version" to be zero the first time, instead of
	 * same as the "code version".
	 * @param firstIsZero whether or not to enable this functionality.
	 */
	public void setFirstVersionIsAlwaysZero( boolean firstIsZero )
	{
		this.firstVersionIsAlwaysZero = firstIsZero;
	}
	
	/**
	 * This is the method which should be called by the client to tell this
	 * migration unit to look at differences between {@link #getDataVersion()}
	 * and "code version" and migrate accordingly from the
	 * {@link #getDataVersion()} to the "code version".
	 * @throws RuntimeException if the migration (or parts of it) couldn't
	 * be done. It describes which version failed.
	 */
	public void syncVersion()
	{
		int codeVersion = this.getCodeVersion();
		int dataVersion = this.getDataVersion();
		if ( codeVersion == dataVersion )
		{
			// The data version is the same as the code version
			return;
		}
		if ( codeVersion < dataVersion )
		{
			throw new RuntimeException(
				"Backwards migration not supported" );
		}
		
		int versionToMigrate = 0;
		try
		{
			for ( versionToMigrate = dataVersion + 1;
				versionToMigrate <= codeVersion; versionToMigrate++ )
			{
				migrateOne( versionToMigrate );
			}
		}
		catch ( Exception e )
		{
			throw new RuntimeException( "Migration[v" + dataVersion + "-v" +
				codeVersion + "] FAILED, specifically v" +
				versionToMigrate, e );
		}
	}
	
	private void migrateOne( int version )
	{
		Transaction tx = neo.beginTx();
		try
		{
			System.out.println( "Migrating ==> version " + version );
			findMigrator( version ).performMigration( this.neo );
			setDataVersion( version );
			if ( !this.pretending )
			{
				tx.success();
			}
		}
		finally
		{
			tx.finish();
		}
	}
}
