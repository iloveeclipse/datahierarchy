/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.tree.DelayedJavaElement;
import de.loskutov.dh.tree.TreeElement;

public class SearchHelper {

    private final TreeElement<?, ?> searchRoot;
    private final ISearchConfiguration conf;
    private IJavaSearchScope scope;
    private SearchPattern pattern;

    public SearchHelper(TreeElement<?, ?> searchRoot, ISearchConfiguration conf) {
        this.searchRoot = searchRoot;
        this.conf = conf;
    }

    /**
     * Blocks current thread until search is done
     */
    public List<IField> getReferencedFields(IProgressMonitor monitor) throws CoreException {

        boolean searchOnlyStatics = conf.get(ISearchConfiguration.SEARCH_STATICS_ONLY);
        searchOnlyStatics = searchOnlyStatics && searchRoot.isVisibleRoot();
        FieldReferencesRequestor searchRequestor = SearchUtils.createFieldRequestor(conf,
                searchOnlyStatics);

        IType[] typesForAllElements = SearchUtils.getAlSuperTypesForAllElements(
                searchRoot, monitor);
        IJavaElement javaElement = searchRoot.getJavaElement();
        if(typesForAllElements.length == 0
                || javaElement instanceof IPackageFragmentRoot
                || javaElement instanceof IPackageFragment){
            pattern = SearchHelper.createAnyFieldPattern();
            scope = SearchEngine.createJavaSearchScope(new IJavaElement[]{javaElement}, IJavaSearchScope.SOURCES);
        } else {
            pattern = SearchHelper.createAnyFieldPattern(typesForAllElements);
            scope = SearchEngine.createJavaSearchScope(typesForAllElements, IJavaSearchScope.SOURCES);
        }

        return search(searchRequestor, monitor);
    }

    /**
     * Blocks current thread until search is done
     * @param callback
     */
    public List<IField> getDeclaredFields(IProgressMonitor monitor) throws CoreException {

        IJavaElement javaElement = searchRoot.getJavaElement();


        if (!(javaElement instanceof IField)) {
            throw new CoreException(new Status(IStatus.ERROR, DataHierarchyPlugin.getId(),
                    "JavaElement must be IField: " + javaElement));
        }
        IField field = (IField) javaElement;

        FieldReferencesRequestor searchRequestor = SearchUtils.createFieldRequestor(conf, false);
        IType fieldType = searchRequestor.getFieldTypeFiltered(field);
        if (fieldType == null || SearchUtils.isJavaLangObject(fieldType)) {
            return new ArrayList<IField>();
        }

        TreeElement<?, ?> parent = searchRoot.getParent();
        if (fieldType.equals(parent.getJavaElement())) {
            return new ArrayList<IField>();
        }

        DelayedJavaElement<?,?> treeElement = DelayedJavaElement.lookup(field);
        if(treeElement != null){
            List<IField> fields = addExistingData(treeElement);
            return fields;
        }

        treeElement = DelayedJavaElement.lookup(fieldType);
        if(treeElement != null){
            List<IField> fields = addExistingData(treeElement);
            return fields;
        }

        IType[] typesAndType = SearchUtils.getAlSuperTypesAndType(fieldType,
                monitor);
        scope = SearchEngine.createJavaSearchScope(typesAndType, IJavaSearchScope.SOURCES);

        pattern = SearchHelper.createAnyFieldPattern(typesAndType);

        return search(searchRequestor, monitor);
    }

    private List<IField> addExistingData(DelayedJavaElement<?, ?> treeElement) {
        List<IField> fields = new ArrayList<IField>();
        Set<?> children = treeElement.getChildren(null);
        for (Object elt : children) {
            TreeElement<?,?> treeElt = (TreeElement<?, ?>) elt;
            Object data = treeElt.getData();
            if(data instanceof IType){
                Set<?> childrenData = treeElt.getChildrenData();
                for (Object object : childrenData) {
                    if(object instanceof IField && !fields.contains(object)){
                        fields.add((IField) object);
                    }
                }
            } else if(data instanceof IField && !fields.contains(data)){
                fields.add((IField) data);
            }
        }
        return fields;
    }

    /**
     * Blocks current thread until search is done
     */
    public List<IJavaElement> getReferences(IProgressMonitor monitor) throws CoreException {

        pattern = SearchPattern.createPattern(searchRoot.getJavaElement(),
                IJavaSearchConstants.REFERENCES
                        | ~IJavaSearchConstants.IMPORT_DECLARATION_TYPE_REFERENCE,
                // | IJavaSearchConstants.IGNORE_DECLARING_TYPE,
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE
                        | SearchPattern.R_ERASURE_MATCH);

        if (pattern == null) {
            // this is packages???
            // System.out.println("Search patter is null for: " + elt.getElementName());
            return new ArrayList<IJavaElement>();
        }

        scope = conf.createScope(searchRoot.getJavaElement());
        JavaReferencesRequestor searchRequestor = new JavaReferencesRequestor();

        return search(searchRequestor, monitor);
    }

    private <V extends IJavaElement> List<V> search(AbstractReferencesRequestor<V> requestor,
            IProgressMonitor monitor) throws CoreException {
        SearchEngine searchEngine = new SearchEngine();
        try {
            searchEngine.search(pattern, new SearchParticipant[] { SearchEngine
                    .getDefaultSearchParticipant() }, scope, requestor, monitor);
        } catch (OperationCanceledException e) {
            requestor.endReporting();
        }
        return requestor.getResults();
    }

    private static SearchPattern createAnyFieldPattern(IType[] types) {
        if (types.length == 0) {
            return createAnyFieldPattern();
        }
        SearchPattern result = null;
        for (IType type : types) {
            SearchPattern searchPattern = SearchPattern.createPattern(type.getFullyQualifiedName()
                    + ".*", IJavaSearchConstants.FIELD, IJavaSearchConstants.DECLARATIONS,
                    SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE);
            if(result == null){
                result = searchPattern;
            } else {
                result = SearchPattern.createOrPattern(result, searchPattern);
            }
        }
        return result;
    }

    private static SearchPattern createAnyFieldPattern() {
        return SearchPattern.createPattern("*", IJavaSearchConstants.FIELD,
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH);
    }

}
