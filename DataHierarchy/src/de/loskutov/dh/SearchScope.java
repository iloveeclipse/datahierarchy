/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;


public enum SearchScope {
    WORKSPACE {
        @Override
        public IJavaSearchScope createScope(IJavaElement elt) {
            if(elt instanceof IJavaProject){
                return PROJECT.createScope(elt);
            }
            return SearchEngine.createWorkspaceScope();
        }
    },
    PROJECT {
        @Override
        public IJavaSearchScope createScope(IJavaElement elt) {
            return SearchEngine.createJavaSearchScope(new IJavaElement[] { elt.getJavaProject() },
                    IJavaSearchScope.SOURCES);
        }
    },
    HIERARCHY {
        @Override
        public IJavaSearchScope createScope(IJavaElement elt) {
            if(elt instanceof IJavaProject){
                return PROJECT.createScope(elt);
            }
            IType type = (IType) (elt instanceof IType ? elt : (IType) elt
                    .getAncestor(IJavaElement.TYPE));

            if(Object.class.getName().equals(type.getFullyQualifiedName())){
                return PROJECT.createScope(type);
            }

            try {
                return SearchEngine.createHierarchyScope(type);
            } catch (JavaModelException e) {
                DataHierarchyPlugin.logError("createHierarchyScope() failed for " + type, e);
            }
            return PROJECT.createScope(elt);
        }
    };

    abstract public IJavaSearchScope createScope(IJavaElement elt);

    public static final SearchScope DEFAULT = PROJECT;

    public static SearchScope getSearchScope(String actionId) {
        if (actionId == null) {
            return DEFAULT;
        }
        for (SearchScope scope : values()) {
            if (actionId.endsWith(scope.name().toLowerCase())) {
                return scope;
            }
        }
        return DEFAULT;
    }
}
