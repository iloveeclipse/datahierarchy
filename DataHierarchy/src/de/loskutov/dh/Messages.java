/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

public class Messages  extends NLS {
    private static final String BUNDLE_NAME = "de.loskutov.dh.messages";//$NON-NLS-1$

    private Messages() {
        // Do not instantiate
    }

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    public static String get(String key){
        Field field;
        try {
            field = Messages.class.getField(key);
            return (String) field.get(null);
        } catch (Exception e) {
            DataHierarchyPlugin.logError("Missing resource for key: " + key, e);
            return key;
        }
    }

    public static String title;
    public static String error;
    public static String pref_Add_filter;
    public static String pref_Add_filterTip;
    public static String pref_RemoveFilter;
    public static String pref_RemoveFilterTip;
    public static String pref_Enable_all;
    public static String pref_Enable_allTip;
    public static String pref_Disable_all;
    public static String pref_Disable_allTip;
    public static String pref_Invalid_file_filter;
    public static String pref_Edit_filter;
    public static String pref_Edit_filterTip;

    public static String action_showStaticsOnly_text;
    public static String action_showStaticsOnly_toolTipText;
    public static String action_showStaticsOnly_image;

    public static String action_showPrimitivesToo_text;
    public static String action_showPrimitivesToo_toolTipText;
    public static String action_showPrimitivesToo_image;

    public static String action_showArrays_text;
    public static String action_showArrays_toolTipText;
    public static String action_showArrays_image;

    public static String action_autoRemoveEmptyElements_text;
    public static String action_autoRemoveEmptyElements_toolTipText;
    public static String action_autoRemoveEmptyElements_image;


}
