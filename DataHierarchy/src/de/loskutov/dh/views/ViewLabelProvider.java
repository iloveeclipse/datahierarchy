/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.tree.TreeElement;

class ViewLabelProvider extends JavaElementLabelProvider {
    private static final String OCC_MATCH_ICON = "platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/occ_match.gif";

    ISharedImages sharedImages = JavaUI.getSharedImages();

    static Image referencesIcon;
    static {
        try {
            URL url = FileLocator.resolve(new URL(OCC_MATCH_ICON));
            ImageDescriptor imd = ImageDescriptor.createFromURL(url);
            JavaElementImageDescriptor jDescr = new JavaElementImageDescriptor(imd, 0,
                    JavaElementImageProvider.BIG_SIZE);
            referencesIcon = jDescr.createImage(true);
        } catch (IOException e) {
            DataHierarchyPlugin.logError("resolve() failed for icon: " + OCC_MATCH_ICON, e);
        }
    }

    public ViewLabelProvider() {
        super(SHOW_POST_QUALIFIED | SHOW_OVERLAY_ICONS | SHOW_PARAMETERS |SHOW_TYPE);
    }

    @Override
    public String getText(Object obj) {
        if (obj instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> element = (TreeElement<?, ?>) obj;
            return element.getName();
        }
        return super.getText(obj);
    }

    public String getDecription(Object obj) {
        return getDecription(obj, JavaElementLabels.ALL_FULLY_QUALIFIED
                | JavaElementLabels.APPEND_ROOT_PATH);
    }

    public String getDecription(Object obj, long flags) {
        if (obj instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> element = (TreeElement<?, ?>) obj;
            Object data = element.getData();
            if (data instanceof IJavaElement) {
                return JavaElementLabels.getElementLabel((IJavaElement) data, flags);
            }
            if (data instanceof IResource) {
                IJavaElement javaElt = JavaCore.create((IResource) data);
                if (javaElt != null) {
                    return JavaElementLabels.getElementLabel(javaElt, flags);
                }
            }
        }
        return getText(obj);
    }

    @Override
    public Image getImage(Object obj) {

        if (obj instanceof TreeElement<?, ?>) {
            TreeElement<?, ?> element = (TreeElement<?, ?>) obj;
            if (element.isVirtual() && element.getParent() == null) {
                if (referencesIcon != null) {
                    return referencesIcon;
                }
                return sharedImages.getImage(JavaPluginImages.IMG_OBJS_SEARCH_REF);
            }

            Object data = element.getData();
            if (data instanceof IJavaElement) {
                return super.getImage(data);
            }
            if (data instanceof IResource) {
                IJavaElement javaElt = JavaCore.create((IResource) data);
                if (javaElt != null) {
                    return super.getImage(javaElt);
                }
            }
        }
        // return
        // WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage
        // (obj);
        return super.getImage(obj);
    }
}
