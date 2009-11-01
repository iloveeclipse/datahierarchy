/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.Messages;


/**
 * Default action which could be used as template for "toggle" action.
 * Action image, text and tooltip will be initialized by default.
 * To use it, register IPropertyChangeListener and check for IAction.CHECKED
 * event name.
 * @author Andrei
 */
public abstract class DefaultToggleAction extends Action {

    private static final String ACTION = "action";

    public DefaultToggleAction(String id) {
        super();
        setId(id);
        init();

        IPreferenceStore store = DataHierarchyPlugin.getDefault().getPreferenceStore();

        boolean isChecked = store.getBoolean(id);
        setChecked(isChecked);
    }

    private void init(){
        String myId = getId();
        String imageFilePath = Messages.get(ACTION + "_" + myId + "_" + IMAGE);
        if(imageFilePath != null && imageFilePath.length() != 0) {
            setImageDescriptor(AbstractUIPlugin
                .imageDescriptorFromPlugin(
                        DataHierarchyPlugin.getDefault().getBundle()
                        .getSymbolicName(),
                        imageFilePath));
        }

        setText(Messages.get(ACTION + "_" + myId + "_" + TEXT));
        setToolTipText(Messages.get(ACTION + "_" + myId + "_" + TOOL_TIP_TEXT));
    }

    /**
     * @see org.eclipse.jface.action.IAction#run()
     */
    @Override
    public final void run() {
        boolean isChecked = isChecked();
        IPreferenceStore store = DataHierarchyPlugin.getDefault().getPreferenceStore();
        store.setValue(getId(), isChecked);
        run(isChecked);
    }

    public abstract void run(boolean newState);
}
