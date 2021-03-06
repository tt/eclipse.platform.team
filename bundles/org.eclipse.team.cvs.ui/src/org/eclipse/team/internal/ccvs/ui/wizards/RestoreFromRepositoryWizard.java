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
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;

/**
 * This wizard allows the user to show deleted resources in the history view
 */
public class RestoreFromRepositoryWizard extends Wizard {

	private RestoreFromRepositoryFileSelectionPage fileSelectionPage;
	private IContainer parent;
	private ICVSFile[] files;
	
	/**
	 * Constructor for RestoreFromRepositoryWizard.
	 */
	public RestoreFromRepositoryWizard(IContainer parent, ICVSFile[] files) {
		this.parent = parent;
		this.files = files;
		setWindowTitle(CVSUIMessages.RestoreFromRepositoryWizard_fileSelectionPageTitle); 
	}

	/**
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		return fileSelectionPage.restoreSelectedFiles();
	}
	
	/**
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);
		ImageDescriptor substImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_CHECKOUT);
		
		fileSelectionPage = new RestoreFromRepositoryFileSelectionPage("FileSelectionPage", CVSUIMessages.RestoreFromRepositoryWizard_fileSelectionPageTitle, substImage, CVSUIMessages.RestoreFromRepositoryWizard_fileSelectionPageDescription); //$NON-NLS-1$  
		fileSelectionPage.setInput(parent, files);
		addPage(fileSelectionPage);
	}
}
