/*
 * NeoPreferencePage.java
 */
package org.neo4j.neoclipse.preference;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

/**
 * The page for neo preferences.
 * @author Peter H&auml;nsgen
 * @author Anders Nawroth
 */
public class NeoPreferencePage extends AbstractPreferencePage
{
    // database location
    private static final String NEO_DATABASE_LOCATION_LABEL = "Neo database location:";
    private static final String NEO_DATABASE_LOCATION_ERROR = "The Neo database location is invalid.";
    // node label properties
    private static final String NODE_LABEL_PROPERTIES_LABEL = "Node label properties:";
    private static final String PROPTERTY_NAMES_NOTE = "comma-separated list of property names; will be evaluated from left to right, and the first non-empty value is used";
    // icon locations
    private static final String NODE_ICONS_LOCATION_LABEL = "Node icons location:";
    private static final String NODE_ICONS_LOCATION_ERROR = "The Node icons location is invalid.";
    private static final String ICON_LOCATION_NOTE = "the icon filenames should correspond to the settings for node icon filename properties";
    // node icon filename properties
    private static final String NODE_ICON_FILENAME_PROPERTIES_LABEL = "Node icon filename properties:";
    private static final String ICON_PROPERTY_NAMES_NOTE = "comma-separated list (see node labels), file extensions are added automatically to the property values found";

    /**
     * Initializes the several input fields.
     */
    protected void createFieldEditors()
    {
        // database location
        DirectoryFieldEditor locationField = new DirectoryFieldEditor(
            NeoPreferences.DATABASE_LOCATION, NEO_DATABASE_LOCATION_LABEL,
            getFieldEditorParent() );
        locationField.setEmptyStringAllowed( false );
        locationField.setErrorMessage( NEO_DATABASE_LOCATION_ERROR );
        addField( locationField );
        // node label properties
        StringFieldEditor propertyNameField = new StringFieldEditor(
            NeoPreferences.NODE_PROPERTY_NAMES, NODE_LABEL_PROPERTIES_LABEL,
            getFieldEditorParent() );
        propertyNameField.setEmptyStringAllowed( true );
        addField( propertyNameField, PROPTERTY_NAMES_NOTE );
        // icon locations
        DirectoryFieldEditor iconLocationField = new DirectoryFieldEditor(
            NeoPreferences.NODE_ICON_LOCATION, NODE_ICONS_LOCATION_LABEL,
            getFieldEditorParent() );
        iconLocationField.setEmptyStringAllowed( true );
        iconLocationField.setErrorMessage( NODE_ICONS_LOCATION_ERROR );
        addField( iconLocationField, ICON_LOCATION_NOTE );
        // node icon filename properties
        StringFieldEditor iconPropertyNameField = new StringFieldEditor(
            NeoPreferences.NODE_ICON_PROPERTY_NAMES,
            NODE_ICON_FILENAME_PROPERTIES_LABEL, getFieldEditorParent() );
        iconPropertyNameField.setEmptyStringAllowed( true );
        addField( iconPropertyNameField, ICON_PROPERTY_NAMES_NOTE );
    }

}
