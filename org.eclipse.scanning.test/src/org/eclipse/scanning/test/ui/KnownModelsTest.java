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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.richbeans.test.ui.ShellTest;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.api.points.models.CollatedStepModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.LissajousModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.ui.auto.IInterfaceService;
import org.eclipse.scanning.api.ui.auto.IModelViewer;
import org.eclipse.scanning.api.ui.auto.InterfaceInvalidException;
import org.eclipse.scanning.device.ui.model.InterfaceService;
import org.eclipse.scanning.example.detector.ConstantVelocityModel;
import org.eclipse.scanning.example.detector.DarkImageModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class KnownModelsTest extends ShellTest{
	
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

	private IModelViewer<Object> viewer;

	@Override
	protected Shell createShell(Display display) throws Exception {
				
		this.viewer = interfaceService.createModelViewer();

		Shell shell = new Shell(display);
		shell.setText("Point Model");
		shell.setLayout(new GridLayout(1, false));
        viewer.createPartControl(shell);
		
		shell.pack();
		shell.setSize(500, 500);
		shell.open();

		return shell;
	}

	@Test
	public void checkShell() throws Exception {
		assertNotNull(bot.shell("Point Model"));
	}

	@Test
	public void testVariousPointsModels() throws Exception {
		assertNotNull(bot.shell("Point Model"));
		
		List<ModelTest> models = createTestPointsModels();
		testModels(models);
	}

	private void testModels(List<ModelTest> models) {
		for (ModelTest tcase : models) {
			bot.shell("Point Model").display.syncExec(()->{
				try {
					viewer.setModel(tcase.getModel());
				} catch (InterfaceInvalidException e) {
					e.printStackTrace();
				}
			});
			
			String className = tcase.getModel().getClass().getSimpleName();
			assertEquals("Checking editable fields of "+className, tcase.getFieldCount(), bot.table(0).rowCount());
			System.out.println(className+" Passed");
		}
	}

	private List<ModelTest> createTestPointsModels() {
		List<ModelTest> models = new ArrayList<>();
		models.add(new ModelTest(new StepModel("x", 0, 10, 1), 4));
		models.add(new ModelTest(new CollatedStepModel(0, 10, 1, "x1", "y1"), 5));
		models.add(new ModelTest(new ArrayModel(0,1,2,3,4,5,6,7,8,9), 1));
		models.add(new ModelTest(new GridModel("x", "y"), 6));	
		models.add(new ModelTest(new RasterModel("x", "y"), 6));
		models.add(new ModelTest(new SpiralModel("x", "y", 2, null), 4));
		models.add(new ModelTest(new LissajousModel(), 8));
		return models;
	}
	
	@Test
	public void testVariousDetectorModels() throws Exception {
		assertNotNull(bot.shell("Point Model"));
		
		List<ModelTest> models = createTestDetectorModels();
		testModels(models);

	}
	
	private List<ModelTest> createTestDetectorModels() {
		List<ModelTest> models = new ArrayList<>();
		models.add(new ModelTest(new MandelbrotModel("x", "y"), 17));
		models.add(new ModelTest(new DarkImageModel(), 4));
		models.add(new ModelTest(new ConstantVelocityModel(), 9));
		return models;
	}

	private class ModelTest {
		private Object model;
		private int fieldCount;
		public ModelTest(Object model, int fieldCount) {
			super();
			this.model = model;
			this.fieldCount = fieldCount;
		}
		public Object getModel() {
			return model;
		}
		public void setModel(Object model) {
			this.model = model;
		}
		public int getFieldCount() {
			return fieldCount;
		}
		public void setFieldCount(int fieldCount) {
			this.fieldCount = fieldCount;
		}
	}

}
