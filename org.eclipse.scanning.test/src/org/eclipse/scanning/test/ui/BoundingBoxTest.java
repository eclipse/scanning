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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.richbeans.test.ui.ShellTest;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.ui.auto.IInterfaceService;
import org.eclipse.scanning.api.ui.auto.IModelDialog;
import org.eclipse.scanning.api.ui.auto.IModelViewer;
import org.eclipse.scanning.device.ui.model.InterfaceService;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class BoundingBoxTest extends ShellTest{
	
	private static IInterfaceService interfaceService; // We really get this from OSGi services!
	
	@BeforeClass
	public static void createServices() throws Exception {	
		interfaceService = new InterfaceService(); // Just for testing! This comes from OSGi really.
		UISuite.createTestServices(false);
	}
	
	@AfterClass
	public static void disposeServices() throws Exception {	
		interfaceService = null;
		UISuite.disposeTestServices();
	}

	private BoundingBox    bbox;
	private IModelViewer<Object> viewer;

	@Override
	protected Shell createShell(Display display) throws Exception {
		
		this.bbox = new BoundingBox();
		bbox.setFastAxisName("stage_x");
		bbox.setSlowAxisName("T");
	    bbox.setFastAxisStart(0);
		bbox.setSlowAxisStart(1);
		bbox.setFastAxisLength(10);
		bbox.setSlowAxisLength(11);
		bbox.setRegionName("fred");
		
		this.viewer = interfaceService.createModelViewer();

		Shell shell = new Shell(display);
		shell.setText("Bounding Box");
		shell.setLayout(new GridLayout(1, false));
        viewer.createPartControl(shell);
		viewer.setModel(bbox);
		
		shell.pack();
		shell.setSize(500, 500);
		shell.open();

		return shell;
	}
	

	@Test
	public void checkShell() throws Exception {
		assertNotNull(bot.shell("Bounding Box"));
	}
	
	@Test
	public void testDialog() throws Exception {
		Shell shell = bot.shell("Bounding Box").widget;
		
		List<Exception> errors = new ArrayList<>();
 		shell.getDisplay().syncExec(()->{
 			try {
 				// Intentionally loose generics here because the
 				// ModelCellEditor cannot type check so we mimik
 				// what it does to reproduce the issue that BoundingBox
 				// was not serializable
 				IModelDialog dialog = interfaceService.createModelDialog(shell);
 				dialog.create();
				dialog.setModel((Serializable)bbox);
 			} catch (Exception ne) {
 				errors.add(ne);
 			}
		});
 		if (errors.size()>0) throw errors.get(0);
	}
	
	@Test
	public void checkInitialValues() throws Exception {
		
		// stage_x is mm and T is K. This tests picking up the units from the scannable!
		assertEquals(bbox.getFastAxisName(),             bot.table(0).cell(0, 1));
		assertEquals(bbox.getFastAxisStart()+" mm",      bot.table(0).cell(1, 1));
		assertEquals(bbox.getFastAxisLength()+" mm",     bot.table(0).cell(2, 1));
		assertEquals(bbox.getSlowAxisName(),             bot.table(0).cell(3, 1));
		assertEquals(bbox.getSlowAxisStart()+" K",       bot.table(0).cell(4, 1));
		assertEquals(bbox.getSlowAxisLength()+" K",      bot.table(0).cell(5, 1));

	}

	@Test(expected=Exception.class)
	public void checkNameIneditable() throws Exception {
		
		// stage_x is mm and T is K. This tests picking up the units from the scannable!
		assertEquals(bbox.getFastAxisName(),             bot.table(0).cell(0, 1));
		bot.table(0).click(0, 1); // Make the file editor
		
		// This does pass if fast axis name is editable, I checked by doing that in BoundingBox.
		SWTBotText text = bot.performWithTimeout(()->bot.text(0), 500);
		assertNull(text);

	}

	@Test
	public void checkSettingFastValue() throws Exception {
		
		// stage_x is mm and T is K. This tests picking up the units from the scannable!
		assertEquals(bbox.getFastAxisStart()+" mm",      bot.table(0).cell(1, 1));
		bot.table(0).click(1, 1); // Make the file editor
		
		SWTBotText text = bot.text(0);
		assertNotNull(text);
		
		text.setText("10");
		text.display.syncExec(()->viewer.applyEditorValue());
		
		assertEquals("10.0 mm", bbox.getFastAxisStart()+" mm");
	}

}
