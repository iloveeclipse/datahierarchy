/*******************************************************************************
 * Copyright (c) 2009 - 2015 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.tree;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.internal.ui.JavaElementAdapterFactory;

@SuppressWarnings("restriction")
public class ElementAdapterFactory extends JavaElementAdapterFactory {

    private static ElementAdapterFactory factory;

    static ElementAdapterFactory instance() {
        if (factory == null) {
            factory = new ElementAdapterFactory();
        }
        return factory;
    }

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adaptableObject instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> adaptable = (TreeElement<?, ?>) adaptableObject;
            return super.getAdapter(adaptable.getData(), adapterType);
        }
        return super.getAdapter(adaptableObject, adapterType);
    }

    @Override
    public IResource getAdaptedResource(IAdaptable adaptable) {
        if (adaptable instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> elt = (TreeElement<?, ?>) adaptable;
            return super.getAdaptedResource(elt.getJavaElement());
        }
        return super.getAdaptedResource(adaptable);
    }

    @Override
    public ResourceMapping getAdaptedResourceMapping(IAdaptable adaptable) {
        if (adaptable instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> elt = (TreeElement<?, ?>) adaptable;
            return super.getAdaptedResourceMapping(elt.getJavaElement());
        }
        return super.getAdaptedResourceMapping(adaptable);
    }


}
