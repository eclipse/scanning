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
package org.eclipse.scanning.test.epics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.scanning.api.points.IMutator;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.LissajousModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.connector.epics.EpicsV4ConnectorService;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.mutators.RandomOffsetMutator;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Union;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for serialisation into EPICS V4 structures for transmission over PVAccess
 * @author Matt Taylor
 *
 */
public class PVDataSerializationTest {

	EpicsV4ConnectorService connectorService;

	@Before
	public void create() throws Exception {
		this.connectorService = new EpicsV4ConnectorService();
	}
	
	@Test
	public void testCircularROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		regions.add(new CircularROI(2, 6, 7));
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();

		Structure expectedCircularRoiStructure = fieldCreate.createFieldBuilder().
				addArray("centre", ScalarType.pvDouble).
				add("radius", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/CircularROI:1.0").					
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				createStructure();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				add("duration", ScalarType.pvDouble).
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);
		
		PVStructure expectedROIPVStructure = pvDataCreate.createPVStructure(expectedCircularRoiStructure);
		PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray centreVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {6, 7};
		centreVal.put(0, centre.length, centre, 0);
		PVDouble radiusVal = expectedROIPVStructure.getSubField(PVDouble.class, "radius");
		radiusVal.put(2);

		PVUnion[] roiArray = new PVUnion[1];
		roiArray[0] = pvDataCreate.createPVUnion(union);
		roiArray[0].set(expectedROIPVStructure);
		rois.put(0, roiArray.length, roiArray, 0);

		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray excluders = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
				
