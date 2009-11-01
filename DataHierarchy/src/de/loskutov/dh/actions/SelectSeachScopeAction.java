/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.SearchScope;
import de.loskutov.dh.preferences.IPrefConstants;
import de.loskutov.dh.views.DataHierarchyView;

public class SelectSeachScopeAction implements IViewActionDelegate {

    private DataHierarchyView view;

    public void init(IViewPart part) {
        if (part instanceof DataHierarchyView) {
            this.view = (DataHierarchyView) part;
        }
    }

    public void run(IAction action) {
        if (view == null) {
            return;
        }
        SearchScope searchScope = SearchScope.getSearchScope(action.getId());
        view.setSearchScope(searchScope);
        DataHierarchyPlugin.getDefault().getPreferenceStore().setValue(
                IPrefConstants.PREF_SEARCH_SCOPE, searchScope.name());
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // noop
    }

}
