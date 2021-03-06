/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

/**
 * This operation checks out a multiple remote folders into the workspace.
 * Each one will become a new project (overwritting any exsiting projects
 * with the same name).
 */
public class CheckoutMultipleProjectsOperation extends CheckoutProjectOperation {

	boolean hasTargetLocation;
	//The working set to add all of the projects to
	IWorkingSet[] workingSets;
	
	public CheckoutMultipleProjectsOperation(IWorkbenchPart part, ICVSRemoteFolder[] remoteFolders, String targetLocation) {
		this(part,remoteFolders,targetLocation, null);
	}
	
	public CheckoutMultipleProjectsOperation(IWorkbenchPart part, ICVSRemoteFolder[] remoteFolders, String targetLocation, IWorkingSet[] workingSets) {
		super(part, remoteFolders, targetLocation);
		hasTargetLocation = targetLocation != null;
		setInvolvesMultipleResources(remoteFolders.length > 1);
		this.workingSets=workingSets;
	}
	
	/**
	 * Return the target location where the given project should be located or
	 * null if the default location should be used.
	 * 
	 * @param project
	 */
	protected IPath getTargetLocationFor(IProject project) {
		IPath targetLocation = super.getTargetLocationFor(project);
		if (targetLocation == null) return null;
		return targetLocation.append(project.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CheckoutOperation#checkout(org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus checkout(ICVSRemoteFolder folder, IProgressMonitor monitor) throws CVSException {
		return checkout(folder, null, monitor);
	}
	
	protected IWorkingSet[] getWorkingSets(){
		return workingSets;
	}
	
}
