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
package org.eclipse.scanning.test.scan.preprocess;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.SubscanModerator;
import org.junit.BeforeClass;
import org.junit.Test;

public class SubscanModeratorTest {
	
	protected static IPointGeneratorService  gservice;

	@BeforeClass
	public static void setServices() throws Exception {
		gservice    = new PointGeneratorService();
	}
	
	@Test
	public void testSimpleWrappedScan() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"x", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 6 not "+moderated.size(), moderated.size()==6);
		assertTrue(moderator.getInnerModels().size()==1);
	}
	
	@Test
	public void testSimpleWrappedScanSubscanOutside() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(gmodel, new StepModel("T", 290, 300, 2)));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"x", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 150 not "+moderated.size(), moderated.size()==150);
		assertTrue(moderator.getOuterModels().size()==2);
		assertTrue(moderator.getInnerModels().size()==0);
	}

	
	@Test
	public void testSubscanOnlyScan() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"x", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 1 not "+moderated.size(), moderated.size()==1);
		assertTrue(moderator.getOuterModels().size()==0);
		assertTrue(moderator.getInnerModels().size()==1);
	}
	
	@Test
	public void testNoSubscanDevice1() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final MandelbrotModel mmodel = new MandelbrotModel();
		final MandelbrotDetector det = new MandelbrotDetector();
		det.setModel(mmodel);
			
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 25 not "+moderated.size(), moderated.size()==25);
		assertTrue(moderator.getOuterModels()==null);
		assertTrue(moderator.getInnerModels()==null);
	}
	
	@Test
	public void testNoSubscanDevice2() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final MandelbrotModel mmodel = new MandelbrotModel();
		final MandelbrotDetector det = new MandelbrotDetector();
		det.setModel(mmodel);
			
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The size should be 150 not "+moderated.size(), moderated.size()==150);
		assertTrue(moderator.getOuterModels()==null);
		assertTrue(moderator.getInnerModels()==null);
	}


	@Test
	public void testDifferentAxes1() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"p", "y"});
			
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 25 not "+moderated.size(), moderated.size()==25);
		assertTrue(moderator.getOuterModels().size()==1);
		assertTrue(moderator.getInnerModels().size()==0);
	}
	
	@Test
	public void testDifferentAxes2() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("x", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"p", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 150 not "+moderated.size(), moderated.size()==150);
		assertTrue(moderator.getOuterModels().size()==2);
		assertTrue(moderator.getInnerModels().size()==0);
	}
	
	@Test
	public void testDifferentAxes3() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("p", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"x", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 150 not "+moderated.size(), moderated.size()==150);
		assertTrue(moderator.getOuterModels().size()==2);
		assertTrue(moderator.getInnerModels().size()==0);
	}
	
	@Test
	public void testNestedAxes() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		GridModel gmodel = new GridModel("p", "y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("p", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"p", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 1 not "+moderated.size(), moderated.size()==1);
		assertTrue(moderator.getOuterModels().size()==0);
		assertTrue(moderator.getInnerModels().size()==2);
	}

	@Test
	public void testSimpleWrappedScanSpiral() throws Exception {
		
		CompoundModel cmodel = new CompoundModel<>();
		
		SpiralModel gmodel = new SpiralModel("p", "y");
		gmodel.setScale(2d);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		
		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), gmodel));
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);
		
		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[]{"p", "y"});
		
		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);
		IPointGenerator<?> moderated = (IPointGenerator<?>)moderator.getOuterIterable();
		
		assertTrue("The moderated size should be 6 not "+moderated.size(), moderated.size()==6);
		assertTrue(moderator.getOuterModels().size()==1);
		assertTrue(moderator.getInnerModels().size()==1);
	}


	/** TODO
	 * 1. test where all scan is malc
	 * 2. test where none of the scan is malc
	 * 3. test scan where all scan is malc
	 * 4. test scan indices where part of scan is malc.
	 * 5. test a lot of levels of outer scan
	 * 6. test errors with malcolm device during execution
	 */
}
