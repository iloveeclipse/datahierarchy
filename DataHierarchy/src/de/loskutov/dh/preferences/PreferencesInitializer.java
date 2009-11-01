/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.SearchScope;

public class PreferencesInitializer extends AbstractPreferenceInitializer {

    private static String [] DEFAULT_FILTER = {
        "java.lang.String", "java.lang.StringBuilder",
        "java.lang.StringBuffer", "java.lang.Class", "java.lang.Boolean", "java.lang.Byte",
        "java.lang.Character", "java.lang.Double", "java.lang.Float", "java.lang.Integer",
        "java.lang.Long", "java.lang.Short", "java.lang.Void", "java.util.regex.Pattern",
        "java.text.NumberFormat", "java.io.File", "java.lang.Number", "java.awt.Color",
        "java.awt.Font", "java.util.Locale"
    };

    public PreferencesInitializer() {
        super();
    }

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = DataHierarchyPlugin.getDefault().getPreferenceStore();
        store.setDefault(IPrefConstants.PREF_ACTIVE_FILTERS_LIST, serializeList(DEFAULT_FILTER));
        store.setDefault(IPrefConstants.PREF_SHOW_ARRAYS, false);
        store.setDefault(IPrefConstants.PREF_SHOW_PRIMITIVES_TOO, false);
        store.setDefault(IPrefConstants.PREF_SHOW_REFERENCES, true);
        store.setDefault(IPrefConstants.PREF_SEARCH_INHERITED_TYPES, true);
        store.setDefault(IPrefConstants.PREF_SHOW_STATICS_ONLY, false);
        store.setDefault(IPrefConstants.PREF_AUTO_REMOVE_EMPTY_ELEMENNTS, true);
        store.setDefault(IPrefConstants.PREF_SEARCH_SCOPE, SearchScope.DEFAULT.name());
    }

    /**
     * Parses the comma separated string into an array of strings
     *
     * @return list
     */
    public static List<String> parseList(String listString) {
        List<String> list = new ArrayList<String>(10);
        if (listString == null || listString.length() == 0) {
            return list;
        }
        StringTokenizer tokenizer = new StringTokenizer(listString, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            list.add(token);
        }
        return list;
    }

    static String serializeList(String[] list) {
        if (list == null) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                buffer.append(',');
            }
            buffer.append(list[i]);
        }
        return buffer.toString();
    }

}
