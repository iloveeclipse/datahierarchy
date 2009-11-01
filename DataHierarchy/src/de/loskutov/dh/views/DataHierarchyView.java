/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.SearchScope;
import de.loskutov.dh.SelectionHelper;
import de.loskutov.dh.preferences.IPrefConstants;
import de.loskutov.dh.preferences.PreferencesInitializer;
import de.loskutov.dh.tree.TreeElement;

public class DataHierarchyView extends ViewPart implements IShowInSource/*, IShowInTarget*/ {

    public static final String ID = DataHierarchyView.class.getName();

    private TreeViewer viewer;
    // private Action searchAgainAction;
    private List<IJavaElement> folders;

    private ViewContentProvider contentProvider;

    private Action searchPrimitivesTooAction;
    private Action doubleClickAction;
    private Action searchArraysAction;
    private Action autoRemoveEmptyAction;
    private Action searchStaticsAction;

    private ViewLabelProvider labelProvider;

    private SelectionListener selectionProvider;

    @SuppressWarnings("restriction")
    private CompositeActionGroup actionGroups;

    private boolean isPinned;

    private SearchScope searchScope;

    private TypeFilter filter;

    /**
     * The constructor.
     */
    public DataHierarchyView() {
        super();
        setSearchScope(getSavedScope());
    }

    static SearchScope getSavedScope() {
        String name = DataHierarchyPlugin.getDefault().getPreferenceStore().getString(
                IPrefConstants.PREF_SEARCH_SCOPE);
        try {
            return SearchScope.valueOf(name);
        } catch (Exception e) {
            return SearchScope.DEFAULT;
        }
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent) {
        // | SWT.VIRTUAL
        setViewer(new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL));
        updateFilter(DataHierarchyPlugin.getDefault().getPreferenceStore().getString(
                IPrefConstants.PREF_ACTIVE_FILTERS_LIST));
        setLabelProvider(new ViewLabelProvider());
        contentProvider = new ViewContentProvider(this);
        getViewer().setContentProvider(contentProvider);
        selectionProvider = new SelectionListener(getViewer());
        getSite().setSelectionProvider(selectionProvider);

