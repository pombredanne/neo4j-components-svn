/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

package org.neo4j.kernel.impl.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StringLogger
{
    private final PrintWriter out;
    
    private StringLogger( String filename )
    {
        try
        {
            File file = new File( filename );
            file.getParentFile().mkdirs();
            out = new PrintWriter( new FileWriter( file, true ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    private static final Map<String,StringLogger> loggers = 
        new HashMap<String, StringLogger>();
    
    public static StringLogger getLogger( String filename )
    {
        StringLogger logger = loggers.get( filename );
        if ( logger == null )
        {
            logger = new StringLogger( filename );
            loggers.put( filename, logger );
        }
        return logger;
    }
    
    public synchronized void logMessage( String msg )
    {
        out.println( new Date() + ": " + msg );
        out.flush();
    } 

    public synchronized void logMessage( String msg, Throwable cause )
    {
        out.println( new Date() + ": " + msg + " " + cause.getMessage() );
        cause.printStackTrace( out );
        out.flush();
    }
    
    public synchronized static void close( String filename )
    {
        StringLogger logger = loggers.remove( filename );
        if ( logger != null )
        {
            logger.out.close();
        }
    }
}
