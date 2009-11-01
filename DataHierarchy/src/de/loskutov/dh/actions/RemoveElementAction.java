/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.dh.views.DataHierarchyView;

/**
 * @author Andrei
 *
 */
public class RemoveElementAction implements IObjectActionDelegate, IHandler, IViewActionDelegate {

    private IWorkbenchPart part;

    public RemoveElementAction() {
        super();
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.part = targetPart;
    }

    public void run(IAction action) {
        if(!(part instanceof DataHierarchyView)){
            return;
        }
        DataHierarchyView view = (DataHierarchyView) part;
        view.removeResults();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // noop
    }

    public void addHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    public void dispose() {
        // noop
    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        part = HandlerUtil.getActivePart(event);
        if (!(part instanceof DataHierarchyView)) {
            return null;
        }

        ((DataHierarchyView)part).removeResults();
        return null;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean isHandled() {
        return true;
    }

    public void removeHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    public void init(IViewPart view) {
        if (view instanceof DataHierarchyView) {
            part = view;
        } else {
            part = null;
        }

    }

}
