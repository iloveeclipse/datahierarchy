/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import de.loskutov.dh.DataHierarchyPlugin;

public abstract class AbstractReferencesRequestor<V extends IJavaElement> extends SearchRequestor {
    private volatile boolean isDone;

    /**
     * List is used, may be later we will allow duplicated elements?
     */
    private final List<SearchMatch> searchResults;

    public AbstractReferencesRequestor() {
        searchResults = new LinkedList<SearchMatch>();
    }

    @Override
    public void endReporting() {
        isDone = true;
    }

    public List<V> getResults() {
        while (!isDone) {
            synchronized (this) {
                try {
                    wait(50);
                } catch (InterruptedException e) {
                    DataHierarchyPlugin.logError("getResults() interrupted...", e);
                }
            }
        }
        List<V> results = new ArrayList<V>();
        for (SearchMatch match : searchResults) {
            results.add((V) match.getElement());
        }
        return results;
    }

    void addMatch(SearchMatch match){
        searchResults.add(match);
    }
}
