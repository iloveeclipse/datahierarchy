/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import de.loskutov.dh.DataHierarchyPlugin;

public class CancelSearchAction implements IViewActionDelegate {

    private IAction action;

    public void init(IViewPart view) {
        IJobChangeListener listener = new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event) {
                if (action == null || action.isEnabled()) {
                    return;
                }
                action.setEnabled(event.getJob().belongsTo(DataHierarchyPlugin.JOB_FAMILY));
            }

            @Override
            public void done(IJobChangeEvent event) {
                if (action == null || !action.isEnabled()) {
                    return;
                }
                action.setEnabled(!event.getJob().belongsTo(DataHierarchyPlugin.JOB_FAMILY));
            }
        };
        Job.getJobManager().addJobChangeListener(listener);
    }

    public void run(IAction newAction) {
        Job.getJobManager().cancel(DataHierarchyPlugin.JOB_FAMILY);
        newAction.setEnabled(false);
    }

    public void selectionChanged(IAction newAction, ISelection selection) {
        if (this.action == null) {
            newAction.setEnabled(Job.getJobManager().find(DataHierarchyPlugin.JOB_FAMILY).length > 0);
        }
        this.action = newAction;
    }

}
