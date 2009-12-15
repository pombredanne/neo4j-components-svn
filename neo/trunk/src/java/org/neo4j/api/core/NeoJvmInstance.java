/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.api.core;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.transaction.TransactionManager;

import org.neo4j.impl.core.LockReleaser;
import org.neo4j.impl.core.PropertyIndex;
import org.neo4j.impl.nioneo.store.PropertyStore;
import org.neo4j.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.impl.nioneo.xa.NioNeoDbPersistenceSource;
import org.neo4j.impl.transaction.LockManager;
import org.neo4j.impl.transaction.TransactionFailureException;
import org.neo4j.impl.transaction.TxModule;
import org.neo4j.impl.transaction.xaframework.XaDataSource;
import org.neo4j.impl.util.FileUtils;
import org.neo4j.impl.util.UdpPinger;

class NeoJvmInstance
{
    private static final String NIO_NEO_DB_CLASS = "org.neo4j.impl.nioneo.xa.NeoStoreXaDataSource";
    private static final String DEFAULT_DATA_SOURCE_NAME = "nioneodb";

    private static final String LUCENE_DS_CLASS = "org.neo4j.util.index.LuceneDataSource";

    private boolean started = false;
    private boolean create;
    private String storeDir;
    
    private final Timer timer = new Timer();

    NeoJvmInstance( String storeDir, boolean create )
    {
        this.storeDir = storeDir;
        this.create = create;
    }

    private Config config = null;

    private NioNeoDbPersistenceSource persistenceSource = null;

    public Config getConfig()
    {
        return config;
    }

    public void start()
    {
        start( new HashMap<String,String>() );
    }

    private Map<Object,Object> getDefaultParams()
    {
        Map<Object,Object> params = new HashMap<Object,Object>();
        params.put( "neostore.nodestore.db.mapped_memory", "20M" );
        params.put( "neostore.propertystore.db.mapped_memory", "90M" );
        params.put( "neostore.propertystore.db.index.mapped_memory", "1M" );
        params.put( "neostore.propertystore.db.index.keys.mapped_memory", "1M" );
        params.put( "neostore.propertystore.db.strings.mapped_memory", "130M" );
        params.put( "neostore.propertystore.db.arrays.mapped_memory", "130M" );
        params.put( "neostore.relationshipstore.db.mapped_memory", "100M" );
        // if on windows, default no memory mapping
        String nameOs = System.getProperty( "os.name" );
        if ( nameOs.startsWith( "Windows" ) )
        {
            params.put( "use_memory_mapped_buffers", "false" );
        }
        return params;
    }

    /**
     * Starts Neo with default configuration using NioNeo DB as persistence
     * store.
     * 
     * @param storeDir
     *            path to directory where NionNeo DB store is located
     * @param create
     *            if true a new NioNeo DB store will be created if no store
     *            exist at <CODE>storeDir</CODE>
     * @param configuration
     *            parameters
     * @throws StartupFailedException
     *             if unable to start
     */
    public synchronized void start( Map<String,String> stringParams )
    {
        if ( started )
        {
            throw new IllegalStateException( "A Neo instance already started" );
        }
        Map<Object,Object> params = getDefaultParams();
        for ( Map.Entry<String,String> entry : stringParams.entrySet() )
        {
            params.put( entry.getKey(), entry.getValue() );
        }
        config = new Config( storeDir, params );
        // create NioNeo DB persistence source
        storeDir = FileUtils.fixSeparatorsInPath( storeDir );
        String separator = System.getProperty( "file.separator" );
        String store = storeDir + separator + "neostore";
        params.put( "store_dir", storeDir );
        params.put( "neo_store", store );
        params.put( "create", String.valueOf( create ) );
        String logicalLog = storeDir + separator + "nioneo_logical.log";
        params.put( "logical_log", logicalLog );
        byte resourceId[] = "414141".getBytes();
        params.put( LockManager.class, config.getLockManager() );
        params.put( LockReleaser.class, config.getLockReleaser() );
        config.getTxModule().registerDataSource( DEFAULT_DATA_SOURCE_NAME,
            NIO_NEO_DB_CLASS, resourceId, params );
        // hack for lucene index recovery if in path
        XaDataSource lucene = null;
        if ( !config.isReadOnly() )
        {
            try
            {
                Class clazz = Class.forName( LUCENE_DS_CLASS );
                cleanWriteLocksInLuceneDirectory( storeDir + "/lucene" );
                lucene = registerLuceneDataSource( clazz.getName(), config
                    .getTxModule(), storeDir + "/lucene", config.getLockManager() );
            }
            catch ( ClassNotFoundException e )
            { // ok index util not on class path
            }
        }
        // System.setProperty( "neo.tx_log_directory", storeDir );
        persistenceSource = new NioNeoDbPersistenceSource();
        config.setNeoPersistenceSource( DEFAULT_DATA_SOURCE_NAME, create );
        config.getIdGeneratorModule().setPersistenceSourceInstance(
            persistenceSource );
        config.getEventModule().init();
        config.getTxModule().init();
        config.getPersistenceModule().init();
        persistenceSource.init();
        config.getIdGeneratorModule().init();
        config.getNeoModule().init();

        config.getEventModule().start();
        config.getTxModule().start();
        config.getPersistenceModule().start( config.getTxModule().getTxManager(), 
            persistenceSource );
        persistenceSource.start( config.getTxModule().getXaDataSourceManager() );
        config.getIdGeneratorModule().start();
        config.getNeoModule().start( config.getLockReleaser(),  
            config.getPersistenceModule().getPersistenceManager(), params );
        if ( lucene != null )
        {
            config.getTxModule().getXaDataSourceManager().unregisterDataSource(
                "lucene" );
            lucene = null;
        }
        String sendPing = (String) params.get( "send_udp_ping" );
        if ( sendPing == null || !sendPing.toLowerCase().equals( "no" ) )
        {
            sendUdpPingStarted();
            
        }
        started = true;
    }
    
