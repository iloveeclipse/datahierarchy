/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.preferences.IPrefConstants;
import de.loskutov.dh.preferences.PreferencesInitializer;

public final class FieldReferencesRequestor extends AbstractReferencesRequestor<IField> {

    private final Set<String> filteredTypes;

    private final boolean searchPrimitivesToo;
    private final boolean searchArrays;
    private final boolean searchOnlyStatics;
    /**
     * This is required to restrict Java search on one thread. Allthoug javadoc states that
     * Java search API should be multi-thread safe, it is NOT...
     */
    private final static Semaphore semapfore = new Semaphore(1, true);

    public FieldReferencesRequestor() {
        this(false, false, false);
    }

    @Override
    public void enterParticipant(SearchParticipant participant) {
        try {
            semapfore.acquire();
        } catch (InterruptedException e) {
            DataHierarchyPlugin.logError("FieldReferencesRequestor: waiting on semapfore interrupted", e);
        }
    }

    @Override
    public void exitParticipant(SearchParticipant participant) {
        semapfore.release();
    }

    public FieldReferencesRequestor(boolean searchPrimitivesToo, boolean searchArrays,
            boolean searchOnlyStatics) {
        super();
        this.searchPrimitivesToo = searchPrimitivesToo;
        this.searchArrays = searchArrays;
        this.searchOnlyStatics = searchOnlyStatics;
        String types = DataHierarchyPlugin.getDefault().getPreferenceStore().getString(
                IPrefConstants.PREF_ACTIVE_FILTERS_LIST);
        filteredTypes = new HashSet<String>(PreferencesInitializer.parseList(types));
    }

    @Override
    public void acceptSearchMatch(SearchMatch match) {
        if (match.isInsideDocComment()) {
            return;
        }

        if (match.getElement() != null && match.getElement() instanceof IField) {
            IField field = (IField) match.getElement();


            /*
             * Check if the reported field is REALLY from the parent element.
             * This can be NOT the case for fields from member classes of inner types...
             * See bug 237200...
             */
            IParent parent = (IParent) field.getParent();
            boolean fixedBug237200 = false;
            try {
                IJavaElement[] children = parent.getChildren();
                for (IJavaElement child : children) {
                    if(child instanceof IField && field.equals(child)){
                        fixedBug237200 = true;
                    }
                }
            } catch (JavaModelException e) {
                DataHierarchyPlugin.logError("getChildren() failed on: " + parent, e);
            }

            if(!fixedBug237200){
                return;
            }
            if (searchOnlyStatics) {
                int flags = 0;
                try {
                    flags = field.getFlags();
                } catch (JavaModelException e) {
                    DataHierarchyPlugin.logError("acceptSearchMatch() failed on: " + field, e);
                }

                if ((flags & Flags.AccStatic) == 0 /* || member.isEnumConstant() */) {
                    return;
                }
            }
            if (!isTypeAccepted(field)) {
                return;
            }
            addMatch(match);
        }
    }

    private boolean isTypeAccepted(IField field) {
        String signature = SearchUtils.getTypeSignature(field);
        if (signature == null) {
            return false;
        }

        boolean isPrimitive = SearchUtils.isPrimitive(signature);
        if (!searchPrimitivesToo && isPrimitive) {
            return false;
        }
        int arrayCount = Signature.getArrayCount(signature);
        if (arrayCount > 0) {
            return searchArrays;
        }

        // have to resolve types now...
        String resolvedType = SearchUtils.getResolvedType(signature, field);
        if (resolvedType == null || isFiltered(resolvedType)) {
            return false;
        }
        return true;
    }

    public boolean isFiltered(String resolvedType) {
        return filteredTypes.contains(resolvedType);
    }

    public IType getFieldTypeFiltered(IField field) {
        String signature;
        try {
            signature = field.getTypeSignature();
            IType primaryType = field.getTypeRoot().findPrimaryType();
            String name = JavaModelUtil.getResolvedTypeName(signature, primaryType);

            if (name == null || isFiltered(name)) {
                return null;
            }
            return field.getJavaProject().findType(name);
        } catch (JavaModelException e) {
            DataHierarchyPlugin.logError("getUnfilteredFieldType() failed for field: " + field, e);
        }
        return null;
    }

    public static IType getFieldType(IField field) {
        String signature;
        try {
            signature = field.getTypeSignature();
            IType primaryType = field.getTypeRoot().findPrimaryType();
            String name = JavaModelUtil.getResolvedTypeName(signature, primaryType);
            if (name == null) {
                return null;
            }
            return field.getJavaProject().findType(name);
        } catch (JavaModelException e) {
            DataHierarchyPlugin.logError("getFieldType() failed for field: " + field, e);
        }
        return null;
    }
}
