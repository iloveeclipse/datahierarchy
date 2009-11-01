/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.preferences.IPrefConstants;
import de.loskutov.dh.search.SearchUtils;
import de.loskutov.dh.tree.TreeElement;
import de.loskutov.dh.views.DataHierarchyView;

public class AddToFilterAction implements IViewActionDelegate, IObjectActionDelegate {

    private DataHierarchyView part;
    private IStructuredSelection selection2;

    public AddToFilterAction() {
        super();
    }

    public void run(IAction action) {
        List<String> typesToFilter = getTypesToFilter(selection2);

        String preferencePageId = "DataHierarchy.globalPrefPage";
        String types = DataHierarchyPlugin.getDefault().getPreferenceStore().getString(
                IPrefConstants.PREF_ACTIVE_FILTERS_LIST);
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, preferencePageId,
                null, typesToFilter);
        int open = dialog.open();
        if (open != Window.OK) {
            return;
        }
        String newTypes = DataHierarchyPlugin.getDefault().getPreferenceStore().getString(
                IPrefConstants.PREF_ACTIVE_FILTERS_LIST);
        if (types.equals(newTypes)) {
            return;
        }
        part.updateFilter(newTypes);
//        SearchAgainAction search = new SearchAgainAction();
//        search.init(part);
//        search.run(action);
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            selection2 = null;
        } else {
            selection2 = (IStructuredSelection) selection;
        }
    }

    private List<String> getTypesToFilter(IStructuredSelection selection) {
        if (selection == null) {
            return null;
        }
        List<String> typesToFilter = new ArrayList<String>();
        for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
            Object elt = iterator.next();
            if(elt instanceof TreeElement<?,?>){
                elt = ((TreeElement)elt).getJavaElement();
            }
            if (elt instanceof IField) {
                IField field = (IField) elt;
                String resolvedType = SearchUtils.getResolvedType(field);
                if (resolvedType != null) {
                    typesToFilter.add(resolvedType);
                }
            } else if(elt instanceof IType){
                IType type = (IType) elt;
                typesToFilter.add(type.getFullyQualifiedName());
            }
        }
        return typesToFilter;
    }

    public void init(IViewPart view) {
        if (!(view instanceof DataHierarchyView)) {
            return;
        }
        this.part = (DataHierarchyView) view;
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        init((IViewPart) targetPart);
    }

}
