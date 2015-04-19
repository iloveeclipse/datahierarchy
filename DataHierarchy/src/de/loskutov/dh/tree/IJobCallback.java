/*******************************************************************************
 * Copyright (c) 2009 - 2015 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.dh.tree;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;


public interface IJobCallback {
    public Job sheduleJob(final DelayedJavaElement<?, ? extends IJavaElement> searchRoot);
}
