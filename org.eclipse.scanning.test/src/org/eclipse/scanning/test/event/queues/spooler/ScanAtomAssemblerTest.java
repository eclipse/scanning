package org.eclipse.scanning.test.event.queues.spooler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.spooler.ServicesHolder;
import org.eclipse.scanning.event.queues.spooler.QueueBeanFactory;
import org.eclipse.scanning.event.queues.spooler.beanassemblers.ScanAtomAssembler;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScanAtomAssemblerTest {
	
	private IQueueBeanFactory qbf;
	
	@Before
	public void setUp() throws ScanningException {
		IScannableDeviceService connector = new MockScannableConnector(null);
		ServicesHolder.setScannableDeviceService(connector);
		IRunnableDeviceService dservice = new RunnableDeviceServiceImpl(connector); // Not testing OSGi so using hard coded service.
		ServicesHolder.setRunnableDeviceService(dservice);
		
		((RunnableDeviceServiceImpl) dservice)._register(MandelbrotModel.class, MandelbrotDetector.class);
		
		//Lifted from org.eclipse.scanning.test.scan.nexus.MandelbrotExampleTest
		MandelbrotModel modelA = makeMandelbrotModelA(), modelB = makeMandelbrotModelB();
		dservice.createRunnableDevice(modelA);
		dservice.createRunnableDevice(modelB);
		
		qbf = new QueueBeanFactory();
	}
	
	private MandelbrotModel makeMandelbrotModelA() {
		MandelbrotModel modelA = new MandelbrotModel();
		modelA.setName("mandelbrotA");
		modelA.setRealAxisName("xNex");
		modelA.setImaginaryAxisName("yNex");
		modelA.setColumns(64);
		modelA.setRows(64);
		return modelA;
	}
	
	private MandelbrotModel makeMandelbrotModelB() {
		MandelbrotModel modelB = new MandelbrotModel();
		modelB.setName("mandelbrotB");
		modelB.setRealAxisName("xNex");
		modelB.setImaginaryAxisName("yNex");
		modelB.setColumns(64);
		modelB.setRows(64);
		return modelB;
	}

	@After
	public void tearDown() {
		ServicesHolder.setScannableDeviceService(null);
		ServicesHolder.setRunnableDeviceService(null);
	}
	
	/**
	 * Test that path models can be configured either from the stored template, 
	 * from the provided ExperimentConfiguration or from a mixture of both.
	 * This also tests that the StepModel is created properly.
	 */
	@Test
	public void testPathConfig() throws QueueModelException {
		ScanAtomAssembler scAtAss = new ScanAtomAssembler(null);
		CompoundModel<?> cMod = new CompoundModel<>();
		cMod.addData(new StepModel("stage_x", 0.0, 10.5, 1.5), null);
		
		/*
		 * Fully specified by template
		 */
		Map<String, DeviceModel> pMods = new LinkedHashMap<>();
		Map<String, Object> devConf = new HashMap<>();
		devConf.put("start", 0.0);
		devConf.put("stop", 10.5);
		devConf.put("step", 1.5);
		DeviceModel pathModel = new DeviceModel("Step", devConf);
		pMods.put("stage_x", pathModel);
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, DeviceModel>(), new ArrayList<Object>());
		
		ScanAtom assAt = scAtAss.assemble(scAtMod, new ExperimentConfiguration(null, null, null));
		assertEquals("Template specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
		
		/*
		 * Fully specified by ExperimentConfiguration
		 */
		scAtMod = new ScanAtom("testScan", new HashMap<String, DeviceModel>(), new HashMap<String, DeviceModel>(), new ArrayList<Object>());
		ExperimentConfiguration config = new ExperimentConfiguration(null, pMods, null);
		assAt = scAtAss.assemble(scAtMod, config);
		assertEquals("ExperimentConfiguration specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
		
		try {
			scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, DeviceModel>(), new ArrayList<Object>());
			assAt = scAtAss.assemble(scAtMod, config);
			fail("Should not be able to specify path model for same axis twice");
		} catch (QueueModelException meEx) {
			//expected - config and scAtMod both define the "stage_x" path. We don't know which to use, so just don't allow this behaviour
		}
		
		/*
		 * Part specified by template, part specified by localValues
		 */
		pMods = new LinkedHashMap<>();
		devConf = new HashMap<>();
		devConf.put("start", new QueueValue<String>("start", "start", true));
		devConf.put("stop", new QueueValue<String>("stop", "stop", true));
		devConf.put("step", new QueueValue<String>("step", "step", true));
		pathModel = new DeviceModel("Step", devConf);
		pMods.put("stage_x", pathModel);
		scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, DeviceModel>(), new ArrayList<Object>());
		List<IQueueValue<?>> localValues = new ArrayList<>();
		localValues.add(new QueueValue<Double>("start", 0.0));
		localValues.add(new QueueValue<Double>("stop", 10.5));
		localValues.add(new QueueValue<Double>("step", 1.5));
		config = new ExperimentConfiguration(localValues, null, null);
		
		assAt = scAtAss.assemble(scAtMod, config);
		assertEquals("Mixed template/localValues specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
		
		/*
		 * More complicated, one device from template, one device from config, both specified by config
		 */
		cMod.addData(new StepModel("stage_y", 15.8, 16.5, 0.1), null);
		pMods = new LinkedHashMap<>();
		devConf.put("start", new QueueValue<String>("start", "startx", true));
		devConf.put("stop", new QueueValue<String>("stop", "stopx", true));
		devConf.put("step", new QueueValue<String>("step", "stepx", true));
		DeviceModel pathModelX = new DeviceModel("Step", devConf);
		pMods.put("stage_x", pathModelX);
		scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, DeviceModel>(), new ArrayList<Object>());
		
		Map<String, DeviceModel> pModsConf = new LinkedHashMap<>();
		Map<String, Object> devConfY = new HashMap<>();
		devConfY.put("start", new QueueValue<String>("start", "starty", true));
		devConfY.put("stop", new QueueValue<String>("stop", "stopy", true));
		devConfY.put("step", new QueueValue<String>("step", "stepy", true));
		DeviceModel pathModelY = new DeviceModel("Step", devConfY);
		pModsConf.put("stage_y", pathModelY);
		localValues = new ArrayList<>();
		localValues.add(new QueueValue<Double>("startx", 0.0));
		localValues.add(new QueueValue<Double>("stopx", 10.5));
		localValues.add(new QueueValue<Double>("stepx", 1.5));
		localValues.add(new QueueValue<Double>("starty", 15.8));
		localValues.add(new QueueValue<Double>("stopy", 16.5));
		localValues.add(new QueueValue<Double>("stepy", 0.1));
		config = new ExperimentConfiguration(localValues, pModsConf, null);
		assAt = scAtAss.assemble(scAtMod, config);
		assertEquals("Complex mixed template/localValues specified multiple paths differ from expected", cMod, assAt.getScanReq().getCompoundModel());
	}
	
	/**
	 * Test that the ArrayModel can be used to create paths
	 */
	@Test
	public void testArrayPath() throws QueueModelException {
		ScanAtomAssembler scAtAss = new ScanAtomAssembler(null);
		ArrayModel arrayModel = new ArrayModel(new double[]{15.8, 16.5, 15.9, 14.2});
		arrayModel.setName("stage_y");
		CompoundModel<?> cMod = new CompoundModel<>();
		cMod.addData(arrayModel, null);
		
		/*
		 * Fully specified by template
		 */
		Map<String, DeviceModel> pMods = new LinkedHashMap<>();
		Map<String, Object> devConf = new HashMap<>();
		devConf.put("positions", new Double[]{15.8, 16.5, 15.9, 14.2});
		DeviceModel pathModel = new DeviceModel("Array", devConf);
		pMods.put("stage_y", pathModel);
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, DeviceModel>(), new ArrayList<Object>());
		
		ScanAtom assAt = scAtAss.assemble(scAtMod, new ExperimentConfiguration(null, null, null));
		assertEquals("Template specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
	}
	
	@Test
	public void testDetectorConfig() throws QueueModelException {
		ScanAtomAssembler scAtAss = new ScanAtomAssembler(null);
		Map<String, Object> detectors = new HashMap<>();
		MandelbrotModel modA = makeMandelbrotModelA(), modB = makeMandelbrotModelB();
		modA.setExposureTime(30.0);
		detectors.put("mandelbrotA", modA);
		modB.setExposureTime(22.0);
		detectors.put("mandelbrotB", modB);
		
		/*
		 * Fully specified by template
		 */
		Map<String, DeviceModel> dMods = new LinkedHashMap<>();
		Map<String, Object> devConfA = new HashMap<>();
		devConfA.put("exposureTime", 30.0);
		DeviceModel devModelA = new DeviceModel(null, devConfA);
		dMods.put("mandelbrotA", devModelA);
		Map<String, Object> devConfB = new HashMap<>();
		devConfB.put("exposureTime", 22.0);
		DeviceModel devModelB = new DeviceModel(null, devConfB);
		dMods.put("mandelbrotB", devModelB);
		ScanAtom scAtMod = new ScanAtom("testScan", new HashMap<String, DeviceModel>(), dMods, new ArrayList<Object>());
		
		ScanAtom assAt = scAtAss.assemble(scAtMod, new ExperimentConfiguration(null, null, null));
		assertEquals("Template specified detectors differ from expected", detectors, assAt.getScanReq().getDetectors());
	}
	
	@Test
	public void testSetName() throws QueueModelException {
		Map<String, DeviceModel> pMods = new LinkedHashMap<>();
		Map<String, Object> devConfX = new HashMap<>(), devConfY = new HashMap<>();
		devConfX.put("start", 0.0);
		devConfX.put("stop", 10.5);
		devConfX.put("step", 1.5);
		DeviceModel pathModelX = new DeviceModel("Step", devConfX);
		devConfY.put("start", 15.8);
		devConfY.put("stop", 16.5);
		devConfY.put("step", 0.1);
		DeviceModel pathModelY = new DeviceModel("Step", devConfY);
		pMods.put("stage_x", pathModelX);
		pMods.put("stage_y", pathModelY);
		
		Map<String, DeviceModel> dMods = new LinkedHashMap<>();
		Map<String, Object> devConfA = new LinkedHashMap<>();
		devConfA.put("exposureTime", 30.0);
		DeviceModel devModelA = new DeviceModel(null, devConfA);
		dMods.put("mandelbrotA", devModelA);
		Map<String, Object> devConfB = new HashMap<>();
		devConfB.put("exposureTime", 22.0);
		DeviceModel devModelB = new DeviceModel(null, devConfB);
		dMods.put("mandelbrotB", devModelB);
		
		Collection<Object> mMods = new ArrayList<Object>();
		mMods.add("monitor2");
		
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, dMods, mMods);
		ScanAtomAssembler scAtAss = new ScanAtomAssembler(null);
		ScanAtom produced = scAtAss.assemble(scAtMod, new ExperimentConfiguration(null, null, null));
		
		assertEquals("Names of ScanAtoms differ", "Scan of 'stage_x' (Step), 'stage_y' (Step) collecting data with 'mandelbrotB', 'mandelbrotA' detector(s)", produced.getName());
	}
	
	/**
	 * Tests the creation of a fully configured ScanAtom
	 * @throws Exception
	 */
	@Test
	public void testScanAtomCreation() throws Exception {
		//ScanAtom model
		Map<String, DeviceModel> pMods = new LinkedHashMap<>();
		Map<String, Object> pModDevConf = new HashMap<>();
		pModDevConf.put("start", 0.0);
		pModDevConf.put("stop", 10.5);
		pModDevConf.put("step", 1.5);
		DeviceModel pathModel = new DeviceModel("Step", pModDevConf);
		pMods.put("stage_x", pathModel);
		Map<String, DeviceModel> dMods = new LinkedHashMap<>();
		Map<String, Object> dModDevConf = new HashMap<>();
		dModDevConf.put("exposureTime", new QueueValue<String>("exposureTime", true));
		DeviceModel detectorModel = new DeviceModel(null, dModDevConf);
		dMods.put("mandelbrotA", detectorModel);
		dMods.put("mandelbrotB", detectorModel);
		Collection<Object> mons = Arrays.asList(new QueueValue<String>("monitor2"));
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, dMods, mons);
		qbf.registerAtom(scAtMod);
		
		ExperimentConfiguration config = new ExperimentConfiguration(Arrays.asList(new QueueValue<Double>("exposureTime", 30.0)), null, null);
		
		ScanAtom produced = qbf.assembleQueueAtom(new QueueValue<>("testScan", true), config);
		ScanAtom exemplar = createScanAtom();
		
		assertEquals("Produced task has wrong paths configured", exemplar.getScanReq().getCompoundModel(), produced.getScanReq().getCompoundModel());
		assertEquals("Produced task has wrong detectors configured", exemplar.getScanReq().getDetectors(), produced.getScanReq().getDetectors());
		assertEquals("Produced task has wrong monitors configured", exemplar.getScanReq().getMonitorNames(), produced.getScanReq().getMonitorNames());
		assertEquals("Produced task is not correctly configured", exemplar, produced);
	}
	
	private <T> ScanAtom createScanAtom() throws ScanningException {
		ScanRequest<T> scanReq = new ScanRequest<>();
		CompoundModel<T> cMod = new CompoundModel<>();
		cMod.addData(new StepModel("stage_x", 0.0, 10.5, 1.5), null);
		scanReq.setCompoundModel(cMod);
		Map<String, Object> detectors = new HashMap<>();
		detectors.put("mandelbrotA", ServicesHolder.getRunnableDeviceService().getRunnableDevice("mandelbrotA").getModel());
		((IDetectorModel)detectors.get("mandelbrotA")).setExposureTime(30);
		detectors.put("mandelbrotB", ServicesHolder.getRunnableDeviceService().getRunnableDevice("mandelbrotB").getModel());
		((IDetectorModel)detectors.get("mandelbrotB")).setExposureTime(30);
		scanReq.setDetectors(detectors);
		scanReq.setMonitorNames(Arrays.asList("monitor2"));
		ScanAtom scAt = new ScanAtom("testScan", scanReq);
		scAt.setName("Scan of 'stage_x' (Step) collecting data with 'mandelbrotB', 'mandelbrotA' detector(s)");
		return scAt;
	}

}
