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
package org.neo4j.impl.core;


// TODO: make LRU x elements
public class PropertyIndex
{
	private final String key;
	private final int keyId;
	
	protected PropertyIndex( String key, int keyId )
	{
		this.key = key;
		this.keyId = keyId;
	}
	
	public String getKey()
	{
		return key;
	}
	
	@Override
	public int hashCode()
	{
		return keyId;
	}
	
	public int getKeyId()
	{
		return this.keyId;
	}
	
	@Override
	public boolean equals( Object o )
	{
		if ( o instanceof PropertyIndex )
		{
			return keyId == ((PropertyIndex ) o).getKeyId();
		}
		return false;
	}
}