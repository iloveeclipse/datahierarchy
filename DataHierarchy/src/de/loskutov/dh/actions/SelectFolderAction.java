/*******************************************************************************
 * Copyright (c) 2009 - 2015 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.SelectionHelper;
import de.loskutov.dh.views.DataHierarchyView;

public class SelectFolderAction implements IObjectActionDelegate {

    private final List<IJavaElement> folders;

    public SelectFolderAction() {
        folders = new ArrayList<IJavaElement>();
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // noop
    }

    @Override
    public void run(IAction action) {
        try {
            DataHierarchyView view = (DataHierarchyView) PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage().showView(DataHierarchyView.ID);
            view.setFolders(folders);
        } catch (PartInitException e) {
            DataHierarchyPlugin.logError("Can't open Data Hierarchy view", e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        folders.clear();
        List<IJavaElement> openables = SelectionHelper.getFromSelection(selection, IJavaElement.class);
        for (IJavaElement resource : openables) {
            if (!folders.contains(resource)) {
                folders.add(resource);
            }
        }
        if(!folders.isEmpty() && "DataHierarchy.focusOn".equals(action.getId())){
            action.setText("Focus on '" + folders.get(0).getElementName() + "'");
        }
    }

}
