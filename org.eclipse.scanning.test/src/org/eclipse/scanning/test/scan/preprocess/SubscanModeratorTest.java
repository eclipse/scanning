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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StaticModel;
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

		assertEquals(6, moderated.size());
		assertEquals(1, moderator.getInnerModels().size());
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

		assertEquals(150, moderated.size());
		assertEquals(2, moderator.getOuterModels().size());
		assertEquals(0, moderator.getInnerModels().size());
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

		assertEquals(1, moderated.size());
		assertEquals(0, moderator.getOuterModels().size());
		assertEquals(1, moderator.getInnerModels().size());
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

		assertEquals(25, moderated.size());
		assertNull(moderator.getOuterModels());
		assertNull(moderator.getInnerModels());
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

		assertEquals(150, moderated.size());
		assertNull(moderator.getOuterModels());
		assertNull(moderator.getInnerModels());
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

		assertEquals(1, moderator.getOuterModels().size());
		assertEquals(0, moderator.getInnerModels().size());

		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(25, outer.size());

		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
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

		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(150, outer.size());

		assertEquals(2, moderator.getOuterModels().size());
		assertEquals(0, moderator.getInnerModels().size());

		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
	}

	@Test
	public void testEmptyAxes() throws Exception {

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
		det.setAttributeValue("axesToMove", new String[0]);

		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);

		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(150, outer.size());

		assertEquals(2, moderator.getOuterModels().size());
		assertEquals(0, moderator.getInnerModels().size());

		// check that the inner iterator has a single static point
		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
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

		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(150, outer.size());

		assertEquals(2, moderator.getOuterModels().size());
		assertEquals(0, moderator.getInnerModels().size());

		// check that the inner has a single static position
		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
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

		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(1, outer.size());

		assertEquals(0, moderator.getOuterModels().size());
		assertEquals(2, moderator.getInnerModels().size());

		// check that the outer iterator has a single static position
		Iterator<IPosition> outerIter = outer.iterator();
		IPosition outerPos = outerIter.next();
		assertTrue(outerPos.getNames().isEmpty());
		assertTrue(outerPos.getValues().isEmpty());
		assertFalse(outerIter.hasNext());
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
		IPointGenerator<?> outer = (IPointGenerator<?>)moderator.getOuterIterable();
		assertEquals(6, outer.size());

		assertEquals(1, moderator.getOuterModels().size());
		assertEquals(1, moderator.getInnerModels().size());
	}

	@Test
	public void testStaticScan() throws Exception {
		CompoundModel cmodel = new CompoundModel<>();

		StaticModel smodel = new StaticModel();

		cmodel.setModels(Arrays.asList(smodel));

		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);

		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[0]);

		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);

		assertEquals(0, moderator.getOuterModels().size());
		assertEquals(1, moderator.getInnerModels().size());

		IPointGenerator<?> outer = (IPointGenerator<?>) moderator.getOuterIterable();
		assertEquals(1, outer.size());
		Iterator<IPosition> outerIter = outer.iterator();
		IPosition outerPos = outerIter.next();
		assertTrue(outerPos.getNames().isEmpty());
		assertTrue(outerPos.getValues().isEmpty());
		assertFalse(outerIter.hasNext());

		// check that the inner iterator has a single static position
		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
	}

	@Test
	public void testStaticScan2() throws Exception {
		// the malcolm device's axesToMove is not empty
		CompoundModel cmodel = new CompoundModel<>();

		StaticModel smodel = new StaticModel();

		cmodel.setModels(Arrays.asList(smodel));

		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);

		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[] { "x", "y" });

		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);

		IPointGenerator<?> outer = (IPointGenerator<?>) moderator.getOuterIterable();
		assertEquals(1, outer.size());
		Iterator<IPosition> outerIter = outer.iterator();
		IPosition outerPos = outerIter.next();
		assertTrue(outerPos.getNames().isEmpty());
		assertTrue(outerPos.getValues().isEmpty());
		assertFalse(outerIter.hasNext());

		// check that the inner iterator has a single static position
		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
	}

	@Test
	public void testStaticScanWithOuterScan() throws Exception {
		// the malcolm device's axesToMove is not empty
		CompoundModel cmodel = new CompoundModel<>();

		StaticModel smodel = new StaticModel();

		cmodel.setModels(Arrays.asList(new StepModel("T", 290, 300, 2), smodel));

		IPointGenerator<?> gen = gservice.createCompoundGenerator(cmodel);

		final DummyMalcolmModel tmodel = new DummyMalcolmModel();
		final DummyMalcolmDevice det = new DummyMalcolmDevice();
		det.setModel(tmodel);
		det.setAttributeValue("axesToMove", new String[] { "x", "y" });

		SubscanModerator moderator = new SubscanModerator(gen, Arrays.asList(det), gservice);

		IPointGenerator<?> outer = (IPointGenerator<?>) moderator.getOuterIterable();
		assertEquals(6, outer.size());

		// check that the inner iterator has a single static position
		IPointGenerator<?> inner = (IPointGenerator<?>) moderator.getInnerIterable();
		assertEquals(1, inner.size());
		Iterator<IPosition> innerIter = inner.iterator();
		IPosition innerPos = innerIter.next();
		assertTrue(innerPos.getNames().isEmpty());
		assertTrue(innerPos.getValues().isEmpty());
		assertFalse(innerIter.hasNext());
	}

}
