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
package org.eclipse.team.internal.ccvs.ui.wizards;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.ReconcileProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ShareProjectOperation;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * This wizard helps the user to import a new project in their workspace
 * into a CVS repository for the first time.
 */
public class SharingWizard extends Wizard implements IConfigurationWizard, ICVSWizard {
	// The project to configure
	private IProject project;

	// The autoconnect page is used if CVS/ directories already exist.
	private ConfigurationWizardAutoconnectPage autoconnectPage;
	
	// The import page is used if CVS/ directories do not exist.
	private RepositorySelectionPage locationPage;
	
	// The page that prompts the user for connection information.
	private ConfigurationWizardMainPage createLocationPage;
	
	// The page that prompts the user for module name.
	private ModuleSelectionPage modulePage;
	
	// The page that tells the user what's going to happen.
	private SharingWizardFinishPage finishPage;

	// The page that lets the user pick a branch to share against
	private SharingWizardTagPage tagPage;
	
	// Keep track of location state so we know what to do at the end
	private ICVSRepositoryLocation location;
	private boolean isNewLocation;
	
	// Keep track of the folder that existed the last time we checked
	private ICVSRemoteFolder existingRemote;

	private SharingWizardSyncPage syncPage;
	
	public SharingWizard() {
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("NewLocationWizard");//$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("NewLocationWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
		setWindowTitle(Policy.bind("SharingWizard.title")); //$NON-NLS-1$
		//set
	}	
		
