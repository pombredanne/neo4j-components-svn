/*
 * Licensed to "Neo Technology," Network Engine for Objects in Lund AB
 * (http://neotechnology.com) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at (http://www.apache.org/licenses/LICENSE-2.0). Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.neo4j.neoclipse.decorate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.PropertyContainer;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.neoclipse.NeoIcons;
import org.neo4j.neoclipse.action.decorate.node.ShowNodeColorsAction;
import org.neo4j.neoclipse.action.decorate.node.ShowNodeIconsAction;
import org.neo4j.neoclipse.action.decorate.node.ShowNodeIdsAction;
import org.neo4j.neoclipse.action.decorate.node.ShowNodeLabelAction;
import org.neo4j.neoclipse.action.decorate.node.ShowNodePropertiesAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipColorsAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipDirectionsAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipIdsAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipLabelAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipPropertiesAction;
import org.neo4j.neoclipse.action.decorate.rel.ShowRelationshipTypesAction;
import org.neo4j.neoclipse.property.PropertyTransform;

public class SimpleGraphDecorator
{
    private static final int RELATIONSHIP = 0;
    private static final int NODE_INCOMING = 1;
    private static final int NODE_OUTGOING = 2;
    private static final int RELATIONSHIP_MARKED = 3;
    private static final int NODE_INCOMING_MARKED = 4;
    private static final int NODE_OUTGOING_MARKED = 5;

    /**
     * The icon for nodes.
     */
    private static final Image nodeImage = NeoIcons.NEO.image();
    /**
     * The icon for the root node.
     */
    private static final Image rootImage = NeoIcons.NEO_ROOT.image();
    /**
     * User icons for nodes.
     */
    private final UserIcons userIcons;
    /**
     * Default relationship "color" (gray).
     */
    private static final Color RELATIONSHIP_COLOR = new Color( Display
        .getDefault(), new RGB( 70, 70, 70 ) );
    /**
     * Default node background color.
     */
    private static final Color NODE_BACKGROUND_COLOR = new Color( Display
        .getDefault(), new RGB( 255, 255, 255 ) );
    /**
     * Color of node foreground/text.
     */
    private static final Color NODE_FOREGROUND_COLOR = new Color( Display
        .getDefault(), new RGB( 0, 0, 0 ) );
    /**
     * Color of node foreground/text.
     */
    private static final Color INPUTNODE_FOREGROUND_COLOR = new Color( Display
        .getDefault(), new RGB( 60, 60, 200 ) );
    /**
     * Highlight color for relationships.
     */
    private static final Color HIGHLIGHTED_RELATIONSHIP_COLOR = new Color(
        Display.getDefault(), new RGB( 0, 0, 0 ) );
    /**
     * Map colors to relationship types.
     */
    private final SimpleColorMapper<RelationshipType> colorMapper;
    /**
     * Settings for this decorator.
     */
    private final Settings settings;
    /**
     * View settings for this decorator.
     */
    private final ViewSettings viewSettings;

    public static class Settings
    {
        /**
         * List defining order of relationship lookups for nodes.
         */
        private List<Direction> directions;
        /**
         * Property names to look for in nodes.
         */
        private List<String> nodePropertyNames;
        /**
         * property names to look for in relationships.
         */
        private List<String> relPropertyNames;
        /**
         * Properties to look for icon information in.
         */
        private List<String> nodeIconPropertyNames;
        /**
         * Current location of icons.
         */
        private String nodeIconLocation;

        public List<Direction> getDirections()
        {
            return directions;
        }

        public void setDirections( final List<Direction> directions )
        {
            this.directions = directions;
        }

        public String getNodeIconLocation()
        {
            return nodeIconLocation;
        }

        public void setNodeIconLocation( final String nodeIconLocation )
        {
            this.nodeIconLocation = nodeIconLocation;
        }

        public List<String> getNodePropertyNames()
        {
            return nodePropertyNames;
        }

        public void setNodePropertyNames( final String nodePropertyNames )
        {
            this.nodePropertyNames = listFromString( nodePropertyNames );
        }

        public List<String> getRelPropertyNames()
        {
            return relPropertyNames;
        }

        public void setRelPropertyNames( final String relPropertyNames )
        {
            this.relPropertyNames = listFromString( relPropertyNames );
        }

        public List<String> getNodeIconPropertyNames()
        {
            return nodeIconPropertyNames;
        }

        public void setNodeIconPropertyNames( String nodeIconPropertyNames )
        {
            this.nodeIconPropertyNames = listFromString( nodeIconPropertyNames );
        }

        /**
         * Convert a string containing a comma-separated list of names to a list
         * of strings. Ignores "" as a name.
         * @param names
         *            comma-separated names
         * @return list of names
         */
        private List<String> listFromString( String names )
        {
            final List<String> list = new ArrayList<String>();
            for ( String name : names.split( "," ) )
            {
                name = name.trim();
                if ( "".equals( name ) )
                {
                    continue;
                }
                list.add( name );
            }
            return list;
        }
    }

    public static class ViewSettings
    {
        /**
         * Keep track of relationship types display on/off.
         */
        private boolean showRelationshipTypes = ShowRelationshipTypesAction.DEFAULT_STATE;
        /**
         * Keep track of relationship names display on/off.
         */
        private boolean showRelationshipNames = ShowRelationshipLabelAction.DEFAULT_STATE;
        /**
         * Keep track of relationship properties display on/off.
         */
        private boolean showRelationshipProperties = ShowRelationshipPropertiesAction.DEFAULT_STATE;
        /**
         * Keep track of relationship id's display on/off.
         */
        private boolean showRelationshipIds = ShowRelationshipIdsAction.DEFAULT_STATE;
        /**
         * Keep track of relationship colors display on/off.
         */
        private boolean showRelationshipColors = ShowRelationshipColorsAction.DEFAULT_STATE;
        /**
         * Keep track of arrows display on/off.
         */
        private boolean showArrows = ShowRelationshipDirectionsAction.DEFAULT_STATE;
        /**
         * Keep track of node id's display on/off.
         */
        private boolean showNodeIds = ShowNodeIdsAction.DEFAULT_STATE;
        /**
         * Keep track of node names display on/off.
         */
        private boolean showNodeNames = ShowNodeLabelAction.DEFAULT_STATE;
        /**
         * Keep track of node properties display on/off.
         */
        private boolean showNodeProperties = ShowNodePropertiesAction.DEFAULT_STATE;
        /**
         * Keep track of node icons display on/off.
         */
        private boolean showNodeIcons = ShowNodeIconsAction.DEFAULT_STATE;
        /**
         * Keep track of node colors display on/off.
         */
        private boolean showNodeColors = ShowNodeColorsAction.DEFAULT_STATE;

        public boolean isShowRelationshipTypes()
        {
            return showRelationshipTypes;
        }

        public void setShowRelationshipTypes( boolean showRelationshipTypes )
        {
            this.showRelationshipTypes = showRelationshipTypes;
        }

        public boolean isShowRelationshipNames()
        {
            return showRelationshipNames;
        }

        public void setShowRelationshipNames( boolean showRelationshipNames )
        {
            this.showRelationshipNames = showRelationshipNames;
        }

        public boolean isShowRelationshipProperties()
        {
            return showRelationshipProperties;
        }

        public void setShowRelationshipProperties(
            boolean showRelationshipProperties )
        {
            this.showRelationshipProperties = showRelationshipProperties;
        }

        public boolean isShowRelationshipIds()
        {
            return showRelationshipIds;
        }

        public void setShowRelationshipIds( boolean showRelationshipIds )
        {
            this.showRelationshipIds = showRelationshipIds;
        }

        public boolean isShowRelationshipColors()
        {
            return showRelationshipColors;
        }

        public void setShowRelationshipColors( boolean showRelationshipColors )
        {
            this.showRelationshipColors = showRelationshipColors;
        }

        public boolean isShowArrows()
        {
            return showArrows;
        }

        public void setShowArrows( boolean showArrows )
        {
            this.showArrows = showArrows;
        }

        public boolean isShowNodeIds()
        {
            return showNodeIds;
        }

        public void setShowNodeIds( boolean showNodeIds )
        {
            this.showNodeIds = showNodeIds;
        }

        public boolean isShowNodeNames()
        {
            return showNodeNames;
        }

        public void setShowNodeNames( boolean showNodeNames )
        {
            this.showNodeNames = showNodeNames;
        }

        public boolean isShowNodeProperties()
        {
            return showNodeProperties;
        }

        public void setShowNodeProperties( boolean showNodeProperties )
        {
            this.showNodeProperties = showNodeProperties;
        }

        public boolean isShowNodeIcons()
        {
            return showNodeIcons;
        }

        public void setShowNodeIcons( boolean showNodeIcons )
        {
            this.showNodeIcons = showNodeIcons;
        }

        public boolean isShowNodeColors()
        {
            return showNodeColors;
        }

        public void setShowNodeColors( boolean showNodeColors )
        {
            this.showNodeColors = showNodeColors;
        }
    }

    public SimpleGraphDecorator( Settings settings, ViewSettings viewSettings )
    {
        if ( settings.getDirections() == null )
        {
            throw new IllegalArgumentException( "Null directions list given." );
        }
        if ( settings.getDirections().isEmpty() )
        {
            throw new IllegalArgumentException( "Empty directions list given." );
        }
        this.settings = settings;
        this.viewSettings = viewSettings;
        final float[] saturations = new float[6];
        final float[] brightnesses = new float[6];
        saturations[RELATIONSHIP] = 0.8f;
        brightnesses[RELATIONSHIP] = 0.7f;
        saturations[NODE_INCOMING] = 0.17f;
        brightnesses[NODE_INCOMING] = 1.0f;
        saturations[NODE_OUTGOING] = 0.08f;
        brightnesses[NODE_OUTGOING] = 0.95f;
        saturations[RELATIONSHIP_MARKED] = 0.8f;
        brightnesses[RELATIONSHIP_MARKED] = 0.5f;
        saturations[NODE_INCOMING_MARKED] = 0.3f;
        brightnesses[NODE_INCOMING_MARKED] = 0.7f;
        saturations[NODE_OUTGOING_MARKED] = 0.2f;
        brightnesses[NODE_OUTGOING_MARKED] = 0.6f;
        colorMapper = new SimpleColorMapper<RelationshipType>( saturations,
            brightnesses );
        userIcons = new UserIcons( settings.getNodeIconLocation() );
    }

    public Color getNodeColor()
    {
        return NODE_BACKGROUND_COLOR;
    }

    public Color getNodeColor( final Node node )
    {
        return getNodeColor( node, false );
    }

    /**
     * @param node
     * @param marked
     *            TODO
     * @return
     */
    private Color getNodeColor( final Node node, boolean marked )
    {
        Relationship randomRel = null;
        Direction randomDir = null;
        for ( Direction direction : settings.getDirections() )
        {
            for ( Relationship rel : node.getRelationships( direction ) )
            {
                RelationshipType type = rel.getType();
                if ( !colorMapper.colorExists( type ) )
                {
                    if ( randomRel == null )
                    {
                        randomRel = rel;
                        randomDir = direction;
                    }
                    continue;
                }
                else
                {
                    return getColorFromDirection( type, direction, marked );
                }
            }
        }
        if ( randomRel != null )
        {
            return getColorFromDirection( randomRel.getType(), randomDir,
                marked );
        }
        return getNodeColor();
    }

    private Color getColorFromDirection( final RelationshipType type,
        final Direction direction, boolean marked )
    {
        switch ( direction )
        {
            case INCOMING:
                if ( marked )
                {
                    return colorMapper.getColor( type, NODE_INCOMING_MARKED );
                }
                else
                {
                    return colorMapper.getColor( type, NODE_INCOMING );
                }
            case OUTGOING:
                if ( marked )
                {
                    return colorMapper.getColor( type, NODE_OUTGOING_MARKED );
                }
                else
                {
                    return colorMapper.getColor( type, NODE_OUTGOING );
                }
            default:
                if ( marked )
                {
                    return colorMapper.getColor( type, RELATIONSHIP_MARKED );
                }
                else
                {
                    return colorMapper.getColor( type, RELATIONSHIP );
                }
        }
    }

    public Color getRelationshipColor()
    {
        return RELATIONSHIP_COLOR;
    }

    public Color getRelationshipColor( final Relationship rel )
    {
        return colorMapper.getColor( rel.getType(), RELATIONSHIP );
    }

    public Color getRelationshipColor( final RelationshipType relType )
    {
        return colorMapper.getColor( relType, RELATIONSHIP );
    }

    private String getSimpleNodeText( final Node node, final boolean isReferenceNode )
    {
        if ( isReferenceNode )
        {
            return "Reference Node";
        }
        else
        {
            return "Node";
        }
    }

    public String getNodeText( final Node node,
        final boolean isReferenceNode )
    {
        StringBuilder str = new StringBuilder( 48 );
        if ( viewSettings.isShowNodeProperties() )
        {
            return readAllProperties( node, viewSettings.isShowNodeIds() );
        }
        else
        {
            if ( viewSettings.isShowNodeNames()
                && !settings.getNodePropertyNames().isEmpty() )
            {
                String propertyValue = readProperties( node, settings
                    .getNodePropertyNames() );
                if ( propertyValue == null )
                {
                    propertyValue = getSimpleNodeText( node, isReferenceNode );
                }
                if ( propertyValue != null )
                {
                    if ( str.length() > 0 )
                    {
                        str.append( ", " );
                    }
                    str.append( propertyValue );
                }
            }
            else
            {
                // don't look for the default property
                str.append( getSimpleNodeText( node, isReferenceNode ) );
            }
            if ( viewSettings.isShowNodeIds() )
            {
                if ( str.length() > 0 )
                {
                    str.append( ", " );
                }
                str.append( node.getId() );
            }
        }
        return str.toString();
    }

    private String readProperties( final PropertyContainer container,
        final List<String> propertyNames )
    {
        List<String> values = new ArrayList<String>();
        for ( String propertyName : propertyNames )
        {
            Object propertyValue = container.getProperty( propertyName, "" );
            if ( propertyValue instanceof String )
            {
                if ( "".equals( propertyValue ) )
                {
                    // no empty strings here, thanks
                    continue;
                }
                values.add( (String) propertyValue );
            }
            else
            {
                // get a proper String from other types
                String render = PropertyTransform.getHandler( propertyValue )
                    .render( propertyValue );
                values.add( render );
            }
        }
        if ( values.size() > 0 )
        {
            String result = values.toString();
            return result.substring( 1, result.length() - 1 );
        }
        return null;
    }

    private String readAllProperties( final PropertyContainer container,
        final boolean includeId )
    {
        SortedSet<String> values = new TreeSet<String>();
        Iterable<String> allProperties = container.getPropertyKeys();
        for ( String propertyName : allProperties )
        {
            Object propertyValue = container.getProperty( propertyName, "" );
            if ( propertyValue instanceof String )
            {
                values.add( propertyName + ": " + propertyValue );
            }
            else
            {
                // get a proper String from other types
                String render = PropertyTransform.getHandler( propertyValue )
                    .render( propertyValue );
                values.add( propertyName + ": " + render );
            }
        }
        StringBuilder str = new StringBuilder( 128 );
        if ( includeId )
        {
            if ( container instanceof Node )
            {
                str.append( "id: " ).append( ((Node) container).getId() )
                    .append( '\n' );
            }
            else if ( container instanceof Relationship )
            {
                str.append( "id: " )
                    .append( ((Relationship) container).getId() ).append( '\n' );
            }
        }
        if ( values.size() > 0 )
        {
            for ( String value : values )
            {
                str.append( value ).append( '\n' );
            }
        }
        if ( str.length() > 1 )
        {
            return str.substring( 0, str.length() - 1 );
        }
        return "";
    }

    public String getRelationshipText( final Relationship rel )
    {
        StringBuilder str = new StringBuilder( 48 );
        if ( viewSettings.isShowRelationshipTypes() )
        {
            str.append( rel.getType().name() );
        }
        if ( viewSettings.isShowRelationshipProperties() )
        {
            if ( viewSettings.isShowRelationshipTypes() )
            {
                str.append( '\n' );
            }
            String propertyValue = readAllProperties( rel, viewSettings
                .isShowRelationshipIds() );
            if ( propertyValue != null )
            {
                str.append( propertyValue );
            }
        }
        else
        {
            if ( viewSettings.isShowRelationshipIds() )
            {
                if ( str.length() > 0 )
                {
                    str.append( ", " );
                }
                str.append( rel.getId() );
            }
            if ( viewSettings.isShowRelationshipNames()
                && !settings.getRelPropertyNames().isEmpty() )
            {
                String propertyValue = readProperties( rel, settings
                    .getRelPropertyNames() );
                if ( propertyValue != null )
                {
                    if ( str.length() > 0 )
                    {
                        str.append( ", " );
                    }
                    str.append( propertyValue );
                }
            }
        }
        return str.toString();
    }

    public Image getNodeImage( final Node node, final boolean isReferenceNode )
    {
        Image img;

        if ( isReferenceNode )
        {
            img = rootImage;
        }
        else
        {
            img = nodeImage;
        }
        return img;
    }

    public Image getNodeImageFromProperty( final Node node,
        final boolean isReferenceNode )
    {
        Image img = null;
        // look in properties
        for ( String propertyName : settings.getNodeIconPropertyNames() )
        {
            String tmpPropVal = (String) node.getProperty( propertyName, "" );
            if ( !"".equals( tmpPropVal ) ) // no empty strings
            {
                img = userIcons.getImage( tmpPropVal );
                if ( img != null )
                {
                    return img;
                }
            }
        }
        // look in relations
        for ( Direction direction : settings.getDirections() )
        {
            for ( Relationship rel : node.getRelationships( direction ) )
            {
                img = userIcons.getImage( rel.getType(), direction );
                if ( img != null )
                {
                    return img;
                }
            }
        }
        return getNodeImage( node, isReferenceNode );
    }

    public Color getRelationshipHighlightColor( Relationship rel )
    {
        return HIGHLIGHTED_RELATIONSHIP_COLOR;
    }

    public Color getNodeForegroundColor( final Node node,
        final boolean isInputNode )
    {
        if ( isInputNode )
        {
            return INPUTNODE_FOREGROUND_COLOR;
        }
        else
        {
            return NODE_FOREGROUND_COLOR;
        }
    }

    public Set<RelationshipType> getRelationshipTypes()
    {

        return colorMapper.getKeys();
    }

    public Color getMarkedRelationshipColor( Relationship rel )
    {
        return colorMapper.getColor( rel.getType(), RELATIONSHIP_MARKED );
    }

    public int getMarkedRelationshipStyle( Object rel )
    {
        return 0;
    }

    public Color getMarkedNodeColor( Node element )
    {
        return getNodeColor( element, true );
    }

    public int getMarkedLineWidth()
    {
        return 2;
    }

    public int getLineWidth()
    {
        return -1;
    }
}
