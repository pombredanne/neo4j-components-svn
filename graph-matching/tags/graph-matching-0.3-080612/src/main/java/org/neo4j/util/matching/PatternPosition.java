package org.neo4j.util.matching;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;

class PatternPosition
{
	private Node currentNode;
	private PatternNode pNode;
	private Iterator<PatternRelationship> itr;
	private PatternRelationship nextPRel = null;
	private PatternRelationship previous = null;
	private PatternRelationship returnPrevious = null;
	private boolean optional = false;
    private PatternRelationship fromPRel = null;
    private Relationship fromRel = null;

	PatternPosition( Node currentNode, PatternNode pNode, boolean optional )
	{
		this.currentNode = currentNode;
		this.pNode = pNode;
		itr = pNode.getRelationships( optional ).iterator();
		this.optional = optional;
	}

    PatternPosition( Node currentNode, PatternNode pNode, 
        PatternRelationship fromPRel, Relationship fromRel, boolean optional )
    {
        this.currentNode = currentNode;
        this.pNode = pNode;
        itr = pNode.getRelationships( optional ).iterator();
        this.optional = optional;
        this.fromPRel = fromPRel;
        this.fromRel = fromRel;
    }
    
	Node getCurrentNode()
	{
		return currentNode;
	}

	private void setNextQRel()
	{
		while ( itr.hasNext() )
		{
			nextPRel = itr.next();
			if ( !nextPRel.isMarked() )
			{
				return;
			}
			nextPRel = null;
		}
	}

	PatternNode getPatternNode()
	{
		return pNode;
	}

	boolean hasNext()
	{
		if ( returnPrevious != null )
		{
			return true;
		}
		if ( nextPRel == null )
		{
			setNextQRel();
		}
		return nextPRel != null;
	}

	PatternRelationship next()
	{
		if ( returnPrevious != null )
		{
			PatternRelationship relToReturn = returnPrevious;
			returnPrevious = null;
			return relToReturn;
		}
		if ( nextPRel == null )
		{
			setNextQRel();
		}
		else
		{
			return resetNextPRel();
		}
		if ( nextPRel == null )
		{
			throw new NoSuchElementException();
		}
		return resetNextPRel();
	}

	private PatternRelationship resetNextPRel()
	{
		PatternRelationship relToReturn = nextPRel;
		previous = nextPRel;
		nextPRel = null;
		return relToReturn;
	}

	void reset()
    {
		returnPrevious = null;
		previous = null;
		nextPRel = null;
		itr = pNode.getRelationships( optional ).iterator();
    }

	public void returnPreviousAgain()
    {
		returnPrevious = previous;
    }
	
	@Override
	public String toString()
	{
		return pNode.toString();
	}

    public PatternRelationship fromPatternRel()
    {
        return fromPRel;
    }
    
    public Relationship fromRelationship()
    {
        return fromRel;
    }
}
