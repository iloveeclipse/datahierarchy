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

import de.loskutov.dh.views.DataHierarchyView;

public class ExpandCollapseAction implements IViewActionDelegate {

    private DataHierarchyView view;

    public void init(IViewPart viewPart) {
        this.view = (DataHierarchyView) viewPart;
    }

    public void run(IAction action) {
        if("DataHierarchy.expandAll".equals(action.getId())){
            view.getViewer().expandAll();
        } else {
            view.getViewer().collapseAll();
        }

    }

    public void selectionChanged(IAction action, ISelection selection) {
        //
    }

}
