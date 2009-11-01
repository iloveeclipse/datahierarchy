/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class DataHierarchyPlugin extends AbstractUIPlugin {

    private static DataHierarchyPlugin plugin;

    private int viewCount;

    public static final Object JOB_FAMILY = DataHierarchyPlugin.class;

    public DataHierarchyPlugin() {
        super();
    }

    public static void logError(String message, Throwable e) {
        if (message == null) {
            message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
        }
        getDefault().getLog().log(new Status(IStatus.ERROR, getId(), IStatus.OK, message, e));
    }

    public static void showError(String message, Throwable error) {
        Shell shell = getShell();
        if (message == null) {
            message = Messages.error;
        }
        message = message + " " + error.getMessage();

        getDefault().getLog().log(new Status(IStatus.ERROR, getId(), IStatus.OK, message, error));

        MessageDialog.openError(shell, Messages.title, message);
    }

    public static Shell getShell() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static String getId() {
        return getDefault().getBundle().getSymbolicName();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        if(plugin == null) {
            plugin = this;
        }
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static DataHierarchyPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(getId(), path);
    }

    public static String nextViewId(){
        DataHierarchyPlugin hierarchyPlugin = getDefault();
        int next;
        synchronized (hierarchyPlugin) {
            hierarchyPlugin.viewCount ++;
            next = hierarchyPlugin.viewCount;
        }
        return String.valueOf(next);
    }
}
