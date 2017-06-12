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
package org.eclipse.scanning.test.malcolm.real;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.malcolm.MalcolmTable;
import org.eclipse.scanning.api.malcolm.attributes.HealthAttribute;
import org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.TableAttribute;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.connector.epics.EpicsV4ConnectorService;
import org.eclipse.scanning.example.malcolm.EPICSv4ExampleModel;
import org.eclipse.scanning.example.malcolm.IEPICSv4Device;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.eclipse.scanning.malcolm.core.MalcolmService;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.test.epics.DeviceRunner;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Union;
import org.junit.Test;

public class ExampleMalcolmDeviceTest {

	private IMalcolmService      service;
	private IEPICSv4Device epicsv4Device;

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then probes it with the configure() and call() methods
	 * as well as getting a list of all attributes and several specific attributes
	 * @throws Exception
	 */
	@Test
	public void configureAndRunDummyMalcolm() throws Exception {

		try {
	
			// The real service, get it from OSGi outside this test!
			// Not required in OSGi mode (do not add this to your real code GET THE SERVICE FROM OSGi!)
			this.service = new MalcolmService(new EpicsV4ConnectorService(), null);

			// Start the dummy test device
			DeviceRunner runner = new DeviceRunner();
			epicsv4Device = runner.start();

			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

			// Setup the model and other configuration items
			List<IROI> regions = new LinkedList<>();
			regions.add(new CircularROI(2, 0, 0));
			regions.add(new CircularROI(4, -1, -2));
			
			IPointGeneratorService pgService = new PointGeneratorService();
			IPointGenerator<SpiralModel> temp = pgService.createGenerator(
					new SpiralModel("stage_x", "stage_y", 1, new BoundingBox(0, -5, 8, 3)), regions);
			IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
			
			EPICSv4ExampleModel pmac1 = new EPICSv4ExampleModel();
			pmac1.setExposureTime(23.1);
			pmac1.setFileDir("/path/to/ixx-1234");

			// Set the generator on the device
			// Cannot set the generator from @PreConfigure in this unit test.
			((AbstractMalcolmDevice<?>)modelledDevice).setPointGenerator(scan);
			
			// Call configure
			modelledDevice.configure(pmac1);

			// Call run
			modelledDevice.run(null);

			// Get a list of all attributes on the device
			List<IDeviceAttribute<?>> attribs = modelledDevice.getAllAttributes();
			assertEquals(11, attribs.size());

			boolean stateFound = false;
			boolean healthFound = false;
			boolean busyFound = false;
			boolean totalStepsFound = false;
			boolean aFound = false;
			boolean bFound = false;
			boolean axesFound = false;
			boolean layoutFound = false;
			boolean datasetsFound = false;
			boolean generatorFound = false;
			boolean completedStepsFound = false;
			boolean aIsNotWriteable = false;
			boolean bIsWriteable = false;

			for (IDeviceAttribute<?> ma : attribs) {
				if (ma.getName().equals("state")) {
					stateFound = true;
				} else if (ma.getName().equals("health")) {
					healthFound = true;
				} else if (ma.getName().equals("busy")) {
					busyFound = true;
				} else if (ma.getName().equals("totalSteps")) {
					totalStepsFound = true;
				} else if (ma.getName().equals("A")) {
					aFound = true;
					aIsNotWriteable = !ma.isWriteable();
				} else if (ma.getName().equals("B")) {
					bFound = true;
					bIsWriteable = ma.isWriteable();
				} else if (ma.getName().equals("axesToMove")) {
					axesFound = true;
				} else if (ma.getName().equals("datasets")) {
					datasetsFound = true;
				} else if (ma.getName().equals("generator")) {
					generatorFound = true;
				} else if (ma.getName().equals("completedSteps")) {
					completedStepsFound = true;
				} else if (ma.getName().equals("layout")) {
					layoutFound = true;
				} 
			}

			assertTrue(stateFound);
			assertTrue(healthFound);
			assertTrue(busyFound);
			assertTrue(totalStepsFound);
			assertTrue(aFound);
			assertTrue(aIsNotWriteable);
			assertTrue(bFound);
			assertTrue(bIsWriteable);
			assertTrue(axesFound);
			assertTrue(datasetsFound);
			assertTrue(generatorFound);
			assertTrue(completedStepsFound);
			assertTrue(layoutFound);

			// Get a specific choice (string) attribute
			Object stateValue = modelledDevice.getAttributeValue("state");
			if (stateValue instanceof String) {
				String stateValueStr = (String) stateValue;
				assertEquals("ARMED", stateValueStr);
			} else {
				fail("state value was expected to be a string but wasn't");
			}

			// Get a specific string attribute
			Object healthValue = modelledDevice.getAttributeValue("health");
			if (healthValue instanceof String) {
				String healthValueStr = (String) healthValue;
				assertEquals("Test Health", healthValueStr);
			} else {
				fail("health value was expected to be a string but wasn't");
			}

			// Get a specific boolean attribute
			Object busyValue = modelledDevice.getAttributeValue("busy");
			if (busyValue instanceof Boolean) {
				Boolean busyValueBool = (Boolean) busyValue;
				assertTrue(busyValueBool.equals(false));
			} else {
				fail("busy value was expected to be a boolean but wasn't");
			}

			// Get a specific number attribute
			Object totalStepsValue = modelledDevice.getAttributeValue("totalSteps");
			if (totalStepsValue instanceof Integer) {
				Integer totalStepsValueInt = (Integer) totalStepsValue;
				assertEquals((Integer)123, totalStepsValueInt);
			} else {
				fail("totalSteps value was expected to be an int but wasn't");
			}

			// Get a specific string attribute (full attribute)
			Object healthAttributeValue = modelledDevice.getAttribute("health");
			if (healthAttributeValue instanceof HealthAttribute) {
				HealthAttribute healthAttributeValueStr = (HealthAttribute) healthAttributeValue;
				assertEquals("health", healthAttributeValueStr.getName());
				assertEquals("Test Health", healthAttributeValueStr.getValue());
			} else {
				fail("health value was expected to be a HealthAttribute but wasn't");
			}
			
			// Get a specific table attribute (full attribute)
			Object datasetsAttributeValue = modelledDevice.getAttribute("datasets");
			if (datasetsAttributeValue instanceof TableAttribute) {
				TableAttribute datasetAttributeValueTable = (TableAttribute) datasetsAttributeValue;
				assertEquals("datasets", datasetAttributeValueTable.getName());
				MalcolmTable malcolmTable = datasetAttributeValueTable.getValue();
				
				assertEquals(4, malcolmTable.getHeadings().size());
				assertEquals("detector", malcolmTable.getHeadings().get(0));
				assertEquals("filename", malcolmTable.getHeadings().get(1));
				assertEquals("dataset", malcolmTable.getHeadings().get(2));
				assertEquals("users", malcolmTable.getHeadings().get(3));
				assertEquals(3, malcolmTable.getColumn("dataset").size());
				assertEquals("/entry/detector/I200", malcolmTable.getColumn("dataset").get(0));
				assertEquals("/entry/detector/Iref", malcolmTable.getColumn("dataset").get(1));
				assertEquals("/entry/detector/det1", malcolmTable.getColumn("dataset").get(2));
			} else {
				fail("datasets value was expected to be a TableAttribute but wasn't");
			}
			
			// Check seek method works
			modelledDevice.seek(4);

			// Check the RPC calls were received correctly by the device
			Map<String, PVStructure> rpcCalls = epicsv4Device.getReceivedRPCCalls();

			assertEquals(4, rpcCalls.size());

			// configure
			PVStructure configureCall = rpcCalls.get("configure");

			Union union = FieldFactory.getFieldCreate().createVariantUnion();

			Structure generatorStructure = FieldFactory.getFieldCreate().createFieldBuilder()
					.addArray("mutators", union)
					.add("duration", ScalarType.pvDouble)
					.addArray("generators", union)
					.addArray("excluders", union)
					.setId("scanpointgenerator:generator/CompoundGenerator:1.0").createStructure();

			Structure spiralGeneratorStructure = FieldFactory.getFieldCreate().createFieldBuilder()
					.addArray("axes", ScalarType.pvString)
					.addArray("centre", ScalarType.pvDouble)
					.add("scale", ScalarType.pvDouble)
					.add("alternate", ScalarType.pvBoolean)
					.addArray("units", ScalarType.pvString)
					.add("radius", ScalarType.pvDouble)
					.setId("scanpointgenerator:generator/SpiralGenerator:1.0").createStructure();

			Structure circularRoiStructure = FieldFactory.getFieldCreate().createFieldBuilder().
					addArray("centre", ScalarType.pvDouble).
					add("radius", ScalarType.pvDouble).
					setId("scanpointgenerator:roi/CircularROI:1.0").					
					createStructure();
			
			Structure excluderStructure = FieldFactory.getFieldCreate().createFieldBuilder().
					addArray("axes", ScalarType.pvString).
					addArray("rois", union).
					setId("scanpointgenerator:excluder/ROIExcluder:1.0").
					createStructure();

			Structure configureStructure = FieldFactory.getFieldCreate().createFieldBuilder()
					.add("generator", generatorStructure)
					.add("fileDir", ScalarType.pvString)
					.add("fileTemplate", ScalarType.pvString)
					.createStructure();

			PVStructure spiralGeneratorPVStructure = PVDataFactory.getPVDataCreate()
					.createPVStructure(spiralGeneratorStructure);
			double[] centre = new double[] { 1.5, -2.0 };
			spiralGeneratorPVStructure.getSubField(PVDoubleArray.class, "centre").put(0, centre.length, centre, 0);
			spiralGeneratorPVStructure.getDoubleField("scale").put(1.0);
			String[] units = new String[] {"mm", "mm"};
			spiralGeneratorPVStructure.getSubField(PVStringArray.class, "units").put(0, units.length, units, 0);
			String[] names = new String[] { "stage_x", "stage_y" };
			spiralGeneratorPVStructure.getSubField(PVStringArray.class, "axes").put(0, names.length, names, 0);
			spiralGeneratorPVStructure.getBooleanField("alternate").put(false);
			spiralGeneratorPVStructure.getDoubleField("radius").put(7.632168761236874);

			
			PVStructure configurePVStructure = PVDataFactory.getPVDataCreate().createPVStructure(configureStructure);
			PVUnion pvu1 = PVDataFactory.getPVDataCreate().createPVVariantUnion();
			pvu1.set(spiralGeneratorPVStructure);
			PVUnion[] unionArray = new PVUnion[1];
			unionArray[0] = pvu1;
			configurePVStructure.getUnionArrayField("generator.generators").put(0, unionArray.length, unionArray, 0);

			PVStructure expectedExcluderPVStructure = PVDataFactory.getPVDataCreate().createPVStructure(excluderStructure);
			PVStringArray scannablesVal = expectedExcluderPVStructure.getSubField(PVStringArray.class, "axes");
			String[] scannables = new String[] {"stage_x", "stage_y"};
			scannablesVal.put(0, scannables.length, scannables, 0);
			PVUnionArray rois = expectedExcluderPVStructure.getSubField(PVUnionArray.class, "rois");
			
			PVStructure expectedROIPVStructure1 = PVDataFactory.getPVDataCreate().createPVStructure(circularRoiStructure);
			PVDoubleArray cr1CentreVal = expectedROIPVStructure1.getSubField(PVDoubleArray.class, "centre");
			double[] cr1Centre = new double[] {0, 0};
			cr1CentreVal.put(0, cr1Centre.length, cr1Centre, 0);
			PVDouble radius1Val = expectedROIPVStructure1.getSubField(PVDouble.class, "radius");
			radius1Val.put(2);

			PVStructure expectedROIPVStructure2 = PVDataFactory.getPVDataCreate().createPVStructure(circularRoiStructure);
			PVDoubleArray cr2CentreVal = expectedROIPVStructure2.getSubField(PVDoubleArray.class, "centre");
			double[] cr2Centre = new double[] {-1, -2};
			cr2CentreVal.put(0, cr2Centre.length, cr2Centre, 0);
			PVDouble radius2Val = expectedROIPVStructure2.getSubField(PVDouble.class, "radius");
			radius2Val.put(4);

			PVUnion[] roiArray = new PVUnion[2];
			roiArray[0] = PVDataFactory.getPVDataCreate().createPVUnion(union);
			roiArray[0].set(expectedROIPVStructure1);
			roiArray[1] = PVDataFactory.getPVDataCreate().createPVUnion(union);
			roiArray[1].set(expectedROIPVStructure2);
			rois.put(0, roiArray.length, roiArray, 0);

			PVUnion[] crUnionArray = new PVUnion[1];
			crUnionArray[0] = PVDataFactory.getPVDataCreate().createPVUnion(union);
			crUnionArray[0].set(expectedExcluderPVStructure);

			configurePVStructure.getUnionArrayField("generator.excluders").put(0, crUnionArray.length, crUnionArray, 0);

			configurePVStructure.getSubField(PVDouble.class, "generator.duration").put(23.1);

			PVString fileDirVal = configurePVStructure.getSubField(PVString.class, "fileDir");
			fileDirVal.put("/path/to/ixx-1234");
			PVString fileTemplateVal = configurePVStructure.getSubField(PVString.class, "fileTemplate");
			fileTemplateVal.put("ixx-1234-%s.h5");

			assertEquals(configureStructure, configureCall.getStructure());
			assertEquals(configurePVStructure, configureCall);

			// run
			PVStructure runCall = rpcCalls.get("run");

			Structure runStructure = FieldFactory.getFieldCreate().createFieldBuilder().createStructure();
			PVStructure runPVStructure = PVDataFactory.getPVDataCreate().createPVStructure(runStructure);

			assertEquals(runStructure, runCall.getStructure());
			assertEquals(runPVStructure, runCall);
			
			// seek
			PVStructure seekCall = rpcCalls.get("pause");

			Structure seekStructure = FieldFactory.getFieldCreate().createFieldBuilder()
					.add("completedSteps", ScalarType.pvInt).createStructure();
			PVStructure seekPVStructure = PVDataFactory.getPVDataCreate().createPVStructure(seekStructure);
			seekPVStructure.getIntField("completedSteps").put(4);
			assertEquals(seekStructure, seekCall.getStructure());
			assertEquals(seekPVStructure, seekCall);

			modelledDevice.dispose();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} finally {
			// Stop the device
			epicsv4Device.stop();
			service.dispose();
		}
	}

}
