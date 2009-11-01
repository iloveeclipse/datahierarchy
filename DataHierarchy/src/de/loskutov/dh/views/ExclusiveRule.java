/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.dh.views;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;

import de.loskutov.dh.DataHierarchyPlugin;
import de.loskutov.dh.tree.TreeElement;

class ExclusiveRule implements ISchedulingRule {
    // enable multicore
    private static final boolean MULTICORE = Runtime.getRuntime().availableProcessors() > 1;

    private static final int MAX_JOBS = Runtime.getRuntime().availableProcessors() + 1;

    private final TreeElement<?, ? extends IJavaElement> root;


    public ExclusiveRule(TreeElement<?, ? extends IJavaElement> searchRoot) {
        root = searchRoot;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        if(this == rule){
            return true;
        }
        if(rule instanceof ExclusiveRule) {
            ExclusiveRule rule2 = (ExclusiveRule) rule;
            IJavaElement element = root.getJavaElement();
            IJavaElement element2 = rule2.root.getJavaElement();
            if(element.equals(element2)){
                return true;
            }
            if(MULTICORE) {
                return tooManyJobsThere();
            }
            return true;
        }
        return false;
    }

    private static boolean tooManyJobsThere() {
        Job[] fbJobs = Job.getJobManager().find(DataHierarchyPlugin.JOB_FAMILY);
        int runningCount = 0;
        for (Job job : fbJobs) {
            if(job.getState() == Job.RUNNING){
                runningCount ++;
            }
            if(runningCount > MAX_JOBS){
                return true;
            }
        }
        return false;
    }

    public boolean contains(ISchedulingRule rule) {
        return isConflicting(rule);
    }
}
