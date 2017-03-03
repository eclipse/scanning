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
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.device.ui.device.ScannableViewer;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class ScannableViewerTest extends ShellTest {
	
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
	public void checkShell() throws Exception {
		assertNotNull(bot.shell("Monitors"));
	}
	
	@Test
	public void checkRows() throws Exception {
		assertEquals(6, bot.table(0).rowCount());
	}

	@Test
	public void checkRefresh() throws Exception {

		synchExec(()->viewer.refresh());
		assertEquals(6, bot.table(0).rowCount());
		
		IScannable<?> x = Services.getConnector().getScannable("x");
		x.setActivated(true);
		
		synchExec(()->viewer.refresh());
		assertEquals(7, bot.table(0).rowCount());

		x.setActivated(false);
		synchExec(()->viewer.refresh()); // Does not remove a deselected scannable, they might want to check it again!
		assertEquals(7, bot.table(0).rowCount());
		assertEquals(6, getMonitors().size());
	}
	
	@Test
	public void checkActivate() throws Exception {

		assertEquals(6, bot.table(0).rowCount());
		assertEquals(6, getMonitors().size());
		bot.table(0).click(0, 0); // deselect
		Thread.sleep(500);
		
		assertEquals(6, bot.table(0).rowCount());
		assertEquals(5, getMonitors().size());

		bot.table(0).click(0, 0); // select
		Thread.sleep(500);
		assertEquals(6, getMonitors().size());
		
	}

	@Test
	public void checkDelete() throws Exception {

		assertEquals(6, bot.table(0).rowCount());
		IScannable<?> orig = Services.getConnector().getScannable("monitor0");
		synchExec(()->viewer.setScannableSelected("monitor0"));
		
		IScannable<?> mon = synchExec(()->viewer.getSelection());
		assertEquals(orig.getName(), mon.getName());
		
		synchExec(()->viewer.removeScannable());
		assertFalse(orig.isActivated());
		assertEquals(5, bot.table(0).rowCount());
		
		orig.setActivated(true);
		synchExec(()->viewer.refresh());
		assertEquals(6, bot.table(0).rowCount());
		
	}

	@Test
	public void checkValueChange() throws Exception {

		Thread.sleep(100);
		assertEquals(6, bot.table(0).rowCount());
		
		IScannable<Double> p = Services.getConnector().getScannable("p");		
		assertEquals(p.getPosition()+"    µm", bot.table(0).cell(5, 2));
		
		p.setPosition(11.0);
		synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
		assertEquals(p.getPosition()+"    µm", bot.table(0).cell(5, 2));
		
		p.setPosition(10.0);
		synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
		assertEquals(p.getPosition()+"    µm", bot.table(0).cell(5, 2));
	}

	@Test
	public void nothingThere() throws Exception {
		
		List<IScannable<?>> activated = new ArrayList<>();
		try {
			for (String name : Services.getConnector().getScannableNames()) {
				IScannable<?> scannable = Services.getConnector().getScannable(name);
				if (scannable.isActivated()) activated.add(scannable);
				scannable.setActivated(false);
			}
			
			synchExec(()->viewer.reset());
	
			assertEquals(0, bot.table(0).rowCount());

		} finally {
			for (IScannable<?> scannable : activated) {
				scannable.setActivated(true);
			}
	
			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
		}
	}
	
	@Test
	public void somethingFromNothing() throws Exception {
		
		List<IScannable<?>> activated = new ArrayList<>();
		try {
			for (String name : Services.getConnector().getScannableNames()) {
				IScannable<?> scannable = Services.getConnector().getScannable(name);
				if (scannable.isActivated()) activated.add(scannable);
				scannable.setActivated(false);
			}
			
			synchExec(()->viewer.reset());
	
			assertEquals(0, bot.table(0).rowCount());

			IScannable<?> orig = activated.get(0);
			orig.setActivated(true);
			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
			assertEquals(1, bot.table(0).rowCount());
		
			synchExec(()->viewer.setScannableSelected(orig.getName()));
			
			IScannable<?> mon = synchExec(()->viewer.getSelection());
			assertEquals(orig.getName(), mon.getName());
			
			synchExec(()->viewer.removeScannable());
			assertFalse(orig.isActivated());
			assertEquals(0, bot.table(0).rowCount());
			
		} finally {
			for (IScannable<?> scannable : activated) {
				scannable.setActivated(true);
			}
	
			synchExec(()->viewer.refresh()); // Shouldn't need this! Does not need it in the main UI.
		}
	}

	@Test
	public void checkMonitorRole() throws Exception {

		assertEquals(6, bot.table(0).rowCount());
		
		// The mocks which MockScannableConnector creates.
		IScannable<?> a    = Services.getConnector().getScannable("a");
		a.setMonitorRole(MonitorRole.NONE);
		IScannable<?> mon0 = Services.getConnector().getScannable("monitor0");
		mon0.setMonitorRole(MonitorRole.PER_SCAN);
 	    IScannable<?> mon3 = Services.getConnector().getScannable("monitor3");
		mon3.setMonitorRole(MonitorRole.PER_POINT);
		IScannable<?> mon6 = Services.getConnector().getScannable("monitor6");
		mon6.setMonitorRole(MonitorRole.NONE);
		IScannable<?> mon9 = Services.getConnector().getScannable("monitor9");
		mon9.setMonitorRole(MonitorRole.PER_SCAN);
		IScannable<?> p    = Services.getConnector().getScannable("p");
		p.setMonitorRole(MonitorRole.PER_POINT);
		
		synchExec(()->viewer.refresh());

		SWTBotTable table = bot.table(0);
		assertEquals(MonitorRole.NONE.getLabel(), table.cell(0, 3));
		assertEquals(MonitorRole.PER_SCAN.getLabel(), table.cell(1, 3));
		assertEquals(MonitorRole.PER_POINT.getLabel(), table.cell(2, 3));
		assertEquals(MonitorRole.NONE.getLabel(), table.cell(3, 3));
		assertEquals(MonitorRole.PER_SCAN.getLabel(), table.cell(4, 3));
		assertEquals(MonitorRole.PER_POINT.getLabel(), table.cell(5, 3));
	}
		
	private Collection<String> getMonitors() throws Exception {
		
		final Collection<DeviceInformation<?>> scannables = Services.getConnector().getDeviceInformation();
		final List<String> ret = new ArrayList<String>();
		for (DeviceInformation<?> info : scannables) {
			if (info.isActivated()) ret.add(info.getName());
		}
		return ret;
	}

}