		excluders.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}
	
	@Test
	public void testEllipticalROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		EllipticalROI eRoi = new EllipticalROI();
		eRoi.setPoint(3, 4);
		eRoi.setAngle(1.5);
		eRoi.setSemiAxes(new double[]{7, 8});
		regions.add(eRoi);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedEllipticalRoiStructure = fieldCreate.createFieldBuilder().
				addArray("semiaxes", ScalarType.pvDouble).
				addArray("centre", ScalarType.pvDouble).
				add("angle", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/EllipticalROI:1.0").
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				createStructure();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);
		
		PVStructure expectedROIPVStructure = pvDataCreate.createPVStructure(expectedEllipticalRoiStructure);
		PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray semiaxesVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "semiaxes");
		double[] semiaxes = new double[] {7, 8};
		semiaxesVal.put(0, semiaxes.length, semiaxes, 0);
		PVDoubleArray centreVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {3, 4};
		centreVal.put(0, centre.length, centre, 0);
		PVDouble angleVal = expectedROIPVStructure.getSubField(PVDouble.class, "angle");
		angleVal.put(1.5);
	
		PVUnion[] roiArray = new PVUnion[1];
		roiArray[0] = pvDataCreate.createPVUnion(union);
		roiArray[0].set(expectedROIPVStructure);
		rois.put(0, roiArray.length, roiArray, 0);
	
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);

		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}
	
	@Test
	public void testLinearROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		LinearROI lRoi = new LinearROI();
		lRoi.setPoint(3, 4);
		lRoi.setLength(18);
		lRoi.setAngle(0.75);
		regions.add(lRoi);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure. Note, Linear ROIs are not supported so should be empty
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}
	
	@Ignore // TODO: Allow Java Generator construction without calling CompoundGenerator.prepare,
	        // to allow "empty" scans to be described
	@Test
	public void testPointROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		regions.add(new PointROI(new double[]{5, 9.4}));
		regions.add(new CircularROI(2, 6, 7));
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure expectedPointRoiStructure = fieldCreate.createFieldBuilder().
				addArray("point", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/PointROI:1.0").					
				createStructure();
		
		Structure expectedCircularRoiStructure = fieldCreate.createFieldBuilder().
				addArray("centre", ScalarType.pvDouble).
				add("radius", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/CircularROI:1.0").					
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				add("roi", expectedPointRoiStructure).				
				createStructure();
		
		Structure expectedCircleExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				add("roi", expectedCircularRoiStructure).				
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);
		
		PVStructure expectedROIPVStructure = expectedExcluderPVStructure.getStructureField("roi");

		PVDoubleArray pointVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "point");
		double[] point = new double[] {5, 9.4};
		pointVal.put(0, point.length, point, 0);	
		
		// Create Expected for Circle too
		PVStructure expectedCircleExcluderPVStructure = pvDataCreate.createPVStructure(expectedCircleExcluderStructure);
		PVStringArray circleScannablesVal = expectedCircleExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] circleScannables = new String[] {"stage_x", "stage_y"};
		circleScannablesVal.put(0, circleScannables.length, circleScannables, 0);
		
		PVStructure expectedCircleROIPVStructure = expectedCircleExcluderPVStructure.getStructureField("roi");

		PVDoubleArray centreVal = expectedCircleROIPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {6, 7};
		centreVal.put(0, centre.length, centre, 0);
		PVDouble radiusVal = expectedCircleROIPVStructure.getSubField(PVDouble.class, "radius");
		radiusVal.put(2);
		
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[2];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
		unionArray[1] = pvDataCreate.createPVUnion(union);
		unionArray[1].set(expectedCircleExcluderPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}

	@Test
	public void testPolygonalROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		PolygonalROI diamond = new PolygonalROI(new double[] { 1.5, 0 });
		diamond.insertPoint(new double[] { 3, 1.5 });
		diamond.insertPoint(new double[] { 1.5, 3 });
		diamond.insertPoint(new double[] { 0, 1.5 });
		regions.add(diamond);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();

		Structure expectedRoiStructure = fieldCreate.createFieldBuilder().
				addArray("points_x", ScalarType.pvDouble).
				addArray("points_y", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/PolygonalROI:1.0").
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				createStructure();

		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);

		PVStructure expectedROIPVStructure = pvDataCreate.createPVStructure(expectedRoiStructure);
		PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray xVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "points_x");
		double[] points_x = new double[] {1.5, 3, 1.5, 0};
		xVal.put(0, points_x.length, points_x, 0);
		PVDoubleArray yVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "points_y");
		double[] points_y = new double[] {0, 1.5, 3, 1.5};
		yVal.put(0, points_y.length, points_y, 0);

		PVUnion[] roiArray = new PVUnion[1];
		roiArray[0] = pvDataCreate.createPVUnion(union);
		roiArray[0].set(expectedROIPVStructure);
		rois.put(0, roiArray.length, roiArray, 0);

		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}
	
	@Test
	public void testRectangularROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		RectangularROI rRoi = new RectangularROI();
		rRoi.setPoint(new double[]{7, 3});
		rRoi.setLengths(5, 16);
		rRoi.setAngle(1.2);
		regions.add(rRoi);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCircularRoiStructure = fieldCreate.createFieldBuilder().
				addArray("start", ScalarType.pvDouble).
				add("width", ScalarType.pvDouble).
				add("angle", ScalarType.pvDouble).
				add("height", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/RectangularROI:1.0").					
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				createStructure();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				add("duration", ScalarType.pvDouble).
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);
		
		PVStructure expectedROIPVStructure = pvDataCreate.createPVStructure(expectedCircularRoiStructure);
		PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray startVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "start");
		double[] start = new double[] {7, 3};
		startVal.put(0, start.length, start, 0);
		PVDouble widthVal = expectedROIPVStructure.getSubField(PVDouble.class, "width");
		widthVal.put(5);
		PVDouble heightVal = expectedROIPVStructure.getSubField(PVDouble.class, "height");
		heightVal.put(16);
		PVDouble angleVal = expectedROIPVStructure.getSubField(PVDouble.class, "angle");
		angleVal.put(1.2);

		PVUnion[] roiUnionArray = new PVUnion[1];
		roiUnionArray[0] = pvDataCreate.createPVUnion(union);
		roiUnionArray[0].set(expectedROIPVStructure);
		rois.put(0, roiUnionArray.length, roiUnionArray, 0);

		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}
	
	@Test
	public void testSectorROI() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		SectorROI sRoi = new SectorROI();
		sRoi.setPoint(new double[]{12, 1});
		sRoi.setRadii(1, 11);
		sRoi.setAngles(0, Math.PI);
		regions.add(sRoi);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();

		Structure expectedRoiStructure = fieldCreate.createFieldBuilder().
				addArray("radii", ScalarType.pvDouble).
				addArray("angles", ScalarType.pvDouble).
				addArray("centre", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/SectorROI:1.0").					
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				createStructure();

		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedExcluderPVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables = new String[] {"stage_x", "stage_y"};
		scannablesVal.put(0, scannables.length, scannables, 0);
		
		PVStructure expectedROIPVStructure = pvDataCreate.createPVStructure(expectedRoiStructure);
		PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray centreVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {12, 1};
		centreVal.put(0, centre.length, centre, 0);
		PVDoubleArray radiiVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "radii");
		double[] radii = new double[] {1, 11};
		radiiVal.put(0, radii.length, radii, 0);
		PVDoubleArray anglesVal = expectedROIPVStructure.getSubField(PVDoubleArray.class, "angles");
		double[] angles = new double[] {0, Math.PI};
		anglesVal.put(0, angles.length, angles, 0);

		PVUnion[] roiArray = new PVUnion[1];
		roiArray[0] = pvDataCreate.createPVUnion(union);
		roiArray[0].set(expectedROIPVStructure);
		rois.put(0, roiArray.length, roiArray, 0);

		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(-1);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluderPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);

		assertEquals(expectedCompGenPVStructure.getStructure().getField("excluders"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("excluders"), pvStructure.getSubField("excluders"));
	}

	@Test
	public void testRandomOffsetMutator() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		regions.add(new CircularROI(2, 6, 7));
		
		List<IMutator> mutators = new LinkedList<>();
		List<String> axes = new LinkedList<String>();
		axes.add("x");
		Map<String,Double> offsets = new LinkedHashMap<String, Double>();
		offsets.put("x", 34d);
		RandomOffsetMutator rom = new RandomOffsetMutator(3456, axes, offsets);
		mutators.add(rom);
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		CompoundModel<?> cm = (CompoundModel<?>) scan.getModel();
		cm.setMutators(mutators);
		cm.setDuration(2.5);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure maxOffsetStructure = fieldCreate.createFieldBuilder().
				add("x", ScalarType.pvDouble).				
				createStructure();
		
		Structure expectedRandomOffsetMutatorStructure = fieldCreate.createFieldBuilder().
				add("seed", ScalarType.pvInt).
				addArray("axes", ScalarType.pvString).
				add("max_offset", maxOffsetStructure).
				setId("scanpointgenerator:mutator/RandomOffsetMutator:1.0").					
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).				
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedMutatorPVStructure = pvDataCreate.createPVStructure(expectedRandomOffsetMutatorStructure);
		PVInt seedVal = expectedMutatorPVStructure.getSubField(PVInt.class, "seed");
		seedVal.put(3456);
		PVStringArray axesVal = expectedMutatorPVStructure.getSubField(PVStringArray.class, "axes");
		String[] axesStr = new String[] {"x"};
		axesVal.put(0, axesStr.length, axesStr, 0);
		
		PVStructure maxOffsetPVStructure = expectedMutatorPVStructure.getStructureField("max_offset");
		PVDouble xVal = maxOffsetPVStructure.getSubField(PVDouble.class, "x");
		xVal.put(34);
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "mutators");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedMutatorPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure().getField("mutators"), pvStructure.getStructure().getField("excluders"));
		assertEquals(expectedCompGenPVStructure.getSubField("mutators"), pvStructure.getSubField("mutators"));
	}
	
	@Test
	public void testLineGenerator() throws Exception {

		// Create test generator			
		IPointGeneratorService pgService = new PointGeneratorService();
		StepModel stepModel = new StepModel("x", 3, 4, 0.25);
		IPointGenerator<StepModel> temp = pgService.createGenerator(stepModel);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure expectedGeneratorsStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("start", ScalarType.pvDouble).
				add("alternate", ScalarType.pvBoolean).
				addArray("units", ScalarType.pvString).
				addArray("stop", ScalarType.pvDouble).
				add("size", ScalarType.pvInt).
				setId("scanpointgenerator:generator/LineGenerator:1.0").					
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedGeneratorsPVStructure = pvDataCreate.createPVStructure(expectedGeneratorsStructure);
		PVStringArray nameVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "axes");
		String[] name = new String[] {"x"};
		nameVal.put(0, name.length, name, 0);
		PVStringArray unitsVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "units");
		String[] units = new String[] {"mm"};
		unitsVal.put(0, units.length, units, 0);
		PVDoubleArray startVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "start");
		double[] start = new double[] {3};
		startVal.put(0, start.length, start, 0);
		PVDoubleArray stopVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "stop");
		double[] stop = new double[] {4};
		stopVal.put(0, stop.length, stop, 0);
		PVInt numVal = expectedGeneratorsPVStructure.getSubField(PVInt.class, "size");
		numVal.put(5);
		PVBoolean adVal = expectedGeneratorsPVStructure.getSubField(PVBoolean.class, "alternate");
		adVal.put(false);
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(-1);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "generators");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedGeneratorsPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure(), pvStructure.getStructure());
		assertEquals(expectedCompGenPVStructure, pvStructure);
	}
	
	@Test
	public void testLissajousGenerator() throws Exception {

		// Create test generator
		IPointGeneratorService pgService = new PointGeneratorService();
		LissajousModel lissajousModel = new LissajousModel();
		lissajousModel.setBoundingBox(new BoundingBox(0, -5, 10, 6));
		lissajousModel.setPoints(20);
		lissajousModel.setSlowAxisName("san");
		lissajousModel.setFastAxisName("fan");
		IPointGenerator<LissajousModel> temp = pgService.createGenerator(lissajousModel);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure expectedBoxStructure = fieldCreate.createFieldBuilder().
				addArray("centre", ScalarType.pvDouble).
				add("width", ScalarType.pvDouble).
				add("height", ScalarType.pvDouble).				
				createStructure();
		
		Structure expectedGeneratorsStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				add("lobes", ScalarType.pvInt).
				addArray("centre", ScalarType.pvDouble).
				addArray("units", ScalarType.pvString).
				add("size", ScalarType.pvInt).
				addArray("span", ScalarType.pvDouble).
				setId("scanpointgenerator:generator/LissajousGenerator:1.0").
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).
				addArray("excluders", union).
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedGeneratorsPVStructure = pvDataCreate.createPVStructure(expectedGeneratorsStructure);
		PVStringArray nameVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "axes");
		String[] name = new String[] {"fan", "san"};
		nameVal.put(0, name.length, name, 0);
		PVStringArray unitsVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "units");
		String[] units = new String[] {"mm", "mm"};
		unitsVal.put(0, units.length, units, 0);
		PVInt numPointsVal = expectedGeneratorsPVStructure.getSubField(PVInt.class, "size");
		numPointsVal.put(20);
		PVInt numLobesVal = expectedGeneratorsPVStructure.getSubField(PVInt.class, "lobes");
		numLobesVal.put(4);
		
		PVDoubleArray centreVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {5.0, -2};
		centreVal.put(0, centre.length, centre, 0);
		PVDoubleArray spanVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "span");
		double[] span = new double[] {10, 6};
		spanVal.put(0, span.length, span, 0);
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(-1);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "generators");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedGeneratorsPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);

		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);

		assertEquals(expectedCompGenPVStructure.getStructure(), pvStructure.getStructure());
		assertEquals(expectedCompGenPVStructure, pvStructure);
	}
	
	@Test
	public void testSpiralModel() throws Exception {
		
		// Create test generator
		IPointGeneratorService pgService = new PointGeneratorService();
		IPointGenerator<SpiralModel> temp = pgService.createGenerator(new SpiralModel("x", "y", 2, new BoundingBox(0, 5, 2, 4)));
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure expectedGeneratorsStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("centre", ScalarType.pvDouble).
				add("scale", ScalarType.pvDouble).
				add("alternate", ScalarType.pvBoolean).
				addArray("units", ScalarType.pvString).
				add("radius", ScalarType.pvDouble).
				setId("scanpointgenerator:generator/SpiralGenerator:1.0").					
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedGeneratorsPVStructure = pvDataCreate.createPVStructure(expectedGeneratorsStructure);
		PVStringArray nameVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "axes");
		String[] name = new String[] {"x", "y"};
		nameVal.put(0, name.length, name, 0);
		PVStringArray unitsVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "units");
		String[] units = new String[] {"mm", "mm"};
		unitsVal.put(0, units.length, units, 0);
		PVDoubleArray centreVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "centre");
		double[] centre = new double[] {1, 7};
		centreVal.put(0, centre.length, centre, 0);
		PVDouble scaleVal = expectedGeneratorsPVStructure.getSubField(PVDouble.class, "scale");
		scaleVal.put(2);
		PVDouble radiusVal = expectedGeneratorsPVStructure.getSubField(PVDouble.class, "radius");
		radiusVal.put(2.23606797749979);
		PVBoolean adVal = expectedGeneratorsPVStructure.getSubField(PVBoolean.class, "alternate");
		adVal.put(false);
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(-1);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "generators");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedGeneratorsPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);

		assertEquals(expectedCompGenPVStructure.getStructure(), pvStructure.getStructure());
		assertEquals(expectedCompGenPVStructure, pvStructure);
	}
	
	@Test
	public void testSingualurLineGenerator() throws Exception {

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		
		IPointGeneratorService pgService = new PointGeneratorService();
		StepModel stepModel = new StepModel("x", 3, 4, 0.25);
		IPointGenerator<StepModel> temp = pgService.createGenerator(stepModel, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
		
		Structure expectedGeneratorsStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("start", ScalarType.pvDouble).
				add("alternate", ScalarType.pvBoolean).
				addArray("units", ScalarType.pvString).
				addArray("stop", ScalarType.pvDouble).
				add("size", ScalarType.pvInt).
				setId("scanpointgenerator:generator/LineGenerator:1.0").					
				createStructure();

		Union union = fieldCreate.createVariantUnion();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		PVStructure expectedGeneratorsPVStructure = pvDataCreate.createPVStructure(expectedGeneratorsStructure);
		PVStringArray nameVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "axes");
		String[] name = new String[] {"x"};
		nameVal.put(0, name.length, name, 0);
		PVStringArray unitsVal = expectedGeneratorsPVStructure.getSubField(PVStringArray.class, "units");
		String[] units = new String[] {"mm"};
		unitsVal.put(0, units.length, units, 0);
		PVDoubleArray startVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "start");
		double[] start = new double[] {3};
		startVal.put(0, start.length, start, 0);
		PVDoubleArray stopVal = expectedGeneratorsPVStructure.getSubField(PVDoubleArray.class, "stop");
		double[] stop = new double[] {4};
		stopVal.put(0, stop.length, stop, 0);
		PVInt numVal = expectedGeneratorsPVStructure.getSubField(PVInt.class, "size");
		numVal.put(5);
		PVBoolean adVal = expectedGeneratorsPVStructure.getSubField(PVBoolean.class, "alternate");
		adVal.put(false);
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(-1);
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "generators");
		
		PVUnion[] unionArray = new PVUnion[1];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedGeneratorsPVStructure);
				
		generators.put(0, unionArray.length, unionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure(), pvStructure.getStructure());
		assertEquals(expectedCompGenPVStructure, pvStructure);
	}

	@Test
	public void testFullCompoundGenerator() throws Exception {

		// This test will not behave as expected if either rectangular region has angle == 0
		// This is due to the LineGenerators being "trimmed" in this case by CompoundGenerator

		// Create test generator
		List<IROI> regions = new LinkedList<>();
		RectangularROI rRoi1 = new RectangularROI();
		rRoi1.setPoint(new double[]{2, 1});
		rRoi1.setLengths(5, 16);
		rRoi1.setAngle(Math.PI / 2.0);
		regions.add(rRoi1);
		RectangularROI rRoi2 = new RectangularROI();
		rRoi2.setPoint(new double[]{-2, 2});
		rRoi2.setLengths(9, 16);
		rRoi2.setAngle(0);
		regions.add(rRoi2);
		
		List<IMutator> mutators = new LinkedList<>();
		Map<String, Double> offsets = new HashMap<String, Double>();
		offsets.put("stage_x", 0.5);
		mutators.add(new RandomOffsetMutator(112, Arrays.asList(new String[] {"stage_x"}), offsets));
		
		IPointGeneratorService pgService = new PointGeneratorService();
		GridModel gm = new GridModel("stage_x", "stage_y");
		gm.setSnake(true);
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(10);
		
		IPointGenerator<GridModel> temp = pgService.createGenerator(gm, regions);
		IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
		
		CompoundModel<?> cm = (CompoundModel<?>) scan.getModel();
		cm.setMutators(mutators);
		cm.setDuration(1.5);
					
		// Create the expected PVStructure
		FieldCreate fieldCreate = FieldFactory.getFieldCreate();

		PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();

		Union union = fieldCreate.createVariantUnion();

		Structure expectedCircularRoiStructure = fieldCreate.createFieldBuilder().
				addArray("start", ScalarType.pvDouble).
				add("width", ScalarType.pvDouble).
				add("angle", ScalarType.pvDouble).
				add("height", ScalarType.pvDouble).
				setId("scanpointgenerator:roi/RectangularROI:1.0").					
				createStructure();
		
		Structure expectedExcluderStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("rois", union).
				setId("scanpointgenerator:excluder/ROIExcluder:1.0").
				createStructure();

		Structure expectedOffsets = fieldCreate.createFieldBuilder().
				add("stage_x", ScalarType.pvDouble).
				createStructure();
		Structure expectedRandomOffsetMutatorStructure = fieldCreate.createFieldBuilder().
				add("seed", ScalarType.pvInt).
				addArray("axes", ScalarType.pvString).
				add("max_offset", expectedOffsets).
				setId("scanpointgenerator:mutator/RandomOffsetMutator:1.0").
				createStructure();
		
		Structure expectedLineGeneratorsStructure = fieldCreate.createFieldBuilder().
				addArray("axes", ScalarType.pvString).
				addArray("start", ScalarType.pvDouble).
				add("alternate", ScalarType.pvBoolean).
				addArray("units", ScalarType.pvString).
				addArray("stop", ScalarType.pvDouble).
				add("size", ScalarType.pvInt).
				setId("scanpointgenerator:generator/LineGenerator:1.0").					
				createStructure();
		
		Structure expectedCompGenStructure = fieldCreate.createFieldBuilder().
				addArray("mutators", union).
				add("duration", ScalarType.pvDouble).
				addArray("generators", union).				
				addArray("excluders", union).	
				setId("scanpointgenerator:generator/CompoundGenerator:1.0").
				createStructure();
		
		// Excluders
		PVStructure expectedExcluder1PVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannables1Val = expectedExcluder1PVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables1 = new String[] {"stage_x", "stage_y"};
		scannables1Val.put(0, scannables1.length, scannables1, 0);
		
		PVStructure expectedROI1PVStructure = pvDataCreate.createPVStructure(expectedCircularRoiStructure);
		PVUnionArray rois1 = expectedExcluder1PVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray startVal1 = expectedROI1PVStructure.getSubField(PVDoubleArray.class, "start");
		double[] start1 = new double[] {2, 1};
		startVal1.put(0, start1.length, start1, 0);
		PVDouble widthVal1 = expectedROI1PVStructure.getSubField(PVDouble.class, "width");
		widthVal1.put(5);
		PVDouble heightVal1 = expectedROI1PVStructure.getSubField(PVDouble.class, "height");
		heightVal1.put(16);
		PVDouble angleVal1 = expectedROI1PVStructure.getSubField(PVDouble.class, "angle");
		angleVal1.put(Math.PI / 2.0);

		PVUnion[] roi1Array = new PVUnion[1];
		roi1Array[0] = pvDataCreate.createPVUnion(union);
		roi1Array[0].set(expectedROI1PVStructure);
		rois1.put(0, roi1Array.length, roi1Array, 0);
		
		PVStructure expectedExcluder2PVStructure = pvDataCreate.createPVStructure(expectedExcluderStructure);
		PVStringArray scannables2Val = expectedExcluder2PVStructure.getSubField(PVStringArray.class, "axes");
		String[] scannables2 = new String[] {"stage_x", "stage_y"};
		scannables2Val.put(0, scannables2.length, scannables2, 0);
		
		PVStructure expectedROI2PVStructure = pvDataCreate.createPVStructure(expectedCircularRoiStructure);
		PVUnionArray rois2 = expectedExcluder2PVStructure.getSubField(PVUnionArray.class, "rois");

		PVDoubleArray startVal2 = expectedROI2PVStructure.getSubField(PVDoubleArray.class, "start");
		double[] start2 = new double[] {-2, 2};
		startVal2.put(0, start2.length, start2, 0);
		PVDouble widthVal2 = expectedROI2PVStructure.getSubField(PVDouble.class, "width");
		widthVal2.put(9);
		PVDouble heightVal2 = expectedROI2PVStructure.getSubField(PVDouble.class, "height");
		heightVal2.put(16);
		PVDouble angleVal2 = expectedROI2PVStructure.getSubField(PVDouble.class, "angle");
		angleVal2.put(0);

		PVUnion[] roi2Array = new PVUnion[1];
		roi2Array[0] = pvDataCreate.createPVUnion(union);
		roi2Array[0].set(expectedROI2PVStructure);
		rois2.put(0, roi2Array.length, roi2Array, 0);

		// Mutators
		PVStructure expectedMutatorPVStructure = pvDataCreate.createPVStructure(expectedRandomOffsetMutatorStructure);
		PVStructure expectedOffsetPVStructure = expectedMutatorPVStructure.getStructureField("max_offset");
		PVDouble offsetVal = expectedOffsetPVStructure.getSubField(PVDouble.class, "stage_x");
		offsetVal.put(0.5);
		PVInt seedVal = expectedMutatorPVStructure.getSubField(PVInt.class, "seed");
		seedVal.put(112);
		PVStringArray axesVal = expectedMutatorPVStructure.getSubField(PVStringArray.class, "axes");
		String[] axes = new String[] {"stage_x"};
		axesVal.put(0, axes.length, axes, 0);
		
		// Generators
		PVStructure expectedGeneratorsPVStructure1 = pvDataCreate.createPVStructure(expectedLineGeneratorsStructure);
		PVStringArray nameVal1 = expectedGeneratorsPVStructure1.getSubField(PVStringArray.class, "axes");
		String[] name1 = new String[] {"stage_y"};
		nameVal1.put(0, name1.length, name1, 0);
		PVStringArray unitsVal1 = expectedGeneratorsPVStructure1.getSubField(PVStringArray.class, "units");
		String[] units1 = new String[] {"mm"};
		unitsVal1.put(0, units1.length, units1, 0);
		PVDoubleArray gstartVal1 = expectedGeneratorsPVStructure1.getSubField(PVDoubleArray.class, "start");
		double[] gstart1 = new double[] {2.7};
		gstartVal1.put(0, gstart1.length, gstart1, 0);
		PVDoubleArray stopVal1 = expectedGeneratorsPVStructure1.getSubField(PVDoubleArray.class, "stop");
		double[] stop1 = new double[] {16.3};
		stopVal1.put(0, stop1.length, stop1, 0);
		PVInt numVal1 = expectedGeneratorsPVStructure1.getSubField(PVInt.class, "size");
		numVal1.put(5);
		PVBoolean adVal1 = expectedGeneratorsPVStructure1.getSubField(PVBoolean.class, "alternate");
		adVal1.put(true);
		
		PVStructure expectedGeneratorsPVStructure2 = pvDataCreate.createPVStructure(expectedLineGeneratorsStructure);
		PVStringArray nameVal = expectedGeneratorsPVStructure2.getSubField(PVStringArray.class, "axes");
		String[] name = new String[] {"stage_x"};
		nameVal.put(0, name.length, name, 0);
		PVStringArray unitsVal = expectedGeneratorsPVStructure2.getSubField(PVStringArray.class, "units");
		String[] units2 = new String[] {"mm"};
		unitsVal.put(0, units2.length, units2, 0);
		PVDoubleArray startVal = expectedGeneratorsPVStructure2.getSubField(PVDoubleArray.class, "start");
		double[] start = new double[] {-12.95};
		startVal.put(0, start.length, start, 0);
		PVDoubleArray stopVal = expectedGeneratorsPVStructure2.getSubField(PVDoubleArray.class, "stop");
		double[] stop = new double[] {5.950000000000003};
		stopVal.put(0, stop.length, stop, 0);
		PVInt numVal = expectedGeneratorsPVStructure2.getSubField(PVInt.class, "size");
		numVal.put(10);
		PVBoolean adVal = expectedGeneratorsPVStructure2.getSubField(PVBoolean.class, "alternate");
		adVal.put(true);
		
		
		PVStructure expectedCompGenPVStructure = pvDataCreate.createPVStructure(expectedCompGenStructure);
		PVDouble durationVal = expectedCompGenPVStructure.getSubField(PVDouble.class, "duration");
		durationVal.put(1.5);
		PVUnionArray excluders = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "excluders");
		
		PVUnion[] unionArray = new PVUnion[2];
		unionArray[0] = pvDataCreate.createPVUnion(union);
		unionArray[0].set(expectedExcluder1PVStructure);
		unionArray[1] = pvDataCreate.createPVUnion(union);
		unionArray[1].set(expectedExcluder2PVStructure);
				
		excluders.put(0, unionArray.length, unionArray, 0);
		
		PVUnionArray mutatorsPVArray = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "mutators");
		
		PVUnion[] mutUnionArray = new PVUnion[1];
		mutUnionArray[0] = pvDataCreate.createPVUnion(union);
		mutUnionArray[0].set(expectedMutatorPVStructure);
		
		mutatorsPVArray.put(0, mutUnionArray.length, mutUnionArray, 0);
		
		PVUnionArray generators = expectedCompGenPVStructure.getSubField(PVUnionArray.class, "generators");
		
		PVUnion[] genUunionArray = new PVUnion[2];
		genUunionArray[0] = pvDataCreate.createPVUnion(union);
		genUunionArray[0].set(expectedGeneratorsPVStructure1);
		genUunionArray[1] = pvDataCreate.createPVUnion(union);
		genUunionArray[1].set(expectedGeneratorsPVStructure2);
				
		generators.put(0, genUunionArray.length, genUunionArray, 0);
		
		// Marshal and check against expected
		PVStructure pvStructure = connectorService.pvMarshal(scan);
		
		assertEquals(expectedCompGenPVStructure.getStructure(), pvStructure.getStructure());
		assertEquals(expectedCompGenPVStructure, pvStructure);
	}

}

