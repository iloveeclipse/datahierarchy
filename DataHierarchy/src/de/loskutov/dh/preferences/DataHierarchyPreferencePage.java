/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.preferences;

import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.loskutov.dh.DataHierarchyPlugin;

public class DataHierarchyPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private FilterListEditor editor;

    public DataHierarchyPreferencePage() {
        super(GRID);
    }


    @Override
    protected Control createContents(Composite parent) {
        return super.createContents(parent);
    }

    public void init(IWorkbench workbench) {
        // TODO Auto-generated method stub

    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return DataHierarchyPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public boolean performOk() {
//        editor.store();
        return super.performOk();
    }

    @Override
    protected void createFieldEditors() {
        editor = new FilterListEditor("", "Filters for searching", getFieldEditorParent());
        editor.setPage(this);
        addField(editor);
    }

    @Override
    public void applyData(/*List<String>*/Object data) {
        if(data instanceof List){
            editor.addFilter((List<String>)data);
        }
    }
}
