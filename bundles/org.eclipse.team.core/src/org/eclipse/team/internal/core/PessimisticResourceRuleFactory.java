/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A pessimistic rule factory used to ensure older repository providers
 * are not broken by new scheduling rule locking. The workspace root
 * is returned for all rules.
 */
public class PessimisticResourceRuleFactory extends ResourceRuleFactory {

	IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

	@Override
	public ISchedulingRule copyRule(IResource source, IResource destination) {
		return root;
	}
	@Override
	public ISchedulingRule createRule(IResource resource) {
		return root;
	}
	@Override
	public ISchedulingRule deleteRule(IResource resource) {
		return root;
	}
	@Override
	public ISchedulingRule modifyRule(IResource resource) {
		return root;
	}
	@Override
	public ISchedulingRule moveRule(IResource source, IResource destination) {
		return root;
	}
	@Override
	public ISchedulingRule refreshRule(IResource resource) {
		return root;
	}
	@Override
	public ISchedulingRule validateEditRule(IResource[] resources) {
		return root;
	}

	@Override
	public ISchedulingRule charsetRule(IResource resource) {
		return root;
	}
}
