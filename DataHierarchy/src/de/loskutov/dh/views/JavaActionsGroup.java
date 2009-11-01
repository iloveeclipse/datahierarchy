/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.views;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.CopyQualifiedNameAction;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonMenuConstants;

import de.loskutov.dh.tree.TreeElement;

@SuppressWarnings("restriction")
public class JavaActionsGroup extends ActionGroup {

    private final CopyQualifiedNameAction copyQualifiedNameAction;

    private ISelection selection;

    public JavaActionsGroup(final IViewPart view, ISelectionProvider selProvider) {
        super();
        final IStructuredSelection selection1 = getRealSelection((IStructuredSelection) selProvider
                .getSelection());

        copyQualifiedNameAction = new CopyQualifiedNameAction(view.getSite());
        copyQualifiedNameAction.setActionDefinitionId(CopyQualifiedNameAction.ACTION_DEFINITION_ID);
        copyQualifiedNameAction.setSpecialSelectionProvider(selProvider);
        copyQualifiedNameAction.update(selection1);

        selProvider.addSelectionChangedListener(copyQualifiedNameAction);
    }

    static IStructuredSelection getRealSelection(IStructuredSelection selection1) {
        List<Object> elements = new ArrayList<Object>();
        for (Iterator<?> iterator = selection1.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if (object instanceof TreeElement<?, ?>) {
                TreeElement<?, ?> element = (TreeElement<?, ?>) object;
                elements.add(element.getData());
            }
        }
        return new StructuredSelection(elements);
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID,
                copyQualifiedNameAction);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        super.fillContextMenu(menu);
        if (selection != null && !selection.isEmpty()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, copyQualifiedNameAction);
        }
    }

    @Override
    public void updateActionBars() {
        super.updateActionBars();
    }

    @Override
    public void setContext(ActionContext context) {
        if (context == null) {
            return;
        }
        selection = getRealSelection((IStructuredSelection) context.getSelection());
        copyQualifiedNameAction.update(selection);
        super.setContext(context);
    }

    static CompositeActionGroup createActionsGroup(DataHierarchyView part,
            ISelectionProvider selProvider) {
        IViewPart ppart = createPartProxy(part);
        return new CompositeActionGroup(new ActionGroup[] {
                new OpenEditorActionGroup(ppart),
                new OpenViewActionGroup(ppart),
                // new CCPActionGroup(ppart),
                new JavaActionsGroup(ppart, new SelectionProviderProxy(part)),
                new GenerateActionGroup(ppart), new RefactorActionGroup(ppart),
                new JavaSearchActionGroup(ppart) });
    }

    static IViewPart createPartProxy(DataHierarchyView part) {
        return createProxy(new ViewHandler(part, new ViewSiteHandler(part)), IViewPart.class);
    }

    private static <V> V createProxy(InvocationHandler handler, Class<V> clazz) {
        return clazz.cast(Proxy.newProxyInstance(JavaActionsGroup.class.getClassLoader(),
                new Class[] { clazz }, handler));
    }

    static class ViewHandler extends ChainedInvocationHandler<IWorkbenchPartSite> {

        public ViewHandler(DataHierarchyView part, InvocationHandler viewSite) {
            super(part, "getSite", viewSite, IWorkbenchPartSite.class);
        }

        @Override
        public Object invokeReal(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(part, args);
        }
    }

    static class ViewSiteHandler extends AbstractInvocationHandler {

        private final SelectionProviderProxy selProv;

        public ViewSiteHandler(DataHierarchyView part) {
            super(part, "getSelectionProvider");
            selProv = new SelectionProviderProxy(part);
        }

        @Override
        public Object invokeReal(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(part.getSite(), args);
        }

        @Override
        public Object invoke2(Object proxy, Method method, Object[] args) throws Throwable {
            return selProv;
        }
    }

    static class SelectionProviderProxy implements ISelectionProvider {

        Map<ISelectionChangedListener, SelectionListenerHandler> wrapperMap;
        private final DataHierarchyView part;

        public SelectionProviderProxy(DataHierarchyView part) {
            this.part = part;
            wrapperMap = new HashMap<ISelectionChangedListener, SelectionListenerHandler>();
        }

        public void addSelectionChangedListener(ISelectionChangedListener listener) {
            if (wrapperMap.get(listener) != null) {
                return;
            }
            wrapperMap.put(listener, new SelectionListenerHandler(listener));
            part.getSelectionProvider().addSelectionChangedListener(wrapperMap.get(listener));
        }

        public ISelection getSelection() {
            IStructuredSelection sel = part.getSelectionProvider().getSelection();
            return getRealSelection(sel);
        }

        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            if (wrapperMap.get(listener) == null) {
                return;
            }
            SelectionListenerHandler handler = wrapperMap.remove(listener);
            part.getSelectionProvider().removeSelectionChangedListener(handler);
        }

        public void setSelection(ISelection selection) {
            part.getSelectionProvider().setSelection(selection);
        }
    }

    static class SelectionListenerHandler implements ISelectionChangedListener {

        private final ISelectionChangedListener listener;

        public SelectionListenerHandler(ISelectionChangedListener listener) {
            this.listener = listener;
        }

        public void selectionChanged(SelectionChangedEvent event) {
            IStructuredSelection selection2 = (IStructuredSelection) event.getSelection();
            IStructuredSelection realSelection = getRealSelection(selection2);
            SelectionChangedEvent event2 = new SelectionChangedEvent(event.getSelectionProvider(),
                    realSelection);
            listener.selectionChanged(event2);
        }
    }

    abstract static class AbstractInvocationHandler implements InvocationHandler {

        protected final DataHierarchyView part;
        private final String[] methodNames;

        public AbstractInvocationHandler(DataHierarchyView part, String... methodName) {
            this.part = part;
            this.methodNames = methodName;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (String name : methodNames) {
                if (method.getName().equals(name)) {
                    return invoke2(proxy, method, args);
                }
            }
            return invokeReal(proxy, method, args);
        }

        abstract public Object invoke2(Object proxy, Method method, Object[] args) throws Throwable;

        abstract public Object invokeReal(Object proxy, Method method, Object[] args)
                throws Throwable;
    }

    abstract static class ChainedInvocationHandler<V> extends AbstractInvocationHandler {

        private final InvocationHandler invocationHandler;
        private final V proxyObj;

        public ChainedInvocationHandler(DataHierarchyView part, String methodName,
                InvocationHandler handler, Class<V> clazz) {
            super(part, methodName);
            this.invocationHandler = handler;
            proxyObj = createProxy(invocationHandler, clazz);
        }

        @Override
        public Object invoke2(Object proxy, Method method, Object[] args) throws Throwable {
            return proxyObj;
        }

    }

}