        getViewer().setLabelProvider(getLabelProvider());
        getViewer().setSorter(new ViewerSorter(){

            @Override
            public int category(Object element) {
                if(element instanceof TreeElement<?, ?>){
                    TreeElement<?, ?> treeElement = (TreeElement<?, ?>) element;
                    if(treeElement.isVirtual()){
                        return 42;
                    }
                    Object data = treeElement.getData();
                    if(data instanceof IJavaElement){
                        IJavaElement javaElement = (IJavaElement) data;
                        return javaElement.getElementType();
                    }
                }
                return 0;
            }
        });
        getViewer().setInput(DataHierarchyView.class);

        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();
    }

    @Override
    public void dispose() {
        getSite().setSelectionProvider(null);
        super.dispose();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                DataHierarchyView.this.fillContextMenu(manager);
            }
        });

        Menu menu = menuMgr.createContextMenu(getViewer().getControl());
        getViewer().getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, selectionProvider);
    }

    @SuppressWarnings("restriction")
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
        actionGroups.fillActionBars(bars);
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(searchStaticsAction);
        manager.add(searchPrimitivesTooAction);
        manager.add(searchArraysAction);
        manager.add(new Separator());
        manager.add(autoRemoveEmptyAction);
    }

    @SuppressWarnings("restriction")
    private void fillContextMenu(IMenuManager manager) {
        JavaPlugin.createStandardGroups(manager);
        IStructuredSelection selection = selectionProvider.getSelection();
        actionGroups.setContext(new ActionContext(selection));
        actionGroups.fillContextMenu(manager);
        actionGroups.setContext(null);
        // manager.add(new Separator());
        // drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        // manager.add(searchPrimitivesTooAction);
        // manager.add(searchArraysAction);
        manager.add(new Separator("edit"));
        manager.add(new Separator("expand"));
        manager.add(new Separator("run"));
        manager.add(new Separator("navigate"));
        // drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        actionGroups = JavaActionsGroup.createActionsGroup(this, selectionProvider);

        // TODO add filter for:
        // final references
        // custom types
        // final types?
        searchStaticsAction = new DefaultToggleAction(IPrefConstants.PREF_SHOW_STATICS_ONLY) {
            @Override
            public void run(boolean newValue) {
                contentProvider.searchStaticsOnly = newValue;
            }
        };
        contentProvider.searchStaticsOnly = searchStaticsAction.isChecked();

        searchPrimitivesTooAction = new DefaultToggleAction(IPrefConstants.PREF_SHOW_PRIMITIVES_TOO) {
            @Override
            public void run(boolean newValue) {
                contentProvider.searchPrimitivesToo = newValue;
            }
        };
        contentProvider.searchPrimitivesToo = searchPrimitivesTooAction.isChecked();

        searchArraysAction = new DefaultToggleAction(IPrefConstants.PREF_SHOW_ARRAYS)  {
            @Override
            public void run(boolean newValue) {
                contentProvider.searchArrays = newValue;
            }
        };
        contentProvider.searchArrays = searchArraysAction.isChecked();

        autoRemoveEmptyAction = new DefaultToggleAction(IPrefConstants.PREF_AUTO_REMOVE_EMPTY_ELEMENNTS)  {
            @Override
            public void run(boolean newValue) {
                contentProvider.autoRemoveEmptyElements = newValue;
            }
        };
        contentProvider.autoRemoveEmptyElements = autoRemoveEmptyAction.isChecked();


        doubleClickAction = new Action() {
            @Override
            public void run() {
                ISelection selection = getViewer().getSelection();
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                if (obj instanceof TreeElement<?, ?>) {

                    TreeElement<?, ?> treeElement = (TreeElement<?, ?>) obj;
                    if (treeElement.getData() instanceof IJavaElement) {
                        IJavaElement data = (IJavaElement) treeElement.getData();
                        try {
                            // XXX we have to open right line for search matches
                            JavaUI.openInEditor(data, true, true);
                        } catch (PartInitException e) {
                            DataHierarchyPlugin.logError("Can't open editor on: " + data, e);
                        } catch (JavaModelException e) {
                            DataHierarchyPlugin.logError("Can't open editor on: " + data, e);
                        }
                    }
                }
            }
        };
    }



    private void hookDoubleClickAction() {
        getViewer().addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    public ShowInContext getShowInContext() {
        return new ShowInContext(null, selectionProvider.getSelection());
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        getViewer().getControl().setFocus();
    }

    public void setFolders(List<?> folders) {
        this.folders = new ArrayList<IJavaElement>();

        final TreeElement<String, IJavaElement> root = new TreeElement<String, IJavaElement>("", null, IJavaElement.class);
        if (folders != null) {
            ViewLabelProvider labelProv = getLabelProvider();
            for (Object folder : folders) {
                if(!(folder instanceof IJavaElement)) {
                    continue;
                }
                addToRoot(root, labelProv, (IJavaElement) folder);
            }
        }
        final TreeViewer treeViewer = getViewer();
        treeViewer.setInput(root);

        expandViewer(treeViewer);
    }

    public void setRoot(IJavaElement javaRoot) {
        this.folders = new ArrayList<IJavaElement>();

        final TreeElement<String, IJavaElement> root = new TreeElement<String, IJavaElement>("", null, IJavaElement.class);
        addToRoot(root, getLabelProvider(), javaRoot);
        final TreeViewer treeViewer = getViewer();
        treeViewer.setInput(root);

        expandViewer(treeViewer);
    }

    private void addToRoot(TreeElement<String, IJavaElement> root, ViewLabelProvider labelProv,
            IJavaElement elt) {
        folders.add(elt);
        Set<TreeElement<IJavaElement, ?>> children = ViewContentProvider.createTreeElements(labelProv, elt);
        for (TreeElement<IJavaElement, ?> child : children) {
            root.addChild(child);
        }
    }

    static void expandViewer(final TreeViewer treeViewer) {
        final Runnable expand = new Runnable() {
            public void run() {
                treeViewer.expandToLevel(2);
            }
        };
        Display.getDefault().asyncExec(expand);
    }

    public List<IJavaElement> getFolders() {
        return folders;
    }

    public void removeResults() {
        ISelection selection = getViewer().getSelection();
        List<TreeElement> list = SelectionHelper.getFromSelection(selection, TreeElement.class);
        contentProvider.removeResults(list);
    }

    public void setViewer(TreeViewer viewer) {
        this.viewer = viewer;
    }

    public TreeViewer getViewer() {
        return viewer;
    }

    public void updateFilter(String types){
        HashSet<String> filteredTypes = new HashSet<String>(PreferencesInitializer.parseList(types));
        if(filter == null){
            filter = new TypeFilter();
            viewer.addFilter(filter);
        }
        filter.setTypes(filteredTypes);
        viewer.refresh(true);
    }

    public IWorkbenchSiteProgressService getProgressService() {
        IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
                .getAdapter(IWorkbenchSiteProgressService.class);
        return service;
    }

    public class SelectionListener implements ISelectionProvider, ISelectionChangedListener {

        private final TreeViewer viewer2;
        private final Set<ISelectionChangedListener> listeners;

        public SelectionListener(TreeViewer viewer) {
            listeners = new HashSet<ISelectionChangedListener>();
            viewer2 = viewer;
            viewer2.addSelectionChangedListener(this);
        }

        public void addSelectionChangedListener(ISelectionChangedListener listener) {
            listeners.add(listener);
        }

        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            listeners.remove(listener);
        }

        public IStructuredSelection getSelection() {
            return (IStructuredSelection) viewer2.getSelection();
        }

        public void setSelection(ISelection selection) {
            viewer2.setSelection(selection);
        }

        public void selectionChanged(SelectionChangedEvent event) {
            IStatusLineManager manager = getViewSite().getActionBars().getStatusLineManager();
            ISelection selection = event.getSelection();
            if (selection.isEmpty()) {
                manager.setMessage(null);
            }
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection ssel = (IStructuredSelection) selection;
                Object obj = ssel.getFirstElement();
                manager.setMessage(getLabelProvider().getImage(obj), getLabelProvider().getDecription(obj));
                SelectionChangedEvent myEvent = new SelectionChangedEvent(this, ssel);
                for (ISelectionChangedListener listener : listeners) {
                    listener.selectionChanged(myEvent);
                }
            }
        }
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean b) {
        isPinned = b;
    }

    public boolean isReferencesShown() {
        return contentProvider.isReferencesShown;
    }

    public void setReferencesShown(boolean b) {
        contentProvider.setReferencesShown(b);
        getViewer().refresh(false);
    }

    private void setLabelProvider(ViewLabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    ViewLabelProvider getLabelProvider() {
        return labelProvider;
    }

    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }


    public SelectionListener getSelectionProvider() {
        return selectionProvider;
    }
}
