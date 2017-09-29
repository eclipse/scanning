package org.eclipse.scanning.test.event.queues.spooler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.queues.spooler.ServicesHolder;
import org.eclipse.scanning.event.queues.spooler.QueueBeanFactory;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueBeanFactoryTest {

	private IQueueBeanFactory qbf;

	private boolean extendedTest;

	@Before
	public void setUp() {
		qbf = new QueueBeanFactory();
	}

	@After
	public void tearDown() {
		qbf = null;

		if (extendedTest) {
			ServicesHolder.setScannableDeviceService(null);
			ServicesHolder.setRunnableDeviceService(null);
		}
	}

	private void setUpExtendedInfrastructure() throws ScanningException {
		IScannableDeviceService connector = new MockScannableConnector(null);
		ServicesHolder.setScannableDeviceService(connector);
		IRunnableDeviceService dservice = new RunnableDeviceServiceImpl(connector); // Not testing OSGi so using hard coded service.
		ServicesHolder.setRunnableDeviceService(dservice);

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

		extendedTest = true;
	}

	/**
	 * Tests basic registration and deregistration of atoms and beans from the QueueBeanFactory
	 * @throws QueueModelException
	 */
	@Test
	public void testAddRemoveAtoms() throws QueueModelException {
		//Add positioner to the queue atom register
		String reference = "testAtom";
		QueueValue<String> refVal = new QueueValue<>(reference, true);
		PositionerAtom positAtom = new PositionerAtom(reference, "Badger badger mushroom", 10);
		positAtom.setName("Set dummy to 10");
		qbf.registerAtom(positAtom);

		assertEquals("Should only be one queue atoms registered in the factory", 1, qbf.getQueueAtomRegister().size());
		assertEquals("No atom with the expected reference ("+refVal+") registered!", refVal, qbf.getQueueAtomRegister().get(0));

		//Get the atom and check it's config
		PositionerAtom dum = (PositionerAtom)qbf.assembleQueueAtom(refVal, null);
		assertFalse("No atom with the expected reference registered", dum == null);

		//Try adding a second atom...
		PositionerAtom detXAtom = new PositionerAtom("setDetX", "dummy", 225);
		detXAtom.setName("Set detX to 10");//TODO this should be set automatically
		qbf.registerAtom(detXAtom);
		assertEquals("Should be two queue atoms registered in the factory", 2, qbf.getQueueAtomRegister().size());
		//...and now try adding a SubTaskModel to check this can also be accessed in same way as other queue atoms...
		SubTaskAtom simpleSubTask = new SubTaskAtom("mvDum", "Move dummy & set detector X position", qbf.getQueueAtomRegister());
		qbf.registerAtom(simpleSubTask);
		assertEquals("Should be three queue atoms registered in the factory", 3, qbf.getQueueAtomRegister().size());
		//...and test the unregistering method
		qbf.unregisterAtom("setDetX");
		assertEquals("Should be two queue atoms registered in the factory", 2, qbf.getQueueAtomRegister().size());
		try {
			qbf.assembleQueueAtom(new QueueValue<>("setDetX", true), null);
			fail("Fetching unregistered atom did not throw an exception");
		} catch (QueueModelException qme) {
			//Exception is expected - that' what I'm testing for
			//(we alreayd unregistered this atom once)
		}

		qbf.unregisterAtom("mvDum");
		assertEquals("Should be one queue atoms registered in the factory", 1, qbf.getQueueAtomRegister().size());
		try {
			qbf.assembleQueueAtom(new QueueValue<>("mvDum", true), null);
			fail("Fetching unregistered atom did not throw an exception");
		} catch (QueueModelException qme) {
			//Exception is expected - that' what I'm testing for
		}

		//Register another atom with the same short name (this should throw an exception!)
		try {
			qbf.registerAtom(positAtom);
			fail("Should not be able to add ");
		} catch (QueueModelException qme) {
			//Exception is expected - that' what I'm testing for
		}
	}

	/**
	 * Tests the registration of a series of atoms, both subtasks and postioners, and
	 * their assembly into a complete taskbean
	 * @throws QueueModelException
	 */
	@Test
	public void testSimpleConfigAndBuild() throws QueueModelException {
		//Add positioner to the queue atom register
		PositionerAtom positAtom = new PositionerAtom("setDummy","dummy", 10);
		positAtom.setName("Set dummy to 10"); //TODO this should be set automatically
		PositionerAtom detXAtom = new PositionerAtom("setDetX", "dummy", 225);
		detXAtom.setName("Set detX to 10"); //TODO this should be set automatically
		qbf.registerAtom(positAtom);
		qbf.registerAtom(detXAtom);

		//Register a simple SubTaskAtom model containing the positioner & detX setting
		List<QueueValue<String>> atoms = new ArrayList<>();
		atoms.add(new QueueValue<>("setDummy", true));
		atoms.add(new QueueValue<>("setDetX", true));
		SubTaskAtom simpleSubTask = new SubTaskAtom("mvDum", "Move dummy & set detector X position", atoms);
		qbf.registerAtom(simpleSubTask);

		SubTaskAtom mvDum = qbf.assembleQueueAtom(new QueueValue<>("mvDum", true), null);
		assertEquals("Name of returned SubTaskAtom is wrong", "Move dummy & set detector X position", mvDum.getName());
		assertEquals("Short name of returned SubTaskAtom is wrong", "mvDum", mvDum.getShortName());
		assertEquals("Unexpected number of atoms in AtomQueue", 2, mvDum.atomQueueSize());
		List<QueueAtom> atomQueue = mvDum.getAtomQueue();
		assertEquals("First atom is not the expected 'setDummy'", positAtom, atomQueue.get(0));
		assertEquals("Second atom is not the expected 'setDetX'", detXAtom, atomQueue.get(1));

		//Register a simple TaskBean model containing just the SubTaskAtom
		List<QueueValue<String>> subTasks = new ArrayList<>();
		subTasks.add(new QueueValue<>(simpleSubTask.getShortName(), true));
		TaskBean simpleTask = new TaskBean("execMvDum", "Execute move dummy & detX", subTasks);
		qbf.registerTask(simpleTask);

		TaskBean execDum = qbf.assembleTaskBean(new QueueValue<>("execMvDum", true), null);
		assertEquals("Name of returned TaskBean is wrong", "Execute move dummy & detX", execDum.getName());
		assertEquals("Short name of returned TaskBean is wrong", "execMvDum", execDum.getShortName());
		assertEquals("Unexpected number of atoms in AtomQueue", 1, execDum.atomQueueSize());
		SubTaskAtom atomQueueSubTaskAtom = execDum.getAtomQueue().get(0);
		assertEquals("SubTaskAtom is not the expected 'mvDum'", mvDum, atomQueueSubTaskAtom);

		TaskBean secondExecDum = qbf.assembleDefaultTaskBean(null);
		assertEquals("assembleDefault should return the same model as explicitly requested when one model registered", secondExecDum, execDum);
	}

	/**
	 * Tests registration methods for values
	 * @throws QueueModelException
	 */
	@Test
	public void testRegisterUnRegisterValues() throws QueueModelException {
		IQueueValue<?> globValA = new QueueValue<Double>("locVal", 80.);
		IQueueValue<?> globValB = new QueueValue<Double>("homePosition", 1.235);

		qbf.registerGlobalValue(globValA);
		assertEquals("Should be only 1 value registered", 1, qbf.getGlobalValuesRegister().size());
		qbf.registerGlobalValue(globValB);
		assertEquals("Should be only 2 value registered", 2, qbf.getGlobalValuesRegister().size());
		try {
			qbf.registerGlobalValue(globValB);
			fail("Should not be able to register two values with same name");
		} catch (Exception ex) {
			//Expected - see above
		}

		assertEquals("Wrong global value returned", globValA, qbf.getGlobalValue(new QueueValue<String>("locVal", true)));

		qbf.unregisterGlobalValue("locVal");
		assertEquals("Should be only 1 value registered", 1, qbf.getGlobalValuesRegister().size());
		try {
			qbf.getGlobalValue(new QueueValue<String>("locVal", true));
			fail("Should not be able to return unregistered value");
		} catch (Exception ex) {
			//Expected - see above
		}
	}

	/**
	 * This tests the insertion of one value registered in the global values registry into a created PositionerAtom
	 * @throws QueueModelException
	 */
	@Test
	public void testSimpleQueueValueConfigAndBuild() throws QueueModelException {
		//This is the positioner atom we want...
		PositionerAtom positAtomA = new PositionerAtom("setDummy","dummy", 10);
		positAtomA.setName("Set position of 'dummy'=10");
		PositionerAtom positAtomB = new PositionerAtom("setYummy","yummy", 80.0);
		positAtomB.setName("Set position of 'yummy'=80.0");

		//... and these are the bits that are needed to make the atom
		PositionerAtom positAModel = new PositionerAtom("setDummy", true, "dummy", new QueueValue<String>("dummyValue", true));
		PositionerAtom positBModel = new PositionerAtom("setYummy", true, "yummy", new QueueValue<String>("locVal", true));
		qbf.registerAtom(positAModel);
		qbf.registerAtom(positBModel);
		qbf.registerGlobalValue(new QueueValue<>("dummyValue", 10));

		//Try to populate from global values
		PositionerAtom dum = (PositionerAtom)qbf.assembleQueueAtom(new QueueValue<String>("setDummy", true), new ExperimentConfiguration(null, null, null)); //null since we're after a global value
		assertEquals("PositionerAtom configured from global values is wrong", positAtomA, dum);

		//Try to populate from local values
		ExperimentConfiguration config = new ExperimentConfiguration(Arrays.asList(new QueueValue<Double>("locVal", 80.)), null, null);
		dum = (PositionerAtom)qbf.assembleQueueAtom(new QueueValue<String>("setYummy", true), config);
		assertEquals("PositionerAtom configured from local values is wrong", positAtomB, dum);
	}


	/**
	 * A more extended test, which to test the default XBeanAssemblers
	 * @throws QueueModelException
	 */
	@Test
	public void testAllBeansSimpleConfigPseudoIntegrationTest() throws Exception {
		setUpExtendedInfrastructure();
		/*
		 * Build the default TaskBean containing two SubTaskAtoms and with localValues containing exposureTime
		 * SubTask1 contains: a Positioner(stage_x -> var(homePosition)) and a Monitor(monitor1)
		 * SubTask2 contains: a Scan(Step[stage_x,0.0,10.5,1.5], Det1["mandelbrotA", var(exposureTime)], Det2["mandelbrotB", var(exposureTime)], Mon[monitor2])
		 * 					  and a Monitor(monitor2)
		 * Values: exposureTime = 30s; homePosition = 1.235
		 */

		//ScanAtom model
		Map<String, DeviceModel> pMods = new LinkedHashMap<>();
		Map<String, Object> pathConf = new HashMap<>();
		pathConf.put("start", 0.0);
		pathConf.put("stop", 10.5);
		pathConf.put("step", 1.5);
		pMods.put("stage_x", new DeviceModel("step", pathConf));
		Map<String, DeviceModel> dMods = new LinkedHashMap<>();
		Map<String, Object> detAConf = new HashMap<>();
		detAConf.put("exposureTime", new QueueValue<String>("exposureTime", true));
		DeviceModel detDevMod = new DeviceModel(null, detAConf);
		dMods.put("mandelbrotA", detDevMod);
		dMods.put("mandelbrotB", detDevMod);
		Collection<Object> mons = Arrays.asList(new QueueValue<String>("monitor2"));
		ScanAtom scAtMod = new ScanAtom("testScan", pMods, dMods, mons);
		qbf.registerAtom(scAtMod);

		//MonitorAtom models
		MonitorAtom monAtMod1 = new MonitorAtom("testMon1", true, "monitor1"),
					monAtMod2 = new MonitorAtom("testMon2", true, "monitor2");
		qbf.registerAtom(monAtMod1);
		qbf.registerAtom(monAtMod2);

		//PositionerAtom model
		PositionerAtom posAtMod = new PositionerAtom("testHomer", true, "stage_x", new QueueValue<String>("homePosition", true));
		qbf.registerAtom(posAtMod);

		//SubTaskAtom models
		SubTaskAtom stAtMod1 = new SubTaskAtom("testSubTask1", "Reset stage x position", true);
		stAtMod1.addAtom(new QueueValue<String>("testHomer", true));
		stAtMod1.addAtom(new QueueValue<String>("testMon1", true));
		SubTaskAtom stAtMod2 = new SubTaskAtom("testSubTask2", "Run MScan then monitor", Arrays.asList(new QueueValue<String>("testScan", true), new QueueValue<String>("testMon2", true)));
		qbf.registerAtom(stAtMod1);
		qbf.registerAtom(stAtMod2);

		//TaskBean model
		TaskBean tbMod = new TaskBean("testDefaultTask", "Reset stage and measure task", true);
		tbMod.addAtom(new QueueValue<String>("testSubTask1", true));
		tbMod.addAtom(new QueueValue<String>("testSubTask2", true));
		qbf.registerTask(tbMod);

		//Register global value
		qbf.registerGlobalValue(new QueueValue<Double>("homePosition", 1.235));

		ExperimentConfiguration config = new ExperimentConfiguration(Arrays.asList(new QueueValue<Double>("exposureTime", 30.0)), null, null);

		TaskBean exemplar = createCompleteDefaultTask(), produced = qbf.assembleDefaultTaskBean(config);

		if (!exemplar.equals(produced)) {
			assertTrue("Atom queue of "+produced.getName()+" are different lengths", exemplar.atomQueueSize() == produced.atomQueueSize());
			for (int i=0; i < produced.getAtomQueue().size(); i++) {
				SubTaskAtom exemplarSubTask = exemplar.getAtomQueue().get(i);
				SubTaskAtom producedSubTask = produced.getAtomQueue().get(i);
				if (!exemplarSubTask.equals(producedSubTask)) {
					analyseAtomQueues(producedSubTask, exemplarSubTask);
				}
			}
		}

		assertEquals("Produced task is not correctly configured", exemplar, produced);
	}

	private void analyseAtomQueues(SubTaskAtom produced, SubTaskAtom exemplar) {
		List<QueueAtom> exemplarAtomQueue = exemplar.getAtomQueue();
		List<QueueAtom> producedAtomQueue = produced.getAtomQueue();
		assertTrue("Atom queues of "+produced.getName()+" are different lengths", exemplar.atomQueueSize() == produced.atomQueueSize());
		for (int i=0; i < producedAtomQueue.size(); i++) {
			assertEquals(exemplarAtomQueue.get(i), producedAtomQueue.get(i));
		}
	}

	private TaskBean createCompleteDefaultTask() throws ScanningException {
		ScanAtom scAt = createScanAtom();

		MonitorAtom monAt1 = new MonitorAtom("testMon1", "monitor1"),
					monAt2 = new MonitorAtom("testMon2", "monitor2");
		monAt1.setName("Measure current value of 'monitor1'");
		monAt2.setName("Measure current value of 'monitor2'");

		PositionerAtom posAt = new PositionerAtom("testHomer", "stage_x", 1.235);
		posAt.setName("Set position of 'stage_x'=1.235");

		SubTaskAtom stAt1 = new SubTaskAtom("testSubTask1", "Reset stage x position");
		stAt1.addAtom(posAt);
		stAt1.addAtom(monAt1);
		SubTaskAtom stAt2 = new SubTaskAtom("testSubTask2", "Run MScan then monitor");
		stAt2.addAtom(scAt);
		stAt2.addAtom(monAt2);

		TaskBean defaultTB = new TaskBean("testDefaultTask", "Reset stage and measure task");
		defaultTB.addAtom(stAt1);
		defaultTB.addAtom(stAt2);
		return defaultTB;
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

//	TODO OLDprivate ScanAtom createScanAtom() throws ScanningException {
//		ScanRequest<?> scanReq = new ScanRequest<>();
//		scanReq.setCompoundModel(new CompoundModel<>(Arrays.asList(new StepModel("stage_x", 0.0, 10.5, 1.5))));
//		Map<String, Object> detectors = new HashMap<>();
//		detectors.put("mandelbrotA", ServicesHolder.getDeviceService().getRunnableDevice("mandelbrotA").getModel());
//		((IDetectorModel)detectors.get("mandelbrotA")).setExposureTime(30);
//		detectors.put("mandelbrotB", ServicesHolder.getDeviceService().getRunnableDevice("mandelbrotB").getModel());
//		((IDetectorModel)detectors.get("mandelbrotB")).setExposureTime(30);
//		scanReq.setDetectors(detectors);
//		scanReq.setMonitorNames(Arrays.asList("monitor2"));
//		return new ScanAtom("testScan", scanReq);
//	}


//		//This example taken from /dls_sw/i15-1/scripts/m1.py; is for setting the voltages on the I15-1 bimorph mirror
//		Map<String, Object> sesoVoltages = makeSesoMap();
//		Map<String, Object> offVoltages = makeOffMap();
//
//		//These are what we are trying to make
//		PositionerAtom sesoAtom = new PositionerAtom("setMirror", sesoVoltages);
//		PositionerAtom offAtom = new PositionerAtom("setMirror", offVoltages);
//
//		//The template we're going to populate
//		PositionerAtom mirrorTempl = new PositionerAtom("setMirror")
//	}



//	private Map<String, Object> makeSesoMap() {
//		Map<String, Object> sesoVoltages = new HashMap<>();
//		sesoVoltages.put("CH1", 1415);
//		sesoVoltages.put("CH2", 961.5);
//		sesoVoltages.put("CH3", 499.0);
//		sesoVoltages.put("CH4", 221.0);
//		sesoVoltages.put("CH5", 51.5);
//		sesoVoltages.put("CH6", -85.2);
//		sesoVoltages.put("CH7", -205.2);
//		sesoVoltages.put("CH8", -265.6);
//		sesoVoltages.put("CH9", -200.0);
//		sesoVoltages.put("CH10", 20.0);
//		sesoVoltages.put("CH11", 362.0);
//		sesoVoltages.put("CH12", 734.2);
//		sesoVoltages.put("CH13", 1032);
//		sesoVoltages.put("CH14", 1236);
//		sesoVoltages.put("CH15", 1574);
//		return sesoVoltages;
//	}
//	private Map<String, Object> makeOffMap() {
//		Map<String, Object> offVoltages = new HashMap<>();
//		offVoltages.put("CH1", 0);
//		offVoltages.put("CH2", 0);
//		offVoltages.put("CH3", 0);
//		offVoltages.put("CH4", 0);
//		offVoltages.put("CH5", 0);
//		offVoltages.put("CH6", 0);
//		offVoltages.put("CH7", 0);
//		offVoltages.put("CH8", 0);
//		offVoltages.put("CH9", 0);
//		offVoltages.put("CH10", 0);
//		offVoltages.put("CH11", 0);
//		offVoltages.put("CH12", 0);
//		offVoltages.put("CH13", 0);
//		offVoltages.put("CH14", 0);
//		offVoltages.put("CH15", 0);
//		return offVoltages;
//	}

}
