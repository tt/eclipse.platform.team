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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.TeamSubscriberSyncInfoCollector;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.ui.synchronize.SyncInfoDiffNode;
import org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration;
import org.eclipse.team.ui.synchronize.actions.RefreshAction;

/**
 * Provides compare specific support
 */
public class CVSLocalCompareConfiguration extends SyncInfoSetCompareConfiguration {

	private CVSCompareSubscriber subscriber;
	private TeamSubscriberSyncInfoCollector collector;
	private RefreshAction refreshAction;

	/**
	 * Return a <code>SyncInfoSetCompareConfiguration</code> that can be used in a
	 * <code>SyncInfoSetCompareInput</code> to show the comparsion between the local
	 * workspace resources and their tagged counterparts on the server.
	 * @param resources the resources to be compared
	 * @param tag the tag to be compared with
	 * @return a configuration for a <code>SyncInfoSetCompareInput</code>
	 */
	public static CVSLocalCompareConfiguration create(IResource[] resources, CVSTag tag) {
		CVSCompareSubscriber subscriber = new CVSCompareSubscriber(resources, tag);
		TeamSubscriberSyncInfoCollector collector = new TeamSubscriberSyncInfoCollector(subscriber);
		return new CVSLocalCompareConfiguration(subscriber, collector);
	}
	
	public CVSLocalCompareConfiguration(CVSCompareSubscriber subscriber, TeamSubscriberSyncInfoCollector collector) {
		super("org.eclipse.team.cvs.ui.compare-participant", collector.getSyncInfoSet());
		this.subscriber = subscriber;
		this.collector = collector;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#dispose()
	 */
	protected void dispose() {
		collector.dispose();
		subscriber.dispose();
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public SyncInfoDiffNode prepareInput(IProgressMonitor monitor) throws TeamException {
		subscriber.refresh(subscriber.roots(), IResource.DEPTH_INFINITE, monitor);
		collector.waitForCollector(monitor);
		return super.prepareInput(monitor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#fillContextMenu(org.eclipse.jface.viewers.StructuredViewer, org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		manager.add(refreshAction);
		manager.add(new Separator());
		super.fillContextMenu(viewer, manager);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#initializeViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.viewers.StructuredViewer)
	 */
	public void initializeViewer(Composite parent, StructuredViewer viewer) {
		super.initializeViewer(parent, viewer);
		initializeActions(viewer);
	}

	private void initializeActions(StructuredViewer viewer) {
		refreshAction = new RefreshAction(viewer, ((CVSSyncTreeSubscriber)collector.getTeamSubscriber()).getName(), collector, null /* no listener */, false);
		
	}
}
