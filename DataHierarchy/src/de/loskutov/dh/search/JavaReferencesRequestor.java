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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;

public class JavaReferencesRequestor extends  AbstractReferencesRequestor<IJavaElement> {

    private final Set<IJavaElement> searchElements;

    public JavaReferencesRequestor() {
        super();
        searchElements = new HashSet<IJavaElement>();
    }

    @Override
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
        if (match.isInsideDocComment()) {
            return;
        }

        if (match.getElement() != null && match.getElement() instanceof IJavaElement) {
            IJavaElement member = (IJavaElement) match.getElement();
            if(!searchElements.contains(member)) {
                addMatch(match);
                searchElements.add(member);
            }
        }
    }

}
