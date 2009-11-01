/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.search;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

public interface ISearchConfiguration {

    int SEARCH_PRIMITIVES_TOO = 1;
    int SEARCH_ARRAYS = 2;
    int SEARCH_STATICS_ONLY = 3;

    IJavaSearchScope createScope(IJavaElement elt);

    boolean get(int flag);
}
