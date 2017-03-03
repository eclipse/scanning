/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.test.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.richbeans.test.ui.ShellTest;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.device.ui.device.ScannableViewer;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This Test class duplicates the setup of ScannableViewerTest to run a single test which destroys the state
 * required by other tests in ScannableViewerTest, since individual tests are not fully isolated from each other.
 *
 * Ideally, I would have just added a unit test for ScannableContentProvider, but it has no public interface to
 * test against.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ScannableViewerTest2 extends ShellTest {
	
	@BeforeClass
	public static void createServices() throws Exception {	
		UISuite.createTestServices(true);
	}
	
	@AfterClass
	public static void disposeServices() throws Exception {	
		UISuite.disposeTestServices();
	}
	
	private ScannableViewer viewer;

	@Override
	protected Shell createShell(Display display) throws Exception {
		
		this.viewer = new ScannableViewer();
	
		Shell shell = new Shell(display);
		shell.setText("Monitors");
		shell.setLayout(new GridLayout(1, false));
        viewer.createPartControl(shell);
		
		shell.pack();
		shell.setSize(500, 500);
		shell.open();

		return shell;
	}


	@Test
	public void somethingFromNothing() throws Exception {
		
		try {
			for (String name : Services.getConnector().getScannableNames()) {
				synchExec(()->viewer.setScannableSelected(name));
				synchExec(()->viewer.removeScannable());
			}
			
			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
			synchExec(()->viewer.reset());
	
			assertEquals(0, bot.table(0).rowCount());

			synchExec(()->viewer.addScannable());

			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
			assertEquals(1, bot.table(0).rowCount());
		} finally {
			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
		}
	}
}