    private static final byte NEO_STARTED = 1;
    private static final byte NEO_SHUTDOWN = 2;
    private static final byte NEO_RUNNING = 3;
    private static final String UDP_HOST = "127.0.0.1";
    private static final int UDP_PORT = 27090;
    private static final long UDP_PING_DELAY = 1000*60*60*24; // 24h
    private static final Random r = new Random( System.currentTimeMillis() );
    
    private long sessionId = -1;
    
    private void sendUdpPingStarted()
    {
        sessionId = r.nextLong();
        sendUdpPing( NEO_STARTED, sessionId );
        timer.schedule( new TimerTask()
        {
            @Override
            public void run()
            {
                sendUdpPingRunning();
            }
        }, UDP_PING_DELAY );
    }

    private void sendUdpPingRunning()
    {
        sendUdpPing( NEO_RUNNING, 3 );
        timer.schedule( new TimerTask()
        {
            @Override
            public void run()
            {
                sendUdpPingRunning();
            }
        }, UDP_PING_DELAY );
    }
    
    private void sendUdpPingShutdown()
    {
        sendUdpPing( NEO_SHUTDOWN, sessionId );
    }
    
    private void sendUdpPing( byte event, long sessionId )
    {
        NeoStoreXaDataSource xaDs = 
            (NeoStoreXaDataSource) persistenceSource.getXaDataSource();
        ByteBuffer buf = ByteBuffer.allocate( 73 );
        buf.put( NEO_STARTED );
        buf.putLong( sessionId );
        buf.putLong( xaDs.getRandomIdentifier() );
        buf.putLong( xaDs.getCreationTime() );
        buf.putLong( xaDs.getCurrentLogVersion() );
        buf.putLong( xaDs.getNumberOfIdsInUse( Node.class ) );
        buf.putLong( xaDs.getNumberOfIdsInUse( Relationship.class ) );
        buf.putLong( xaDs.getNumberOfIdsInUse( PropertyStore.class ) );
        buf.putLong( xaDs.getNumberOfIdsInUse( RelationshipType.class ) );
        buf.putLong( xaDs.getNumberOfIdsInUse( PropertyIndex.class ) );
        buf.flip();
        SocketAddress host = new InetSocketAddress( UDP_HOST, UDP_PORT );
        new UdpPinger( buf, host ).sendPing();
    }
    
    private void cleanWriteLocksInLuceneDirectory( String luceneDir )
    {
        File dir = new File( luceneDir );
        if ( !dir.isDirectory() )
        {
            return;
        }
        for ( File file : dir.listFiles() )
        {
            if ( file.isDirectory() )
            {
                cleanWriteLocksInLuceneDirectory( file.getAbsolutePath() );
            }
            else if ( file.getName().equals( "write.lock" ) )
            {
                boolean success = file.delete();
                assert success;
            }
        }
    }

    private XaDataSource registerLuceneDataSource( String className,
        TxModule txModule, String luceneDirectory, LockManager lockManager )
    {
        byte resourceId[] = "162373".getBytes();
        Map<Object,Object> params = new HashMap<Object,Object>();
        params.put( "dir", luceneDirectory );
        params.put( LockManager.class, lockManager );
        return txModule.registerDataSource( "lucene", className, resourceId,
            params, true );
    }

    /**
     * Returns true if Neo is started.
     * 
     * @return True if Neo started
     */
    public boolean started()
    {
        return started;
    }

    /**
     * Shut down Neo.
     */
    public synchronized void shutdown()
    {
        if ( started )
        {
            timer.cancel();
            String sendPing = (String) config.getParams().get( "send_udp_ping" );
            if ( sendPing == null || !sendPing.toLowerCase().equals( "no" ) )
            {
                sendUdpPingShutdown();
            }
            config.getNeoModule().stop();
            config.getIdGeneratorModule().stop();
            persistenceSource.stop();
            config.getPersistenceModule().stop();
            config.getTxModule().stop();
            config.getEventModule().stop();
            config.getNeoModule().destroy();
            config.getIdGeneratorModule().destroy();
            persistenceSource.destroy();
            config.getPersistenceModule().destroy();
            config.getTxModule().destroy();
            config.getEventModule().destroy();
        }
        started = false;
    }

    public Iterable<RelationshipType> getRelationshipTypes()
    {
        return config.getNeoModule().getRelationshipTypes();
    }

    public boolean transactionRunning()
    {
        try
        {
            return config.getTxModule().getTxManager().getTransaction() != null;
        }
        catch ( Exception e )
        {
            throw new TransactionFailureException( 
                "Unable to get transaction.", e );
        }
    }

    public TransactionManager getTransactionManager()
    {
        return config.getTxModule().getTxManager();
    }
}