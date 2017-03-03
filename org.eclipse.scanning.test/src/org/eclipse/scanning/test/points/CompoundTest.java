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
package org.eclipse.scanning.test.points;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.PySerializable;
import org.junit.Before;
import org.junit.Test;
import org.python.core.PyDictionary;
import org.python.core.PyList;

public class CompoundTest {
	
	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}

	@Test(expected=org.python.core.PyException.class)
	public void testCompoundCompoundException() throws Exception {

		IPointGenerator<StepModel> pos = service.createGenerator(new StepModel("Position", 1, 4, 0.6));
		
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel model = new GridModel();
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		IPointGenerator<?> scan = service.createCompoundGenerator(pos, gen);
		IPointGenerator<?> scan2 = service.createCompoundGenerator(pos, scan);
		scan2.iterator();
	}
	@Test(expected=org.python.core.PyException.class)
	public void testDuplicateAxisNameException() throws Exception {

		IPointGenerator<StepModel> pos1 = service.createGenerator(new StepModel("Position", 1, 4, 0.6));
		IPointGenerator<StepModel> pos2 = service.createGenerator(new StepModel("Position", 1, 4, 0.6));
		IPointGenerator<?> scan = service.createCompoundGenerator(pos1, pos2);
		scan.iterator().next();
	}
	@Test
	public void testIteratedSize() throws Exception {

		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290,295,1));
		assertEquals(6, temp.size());

		IPointGenerator<StepModel> pos = service.createGenerator(new StepModel("Position", 1,4, 0.6));
		assertEquals(6, pos.size());

		IPointGenerator<?> scan = service.createCompoundGenerator(temp, pos);
		assertTrue(scan.iterator().next()!=null);
		assertEquals(36, scan.size());
		
		Iterator<IPosition> it = scan.iterator();
		int size = scan.size();
		int sz=0;
		while(it.hasNext()) {
			it.next();
			sz++;
			if (sz>size) throw new Exception("Iterator grew too large!");
		}
	}
	
	@Test
	public void testSimpleCompoundStep2Step() throws Exception {
		
		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290,295,1));
		assertEquals(6, temp.size());

		IPointGenerator<StepModel> pos = service.createGenerator(new StepModel("Position", 1,4, 0.6));
		assertEquals(6, pos.size());

		IPointGenerator<?> scan = service.createCompoundGenerator(temp, pos);
		assertEquals(36, scan.size());

		final List<IPosition> points = scan.createPoints();
		
		// 290K
		assertEquals(new Double(290), (Double)points.get(0).get("Temperature"));
		assertEquals(new Double(1),   (Double)points.get(0).get("Position"));
		assertEquals(new Double(290), (Double)points.get(1).get("Temperature"));
		assertEquals(new Double(1.6), (Double)points.get(1).get("Position"));
		assertEquals(new Double(290), (Double)points.get(2).get("Temperature"));
		assertEquals(new Double(2.2), (Double)points.get(2).get("Position"));
		
		// 291K
		assertEquals(new Double(291), (Double)points.get(6).get("Temperature"));
		assertEquals(new Double(1),   (Double)points.get(6).get("Position"));
		assertEquals(new Double(291), (Double)points.get(7).get("Temperature"));
		assertEquals(new Double(1.6), (Double)points.get(7).get("Position"));
		assertEquals(new Double(291), (Double)points.get(8).get("Temperature"));
		assertEquals(new Double(2.2), (Double)points.get(8).get("Position"));
		
		// 295K
		assertEquals(new Double(295), (Double)points.get(30).get("Temperature"));
		assertEquals(new Double(1),   (Double)points.get(30).get("Position"));
		assertEquals(new Double(295), (Double)points.get(31).get("Temperature"));
		assertEquals(new Double(1.6), (Double)points.get(31).get("Position"));
		assertEquals(new Double(295), (Double)points.get(32).get("Temperature"));
		assertEquals(new Double(2.2), (Double)points.get(32).get("Position"));

        GeneratorUtil.testGeneratorPoints(scan);
	}
	
	@Test
	public void testSimpleToDict() throws Exception {
		
		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290, 295, 1));
		IPointGenerator<?> scan = service.createCompoundGenerator(temp);
		
		Map<?,?> dict = ((PySerializable)scan).toDict();
		
		PyList gens = (PyList) dict.get("generators");
		PyDictionary line1 = (PyDictionary) gens.get(0);

		assertEquals("Temperature", (String) ((PyList) line1.get("name")).get(0));
		assertEquals("mm", line1.get("units"));
		assertEquals(290.0, (double) ((PyList) line1.get("start")).get(0), 1E-10);
		assertEquals(295.0, (double) ((PyList) line1.get("stop")).get(0), 1E-10);
		assertEquals(6, (int) line1.get("num"));

		PyList excluders = (PyList) dict.get("excluders");
		PyList mutators = (PyList) dict.get("mutators");
		assertEquals(new PyList(), excluders);
		assertEquals(new PyList(), mutators);
	}
	
	@Test
	public void testNestedToDict() throws Exception {
		
		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290, 295, 1));
		IPointGenerator<StepModel> pos = service.createGenerator(new StepModel("Position", 1, 4, 0.6));
		IPointGenerator<?> scan = service.createCompoundGenerator(temp, pos);
		
		Map<?,?> dict = ((PySerializable)scan).toDict();
		
		PyList gens = (PyList) dict.get("generators");
		PyDictionary line1 = (PyDictionary) gens.get(0);
		PyDictionary line2 = (PyDictionary) gens.get(1);

		assertEquals("Temperature", (String) ((PyList) line1.get("name")).get(0));
		assertEquals("mm", line1.get("units"));
		assertEquals(290.0, (double) ((PyList) line1.get("start")).get(0), 1E-10);
		assertEquals(295.0, (double) ((PyList) line1.get("stop")).get(0), 1E-10);
		assertEquals(6, (int) line1.get("num"));
		
		assertEquals("Position", (String) ((PyList) line2.get("name")).get(0));
		assertEquals("mm", line2.get("units"));
		assertEquals(1.0, (double) ((PyList) line2.get("start")).get(0), 1E-10);
		assertEquals(4.0, (double) ((PyList) line2.get("stop")).get(0), 1E-10);
		assertEquals(6, (int) line2.get("num"));

		PyList excluders = (PyList) dict.get("excluders");
		PyList mutators = (PyList) dict.get("mutators");
		assertEquals(new PyList(), excluders);
		assertEquals(new PyList(), mutators);
	}
	
	@Test
	public void testSimpleCompoundStep3Step() throws Exception {
		
		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290,295,1));
		assertEquals(6, temp.size());

		IPointGenerator<StepModel> y = service.createGenerator(new StepModel("Y", 11, 14, 0.6));
		assertEquals(6, y.size());

		IPointGenerator<StepModel> x = service.createGenerator(new StepModel("X", 1, 4, 0.6));
		assertEquals(6, x.size());
	

		IPointGenerator<?> scan = service.createCompoundGenerator(temp, y, x);
		assertEquals(216, scan.size());

		final List<IPosition> points = scan.createPoints();
		
		// 290K
		assertEquals(new Double(290), (Double)points.get(0).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(0).get("Y"));
		assertEquals(new Double(1),   (Double)points.get(0).get("X"));
		assertEquals(new Double(290), (Double)points.get(1).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(1).get("Y"));
		assertEquals(new Double(1.6), (Double)points.get(1).get("X"));
		assertEquals(new Double(290), (Double)points.get(2).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(2).get("Y"));
		assertEquals(new Double(2.2), (Double)points.get(2).get("X"));
		
		// 291K
		assertEquals(new Double(291), (Double)points.get(36).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(36).get("Y"));
		assertEquals(new Double(1),   (Double)points.get(36).get("X"));
		assertEquals(new Double(291), (Double)points.get(37).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(37).get("Y"));
		assertEquals(new Double(1.6), (Double)points.get(37).get("X"));
		assertEquals(new Double(291), (Double)points.get(38).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(38).get("Y"));
		assertEquals(new Double(2.2), (Double)points.get(38).get("X"));

		// 295K
		assertEquals(new Double(295), (Double)points.get(180).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(180).get("Y"));
		assertEquals(new Double(1),   (Double)points.get(180).get("X"));
		assertEquals(new Double(295), (Double)points.get(181).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(181).get("Y"));
		assertEquals(new Double(1.6), (Double)points.get(181).get("X"));
		assertEquals(new Double(295), (Double)points.get(182).get("Temperature"));
		assertEquals(new Double(11),  (Double)points.get(182).get("Y"));
		assertEquals(new Double(2.2), (Double)points.get(182).get("X"));

        GeneratorUtil.testGeneratorPoints(scan);
	}

	@Test
	public void testSimpleCompoundGrid() throws Exception {
		
		IPointGenerator<StepModel> temp = service.createGenerator(new StepModel("Temperature", 290,300,1));
		assertEquals(11, temp.size());
		
		BoundingBox box = new BoundingBox();		
		box.setFastAxisStart(0);		
		box.setSlowAxisStart(0);		
		box.setFastAxisLength(3);		
		box.setSlowAxisLength(3);		
		
		GridModel model = new GridModel("x", "y");		
		model.setSlowAxisPoints(20);		
		model.setFastAxisPoints(20);		
		model.setBoundingBox(box);		
		
		IPointGenerator<GridModel> grid = service.createGenerator(model);
		
		IPointGenerator<?> scan = service.createCompoundGenerator(temp, grid);
		assertEquals(4400, scan.size());

		List<IPosition> points = scan.createPoints();

		List<IPosition> first400 = new ArrayList<>(400);

		// The first 400 should be T=290
		for (int i = 0; i < 400; i++) {
			assertEquals(new Double(290.0), points.get(i).get("Temperature"));
			first400.add(points.get(i));
		}
		checkPoints(first400);
		
		for (int i = 400; i < 800; i++) {
			assertEquals(new Double(291.0), points.get(i).get("Temperature"));
		}
		for (int i = 4399; i >= 4000; i--) {
			assertEquals(new Double(300.0), points.get(i).get("Temperature"));
		}
        GeneratorUtil.testGeneratorPoints(scan);

	}

	@Test
	public void testGridCompoundGrid() throws Exception {
		
		BoundingBox box = new BoundingBox();		
		box.setFastAxisStart(0);		
		box.setSlowAxisStart(0);		
		box.setFastAxisLength(3);		
		box.setSlowAxisLength(3);		
		
		GridModel model1 = new GridModel();		
		model1.setSlowAxisPoints(5);		
		model1.setFastAxisPoints(5);		
		model1.setBoundingBox(box);	
		model1.setFastAxisName("x");
		model1.setSlowAxisName("y");	
		
		IPointGenerator<GridModel> grid1 = service.createGenerator(model1);	
		
		GridModel model2 = new GridModel();		
		model2.setSlowAxisPoints(5);		
		model2.setFastAxisPoints(5);		
		model2.setBoundingBox(box);	
		model2.setFastAxisName("x2");
		model2.setSlowAxisName("y2");	
		
		IPointGenerator<GridModel> grid2 = service.createGenerator(model2);
		
		IPointGenerator<?> scan = service.createCompoundGenerator(grid1, grid2);
		assertEquals(625, scan.size());
		
        GeneratorUtil.testGeneratorPoints(scan);
	}
	
	private void checkPoints(List<IPosition> pointList) {
		// Check correct number of points
		assertEquals(20 * 20, pointList.size());

		// Check some random points are correct
		assertEquals(0.075, (Double)pointList.get(0).get("x"), 1e-8);
		assertEquals(0.075, (Double)pointList.get(0).get("y"), 1e-8);

		assertEquals(0.075 + 3 * (3.0 / 20.0), (Double)pointList.get(3).get("x"), 1e-8);
		assertEquals(0.075 + 0.0, (Double)pointList.get(3).get("y"), 1e-8);

		assertEquals(0.075 + 2 * (3.0 / 20.0), (Double)pointList.get(22).get("x"), 1e-8);
		assertEquals(0.075 + 1 * (3.0 / 20.0), (Double)pointList.get(22).get("y"), 1e-8);

		assertEquals(0.075 + 10 * (3.0 / 20.0), (Double)pointList.get(350).get("x"), 1e-8);
		assertEquals(0.075 + 17 * (3.0 / 20.0), (Double)pointList.get(350).get("y"), 1e-8);

	}
	
	@Test
	public void testNestedNeXus() throws Exception {
		
		int[] size = {10,8,5};
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setFastAxisPoints(size[size.length-2]);
		gmodel.setSlowAxisName("yNex");
		gmodel.setSlowAxisPoints(size[size.length-1]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		
		IPointGenerator<?> gen = service.createGenerator(gmodel);
		
		// We add the outer scans, if any
		if (size.length > 2) {
			IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length - 1];
			for (int dim = size.length-3; dim>-1; dim--) {
				final StepModel model = new StepModel("neXusScannable"+dim, 10,20,11/size[dim]);
				gens[dim] = service.createGenerator(model);
			}
			gens[size.length - 2] = gen;
			gen = service.createCompoundGenerator(gens);
		}
		
		final IPosition pos = gen.iterator().next();
		assertEquals(size.length, pos.size());
		
	}
}
