/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.tree;

import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

public class DelayedJavaElement<P extends IJavaElement, C extends IJavaElement> extends
        TreeElement<P, C> {

    private Job job;
    private final Object jobLock;
    private final static WeakHashMap<IType, DelayedJavaElement<?, ?>> typeCache = new WeakHashMap<IType, DelayedJavaElement<?, ?>>();
    private final static WeakHashMap<IField, DelayedJavaElement<?, ?>> fieldCache = new WeakHashMap<IField, DelayedJavaElement<?, ?>>();

    public DelayedJavaElement(String name, P data, Class<C> childrenType) {
        super(name, assertNotNull(data), assertNotNull(childrenType));
        jobLock = new Object();
    }

    private void remember(IType type, DelayedJavaElement<?, ?> elt) {
        typeCache.put(type, elt);
    }

    private void remember(IField field, DelayedJavaElement<?, ?> elt) {
        fieldCache.put(field, elt);
    }

    private void forget(IType type) {
        typeCache.remove(type);
    }

    private void forget(IField field) {
        fieldCache.remove(field);
    }

    public static void clearCache() {
        typeCache.clear();
        fieldCache.clear();
    }

    public static DelayedJavaElement<?, ?> lookup(IType type) {
        return typeCache.get(type);
    }

    public static DelayedJavaElement<?, ?> lookup(IField field) {
        return fieldCache.get(field);
    }

    @Override
    public Set<TreeElement<C, ?>> getChildren(IJobCallback callback) {
        if (!isInitialized()) {
            doSearch(getData(), callback);
        }
        return super.getChildren(callback);
    }

    @Override
    public boolean hasChildren(IJobCallback callback) {
        if (!isInitialized()) {
            doSearch(getData(), callback);
        }
        return super.hasChildren(callback);
    }

    private void doSearch(final P folder, final IJobCallback callback) {
        if (callback == null) {
            return;
        }
        synchronized (jobLock) {
            if (job != null) {
                return;
            }
            if (isInitialized()) {
                return;
            }
            job = callback.sheduleJob(this);
        }
    }

    @Override
    public synchronized void setInitialized(boolean initialized) {
        boolean wasInitialized = super.isInitialized();
        super.setInitialized(initialized);
        if (initialized) {
            synchronized (jobLock) {
                job = null;
            }
        } else {
            if (wasInitialized) {
                synchronized (jobLock) {
                    for (TreeElement<C, ?> child : super.getChildren(null)) {
                        child.setInitialized(false);
                        removeChild(child);
                    }
                }
            }
        }
        if (getData() instanceof IType) {
            remember((IType) getData(), this);
        } else if (getData() instanceof IField) {
            remember((IField) getData(), this);
        }
    }

    public TreeElement<IJavaElement, ?> addFieldAndDeclaredTypes(TreeElement<IField, ?> fieldObj) {
        IType declaringType = fieldObj.getData().getDeclaringType();
        String qualifiedName = declaringType.getTypeQualifiedName('.');
        DelayedJavaElement<IJavaElement, IField> enclosingTypeObj = new DelayedJavaElement<IJavaElement, IField>(
                qualifiedName, declaringType, IField.class);
        enclosingTypeObj.addChild(fieldObj);
        enclosingTypeObj.setInitialized(true);
        return enclosingTypeObj;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void removeChild(TreeElement<?, ?> child) {
        clearMyCache();
        super.removeChild(child);
    }

    private void clearMyCache() {
        if (getData() instanceof IType) {
            forget((IType) getData());
        } else if (getData() instanceof IField) {
            forget((IField) getData());
        }
    }

    @Override
    public void setParent(TreeElement<?, P> parent) {
        super.setParent(parent);
        if (parent == null) {
            clearMyCache();
        }
    }
}
