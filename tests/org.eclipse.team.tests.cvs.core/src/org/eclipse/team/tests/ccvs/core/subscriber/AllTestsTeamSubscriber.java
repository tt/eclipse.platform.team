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
package org.eclipse.team.tests.ccvs.core.subscriber;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.ui.ModelParticipantSyncInfoSource;

public class AllTestsTeamSubscriber extends EclipseTest {
	
	public AllTestsTeamSubscriber() {
		super();
	}

	public AllTestsTeamSubscriber(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(CVSMergeSubscriberTest.suite());
		suite.addTest(CVSWorkspaceSubscriberTest.suite());
		suite.addTest(CVSCompareSubscriberTest.suite());
		suite.addTest(SyncSetTests.suite());
		suite.addTest(CompareEditorTests.suite());
		//suite.addTest(CVSChangeSetTests.suite());
		CVSSyncSubscriberTest.setSyncSource(new ModelParticipantSyncInfoSource());
		return new CVSTestSetup(suite);
	}
}
