/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.tree.TreeElement;

/**
 * @author Andrei
 */
public class SearchUtils {

    static final List<String> PRIMITIVE_TYPES = Arrays.asList("B", "C", "D", "F", "I", "J", "S",
            "V", "Z");

    public static FieldReferencesRequestor createFieldRequestor(ISearchConfiguration conf,
            boolean searchOnlyStatics) {
        return new FieldReferencesRequestor(conf.get(ISearchConfiguration.SEARCH_PRIMITIVES_TOO),
                conf.get(ISearchConfiguration.SEARCH_ARRAYS), searchOnlyStatics);
    }

    /**
     * @param searchRoot
     *            can be a package fragment, a compilation unit..
     * @param monitor
     * @return
     * @throws JavaModelException
     */
    static IType[] getAlSuperTypesForAllElements(TreeElement<?, ?> searchRoot,
            IProgressMonitor monitor) throws JavaModelException {
        IJavaElement element = searchRoot.getJavaElement();
        HashSet<IType> allTypesSet = new HashSet<IType>();
        if (element instanceof ICompilationUnit) {
            collectTypesFromCU(monitor, (ICompilationUnit) element, allTypesSet);
        } else if (element instanceof IPackageFragmentRoot) {
            IPackageFragmentRoot root = (IPackageFragmentRoot) element;
            collectTypesFromPackageRoots(monitor, allTypesSet, root);
        } else if (element instanceof IPackageFragment) {
            collectTypesFromPackage(monitor, (IPackageFragment) element, allTypesSet);
        } else if (element instanceof IType) {
            addAlSuperTypesAndType(allTypesSet, (IType) element, monitor);
        } else if (element instanceof IJavaProject) {
            // XXX intentionally empty. The search is TOO slowly later...
//            IJavaProject project = (IJavaProject) element;
//            IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
//            for (IPackageFragmentRoot root : roots) {
//                if(!root.isArchive()) {
//                    collectTypesFromPackageRoots(monitor, allTypesSet, root);
//                }
//            }
        }
        return allTypesSet.toArray(new IType[allTypesSet.size()]);
    }

    private static void collectTypesFromPackageRoots(IProgressMonitor monitor,
            HashSet<IType> allTypesSet, IPackageFragmentRoot root) throws JavaModelException {
        // find all types in this package and all super types for each one
        IJavaElement[] children = root.getChildren();
        for (IJavaElement child : children) {
            if (child instanceof ICompilationUnit) {
                collectTypesFromCU(monitor, (ICompilationUnit) child, allTypesSet);
            } else if (child instanceof IPackageFragment) {
                collectTypesFromPackage(monitor, (IPackageFragment) child, allTypesSet);
            } else if (child instanceof IClassFile) {
                addAlSuperTypesAndType(allTypesSet, ((IClassFile) child).getType(), monitor);
            }
        }
    }

    private static void collectTypesFromPackage(IProgressMonitor monitor, IPackageFragment pack,
            HashSet<IType> allTypesSet) throws JavaModelException {
        // find all types in this package and all super types for each one
        IJavaElement[] children = pack.getChildren();
        for (IJavaElement child : children) {
            if (child instanceof ICompilationUnit) {
                collectTypesFromCU(monitor, (ICompilationUnit) child, allTypesSet);
            } else if (child instanceof IClassFile) {
                addAlSuperTypesAndType(allTypesSet, ((IClassFile) child).getType(), monitor);
            }
        }
    }

    private static void collectTypesFromCU(IProgressMonitor monitor, ICompilationUnit cunit,
            HashSet<IType> allTypesSet) throws JavaModelException {
        // find all types in this CU and all super types for each one
        IType[] allTypes = cunit.getAllTypes();
        for (IType iType : allTypes) {
            addAlSuperTypesAndType(allTypesSet, iType, monitor);
        }
    }

    static IType[] getAlSuperTypesAndType(IType fieldType, IProgressMonitor monitor)
            throws JavaModelException {
        HashSet<IType> types = new HashSet<IType>();
        addAlSuperTypesAndType(types, fieldType, monitor);
        return types.toArray(new IType[types.size()]);
    }

    private static void addAlSuperTypesAndType(Set<IType> types, IType fieldType,
            IProgressMonitor monitor) throws JavaModelException {
        Set<IType> superTypes = getAllSuperTypes(fieldType, monitor);
        types.addAll(superTypes);
        Set<IType> memberTypes = getAllMemberTypes(fieldType, monitor);
        types.addAll(memberTypes);
        for (IType iType : superTypes) {
            if(!isJavaLangObject(iType)){
                memberTypes.addAll(getAllMemberTypes(iType, monitor));
            }
        }
        for (IType iType : memberTypes) {
            if(!isJavaLangObject(iType)) {
                types.addAll(getAllSuperTypes(iType, monitor));
            }
        }
        types.add(fieldType);
    }

    private static Set<IType> getAllSuperTypes(IType type, IProgressMonitor monitor)
            throws JavaModelException {
        HashSet<IType> set = new HashSet<IType>();
        IType[] superTypes = JavaModelUtil.getAllSuperTypes(type, monitor);
        for (IType iType : superTypes) {
            if ((iType.isClass() || iType.isEnum()) && !isJavaLangObject(iType)) {
                set.add(iType);
            }
        }
        return set;
    }

    public static boolean isJavaLangObject(IType fieldType) {
        return ("Object".equals(fieldType.getElementName()) && "java.lang.Object"
                .equals(fieldType.getFullyQualifiedName()));
    }

    private static Set<IType> getAllMemberTypes(IType type, IProgressMonitor monitor) throws JavaModelException {
        HashSet<IType> set = new HashSet<IType>();
        IType[] memberTypes = type.getTypes();
        for (IType iType : memberTypes) {
            if (iType.isClass() && iType.isLocal()) {
                set.add(iType);
            }
        }
        return set;
    }

    public static String getResolvedType(IField field) {
        return getResolvedType(getTypeSignature(field), field);
    }

    public static String getResolvedType(String signature, IField field) {
        if (signature == null) {
            return null;
        }
        // have to resolve types now...
        IType primaryType = field.getTypeRoot().findPrimaryType();

        try {
            return JavaModelUtil.getResolvedTypeName(signature, primaryType);
        } catch (JavaModelException e) {
            DataHierarchyPlugin.logError("getResolvedTypeName() failed for field: " + field, e);
            return null;
        }
    }

    public static String getTypeSignature(IField field) {
        String signature = null;
        try {
            // XXX seems not to work properly with inner *binary* types like
            // java.util.zip.ZipFile$1 (line 212)
            // the field info for fields in inner member types seems not to be
            // present in java model at all...
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=237200
            signature = field.getTypeSignature();
        } catch (JavaModelException e) {
            DataHierarchyPlugin.logError("getTypeSignature() failed for field: " + field, e);
        }
        return signature;
    }

    public static boolean isPrimitive(String signature) {
        String elementType = Signature.getElementType(signature); // getSignatureSimpleName
        return PRIMITIVE_TYPES.contains(elementType);
    }

}
