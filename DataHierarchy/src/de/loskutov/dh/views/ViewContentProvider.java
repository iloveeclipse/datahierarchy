/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.views;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.loskutov.dh.search.ISearchConfiguration;
import de.loskutov.dh.tree.DelayedJavaElement;
import de.loskutov.dh.tree.IJobCallback;
import de.loskutov.dh.tree.TreeElement;

// ILazyTreeContentProvider ?
public class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider, ISearchConfiguration, IJobCallback {

    private TreeElement<String, Object> invisibleRoot;
    private TreeViewer viewer;
    private final DataHierarchyView view;
    boolean searchPrimitivesToo;
    boolean searchArrays;
    boolean autoRemoveEmptyElements;
    boolean searchStaticsOnly;
    public boolean isReferencesShown;

    public ViewContentProvider(DataHierarchyView view) {
        this.view = view;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        this.viewer = (TreeViewer) v;
        DelayedJavaElement.clearCache();
    }

    public void dispose() {
        DelayedJavaElement.clearCache();
    }

    public Object[] getElements(Object parent) {
        if (parent.equals(DataHierarchyView.class)) {
            if (invisibleRoot == null) {
                initialize();
            }
            return getChildren(invisibleRoot);
        }
        return getChildren(parent);
    }

    public Object getParent(Object child) {
        if (child instanceof TreeElement<?, ?>) {
            return ((TreeElement<?, ?>) child).getParent();
        }
        return null;
    }

    public Object[] getChildren(Object parent) {
        if (parent instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> treeElement = (TreeElement<?, ?>) parent;
            return addReferencesNode(treeElement.getChildren(this), treeElement);
        }
        return new Object[0];
    }

    private Object[] addReferencesNode(Set<?> set, TreeElement<?, ?> treeElement) {
        if (!isReferencesShown) {
            return set.toArray();
        }
        if(!treeElement.isVirtual() && treeElement.getData() instanceof IJavaElement){
            Object[] array = set.toArray(new Object[set.size() + 1]);
            array[set.size()] = createReferencesNode((TreeElement<IJavaElement, ?>) treeElement);
            return array;
        }
        return set.toArray();
    }

    private TreeElement<?,?> createReferencesNode(TreeElement<IJavaElement, ?> treeElement) {
        String name = "References to: " + getLabelProvider().getText(treeElement.getData());
        DelayedJavaElement<IJavaElement, IJavaElement> javaElt =
            new DelayedJavaElement<IJavaElement, IJavaElement>(name, treeElement.getData(),
                IJavaElement.class);
        javaElt.setVirtual(true);
        return javaElt;
    }

    public static Set<TreeElement<IJavaElement, ?>> createTreeElements(ViewLabelProvider labelProv,
            IJavaElement elt) {

        // XXX have to think about which elements to allow as root...

        Set<TreeElement<IJavaElement, ?>> children = new HashSet<TreeElement<IJavaElement,?>>();
//        if(elt instanceof IMember){
            DelayedJavaElement<IJavaElement, IJavaElement> child = new DelayedJavaElement<IJavaElement, IJavaElement>(
                    labelProv.getText(elt), elt, IJavaElement.class);
            children.add(child);
            return children;
//        }
//        if(!(elt instanceof IParent)){
//            return children;
//        }
//        IParent parent = (IParent) elt;
//        IJavaElement[] elements;
//        try {
//            elements = parent.getChildren();
//            for (IJavaElement element : elements) {
//                children.addAll(createTreeElements(labelProv, element));
//            }
//        } catch (JavaModelException e) {
//            DataHierarchyPlugin.logError("getChildren() failed on: " + parent, e);
//        }
//        return children;
    }

    public boolean hasChildren(Object parent) {
        if (parent instanceof TreeElement<?, ?>) {
            // if(parent instanceof DelayedFolderElement){
            // DelayedFolderElement element = (DelayedFolderElement) parent;
            // if(!element.isInitialized()){
            // return true;
            // }
            // }
            TreeElement<?, ?> element = (TreeElement<?, ?>) parent;
            return element.hasChildren(this);
        }
        return false;
    }

    /*
     * We will set up a dummy model to initialize tree heararchy. In a real
     * code, you will connect to a real model and expose its hierarchy.
     */
    private void initialize() {
        invisibleRoot = new TreeElement<String, Object>("", null, Object.class);
    }

    public void removeResults(List<TreeElement> list) {
        for (TreeElement<?, ?> treeElement : list) {
            TreeElement<?, ?> parent = treeElement.getParent();
            if(parent == null){
                continue;
            }
            parent.removeChild(treeElement);
            viewer.remove(treeElement);
        }
    }

    public void ensureExpanded(Object elt){
        DataHierarchyView.expandViewer(getViewer());
    }

    public TreeViewer getViewer() {
        return viewer;
    }

    public IWorkbenchSiteProgressService getProgressService() {
        return view.getProgressService();
    }

    public Job sheduleJob(final DelayedJavaElement<?, ? extends IJavaElement> searchRoot){
        Job job = new JavaSearchJob("Searching in " + searchRoot .getJavaElement().getElementName() + "...",
                searchRoot, this);
        job.setUser(true);
        job.addJobChangeListener(new JobChangeAdapter(){
            @Override
            public void done(IJobChangeEvent event) {
                ensureExpanded(searchRoot);
            }
        });
        getProgressService().schedule(job);
        return job;
    }

    public void setReferencesShown(boolean b) {
        isReferencesShown = b;
    }

    public LabelProvider getLabelProvider() {
        return view.getLabelProvider();
    }

    public IJavaSearchScope createScope(IJavaElement elt) {
        return view.getSearchScope().createScope(elt);
    }

    public boolean get(int flag) {
        switch (flag) {
        case SEARCH_ARRAYS:
            return searchArrays;
        case SEARCH_PRIMITIVES_TOO:
            return searchPrimitivesToo;
        case SEARCH_STATICS_ONLY:
            return searchStaticsOnly;
        default:
            break;
        }
        return false;
    }

    // public void updateChildCount(Object element, int currentChildCount) {
    // if (!(element instanceof TreeElement)) {
    // return;
    // }
    // TreeElement prnt = (TreeElement) element;
    // if (prnt instanceof DelayedFolderElement) {
    // DelayedFolderElement element2 = (DelayedFolderElement) prnt;
    // if(element2.isInitialized()) {
    // return;
    // }
    // prnt.getChildren(this);
    // return;
    // }
    // viewer.setChildCount(element, prnt.getChildren(this).length);
    // }
    //
    // public void updateElement(Object parent, int index) {
    // if (!(parent instanceof TreeElement)) {
    // return;
    // }
    // TreeElement prnt = (TreeElement) parent;
    // if (prnt instanceof DelayedFolderElement) {
    // DelayedFolderElement element2 = (DelayedFolderElement) prnt;
    // if(element2.isInitialized()) {
    // return;
    // }
    // prnt.getChildren(this);
    // return;
    // }
    // TreeElement element = prnt.getChildren(this)[index];
    // viewer.replace(prnt, index, element);
    // viewer.setChildCount(prnt, prnt.getChildren(this).length);
    // }
}
