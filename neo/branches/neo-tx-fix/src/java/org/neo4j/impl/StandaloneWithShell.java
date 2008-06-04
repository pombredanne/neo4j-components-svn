/*
 * Copyright 2002-2007 Network Engine for Objects in Lund AB [neotechnology.com]
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;

public class StandaloneWithShell
{
	private static Logger log = Logger.getLogger(
		StandaloneWithShell.class.getName() );	
	private NeoService embeddedNeo;
	private AtomicBoolean shutdownInitiated = new AtomicBoolean( false );
	
	private NeoService getNeo()
	{
		return this.embeddedNeo;
	}
	
	private void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(
			new Thread()
			{
				public void run()
				{
					shutdown();
				}
			} );
	}
	
	private void initialize()
	{
		this.embeddedNeo = new EmbeddedNeo( "var/neo" );
		Map<String, Serializable> shellProperties = Collections.emptyMap();
		getNeo().enableRemoteShell( shellProperties );
		log.info( "Neo started" );
	}
	
	private void blockUntilShutdown()
	{
		try
		{
			while ( shutdownInitiated.get() == false )
			{
				Thread.sleep( 100 );
			}
		}
		catch ( InterruptedException e )
		{
			// Exit
		}		
	}
	
	private void shutdown()
	{
		if ( shutdownInitiated.compareAndSet( false, true ) )
		{
			log.info( "Shutting down..." );
			try
			{
				if ( getNeo() != null )
				{
					getNeo().shutdown();			
					this.embeddedNeo = null;
				}
			}
			catch ( Throwable t )
			{
				log.warning( "Error shutting down Neo: " +  t );
			}
		}		
	}

	public void execute()
	{
		addShutdownHook();
		initialize();
		blockUntilShutdown();
	}
	
	public static void main( String[] args )
	{
		new StandaloneWithShell().execute();
	}
}