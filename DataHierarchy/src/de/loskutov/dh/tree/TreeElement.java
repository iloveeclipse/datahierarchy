/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.tree;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;


/**
 * A container element in the (java model based) tree, which can contain other containers
 * of specified type
 *
 * @author Andrei
 *
 * @param <P> this element type (parent)
 * @param <C> child element type
 */
public class TreeElement<P, C> implements IAdaptable {
    private final Set<TreeElement<C, ?>> children;
    private final P data;
    private final String name;
    private TreeElement<?, P> parent;
    private volatile boolean initialized;
    private final Class<C> childrenType;
    private boolean virtual;

    public TreeElement(String name, P data, Class<C> childrenType) {
        this.childrenType = childrenType;
        this.name = name == null ? "?" : name;
        this.data = data;
        children = new CopyOnWriteArraySet<TreeElement<C, ?>>();
    }

    public String getName() {
        return name;
    }

    public void setParent(TreeElement<?, P> parent) {
        this.parent = parent;
    }

    public TreeElement<?, P> getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class key) {
        if (ITaskListResourceAdapter.class == key) {
            // java.lang.ClassCastException:
            // de.loskutov.bfields.views.DelayedFieldElement
            // cannot be cast to org.eclipse.jdt.core.IJavaElement
            // at
            //org.eclipse.jdt.internal.ui.JavaTaskListAdapter.getAffectedResource
            // (JavaTaskListAdapter.java:28)
            return null;
        }
        if (key == IResource.class && data instanceof IResource) {
            return data;
        }
        if (IContributorResourceAdapter.class == key) {
            return ElementAdapterFactory.instance();
        }
        if (data instanceof IJavaElement) {
            Object adapter = ElementAdapterFactory.instance().getAdapter(data, key);
            if (adapter == null && key == IJavaElement.class) {
                return data;
            }
            return adapter;
        }
        return (data instanceof IAdaptable) ? ((IAdaptable) data).getAdapter(key) : null;
    }

    public <V> V getAdapter2(Class<V> key) {
        return key.cast((data instanceof IAdaptable) ? ((IAdaptable) data).getAdapter(key) : null);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return data == null ? super.hashCode() : data.hashCode();
    }

    public boolean addChild(TreeElement<C, ?> child) {
        if(false && !childrenType.isAssignableFrom(child.data.getClass())){
            return false;
        }
        boolean ok;
        synchronized (children) {
            TreeElement<C, ?> element = find(child.getData());
            if(element != null){
                return false;
            }
            ok = children.add(child);
            child.setParent(this);
        }
        return ok;
    }

    public void removeChild(TreeElement<?, ?> child) {
        synchronized (children) {
            children.remove(child);
            child.setParent(null);
        }
    }

    public Set<TreeElement<C, ?>> getChildren(IJobCallback callback) {
        return children;
    }

    public Set<C> getChildrenData() {
        Set<C> set = new HashSet<C>();
        if(!initialized || children.size() == 0){
            return set;
        }
        for (TreeElement<C, ?> child : children) {
            set.add(child.getData());
        }
        return set;
    }

    public boolean hasChildren(IJobCallback callback) {
        return children.size() > 0;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return true if this is a starting point of the visible search tree
     */
    public boolean isVisibleRoot() {
        return getParent() != null && getParent().getParent() == null;
    }

    public IJavaElement getJavaElement() {
        if (data instanceof IJavaElement) {
            return (IJavaElement) data;
        }
        if (data instanceof IResource) {
            return JavaCore.create((IResource) data);
        }
        return getAdapter2(IJavaElement.class);
    }

    @SuppressWarnings("unchecked")
    public TreeElement<C, ?> find(C eltData) {
        for (TreeElement<C, ?> obj : children) {
            if (obj.data.equals(eltData)) {
                return obj;
            }
        }
        if(data != null && data.equals(eltData)){
            return (TreeElement<C, C>) this;
        }
        return null;
    }

    public TreeElement<?, ?> findRecursive(Object eltData){
        if(data != null && data.equals(eltData)){
            return this;
        }
        if(parent == null){
            return null;
        }
        return parent.findRecursive(eltData);
    }

    protected static <V> V assertNotNull(V data){
        Assert.isNotNull(data, "Attempt to create TreeElement with null data object");
        return data;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public P getData() {
        return data;
    }
}
