/*******************************************************************************
 * Copyright (c) 2009 - 2015 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.preferences;

import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.loskutov.dh.DataHierarchyPlugin;

public class DataHierarchyPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private FilterListEditor editor;

    public DataHierarchyPreferencePage() {
        super(GRID);
    }

    protected static Composite createContainer(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL);
        gridData.grabExcessHorizontalSpace = true;
        composite.setLayoutData(gridData);
        return composite;
    }

    @Override
    protected Control createContents(Composite parent) {
        TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
        tabFolder.setLayout(new GridLayout(1, true));
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem tabFilter = new TabItem(tabFolder, SWT.NONE);
        tabFilter.setText("Filters");

        Group defPanel = new Group(tabFolder, SWT.NONE);
        defPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        defPanel.setLayout(new GridLayout(1, false));
        defPanel.setText("Search Filters");

        tabFilter.setControl(defPanel);

        TabItem support = new TabItem(tabFolder, SWT.NONE);
        support.setText("Misc...");
        Composite supportPanel = createContainer(tabFolder);
        support.setControl(supportPanel);
        SupportPanel.createSupportLinks(supportPanel);

        return super.createContents(defPanel);
    }

    @Override
    public void init(IWorkbench workbench) {
        // noop
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return DataHierarchyPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public boolean performOk() {
        return super.performOk();
    }

    @Override
    protected void createFieldEditors() {
        editor = new FilterListEditor("", "Manage Java types which should be ignored in the "
                + "data hierarchy", getFieldEditorParent());
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
