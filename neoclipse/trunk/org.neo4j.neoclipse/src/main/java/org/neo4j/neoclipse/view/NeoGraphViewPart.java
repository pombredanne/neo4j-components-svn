/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.neoclipse.view;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.neo4j.neoclipse.Activator;
import org.neo4j.neoclipse.NeoIcons;
import org.neo4j.neoclipse.action.DecreaseTraversalDepthAction;
import org.neo4j.neoclipse.action.IncreaseTraversalDepthAction;
import org.neo4j.neoclipse.action.PrintGraphAction;
import org.neo4j.neoclipse.action.RefreshAction;
import org.neo4j.neoclipse.action.ShowNodeColorsAction;
import org.neo4j.neoclipse.action.ShowNodeIconsAction;
import org.neo4j.neoclipse.action.ShowNodeIdsAction;
import org.neo4j.neoclipse.action.ShowRelationshipDirectionsAction;
import org.neo4j.neoclipse.action.ShowGridLayoutAction;
import org.neo4j.neoclipse.action.ShowNodeNamesAction;
import org.neo4j.neoclipse.action.ShowRadialLayoutAction;
import org.neo4j.neoclipse.action.ShowReferenceNodeAction;
import org.neo4j.neoclipse.action.ShowRelationshipColorsAction;
import org.neo4j.neoclipse.action.ShowRelationshipIdsAction;
import org.neo4j.neoclipse.action.ShowRelationshipTypesAction;
import org.neo4j.neoclipse.action.ShowSpringLayoutAction;
import org.neo4j.neoclipse.action.ShowTreeLayoutAction;
import org.neo4j.neoclipse.action.ZoomAction;
import org.neo4j.neoclipse.help.HelpContextConstants;
import org.neo4j.neoclipse.neo.NeoServiceEvent;
import org.neo4j.neoclipse.neo.NeoServiceEventListener;
import org.neo4j.neoclipse.neo.NeoServiceManager;
import org.neo4j.neoclipse.neo.NeoServiceStatus;

/**
 * This class is a view that shows the contents of a Neo database as a graph of
 * connected objects.
 * @author Peter H&auml;nsgen
 */
