/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.SelectionHelper;
import de.loskutov.dh.tree.TreeElement;
import de.loskutov.dh.views.DataHierarchyView;

public class SearchAgainAction implements IViewActionDelegate, IHandler, IObjectActionDelegate {

    private DataHierarchyView part;
    private List<TreeElement> elements;

    public void init(IViewPart view) {
        if(view instanceof DataHierarchyView){
            part = (DataHierarchyView) view;
        } else {
            part = null;
        }
    }

    public void run(IAction action) {
        if(part != null){
            Job.getJobManager().cancel(DataHierarchyPlugin.JOB_FAMILY);
            if(elements == null || elements.isEmpty()) {
                part.setFolders(part.getFolders());
            } else {
                for (TreeElement<?, ?> elt : elements) {
                    elt.setInitialized(false);
                }
                part.getViewer().refresh(true);
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        elements = SelectionHelper.getFromSelection(selection, TreeElement.class);
    }

    public void addHandlerListener(IHandlerListener handlerListener) {
        // noop

    }

    public void dispose() {
        // noop
    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart wpart = HandlerUtil.getActivePart(event);
        if (!(wpart instanceof DataHierarchyView)) {
            return null;
        }
        DataHierarchyView fieldsView = (DataHierarchyView) wpart;
        fieldsView.setFolders(fieldsView.getFolders());
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

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        init((IViewPart) targetPart);
    }

}
