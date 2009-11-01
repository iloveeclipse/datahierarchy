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
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.swt.widgets.Display;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.search.FieldReferencesRequestor;
import de.loskutov.dh.search.SearchHelper;
import de.loskutov.dh.search.SearchUtils;
import de.loskutov.dh.tree.DelayedJavaElement;
import de.loskutov.dh.tree.TreeElement;

final class JavaSearchJob extends Job  {
    private final IJavaElement container;
    private final ViewContentProvider callback;
    private final DelayedJavaElement<?, IJavaElement> searchRoot;
    private final SearchHelper searchHelper;

    @SuppressWarnings("unchecked")
    JavaSearchJob(String name, DelayedJavaElement<?, ? extends IJavaElement> searchRoot, ViewContentProvider callback) {
        super(name);
        this.searchRoot = (DelayedJavaElement<?, IJavaElement>) searchRoot;
        this.container = searchRoot.getJavaElement();
        this.callback = callback;
        searchHelper = new SearchHelper(searchRoot, callback);
        setRule(new ExclusiveRule(searchRoot));
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, "Java Model Search...", 100);
        IStatus status;
        try {
            if (searchRoot.isVirtual()) {
                status = searchReferences(monitor, subMonitor);
            } else {
                status = searchFields(monitor, subMonitor);
            }
        } finally {
            finishSearch(monitor);
        }
        return status;
    }

    private IStatus searchReferences(IProgressMonitor monitor, SubMonitor subMonitor) {
        List<IJavaElement> references;
        try {
            references = searchHelper.getReferences(subMonitor);
        } catch (CoreException e) {
            subMonitor.setCanceled(true);
            DataHierarchyPlugin.logError("searchReferences() failed", e);
            return Status.CANCEL_STATUS;
        }

        subMonitor.done();

        subMonitor = SubMonitor.convert(monitor, "Filtering results", references.size());

        for (IJavaElement jElement : references) {
            subMonitor.worked(1);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            DelayedJavaElement<IJavaElement, IJavaElement> jObj = createNewElement(jElement);

            jObj.setVirtual(true);

            IType declaringType = (IType) jElement.getAncestor(IJavaElement.TYPE);
            TreeElement<IJavaElement, ?> parentElt = searchRoot.find(declaringType);

            if (parentElt == null) {
                searchRoot.addChild(jObj);
            } else {
                TreeElement<IJavaElement, IJavaElement> p2 = (TreeElement<IJavaElement, IJavaElement>) parentElt;
                p2.addChild(jObj);
            }

        }
        return Status.OK_STATUS;
    }

    private IStatus searchFields(IProgressMonitor monitor, SubMonitor subMonitor) {
        List<IField> fields;
        boolean searchForDeclarations = container instanceof IField;
        try {
            if (searchForDeclarations) {
                fields = searchHelper.getDeclaredFields(subMonitor);
            } else {
                fields = searchHelper.getReferencedFields(subMonitor);
            }
        } catch (CoreException e) {
            subMonitor.setCanceled(true);
            DataHierarchyPlugin.logError("searchFields() failed", e);
            return Status.CANCEL_STATUS;
        }
        subMonitor.done();

        subMonitor = SubMonitor.convert(monitor, "Filtering results", fields.size());


        //////////////////////////////////////////
        // if we are searching for the package !!!
        //////////////////////////////////////////
        // TODO check if our direct children contains the type in which the filed is declared
        // if not, then the filed is declared in one of supertypes in one (or more) of the children
        // in this case try to find all children extending this supertype
        // and add the field there

        // collect all types containing fields

        // sort out types which are not direct children of the search root

        // add "direct" child types

        // for each "direct" type:
        // 1) (if at least one "foreign type present"): create supertype hierarchy
        //
        //  for each "foreign" type check if it's

//        FieldReferencesRequestor searchRequestor = SearchUtils.createFieldRequestor(callback, false);
//        IType oldType = null;

        for (IField field : fields) {
            subMonitor.worked(1);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // XXX continue this later
//            if(searchForDeclarations){
//                if(oldType == null){
//                    oldType = searchRequestor.getFieldTypeFiltered((IField) container);
//                }
//                ITypeRoot newRoot = field.getTypeRoot();
//                if(newRoot != null && oldType != null){
//                    IType newType = newRoot.findPrimaryType();
//                    // if field's declared class is not same as search root, it's a predecessor
//                    if(!oldType.equals(newType)){
//                        // TODO add this type to the current search root, add the filed to the new type
//                    }
//                }
//            }

            DelayedJavaElement<IField, IJavaElement> fieldObj = createNewElement(field);

            TreeElement<IJavaElement, ?> parentElt = findParent(field);

            if (parentElt == null) {
                // TODO for searchForDeclarations AND inherited fields, we should NOT add declared type here
                searchRoot.addChild(searchRoot.addFieldAndDeclaredTypes(fieldObj));
            } else {
                TreeElement<IJavaElement, IField> p2 = (TreeElement<IJavaElement, IField>) parentElt;
                p2.addChild(fieldObj);
            }

            String typeSignature = SearchUtils.getTypeSignature(field);
            addGenericContainerTypes(fieldObj, field, typeSignature);
        }
        if(searchForDeclarations){
            IField field = (IField) container;
            String typeSignature = SearchUtils.getTypeSignature(field);
            addGenericContainerTypes((DelayedJavaElement<IField, IJavaElement>) searchRoot, field, typeSignature);
        }
        return Status.OK_STATUS;
    }

    private TreeElement<IJavaElement, ?> findParent(IField field) {
        IType declaringType = field.getDeclaringType();
        TreeElement<IJavaElement, ?> parentElt = searchRoot.find(declaringType);
        return parentElt;
    }

    private DelayedJavaElement<IField, IJavaElement> createNewElement(IField field) {
        String name = getLabel(field);
        return new DelayedJavaElement<IField, IJavaElement>(name, field, IJavaElement.class);
    }

    private DelayedJavaElement<IJavaElement, IJavaElement> createNewElement(IJavaElement elt) {
        String name = getLabel(elt);
        return new DelayedJavaElement<IJavaElement, IJavaElement>(name, elt, IJavaElement.class);
    }

    /**
     * adds parametrized types for containers like List<Data> or Map<key,Value>,
     * creates "delayed" type container with parameter class
     */
    private void addGenericContainerTypes(DelayedJavaElement<IField, IJavaElement> fieldObj,
            IField field, String signature) {
        Set<String> types = new HashSet<String>();
        collectTypeArguments(signature, types);
        FieldReferencesRequestor req = new FieldReferencesRequestor();
        for (String typeName : types) {
            String resolvedType = SearchUtils.getResolvedType(typeName, field);
            if (resolvedType == null || req.isFiltered(resolvedType)) {
                if (resolvedType == null) {
                    // XXX this is a generic type variable. Don't know what to do here
                    // System.out.println("not resolved: " + typeName + " in " + field);
                }
                continue;
            }
            IType type;
            try {
                type = field.getJavaProject().findType(resolvedType);
                if (type == null) {
                    continue;
                }
            } catch (JavaModelException e) {
                DataHierarchyPlugin.logError("addGenericContainerTypes() failed for type: "
                        + typeName, e);
                continue;
            }
            String name = Signature.getSimpleName(resolvedType);
            DelayedJavaElement<IJavaElement, IJavaElement> folderElement =
                new DelayedJavaElement<IJavaElement, IJavaElement>(name, type, IJavaElement.class);
            fieldObj.addChild(folderElement);
        }

    }

    static void collectTypeArguments(String signature, Set<String> types){
        String[] typeArguments = Signature.getTypeArguments(signature);
        for (String arg : typeArguments) {
            String[] newArguments = Signature.getTypeArguments(arg);
            String simpleName = Signature.getTypeErasure(arg);
            types.add(simpleName);
            if(newArguments.length == 0) {
                types.add(arg);
            } else {
                collectTypeArguments(arg, types);
            }
        }
    }


    private void finishSearch(IProgressMonitor monitor) {
        searchRoot.setInitialized(true);

        if(monitor.isCanceled()){
            createCancelledNode();
        }
        monitor.done();
        Runnable run = new Runnable() {
            public void run() {
                updateViewer();
            }
        };
        Display.getDefault().asyncExec(run);
    }

    private void updateViewer() {
        Set<?> children = searchRoot.getChildren(callback);
        if(children.size() > 0) {
            callback.getViewer().add(searchRoot, children.toArray());
        } else if(callback.autoRemoveEmptyElements && searchRoot.getJavaElement() instanceof IType) {
            removeElement(searchRoot);
        }
    }

    private void removeElement(TreeElement<?,?> elt) {
        TreeElement<?,?> parent = elt.getParent();
        if(parent == null){
            return;
        }
        parent.removeChild(elt);
        callback.getViewer().remove(elt);
        if(!parent.isVirtual() && parent.isInitialized() && !parent.hasChildren(callback)){
            if(!parent.isVisibleRoot() && parent.getJavaElement() instanceof IType) {
                removeElement(parent);
            } else {
                // TODO gimmics: add "No Search Results..."
            }
        }
    }

   private void createCancelledNode() {
        // TODO some nice gimmics
//       TreeElement<IJavaElement, String> element = new TreeElement<IJavaElement, String>(
//                "Search cancelled", searchRoot.getData(), String.class);
//        element.setVirtual(true);
//        searchRoot.addChild(element);
    }

 @Override
    public boolean belongsTo(Object family) {
        return family == DataHierarchyPlugin.JOB_FAMILY;
    }

    private String getLabel(IJavaElement elt){
        return callback.getLabelProvider().getText(elt);
    }

}