public class NeoGraphViewPart extends ViewPart implements
    IZoomableWorkbenchPart
{
    /**
     * The Eclipse view ID.
     */
    public static final String ID = "org.neo4j.neoclipse.view.NeoGraphViewPart";
    /**
     * The property sheet page.
     */
    protected PropertySheetPage propertySheetPage;
    /**
     * The graph.
     */
    protected GraphViewer viewer;
    /**
     * Label provider for the graph.
     */
    protected NeoGraphLabelProvider labelProvider;
    /**
     * The decrease traversal depth action.
     */
    protected DecreaseTraversalDepthAction decAction;
    /**
     * The depth how deep we should traverse into the network.
     */
    private int traversalDepth = 1;

    /**
     * Creates the view.
     */
    public void createPartControl( Composite parent )
    {
        viewer = new GraphViewer( parent, SWT.NONE );
        viewer.setContentProvider( new NeoGraphContentProvider( this ) );
        labelProvider = new NeoGraphLabelProvider();
        viewer.setLabelProvider( labelProvider );
        viewer.addDoubleClickListener( new NeoGraphDoubleClickListener() );
        viewer.setLayoutAlgorithm( new SpringLayoutAlgorithm(
            LayoutStyles.NO_LAYOUT_NODE_RESIZING ) );
        makeContributions();
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        sm.addServiceEventListener( new NeoGraphServiceEventListener() );
        getSite().setSelectionProvider( viewer );
        showReferenceNode();
        PlatformUI.getWorkbench().getHelpSystem().setHelp( viewer.getControl(),
            HelpContextConstants.NEO_GRAPH_VIEW_PART );
    }

    /**
     * Initializes menus, toolbars etc.
     */
    protected void makeContributions()
    {
        // initialize actions
        IToolBarManager tm = getViewSite().getActionBars().getToolBarManager();
        IMenuManager mm = getViewSite().getActionBars().getMenuManager();
        // standard actions
        {
            ShowReferenceNodeAction refNodeAction = new ShowReferenceNodeAction(
                this );
            refNodeAction.setText( "Show Reference Node" );
            refNodeAction.setToolTipText( "Show Reference Node" );
            refNodeAction.setImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.HOME ) );
            tm.add( refNodeAction );
            RefreshAction refreshAction = new RefreshAction( this );
            refreshAction.setText( "Refresh" );
            refreshAction.setToolTipText( "Refresh" );
            refreshAction.setImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.REFRESH ) );
            tm.add( refreshAction );
            tm.add( new Separator() );
        }
        // recursion level actions
        {
            IncreaseTraversalDepthAction incAction = new IncreaseTraversalDepthAction(
                this );
            incAction.setText( "Increase Traversal Depth" );
            incAction.setToolTipText( "Increase Traversal Depth" );
            incAction.setImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.PLUS_ENABLED ) );
            incAction.setDisabledImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.PLUS_DISABLED ) );
            tm.add( incAction );
            decAction = new DecreaseTraversalDepthAction( this );
            decAction.setText( "Decrease Traversal Depth" );
            decAction.setToolTipText( "Decrease Traversal Depth" );
            decAction.setImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.MINUS_ENABLED ) );
            decAction.setDisabledImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.MINUS_DISABLED ) );
            tm.add( decAction );
            tm.add( new Separator() );
        }
        // zoom actions
        {
            ZoomAction zoomAction = new ZoomAction( this );
            zoomAction.setText( "Zoom" );
            zoomAction.setToolTipText( "Zoom" );
            zoomAction.setImageDescriptor( Activator.getDefault()
                .getImageRegistry().getDescriptor( NeoIcons.ZOOM ) );
            tm.add( zoomAction );
            tm.add( new Separator() );
        }
        // layout actions
        {
            String groupName = "layout";
            GroupMarker layoutGroup = new GroupMarker( groupName );
            tm.add( layoutGroup );
            mm.add( layoutGroup );
            // spring layout
            ShowSpringLayoutAction springLayoutAction = new ShowSpringLayoutAction(
                this );
            springLayoutAction.setText( "Spring Layout" );
            springLayoutAction.setToolTipText( "Spring Layout" );
            springLayoutAction.setImageDescriptor( NeoIcons
                .getDescriptor( NeoIcons.SPRING ) );
            springLayoutAction.setChecked( true );
            tm.appendToGroup( groupName, springLayoutAction );
            mm.appendToGroup( groupName, springLayoutAction );
            // tree layout
            ShowTreeLayoutAction treeLayoutAction = new ShowTreeLayoutAction(
                this );
            treeLayoutAction.setText( "Tree Layout" );
            treeLayoutAction.setToolTipText( "Tree Layout" );
            treeLayoutAction.setImageDescriptor( NeoIcons
                .getDescriptor( NeoIcons.TREE ) );
            treeLayoutAction.setChecked( false );
            tm.appendToGroup( groupName, treeLayoutAction );
            mm.appendToGroup( groupName, treeLayoutAction );
            // radial layout
            ShowRadialLayoutAction radialLayoutAction = new ShowRadialLayoutAction(
                this );
            radialLayoutAction.setText( "Radial Layout" );
            radialLayoutAction.setToolTipText( "Radial Layout" );
            radialLayoutAction.setImageDescriptor( NeoIcons
                .getDescriptor( NeoIcons.RADIAL ) );
            radialLayoutAction.setChecked( false );
            tm.appendToGroup( groupName, radialLayoutAction );
            mm.appendToGroup( groupName, radialLayoutAction );
            // grid layout
            ShowGridLayoutAction gridLayoutAction = new ShowGridLayoutAction(
                this );
            gridLayoutAction.setText( "Grid Layout" );
            gridLayoutAction.setToolTipText( "Grid Layout" );
            gridLayoutAction.setImageDescriptor( NeoIcons
                .getDescriptor( NeoIcons.GRID ) );
            gridLayoutAction.setChecked( false );
            tm.appendToGroup( groupName, gridLayoutAction );
            mm.appendToGroup( groupName, gridLayoutAction );
        }
        // separator
        {
            mm.add( new Separator() );
        }
        // label settings actions
        {
            String labelsGroupName = "labels";
            GroupMarker labelsGroup = new GroupMarker( labelsGroupName );
            mm.add( labelsGroup );
            // relationship types actions
            ShowRelationshipTypesAction showRelationshipTypesAction = new ShowRelationshipTypesAction(
                this );
            showRelationshipTypesAction.setText( "Relationship types" );
            showRelationshipTypesAction
                .setChecked( ShowRelationshipTypesAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showRelationshipTypesAction );
            // relationship id's actions
            ShowRelationshipIdsAction showRelationshipIdsAction = new ShowRelationshipIdsAction(
                this );
            showRelationshipIdsAction.setText( "Relationship id" );
            showRelationshipIdsAction
                .setChecked( ShowRelationshipIdsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showRelationshipIdsAction );
            // relationship types actions
            ShowRelationshipColorsAction showRelationshipColorsAction = new ShowRelationshipColorsAction(
                this );
            showRelationshipColorsAction.setText( "Relationship colors" );
            showRelationshipColorsAction
                .setChecked( ShowRelationshipColorsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showRelationshipColorsAction );
            // relationship directions actions
            ShowRelationshipDirectionsAction showRelationshipDirectionAction = new ShowRelationshipDirectionsAction(
                this );
            showRelationshipDirectionAction.setText( "Relationship directions" );
            showRelationshipDirectionAction
                .setChecked( ShowRelationshipDirectionsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showRelationshipDirectionAction );
            // separator
            {
                mm.add( new Separator() );
            }
            // names actions
            ShowNodeNamesAction showNodeNamesAction = new ShowNodeNamesAction(
                this );
            showNodeNamesAction.setText( "Node names" );
            showNodeNamesAction.setChecked( ShowNodeNamesAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showNodeNamesAction );
            // relationship id's actions
            ShowNodeIdsAction showNodeIdsAction = new ShowNodeIdsAction( this );
            showNodeIdsAction.setText( "Node id" );
            showNodeIdsAction.setChecked( ShowNodeIdsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showNodeIdsAction );
            // node colors actions
            ShowNodeColorsAction showNodeColorsAction = new ShowNodeColorsAction(
                this );
            showNodeColorsAction.setText( "Node colors" );
            showNodeColorsAction
                .setChecked( ShowNodeColorsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showNodeColorsAction );
            // node icons actions
            ShowNodeIconsAction showNodeIconsAction = new ShowNodeIconsAction(
                this );
            showNodeIconsAction.setText( "Node icons" );
            showNodeIconsAction.setChecked( ShowNodeIconsAction.DEFAULT_STATE );
            mm.appendToGroup( labelsGroupName, showNodeIconsAction );
        }
        // printing
        getViewSite().getActionBars().setGlobalActionHandler(
            ActionFactory.PRINT.getId(), new PrintGraphAction( this ) );
    }

    /**
     * Updates the content of the status bar.
     */
    protected void refreshStatusBar()
    {
        getViewSite().getActionBars().getStatusLineManager().setMessage(
            "Traversal Depth: " + String.valueOf( traversalDepth ) );
    }

    /**
     * Returns the viewer that contains the graph.
     */
    public GraphViewer getViewer()
    {
        return viewer;
    }

    /**
     * Returns the graph viewer for zooming.
     */
    public AbstractZoomableViewer getZoomableViewer()
    {
        return viewer;
    }

    /**
     * This is how the framework determines which interfaces we implement.
     */
    @SuppressWarnings( "unchecked" )
    public Object getAdapter( Class key )
    {
        if ( key.equals( IPropertySheetPage.class ) )
        {
            return getPropertySheetPage();
        }
        else
        {
            return super.getAdapter( key );
        }
    }

    /**
     * This accesses a cached version of the property sheet.
     */
    public IPropertySheetPage getPropertySheetPage()
    {
        if ( propertySheetPage == null )
        {
            propertySheetPage = new PropertySheetPage();
            propertySheetPage
                .setPropertySourceProvider( new NeoGraphPropertySourceProvider() );
        }
        return propertySheetPage;
    }

    /**
     * Cleans up.
     */
    public void dispose()
    {
        if ( propertySheetPage != null )
        {
            propertySheetPage.dispose();
        }
        super.dispose();
    }

    /**
     * Sets the focus.
     */
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    /**
     * Focuses the view on the reference node.
     */
    public void showReferenceNode()
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction txn = Transaction.begin();
            try
            {
                Node node = ns.getReferenceNode();
                viewer.setInput( node );
            }
            finally
            {
                txn.finish();
            }
        }
    }

    /**
     * Focuses the view on the node with the given id.
     */
    public void showNode( long nodeId )
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction txn = Transaction.begin();
            try
            {
                Node node = ns.getNodeById( nodeId );
                viewer.setInput( node );
            }
            finally
            {
                txn.finish();
            }
        }
    }

    /**
     * Focuses the view on the given node.
     */
    public void showNode( Node node )
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction txn = Transaction.begin();
            try
            {
                viewer.setInput( node );
            }
            finally
            {
                txn.finish();
            }
        }
    }

    /**
     * Returns the current traversal depth.
     */
    public int getTraversalDepth()
    {
        return traversalDepth;
    }

    /**
     * Increments the traversal depth.
     */
    public void incTraversalDepth()
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction txn = Transaction.begin();
            try
            {
                traversalDepth++;
                refreshStatusBar();
                viewer.refresh();
                viewer.applyLayout();
            }
            finally
            {
                txn.finish();
            }
        }
        if ( traversalDepth > 0 )
        {
            decAction.setEnabled( true );
        }
    }

    /**
     * Decrements the traversal depth.
     */
    public void decTraversalDepth()
    {
        if ( traversalDepth > 0 )
        {
            NeoServiceManager sm = Activator.getDefault()
                .getNeoServiceManager();
            NeoService ns = sm.getNeoService();
            if ( ns != null )
            {
                Transaction txn = Transaction.begin();
                try
                {
                    traversalDepth--;
                    refreshStatusBar();
                    viewer.refresh();
                    viewer.applyLayout();
                }
                finally
                {
                    txn.finish();
                }
            }
            if ( traversalDepth == 0 )
            {
                decAction.setEnabled( false );
            }
        }
    }

    /**
     * Refreshes the view.
     */
    public void refresh()
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction txn = Transaction.begin();
            try
            {
                viewer.refresh();
                viewer.applyLayout();
            }
            finally
            {
                txn.finish();
            }
        }
    }

    /**
     * Refreshes the view without changing the layout.
     */
    public void refreshPreserveLayout()
    {
        NeoServiceManager sm = Activator.getDefault().getNeoServiceManager();
        NeoService ns = sm.getNeoService();
        if ( ns != null )
        {
            Transaction tn = Transaction.begin();
            try
            {
                viewer.refresh();
            }
            finally
            {
                tn.finish();
            }
        }
    }

    /**
     * Show relationship types in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showRelationshipTypes( boolean checked )
    {
        labelProvider.setShowRelationshipTypes( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show relationship id in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showRelationshipIds( boolean checked )
    {
        labelProvider.setShowRelationshipIds( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show relationship colors in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showRelationshipColors( boolean checked )
    {
        labelProvider.setShowRelationshipColors( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show arrows in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showArrows( boolean checked )
    {
        labelProvider.setShowArrows( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show node names in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showNames( boolean checked )
    {
        labelProvider.setShowNames( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show node id in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showNodeIds( boolean checked )
    {
        labelProvider.setShowNodeIds( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show node icons in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showNodeIcons( boolean checked )
    {
        labelProvider.setShowNodeIcons( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Show node colors in the graph, or hide them.
     * @param checked
     *            true to show, false to hide
     */
    public void showNodeColors( boolean checked )
    {
        labelProvider.setShowNodeColors( checked );
        this.refreshPreserveLayout();
    }

    /**
     * Updates the view according to service changes.
     */
    class NeoGraphServiceEventListener implements NeoServiceEventListener
    {
        /**
         * Refreshes the input source of the view.
         */
        public void serviceChanged( NeoServiceEvent event )
        {
            if ( event.getStatus() == NeoServiceStatus.STOPPED )
            {
                // when called during shutdown the content provider may already
                // have been disposed
                if ( getViewer().getContentProvider() != null )
                {
                    getViewer().setInput( null );
                }
            }
            else if ( event.getStatus() == NeoServiceStatus.STARTED )
            {
                showReferenceNode();
            }
        }
    }
    /**
     * Handles double clicks on graph figures.
     */
    static class NeoGraphDoubleClickListener implements IDoubleClickListener
    {
        /**
         * Sets the selected node as input for the viewer.
         */
        public void doubleClick( DoubleClickEvent event )
        {
            StructuredSelection sel = (StructuredSelection) event
                .getSelection();
            Object s = sel.getFirstElement();
            if ( (s != null) && (s instanceof Node) )
            {
                Transaction txn = Transaction.begin();
                try
                {
                    Viewer viewer = event.getViewer();
                    viewer.setInput( s );
                }
                finally
                {
                    txn.finish();
                }
            }
        }
    }
}
