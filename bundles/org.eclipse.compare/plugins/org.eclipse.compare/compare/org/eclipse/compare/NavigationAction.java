/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import java.util.ResourceBundle;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Utilities;


/**
 * A <code>NavigationAction</code> is used to navigate through the individual
 * differences of a <code>CompareEditorInput</code>.
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 * @since 2.0
 */
public class NavigationAction extends Action {
	
	private boolean fNext;
	private CompareEditorInput fCompareEditorInput;
	
	
	/**
	 * Creates a <code>NavigationAction</code>.
	 *
	 * @param next if <code>true</code> action goes to the next difference; otherwise to the previous difference.
	 */
	public NavigationAction(boolean next) {
		this(CompareUI.getResourceBundle(), next);
	}

	/**
	 * Creates a <code>NavigationAction</code> that initializes its attributes
	 * from the given <code>ResourceBundle</code>.
	 *
	 * @param bundle is used to initialize the action
	 * @param next if <code>true</code> action goes to the next difference; otherwise to the previous difference.
	 */
	public NavigationAction(ResourceBundle bundle, boolean next) {
		Utilities.initAction(this, bundle, next ? "action.Next." : "action.Previous."); //$NON-NLS-2$ //$NON-NLS-1$
		fNext= next;
	}

	public void run() {
		if (fCompareEditorInput != null) {
			Object adapter= fCompareEditorInput.getAdapter(ICompareNavigator.class);
			if (adapter instanceof ICompareNavigator) {
				boolean atEnd= ((ICompareNavigator)adapter).selectChange(fNext);
				Shell shell= CompareUIPlugin.getShell();
				if (atEnd && shell != null) {
					
					Display display= shell.getDisplay();
					if (display != null)
						display.beep();

					String title;
					String message;
					if (fNext) {
						title= CompareMessages.getString("CompareNavigator.atEnd.title"); //$NON-NLS-1$
						message= CompareMessages.getString("CompareNavigator.atEnd.message"); //$NON-NLS-1$
					} else {
						title= CompareMessages.getString("CompareNavigator.atBeginning.title"); //$NON-NLS-1$
						message= CompareMessages.getString("CompareNavigator.atBeginning.message"); //$NON-NLS-1$
					}
					MessageDialog.openInformation(shell, title, message);
				}
			}
		}
	}
	
	/**
	 * Sets the <code>CompareEditorInput</code> on which this action operates.
	 * 
	 * @param input the <code>CompareEditorInput</code> on which this action operates; if <code>null</code> action does nothing
	 */
	public void setCompareEditorInput(CompareEditorInput input) {
		fCompareEditorInput= input;
	}
}
