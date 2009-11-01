/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import de.loskutov.dh.views.DataHierarchyView;

public class SelectReferencesModeAction implements IViewActionDelegate {

//    private DataHierarchyView view;

    public void init(IViewPart part) {
        if (part instanceof DataHierarchyView) {
//            this.view = (DataHierarchyView) part;
        }
    }

    public void run(IAction action) {
        // TODO Auto-generated method stub

    }

    public void selectionChanged(IAction action, ISelection selection) {
        // TODO Auto-generated method stub

    }

}
