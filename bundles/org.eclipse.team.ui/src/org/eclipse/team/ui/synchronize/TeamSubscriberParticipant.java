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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.actions.RefreshAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.actions.SyncInfoFilter;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronize participant that displays synchronization information for local
 * resources that are managed via a {@link TeamSubscriber}.
 *
 * @since 3.0
 */
public abstract class TeamSubscriberParticipant extends AbstractSynchronizeParticipant implements IPropertyChangeListener {
	
	private SyncInfoCollector collector;
	
	private SyncInfoSetCollector filteredSyncSet;
	
	private RefreshSchedule refreshSchedule;
	
	private int currentMode;
	
	private IWorkingSet workingSet;
	
	/**
	 * Key for settings in memento
	 */
	private static final String CTX_SUBSCRIBER_PARTICIPANT_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBERSETTINGS"; //$NON-NLS-1$
	
	/**
	 * Key for schedule in memento
	 */
	private static final String CTX_SUBSCRIBER_SCHEDULE_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBER_REFRESHSCHEDULE"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_WORKINGSET = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_WORKINGSET";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the schedule of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_SCHEDULE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_SCHEDULE";	 //$NON-NLS-1$
	
	/**
	 * Property constant indicating the mode of a page has changed. 
	 */
	public static final String P_SYNCVIEWPAGE_MODE = TeamUIPlugin.ID  + ".P_SYNCVIEWPAGE_MODE";	 //$NON-NLS-1$
		
	/**
	 * Modes are direction filters for the view
	 */
	public final static int INCOMING_MODE = 0x1;
	public final static int OUTGOING_MODE = 0x2;
	public final static int BOTH_MODE = 0x4;
	public final static int CONFLICTING_MODE = 0x8;
	public final static int ALL_MODES = INCOMING_MODE | OUTGOING_MODE | CONFLICTING_MODE | BOTH_MODE;
	
	public final static int[] INCOMING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING};
	public final static int[] OUTGOING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.OUTGOING};
	public final static int[] BOTH_MODE_FILTER = new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING, SyncInfo.OUTGOING};
	public final static int[] CONFLICTING_MODE_FILTER = new int[] {SyncInfo.CONFLICTING};
	
	public TeamSubscriberParticipant() {
		super();
		refreshSchedule = new RefreshSchedule(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public IPageBookViewPage createPage(ISynchronizeView view) {
		return new TeamSubscriberParticipantPage(this, view);
	}
	
	public void setMode(int mode) {
		int oldMode = getMode();
		if(oldMode == mode) return;
		currentMode = mode;
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCVIEW_SELECTED_MODE, mode);
		updateMode(mode);
		firePropertyChange(this, P_SYNCVIEWPAGE_MODE, new Integer(oldMode), new Integer(mode));
	}
	
	public int getMode() {
		return currentMode;
	}
	
	public void setRefreshSchedule(RefreshSchedule schedule) {
		this.refreshSchedule = schedule;
		firePropertyChange(this, P_SYNCVIEWPAGE_SCHEDULE, null, schedule);
	}
	
	public RefreshSchedule getRefreshSchedule() {
		return refreshSchedule;
	}
	
	public void setWorkingSet(IWorkingSet set) {
		IWorkingSet oldSet = workingSet;
		if(filteredSyncSet != null) {
			IResource[] resources = set != null ? Utils.getResources(set.getElements()) : new IResource[0];
			filteredSyncSet.setWorkingSet(resources);
			workingSet = null;
		} else {
			workingSet = set;
		}
		firePropertyChange(this, P_SYNCVIEWPAGE_WORKINGSET, oldSet, set);
	}
	
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}
	
	public void refreshWithRemote(IResource[] resources) {
		if((resources == null || resources.length == 0)) {
			RefreshAction.run(filteredSyncSet.getWorkingSet(), this);
		} else {
			RefreshAction.run(resources, this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#dispose()
	 */
	public void dispose() {
		refreshSchedule.dispose();				
		TeamUI.removePropertyChangeListener(this);
		filteredSyncSet.dispose();
		collector.dispose();
	}
	
	public final SyncInfoSetCollector getSyncInfoSetCollector() {
		return filteredSyncSet; 
	}
	
	public final SyncInfoCollector getSyncInfoCollector() {
		return collector;
	}
	
	protected void setSubscriber(TeamSubscriber subscriber) {
		collector = new SyncInfoCollector(subscriber, true);
		filteredSyncSet = new SyncInfoSetCollector(collector.getSyncInfoSet(), null /* no initial roots */, null /* no initial filter */);

		// listen for global ignore changes
		TeamUI.addPropertyChangeListener(this);
		
		if(workingSet != null) {
			setWorkingSet(workingSet);
		}
		updateMode(getMode());
	}
	
	protected TeamSubscriber getSubscriber() {
		return collector.getSubscriber();
	}
		
	/* (non-Javadoc)
	 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
			try {
				collector.reset(null);
			} catch (TeamException e) {
				TeamUIPlugin.log(e);
			}
		}	
	}
	
	private void updateMode(int mode) {
		if(filteredSyncSet != null) {	
		
		int[] modeFilter = BOTH_MODE_FILTER;
		switch(mode) {
		case TeamSubscriberParticipant.INCOMING_MODE:
			modeFilter = INCOMING_MODE_FILTER; break;
		case TeamSubscriberParticipant.OUTGOING_MODE:
			modeFilter = OUTGOING_MODE_FILTER; break;
		case TeamSubscriberParticipant.BOTH_MODE:
			modeFilter = BOTH_MODE_FILTER; break;
		case TeamSubscriberParticipant.CONFLICTING_MODE:
			modeFilter = CONFLICTING_MODE_FILTER; break;
		}

			getSyncInfoSetCollector().setFilter(
					new SyncInfoFilter.AndSyncInfoFilter(
							new SyncInfoFilter[] {
									new SyncInfoFilter.SyncInfoDirectionFilter(modeFilter), 
									new SyncInfoFilter.SyncInfoChangeTypeFilter(new int[] {SyncInfo.ADDITION, SyncInfo.DELETION, SyncInfo.CHANGE}),
									new SyncInfoFilter.PseudoConflictFilter()
							}), new NullProgressMonitor());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(IMemento memento) throws PartInitException {
		if(memento != null) {
			IMemento settings = memento.getChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
			if(settings != null) {
				String set = settings.getString(P_SYNCVIEWPAGE_WORKINGSET);
				String mode = settings.getString(P_SYNCVIEWPAGE_MODE);
				RefreshSchedule schedule = RefreshSchedule.init(settings.getChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS), this);
				setRefreshSchedule(schedule);
				
				if(set != null) {
					IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(set);
					if(workingSet != null) {
						setWorkingSet(workingSet);
					}
				}
				setMode(Integer.parseInt(mode));
			}
		} else {
			setMode(BOTH_MODE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		IMemento settings = memento.createChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
		IWorkingSet set = getWorkingSet();
		if(set != null) {
			settings.putString(P_SYNCVIEWPAGE_WORKINGSET, getWorkingSet().getName());
		}
		settings.putString(P_SYNCVIEWPAGE_MODE, Integer.toString(getMode()));
		refreshSchedule.saveState(settings.createChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS));
	}
}