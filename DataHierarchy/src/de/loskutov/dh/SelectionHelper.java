/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public class SelectionHelper {

    public static <V> List<V> getFromSelection(ISelection selection, Class<V> clazz) {
        List<V> list = new ArrayList<V>();
        if (!(selection instanceof IStructuredSelection)) {
            return list;
        }
        IStructuredSelection sel = (IStructuredSelection) selection;
        for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
            V adapter = getAdapter(iterator.next(), clazz);
            if (adapter != null) {
                list.add(adapter);
            }
        }
        return list;
    }

    private static <V> V getAdapter(Object object, Class<V> clazz) {
        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }
        if (object instanceof IAdaptable) {
            return clazz.cast(((IAdaptable) object).getAdapter(clazz));
        }
        return null;
    }
}
