package org.eclipse.scanning.test.event.queues;

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
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.spooler.QueueBeanFactory;
import org.eclipse.scanning.event.queues.spooler.ScanAtomAssembler;
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
		ServicesHolder.setDeviceService(dservice);
		
		((RunnableDeviceServiceImpl) dservice)._register(MandelbrotModel.class, MandelbrotDetector.class);
		
		//Lifted from org.eclipse.scanning.test.scan.nexus.MandelbrotExampleTest
		MandelbrotModel modelA = new MandelbrotModel(), modelB = new MandelbrotModel();
		modelA.setName("mandelbrotA");
		modelA.setRealAxisName("xNex");
		modelA.setImaginaryAxisName("yNex");
		modelA.setColumns(64);
		modelA.setRows(64);
		dservice.createRunnableDevice(modelA);
		modelB.setName("mandelbrotB");
		modelB.setRealAxisName("xNex");
		modelB.setImaginaryAxisName("yNex");
		modelB.setColumns(64);
		modelB.setRows(64);
		dservice.createRunnableDevice(modelB);	
		
		qbf = new QueueBeanFactory();
	}

	@After
	public void tearDown() {
		ServicesHolder.setScannableDeviceService(null);
		ServicesHolder.setDeviceService(null);
	}
	
	@Test
	public void testPathConfig() throws QueueModelException {
		ScanAtomAssembler scAtAss = new ScanAtomAssembler(null);
		CompoundModel<?> cMod = new CompoundModel<>(Arrays.asList(new StepModel("stage_x", 0.0, 10.5, 1.5)));
		
		/*
		 * Fully specified by template
		 */
		Map<String, List<IQueueValue<?>>> pMods = new LinkedHashMap<>();
		pMods.put("stage_x", Arrays.asList(new QueueValue<String>("model", "Step"), new QueueValue<Double>("start", 0.0),
				new QueueValue<Double>("stop", 10.5), new QueueValue<Double>("step", 1.5)));
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, List<IQueueValue<?>>>(), new ArrayList<IQueueValue<?>>());
		
		ScanAtom assAt = scAtAss.assemble(scAtMod, new ExperimentConfiguration(null, null, null));
		assertEquals("Template specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
		
		/*
		 * Fully specified by ExperimentConfiguration
		 */
		scAtMod = new ScanAtom("testScan", new HashMap<String, List<IQueueValue<?>>>(), new HashMap<String, List<IQueueValue<?>>>(), new ArrayList<IQueueValue<?>>());
		ExperimentConfiguration config = new ExperimentConfiguration(null, null, pMods);
		assAt = scAtAss.assemble(scAtMod, config);
		assertEquals("ExperimentConfiguration specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
		
		try {
			scAtMod = new ScanAtom("testScan", pMods, new HashMap<String, List<IQueueValue<?>>>(), new ArrayList<IQueueValue<?>>());
			assAt = scAtAss.assemble(scAtMod, config);
			fail("Should not be able to specify path model for same axis twice");
		} catch (ModelEvaluationException meEx) {
			//expected - config and scAtMod both define the "stage_x" path. We don't know which to use, so just don't allow this behaviour
		}
		
		/*
		 * Part specified by template, part specified by localValues
		 */
		pMods = new LinkedHashMap<>();
		pMods.put("stage_x", Arrays.asList(new QueueValue<String>("model", "Step"), new QueueValue<String>("start", true),
				new QueueValue<String>("stop", true), new QueueValue<String>("step", true)));
		List<IQueueValue<?>> localValues = new ArrayList<>();
		localValues.add(new QueueValue<Double>("start", 0.0));
		localValues.add(new QueueValue<Double>("stop", 10.5));
		localValues.add(new QueueValue<Double>("step", 1.5));
		config = new ExperimentConfiguration(localValues, null, pMods);
		
		assAt = scAtAss.assemble(scAtMod, config);
		assertEquals("Mixed template/localValues specified path differs from expected", cMod, assAt.getScanReq().getCompoundModel());
	}
	
	/**
	 * Tests the creation of a fully configured ScanAtom
	 * @throws Exception
	 */
//	@Test
	public void testScanAtomCreation() throws Exception {
		//ScanAtom model
		Map<String, List<IQueueValue<?>>> pMods = new LinkedHashMap<>();
		pMods.put("stage_x", Arrays.asList(new QueueValue<String>("model", "Step"), new QueueValue<Double>("start", 0.0), new QueueValue<Double>("stop", 10.5), new QueueValue<Double>("step", 1.5)));
		Map<String, List<IQueueValue<?>>> dMods = new LinkedHashMap<>();
		dMods.put("mandelbrotA", Arrays.asList(new QueueValue<String>("exposureTime", true)));
		Collection<IQueueValue<?>> mons = Arrays.asList(new QueueValue<String>("monitor2"));
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, dMods, mons);
		qbf.registerAtom(scAtMod);
		
		ExperimentConfiguration config = new ExperimentConfiguration(Arrays.asList(new QueueValue<Double>("exposureTime", 30.0)), null, null);
		
		assertEquals("Produced task is not correctly configured", createScanAtom(), qbf.assembleQueueAtom(new QueueValue<>("testScan", true), config));
	}
	
	private ScanAtom createScanAtom() throws ScanningException {
		ScanRequest<?> scanReq = new ScanRequest<>();
		scanReq.setCompoundModel(new CompoundModel<>(Arrays.asList(new StepModel("stage_x", 0.0, 10.5, 1.5))));
		Map<String, Object> detectors = new HashMap<>();
		detectors.put("mandelbrotA", ServicesHolder.getDeviceService().getRunnableDevice("mandelbrotA").getModel());
		((IDetectorModel)detectors.get("mandelbrotA")).setExposureTime(30);
		detectors.put("mandelbrotB", ServicesHolder.getDeviceService().getRunnableDevice("mandelbrotB").getModel());
		((IDetectorModel)detectors.get("mandelbrotB")).setExposureTime(30);
		scanReq.setDetectors(detectors);
		scanReq.setMonitorNames(Arrays.asList("monitor2"));
		return new ScanAtom("testScan", scanReq);
	}

}