	public void addPages() {
		ImageDescriptor sharingImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_SHARE);
		if (doesCVSDirectoryExist()) {
			autoconnectPage = new ConfigurationWizardAutoconnectPage("autoconnectPage", Policy.bind("SharingWizard.autoConnectTitle"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			autoconnectPage.setProject(project);
			autoconnectPage.setDescription(Policy.bind("SharingWizard.autoConnectTitleDescription")); //$NON-NLS-1$
			addPage(autoconnectPage);
		} else {
			ICVSRepositoryLocation[] locations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
			if (locations.length > 0) {
				locationPage = new RepositorySelectionPage("importPage", Policy.bind("SharingWizard.importTitle"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
				locationPage.setDescription(Policy.bind("SharingWizard.importTitleDescription")); //$NON-NLS-1$
				addPage(locationPage);
			}
			createLocationPage = new ConfigurationWizardMainPage("createLocationPage", Policy.bind("SharingWizard.enterInformation"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			createLocationPage.setDescription(Policy.bind("SharingWizard.enterInformationDescription")); //$NON-NLS-1$
			addPage(createLocationPage);
			createLocationPage.setDialogSettings(getDialogSettings());
			modulePage = new ModuleSelectionPage("modulePage", Policy.bind("SharingWizard.enterModuleName"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			modulePage.setDescription(Policy.bind("SharingWizard.enterModuleNameDescription")); //$NON-NLS-1$
			addPage(modulePage);
			finishPage = new SharingWizardFinishPage("finishPage", Policy.bind("SharingWizard.readyToFinish"), sharingImage); //$NON-NLS-1$ //$NON-NLS-2$
			finishPage.setDescription(Policy.bind("SharingWizard.readyToFinishDescription")); //$NON-NLS-1$
			addPage(finishPage);
			
			addTagPage(sharingImage);
			addSyncPage(sharingImage);
		}
	}
	
	private void addTagPage(ImageDescriptor sharingImage) {
		tagPage = new SharingWizardTagPage("tagPage", 
			Policy.bind("SharingWizard.selectTagTitle"), 
			sharingImage,
			"Select the branch to share the project with");
		addPage(tagPage);
	}
	
	private void addSyncPage(ImageDescriptor sharingImage) {
		syncPage = new SharingWizardSyncPage("syncPagePage", 
			"Share Project Resources", 
			sharingImage,
			"Share individual project resources");
		addPage(syncPage);
	}
	
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		if (page == locationPage) {
			if (locationPage.getLocation() == null) {
				return createLocationPage.isPageComplete();
			} else {
				return modulePage.useProjectName() || modulePage.getModuleName() != null;
			}
		} else if (page == modulePage) {
			ICVSRemoteFolder remoteFolder = getRemoteFolder();
			return (modulePage.isPageComplete() && remoteFolder != null && 
					(existingRemote == null || existingRemote.equals(remoteFolder)));
		} else if (page == tagPage) {
			return false;
		} else if (page == finishPage) {
			return true;
		}
		return super.canFinish();
	}
	protected String getMainPageDescription() {
		return Policy.bind("SharingWizard.description"); //$NON-NLS-1$
	}
	protected String getMainPageTitle() {
		return Policy.bind("SharingWizard.heading"); //$NON-NLS-1$
	}
	public IWizardPage getNextPage(IWizardPage page) {
		// Assume the page is about to be shown when this method is
		// invoked
		return getNextPage(page, true /* about to show*/);
	}
	public IWizardPage getNextPage(IWizardPage page, boolean aboutToShow) {
		if (page == autoconnectPage) return null;
		if (page == locationPage) {
			if (locationPage.getLocation() == null) {
				return createLocationPage;
			} else {
				return modulePage;
			}
		}
		if (page == createLocationPage) {
			return modulePage;
		}
		if (page == modulePage) {
			if (aboutToShow && exists(getRemoteFolder())) {
				tagPage.setFolder(getRemoteFolder());
				return tagPage;
			} else {
				return finishPage;
			}
		}
		if (page == tagPage) {
			return finishPage;
		}
		return null;
	}

	/*
	 * @see IWizard#performFinish
	 */
	public boolean performFinish() {
		final boolean[] result = new boolean[] { true };
		final boolean[] showTagPage = new boolean[] { false };
		final ICVSRemoteFolder remote = getRemoteFolder();
		try {
			final boolean[] doSync = new boolean[] { false };
			getContainer().run(true /* fork */, true /* cancel */, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						if (isAutoconnect()) {
							// Autoconnect to the repository using CVS/ directories
							result[0] = autoconnectCVSProject(monitor);
						} else {
							if (exists(remote, monitor)) {
								if (isOnFinishPage()) {
									result[0] = reconnectProject(monitor);
								} else {
									result[0] = false;
									showTagPage[0] = true;
								}
							} else {
								result[0] = shareProject(monitor);
							}
							doSync[0] = result[0];
						}
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					} catch (InterruptedException e) {
						// must have been cancelled;
						return;
					} finally {
						monitor.done();
					}
				}
			});
			if (showTagPage[0]) {
				tagPage.setFolder(remote);
				getContainer().showPage(tagPage);
				tagPage.setErrorMessage("Module {0} exists on the server" + remote.getRepositoryRelativePath());
			} else {
				if (doSync[0]) {
					// Sync of the project
					WorkspaceSynchronizeParticipant participant = CVSUIPlugin.getPlugin().getCvsWorkspaceSynchronizeParticipant();
					if(participant != null) {
						participant.refresh(new IResource[] {project}, participant.getRefreshListeners().createSynchronizeViewListener(participant), Policy.bind("Participant.synchronizing"), null); //$NON-NLS-1$
					}
				}
			}
		} catch (InterruptedException e) {
			return true;
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getContainer().getShell(), null, null, e);
		}

		return result[0];
	}

	protected boolean isOnFinishPage() {
		return getContainer().getCurrentPage() == finishPage;
	}

	private void mapProject(final String moduleName, final CVSTag tag) throws InvocationTargetException, InterruptedException {
		try {
			ICVSRemoteFolder remote = new RemoteFolder(null, getLocation(), moduleName, tag);
			// TODO: Shoudl have a part but this code will be changing soon anyway
			new ReconcileProjectOperation(null /* part */, project, remote).run();
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		} 
	}
	
	/**
	 * Return an ICVSRepositoryLocation
	 */
	private ICVSRepositoryLocation getLocation() throws TeamException {
		// If there is an autoconnect page then it has the location
		if (autoconnectPage != null) {
			return recordLocation(autoconnectPage.getLocation());
		}
		
		// If the location page has a location, use it.
		if (locationPage != null) {
			ICVSRepositoryLocation newLocation = locationPage.getLocation();
			if (location != null) {
				return recordLocation(newLocation);
			}
		}
		
		// Otherwise, get the location from the create location page
		final ICVSRepositoryLocation[] locations = new ICVSRepositoryLocation[] { null };
		final CVSException[] exception = new CVSException[] { null };
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				try {
					locations[0] = createLocationPage.getLocation();
				} catch (CVSException e) {
					exception[0] = e;
				}
			}
		});
		if (exception[0] != null) {
			throw exception[0];
		}
		return recordLocation(locations[0]);
	}

	private ICVSRepositoryLocation recordLocation(ICVSRepositoryLocation newLocation) {
		if (newLocation == null) return location;
		if (location == null || !newLocation.equals(location)) {
			location = newLocation;
			isNewLocation = !CVSProviderPlugin.getPlugin().isKnownRepository(newLocation.getLocation());
		}
		return location;
	}

	/**
	 * Return the module name.
	 */
	private String getModuleName() {
		// If there is an autoconnect page then it has the module name
		if (autoconnectPage != null) {
			return autoconnectPage.getSharing().getRepository();
		}
		String moduleName = modulePage.getModuleName();
		if (moduleName == null) moduleName = project.getName();
		return moduleName;
	}
	/*
	 * @see IConfigurationWizard#init(IWorkbench, IProject)
	 */
	public void init(IWorkbench workbench, IProject project) {
		this.project = project;
	}
	private boolean doesCVSDirectoryExist() {
		// Determine if there is an existing CVS/ directory from which configuration
		// information can be retrieved.
		Shell shell = null;
		if (getContainer() != null) {
			shell = getContainer().getShell();
		}
		final boolean[] isCVSFolder = new boolean[] { false };
		try {
			CVSUIPlugin.runWithRefresh(shell, new IResource[] { project }, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						ICVSFolder folder = (ICVSFolder)CVSWorkspaceRoot.getCVSResourceFor(project);
						FolderSyncInfo info = folder.getFolderSyncInfo();
						isCVSFolder[0] = info != null;
					} catch (final TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			}, null);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(shell, null, null, e);
		} catch (InterruptedException e) {
			// Cancelled. Just fall through
		}
		return isCVSFolder[0];
	}

	/**
	 * Method findCommonRootInSubfolders.
	 * @return String
	 */
	private void purgeAnyCVSFolders() {
		try {
			ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
			folder.accept(new ICVSResourceVisitor() {
				public void visitFile(ICVSFile file) throws CVSException {
					// nothing to do for files
				}
				public void visitFolder(ICVSFolder folder) throws CVSException {
					if (folder.isCVSFolder()) {
						// for now, just unmanage
						folder.unmanage(null);
					}
				}
			}, true /* recurse */);
		} catch (CVSException e) {
			// log the exception and return null
			CVSUIPlugin.log(e);
		}
	}
	
	/*
	 * Shoudl the project be auto-connected
	 */
	/* private*/ boolean isAutoconnect() {
		return autoconnectPage != null && doesCVSDirectoryExist();
	}
	
	/*
	 * Auto-connect to the repository using CVS/ directories
	 */
	/*private */ boolean autoconnectCVSProject(IProgressMonitor monitor) throws TeamException {
		try {
			monitor.beginTask(null, 100);
			
			FolderSyncInfo info = autoconnectPage.getFolderSyncInfo();
			if (info == null) {
				// Error!
				return false;
			}
			
			// Get the repository location (the get will add the locatin to the provider)
			boolean isPreviouslyKnown = CVSProviderPlugin.getPlugin().isKnownRepository(info.getRoot());
			ICVSRepositoryLocation location = CVSProviderPlugin.getPlugin().getRepository(info.getRoot());
	
			// Validate the connection if the user wants to
			boolean validate = autoconnectPage.getValidate();					
			if (validate) {
				// Do the validation
				try {
					location.validateConnection(Policy.subMonitorFor(monitor, 50));
				} catch (final TeamException e) {
					// Exception validating. We can continue if the user wishes.
					final boolean[] keep = new boolean[] { false };
					getShell().getDisplay().syncExec(new Runnable() {
						public void run() {
							keep[0] = MessageDialog.openQuestion(getContainer().getShell(),
								Policy.bind("SharingWizard.validationFailedTitle"), //$NON-NLS-1$
								Policy.bind("SharingWizard.validationFailedText", new Object[] {e.getStatus().getMessage()})); //$NON-NLS-1$
						}
					});
					if (!keep[0]) {
						// Remove the root
						try {
							if (!isPreviouslyKnown) {
								CVSProviderPlugin.getPlugin().disposeRepository(location);
							}
						} catch (TeamException e1) {
							CVSUIPlugin.openError(getContainer().getShell(), Policy.bind("exception"), null, e1, CVSUIPlugin.PERFORM_SYNC_EXEC); //$NON-NLS-1$
						}
						return false;
					}
					// They want to keep the connection anyway. Fall through.
				}
			}
			
			// Set the sharing
			CVSWorkspaceRoot.setSharing(project, info, Policy.subMonitorFor(monitor, 50));
			return true;
		} finally {
			monitor.done();
		}
	}
	
	private boolean reconnectProject(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final boolean[] result = new boolean[] { true };
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = MessageDialog.openQuestion(getShell(), Policy.bind("SharingWizard.couldNotImport"), Policy.bind("SharingWizard.couldNotImportLong", getModuleName())); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		if (result[0]) {
			mapProject(getModuleName(), getTag());
		}
		return result[0];
	}
	
	private boolean shareProject(IProgressMonitor monitor) throws CVSException, InvocationTargetException, InterruptedException {
		monitor.beginTask(null, 100);
		ICVSRepositoryLocation location = null;
		try {
			location = getLocation();
			location.validateConnection(Policy.subMonitorFor(monitor, 50));
			purgeAnyCVSFolders();
		} catch (TeamException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC);
			if (isNewLocation && location != null) location.flushUserInfo();
			return false;
		}
		// Add the location to the provider if it is new
		if (isNewLocation) {
			CVSProviderPlugin.getPlugin().addRepository(location);
		}
		
		// Create the remote module for the project
		ShareProjectOperation op = new ShareProjectOperation(null, location, project, getModuleName());
		op.setShell(getShell());
		op.run(Policy.subMonitorFor(monitor, 50));
		return true;
	}
	
	private CVSTag getTag() {
		if (tagPage == null || tagPage.getSelectedTag() == null) {
			return CVSTag.DEFAULT;
		}
		return tagPage.getSelectedTag();
	}
	
	private ICVSRemoteFolder getRemoteFolder() {
		try {
			ICVSRepositoryLocation location = getLocation();
			if (location == null) return null;
			return location.getRemoteFolder(getModuleName(), getTag());
		} catch (TeamException e) {
			CVSProviderPlugin.log(e);
			return null;
		}
	}
	
	private boolean exists(ICVSRemoteFolder folder, IProgressMonitor monitor) throws TeamException {
		if (existingRemote != null && existingRemote.equals(folder)) return true;
		if (folder.exists(monitor)) {
			existingRemote = folder;
			return true;
		} else {
			existingRemote = null;
			return false;
		}
	}
	
	private boolean exists(final ICVSRemoteFolder folder) {
		final boolean[] result = new boolean[] { false };
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						result[0] = exists(folder, monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getContainer().getShell(), null, null, e);
		} catch (InterruptedException e) {
			// Cancelled. Assume the folder doesn't exist
		}
		return result[0];
	}
}
