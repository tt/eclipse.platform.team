/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.sets;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.ui.synchronize.MutableSyncInfoSet;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;

/**
 * This is the superclass for all SyncSet input providers
 */
public abstract class SyncSetInput {
	
	private MutableSyncInfoSet syncSet = new MutableSyncInfoSet();
	private SyncInfoFilter filter = new SyncInfoFilter();
	
	public MutableSyncInfoSet getSyncSet() {
		return syncSet;
	}
	
	/**
	 * This method is invoked from reset to get all the sync information from
	 * the input source.
	 */
	protected abstract void fetchInput(IProgressMonitor monitor) throws TeamException;

	/**
	 * The input is no longer being used. Disconnect it from its source.
	 */
	public abstract void disconnect();
		
	/**
	 * Reset the input. This will clear the current contents of the sync set and
	 * obtain the contents from the input source.
	 */
	public void reset(IProgressMonitor monitor) throws TeamException {
		try {
			syncSet.beginInput();
			syncSet.clear();
			fetchInput(monitor);
		} finally {
			getSyncSet().endInput();
		}
	}

	/**
	 * Collect the change in the provided sync info.
	 */
	protected void collect(SyncInfo info) {
		boolean isOutOfSync = filter.select(info);
		SyncInfo oldInfo = syncSet.getSyncInfo(info.getLocal());
		boolean wasOutOfSync = oldInfo != null;
		if (isOutOfSync) {
			if (wasOutOfSync) {
				syncSet.changed(info);
			} else {
				syncSet.add(info);
			}
		} else if (wasOutOfSync) {
			syncSet.remove(info.getLocal());
		}
	}

	protected void remove(IResource resource)  {
		SyncInfo oldInfo = syncSet.getSyncInfo(resource);
		boolean wasOutOfSync = oldInfo != null;
		if (oldInfo != null) {
			syncSet.remove(resource);
		}
	}
	
	public SyncInfoFilter getFilter() {
		return filter;
	}

	public void setFilter(SyncInfoFilter filter) {
		this.filter = filter;
	}
}
