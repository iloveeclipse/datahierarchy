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

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public final class TypeFilter extends ViewerFilter {
    private HashSet<String> types;
    private final ViewLabelProvider labelProvider;

    public TypeFilter() {
        super();
        labelProvider = new ViewLabelProvider();
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(types == null || types.isEmpty()){
            return true;
        }
        String string = labelProvider.getDecription(element, JavaElementLabels.ALL_FULLY_QUALIFIED);
        if(string == null){
            return true;
        }
        for (String name : types) {
            if(string.equals(name)){
                return false;
            }
        }
        return true;
    }

    public void setTypes(HashSet<String> types){
        this.types = types;
    }
}
