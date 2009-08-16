/*
 * Copyright 2008-2009 Network Engine for Objects in Lund AB [neotechnology.com]
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
package org.neo4j.remote;

/**
 * Represents a connection layer used to communicate with a Neo instance in a
 * remote process.
 * @author Tobias Ivarsson
 */
public interface RemoteSite
{
    /**
     * Connect to the remote site.
     * @return The connection to the remote site.
     */
    RemoteConnection connect();

    /**
     * Connect to the remote site.
     * @param username
     *            The name of the user that makes the connection.
     * @param password
     *            The password for the user that makes the connection.
     * @return The connection to the remote site.
     */
    RemoteConnection connect( String username, String password );
}