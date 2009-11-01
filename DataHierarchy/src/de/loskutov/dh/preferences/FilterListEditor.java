/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.Messages;

/**
 * @author Andrei
 */
public class FilterListEditor extends FieldEditor {
    private static final String DEFAULT_NEW_FILTER_TEXT = "";
    private Button addButton;

    private Button removeButton;

    private Button enableAllButton;

    private Button disableAllButton;

    private Table filterTable;

    private CheckboxTableViewer filterViewer;

    private TableEditor tableEditor;

    private FilterContentProvider fileFilterContentProvider;

    private Text editorText;

    private Filter newFilter;

    private TableItem newTableItem;

    private String invalidEditorText;

    protected FilterListEditor(String name, String labelText, Composite parent) {
        //super(name, labelText, parent);
        setPreferenceStore(DataHierarchyPlugin.getDefault().getPreferenceStore());
        init(name, labelText);
        createControl(parent);
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) filterTable.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        createTableControl(parent, numColumns);

        createButtonBoxControl(parent);
    }

    private void createButtonBoxControl(Composite container) {
        // button container
        Composite buttonContainer = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        buttonContainer.setLayoutData(gd);
        GridLayout buttonLayout = new GridLayout();
        buttonLayout.numColumns = 1;
        buttonLayout.marginHeight = 0;
        buttonLayout.marginWidth = 0;
        buttonContainer.setLayout(buttonLayout);

        // Add filter button
        addButton = new Button(buttonContainer, SWT.PUSH);
        addButton.setText(Messages.pref_Add_filter);
        addButton.setToolTipText(Messages.pref_Add_filterTip);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        addButton.setLayoutData(gd);
        addButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                editFilter();
            }
        });

        // Remove button
        removeButton = new Button(buttonContainer, SWT.PUSH);
        removeButton.setText(Messages.pref_RemoveFilter);
        removeButton.setToolTipText(Messages.pref_RemoveFilterTip);
        gd = getButtonGridData(removeButton);
        removeButton.setLayoutData(gd);
        removeButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                removeFilters();
            }
        });
        removeButton.setEnabled(false);

        enableAllButton = new Button(buttonContainer, SWT.PUSH);
        enableAllButton.setText(Messages.pref_Enable_all);
        enableAllButton.setToolTipText(Messages.pref_Enable_allTip);
        gd = getButtonGridData(enableAllButton);
        enableAllButton.setLayoutData(gd);
        enableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(true);
            }
        });

        disableAllButton = new Button(buttonContainer, SWT.PUSH);
        disableAllButton.setText(Messages.pref_Disable_all);
        disableAllButton.setToolTipText(Messages.pref_Disable_allTip);
        gd = getButtonGridData(disableAllButton);
        disableAllButton.setLayoutData(gd);
        disableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(false);
            }
        });

    }

    private void removeFilters() {
        IStructuredSelection selection = (IStructuredSelection) filterViewer.getSelection();
        fileFilterContentProvider.removeFilters(selection.toArray());
    }

    /**
     * Create a new filter in the table (with the default 'new filter' value),
     * then open up an in-place editor on it.
     */
    private void editFilter() {
        // if a previous edit is still in progress, finish it
        if (editorText != null) {
            validateChangeAndCleanup();
        }

        newFilter = fileFilterContentProvider.addFilter(DEFAULT_NEW_FILTER_TEXT, true);
        newTableItem = filterTable.getItem(0);

        // create & configure Text widget for editor
        // Fix for bug 1766. Border behavior on for text fields varies per
        // platform.
        // On Motif, you always get a border, on other platforms,
        // you don't. Specifying a border on Motif results in the characters
        // getting pushed down so that only there very tops are visible. Thus,
        // we have to specify different style constants for the different
        // platforms.
        int textStyles = SWT.SINGLE | SWT.LEFT;
        if (!SWT.getPlatform().equals("motif")) {
            textStyles |= SWT.BORDER;
        }
        editorText = new Text(filterTable, textStyles);
        GridData gd = new GridData(GridData.FILL_BOTH);
        editorText.setLayoutData(gd);

        // set the editor
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.grabHorizontal = true;
        tableEditor.setEditor(editorText, newTableItem, 0);

        // get the editor ready to use
        editorText.setText(newFilter.getName());
        editorText.selectAll();
        setEditorListeners(editorText);
        editorText.setFocus();
    }


    private void setEditorListeners(Text text) {
        // CR means commit the changes, ESC means abort and don't commit
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.CR) {
                    if (invalidEditorText != null) {
                        String infoText = Messages.pref_Invalid_file_filter;
                        if (!invalidEditorText.equals(editorText.getText())
                                && !infoText.equals(editorText.getText())) {
                            validateChangeAndCleanup();
                        } else {
                            editorText.setText(infoText);
                        }
                        invalidEditorText = null;
                    } else {
                        validateChangeAndCleanup();
                    }
                } else if (event.character == SWT.ESC) {
                    removeNewFilter();
                    cleanupEditor();
                }
            }
        });
        // Consider loss of focus on the editor to mean the same as CR
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                if (invalidEditorText != null) {
                    String infoText = Messages.pref_Invalid_file_filter;
                    if (!invalidEditorText.equals(editorText.getText())
                            && !infoText.equals(editorText.getText())) {
                        validateChangeAndCleanup();
                    } else {
                        editorText.setText(infoText);
                    }
                    invalidEditorText = null;
                } else {
                    validateChangeAndCleanup();
                }
            }
        });
        // Consume traversal events from the text widget so that CR doesn't
        // traverse away to dialog's default button. Without this, hitting
        // CR in the text field closes the entire dialog.
        text.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
    }

    private void validateChangeAndCleanup() {
        String trimmedValue = editorText.getText().trim();
        // if the new value is blank, remove the filter
        if (trimmedValue.length() < 1) {
            removeNewFilter();
        }
        // if it's invalid, beep and leave sitting in the editor
        else if (!validateEditorInput(trimmedValue)) {
            invalidEditorText = trimmedValue;
            editorText.setText(Messages.pref_Invalid_file_filter);
            getShell().getDisplay().beep();
            return;
            // otherwise, commit the new value if not a duplicate
        } else {

            Object[] filters = fileFilterContentProvider.getElements(null);
            for (int i = 0; i < filters.length; i++) {
                Filter filter = (Filter) filters[i];
                if (filter.getName().equals(trimmedValue)) {
                    removeNewFilter();
                    cleanupEditor();
                    return;
                }
            }
            newTableItem.setText(trimmedValue);
            newFilter.setName(trimmedValue);
            filterViewer.refresh();
        }
        cleanupEditor();
    }

    /**
     * Cleanup all widgetry & resources used by the in-place editing
     */
    private void cleanupEditor() {
        if (editorText != null) {
            newFilter = null;
            newTableItem = null;
            tableEditor.setEditor(null, null, 0);
            editorText.dispose();
            editorText = null;
        }
    }

    private static int count(String s, char c) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private void removeNewFilter() {
        fileFilterContentProvider.removeFilters(new Object[] { newFilter });
    }

    /**
     * A valid filter is either *.[\w] or [\w].* or [\w]
     */
    private static boolean validateEditorInput(String trimmedValue) {
        char firstChar = trimmedValue.charAt(0);
        if (firstChar == '*' && trimmedValue.length() == 1) {
            return false;
        }
        if (firstChar == '*') {
            // '*' should be followed by '.'
            if (trimmedValue.charAt(1) != '.') {
                return false;
            }
        }
        char lastChar = trimmedValue.charAt(trimmedValue.length() - 1);
        if (lastChar == '*') {
            // '*' should be preceeded by '.' and it should exist only once
            if (trimmedValue.charAt(trimmedValue.length() - 2) != '.') {
                return false;
            }
        }
        if (count(trimmedValue, '*') > 1) {
            return false;
        }
        return true;
    }

    private GridData getButtonGridData(Button button) {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        gd.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);

        // gd.heightHint = Dialog.convertVerticalDLUsToPixels(fontMetrics,
        // IDialogConstants.BUTTON_HEIGHT);
        return gd;
    }

    private void checkAllFilters(boolean check) {

        Object[] filters = fileFilterContentProvider.getElements(null);
        for (int i = 0; i != filters.length; i++) {
            ((Filter) filters[i]).setChecked(check);
        }

        filterViewer.setAllChecked(check);
    }

    private void createTableControl(Composite container, int numColumns) {
        filterTable = new Table(container, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI
                | SWT.FULL_SELECTION | SWT.BORDER);
        filterTable.setHeaderVisible(false);
        // fFilterTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        filterTable.setLayoutData(gd);

        TableLayout tlayout = new TableLayout();
        tlayout.addColumnData(new ColumnWeightData(100, true));
        filterTable.setLayout(tlayout);

        TableColumn tableCol = new TableColumn(filterTable, SWT.LEFT);
        tableCol.setResizable(true);

        filterViewer = new CheckboxTableViewer(filterTable);
        tableEditor = new TableEditor(filterTable);
        filterViewer.setLabelProvider(new FilterLabelProvider());
        filterViewer.setComparator(new FilterViewerSorter());
        fileFilterContentProvider = new FilterContentProvider(filterViewer);
        filterViewer.setContentProvider(fileFilterContentProvider);
        // @todo table width input just needs to be non-null
        filterViewer.setInput(this);

        filterViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                Filter filter = (Filter) event.getElement();
                fileFilterContentProvider.toggleFilter(filter);
            }
        });
        filterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection.isEmpty()) {
                    removeButton.setEnabled(false);
                } else {
                    removeButton.setEnabled(true);
                }
            }
        });
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    public int getNumberOfControls() {
        return 2;
    }


    private Shell getShell() {
        if (addButton == null) {
            return null;
        }
        return addButton.getShell();
    }

    /*
     * (non-Javadoc) Method declared on FieldEditor.
     */
    @Override
    public void setFocus() {
        if (filterTable != null) {
            filterTable.setFocus();
        }
    }

    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        filterTable.setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        enableAllButton.setEnabled(enabled);
        disableAllButton.setEnabled(enabled);
    }

    final static class Filter {

        private String fName;

        private boolean fChecked;

        public Filter(String name, boolean checked) {
            setName(name);
            setChecked(checked);
        }

        public String getName() {
            return fName;
        }

        public void setName(String name) {
            fName = name;
        }

        public boolean isChecked() {
            return fChecked;
        }

        public void setChecked(boolean checked) {
            fChecked = checked;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Filter) {
                Filter other = (Filter) o;
                if (getName().equals(other.getName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }

    /**
     * Label provider for Filter model objects
     */
    static class FilterLabelProvider extends LabelProvider implements ITableLabelProvider {

        private final Image imgPkg = PlatformUI.getWorkbench().getSharedImages().getImage(
                ISharedImages.IMG_OBJ_FILE);

        /**
         * @see ITableLabelProvider#getColumnText(Object, int)
         */
        public String getColumnText(Object object, int column) {
            if (column == 0) {
                return ((Filter) object).getName();
            }
            return "";
        }

        /**
         * @see ILabelProvider#getText(Object)
         */
        @Override
        public String getText(Object element) {
            return ((Filter) element).getName();
        }

        /**
         * @see ITableLabelProvider#getColumnImage(Object, int)
         */
        public Image getColumnImage(Object object, int column) {
            return imgPkg;
        }
    }

    static class FilterViewerSorter extends WorkbenchViewerComparator {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            ILabelProvider lprov = (ILabelProvider) ((ContentViewer) viewer).getLabelProvider();
            String name1 = lprov.getText(e1);
            String name2 = lprov.getText(e2);
            if (name1 == null) {
                name1 = "";
            }
            if (name2 == null) {
                name2 = "";
            }
            if (name1.length() > 0 && name2.length() > 0) {
                char char1 = name1.charAt(name1.length() - 1);
                char char2 = name2.charAt(name2.length() - 1);
                if (char1 == '*' && char1 != char2) {
                    return -1;
                }
                if (char2 == '*' && char2 != char1) {
                    return 1;
                }
            }
            return name1.compareTo(name2);
        }
    }

    private void updateActions() {
        if (enableAllButton != null) {
            boolean enabled = filterViewer.getTable().getItemCount() > 0;
            enableAllButton.setEnabled(enabled);
            disableAllButton.setEnabled(enabled);
        }
    }

    /**
     * Content provider for the table. Content consists of instances of
     * StepFilter.
     */
    protected class FilterContentProvider implements IStructuredContentProvider {

        private final CheckboxTableViewer fViewer;

        private final List<Filter> fFilters;

        public FilterContentProvider(CheckboxTableViewer viewer) {
            fViewer = viewer;
            List<String> active = createActiveStepFiltersList();
            List<String> inactive = createInactiveStepFiltersList();
            fFilters = new ArrayList<Filter>(active.size() + inactive.size());
            populateList(inactive, false);
            populateList(active, true);
            updateActions();
        }

        public void setDefaults() {
            fViewer.remove(fFilters.toArray());
            List<String> defaultlist = createDefaultStepFiltersList();
            fFilters.clear();
            populateList(defaultlist, true);
        }

        protected final void populateList(List<String> list, boolean checked) {
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                addFilter(name, checked);
            }
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected final List<String> createActiveStepFiltersList() {
            return PreferencesInitializer.parseList(getPreferenceStore().getString(
                    IPrefConstants.PREF_ACTIVE_FILTERS_LIST));
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected List<String> createDefaultStepFiltersList() {
            return PreferencesInitializer.parseList(getPreferenceStore().getDefaultString(
                    IPrefConstants.PREF_ACTIVE_FILTERS_LIST));
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected final List<String> createInactiveStepFiltersList() {
            return PreferencesInitializer.parseList(getPreferenceStore().getString(
                    IPrefConstants.PREF_INACTIVE_FILTERS_LIST));
        }

        public Filter addFilter(String name, boolean checked) {
            Filter filter = new Filter(name, checked);
            if (!fFilters.contains(filter)) {
                fFilters.add(filter);
                fViewer.add(filter);
                fViewer.setChecked(filter, checked);
            }
            updateActions();
            return filter;
        }

        public void saveFilters() {

            int filtersSize = fFilters.size();
            List<String> active = new ArrayList<String>(filtersSize);
            List<String> inactive = new ArrayList<String>(filtersSize);
            Iterator<Filter> iterator = fFilters.iterator();
            while (iterator.hasNext()) {
                Filter filter = iterator.next();
                String name = filter.getName();
                if (filter.isChecked()) {
                    active.add(name);
                } else {
                    inactive.add(name);
                }
            }
            String pref = PreferencesInitializer.serializeList(active.toArray(new String[active.size()]));
            IPreferenceStore prefStore = getPreferenceStore();
            prefStore.setValue(IPrefConstants.PREF_ACTIVE_FILTERS_LIST, pref);
            pref = PreferencesInitializer.serializeList(inactive.toArray(new String[inactive.size()]));
            prefStore.setValue(IPrefConstants.PREF_INACTIVE_FILTERS_LIST, pref);
        }

        public void removeFilters(Object[] filters) {
            for (int i = 0; i < filters.length; i++) {
                Filter filter = (Filter) filters[i];
                fFilters.remove(filter);
            }
            fViewer.remove(filters);
            updateActions();
        }

        public void toggleFilter(Filter filter) {
            boolean newState = !filter.isChecked();
            filter.setChecked(newState);
            fViewer.setChecked(filter, newState);
        }

        /**
         * @see IStructuredContentProvider#getElements(Object)
         */
        public Object[] getElements(Object inputElement) {
            return fFilters.toArray();
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            /** ignored */
        }

        public void dispose() {
            /** ignored */
        }
    }

    @Override
    protected void doLoad() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void doLoadDefault() {
        fileFilterContentProvider.setDefaults();
    }

    @Override
    protected void doStore() {
        fileFilterContentProvider.saveFilters();
    }

    public void addFilter(List<String> data) {
        List<Filter> filters = new ArrayList<Filter>();
        for (String name : data) {
            Filter filter = this.fileFilterContentProvider.addFilter(name, true);
            filters.add(filter);
        }
        this.filterViewer.setSelection(new StructuredSelection(filters));
    }
}
