/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * This action marks the local resource as merged by updating the base
 * resource revision to match the remote resource revision
 */
public class ConfirmMergedOperation extends CVSSubscriberOperation {

	public ConfirmMergedOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamOperation#getJobName()
	 */
	protected String getJobName() {
		SyncInfoSet syncSet = getSyncInfoSet();
		return NLS.bind(CVSUIMessages.SubscriberConfirmMergedAction_jobName, new String[] { new Integer(syncSet.size()).toString() }); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation#run(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void runWithProjectRule(IProject project, SyncInfoSet syncSet, IProgressMonitor monitor) throws CVSException {
		SyncInfo[] syncResources = syncSet.getSyncInfos();
		monitor.beginTask(null, 100 * syncResources.length);
		try {
			for (int i = 0; i < syncResources.length; i++) {
				SyncInfo info = syncResources[i];
				if (!makeOutgoing(info, Policy.subMonitorFor(monitor, 100))) {
					// Failure was logged in makeOutgoing
				}
			}
		} catch (TeamException e) {
			handle(e);
		} finally {
			monitor.done();
		}
	}

	private boolean makeOutgoing(SyncInfo info, IProgressMonitor monitor) throws CVSException, TeamException {
		monitor.beginTask(null, 100);
		try {
			CVSSyncInfo cvsInfo = getCVSSyncInfo(info);
			if (cvsInfo == null) {
				CVSUIPlugin.log(IStatus.ERROR, NLS.bind(CVSUIMessages.SubscriberConfirmMergedAction_0, new String[] { info.getLocal().getFullPath().toString() }), null); 
				return false;
			}
			// Make sure the parent is managed
			ICVSFolder parent = CVSWorkspaceRoot.getCVSFolderFor(cvsInfo.getLocal().getParent());
			if (!parent.isCVSFolder()) {
				// the parents must be made outgoing before the child can
				SyncInfo parentInfo = cvsInfo.getSubscriber().getSyncInfo(parent.getIResource());
				if (!makeOutgoing(parentInfo, Policy.subMonitorFor(monitor, 20))) {
					return false;
				}
			}
			IStatus status = cvsInfo.makeOutgoing(Policy.subMonitorFor(monitor, 80));
			if (status.getSeverity() == IStatus.ERROR) {
				logError(status);
				return false;
			}
			return true;
		} finally {
			monitor.done();
		}
	}
}
