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
package org.eclipse.team.internal.ui.synchronize.views;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.synchronize.sets.SyncSet;

/**
 * A compressed folder appears under a project and contains out-of-sync resources
 */
public class CompressedFolder extends SynchronizeViewNode {

	public CompressedFolder(SyncSet input, IResource resource) {
		super(input, resource);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.sync.views.SynchronizeViewNode#getOutOfSyncDescendants()
	 */
	public SyncInfo[] getChildSyncInfos() {
		IResource[] children = getSyncSet().members(getResource());
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			SyncInfo info = getSyncSet().getSyncInfo(child);
			if (info != null) {
				if (child.getType() == IResource.FOLDER) {
					// for folders, add all out-of-sync children
					// NOTE: the method getOutOfSyncDescendants includes the out-of-sync parent
					result.addAll(Arrays.asList(getSyncSet().getOutOfSyncDescendants(child)));
				} else {
					// for files, just add the info
					result.add(info);
				}
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
	}
}
