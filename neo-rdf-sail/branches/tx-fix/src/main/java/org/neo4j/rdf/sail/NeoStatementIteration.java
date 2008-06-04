package org.neo4j.rdf.sail;

import info.aduna.iteration.CloseableIteration;

import java.util.Iterator;

import org.neo4j.rdf.model.CompleteStatement;
import org.openrdf.model.Statement;
import org.openrdf.sail.SailException;

/**
 * Author: josh
 * Date: Apr 25, 2008
 * Time: 7:10:31 PM
 */
public class NeoStatementIteration implements CloseableIteration<Statement, SailException> {
    private final Iterator<org.neo4j.rdf.model.CompleteStatement> iterator;

    private final NeoSailConnection connection;
    
    public NeoStatementIteration(final NeoSailConnection connection, 
    	final Iterator<org.neo4j.rdf.model.CompleteStatement> iterator) {
    	this.connection = connection;
        this.iterator = iterator;
    }

    public void close() throws SailException {
        // Not needed
    }

    public boolean hasNext() throws SailException {
    	synchronized ( connection )
    	{
    		connection.suspendOtherAndResumeThis();
    		try
    		{
    			return iterator.hasNext();
    		}
    		finally
    		{
    			connection.suspendThisAndResumeOther();
    		}
    	}
    }

    public Statement next() throws SailException {
    	synchronized ( connection )
    	{
    		connection.suspendOtherAndResumeThis();
    		try
    		{
	        org.neo4j.rdf.model.CompleteStatement statement = iterator.next();
	//System.out.println("retrieved a statement: " + statement);
	        return (null == statement)
	                // TODO: would be better here if iterator were an Iterator<CompleteStatement>
	                ? null : NeoSesameMapper.createStatement((CompleteStatement) statement);
    		}
    		finally
    		{
    			connection.suspendThisAndResumeOther();
    		}
    	}
    }

    public void remove() throws SailException {
        // TODO: decide whether remove() should be supported
    }
}