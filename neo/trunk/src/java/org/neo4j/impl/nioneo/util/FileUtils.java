package org.neo4j.impl.nioneo.util;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils
{
    private static int WINDOWS_RETRY_COUNT = 3;

    public static boolean deleteFile( File file )
    {
        if ( !file.exists() )
        {
            return true;
        }
        int count = 0;
        boolean deleted = false;
        do
        {
            deleted = file.delete();
            if ( !deleted )
            {
                count++;
                waitSome();
            }
        }
        while ( !deleted && count <= WINDOWS_RETRY_COUNT );
        return deleted;
    }
    
    public static boolean renameFile( File srcFile, File renameToFile )
    {
        if ( !srcFile.exists() )
        {
            throw new RuntimeException( "Source file[" + srcFile.getName()
                + "] not found" );
        }
        if ( renameToFile.exists() )
        {
            throw new RuntimeException( "Source file[" + renameToFile.getName()
                + "] not found" );
        }
        int count = 0;
        boolean renamed = false;
        do
        {
            renamed = srcFile.renameTo( renameToFile );
            if ( !renamed )
            {
                count++;
                waitSome();
            }
        }
        while ( !renamed && count <= WINDOWS_RETRY_COUNT );
        return renamed;
    }

    public static void truncateFile( FileChannel fileChannel, long position )
        throws IOException
    {
        int count = 0;
        boolean success = false;
        IOException cause = null;
        do
        {
            count++;
            try
            {
                fileChannel.truncate( position );
                success = true;
            }
            catch ( IOException e )
            {
                cause = e;
            }

        }
        while ( !success && count <= WINDOWS_RETRY_COUNT );
        if ( !success )
        {
            throw cause;
        }
    }

    private static void waitSome()
    {
        try
        {
            Thread.sleep( 500 );
        }
        catch ( InterruptedException ee )
        {
            Thread.interrupted();
        } // ok
        System.gc();
    }
}