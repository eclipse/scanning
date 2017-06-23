package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.spooler.QueueSpoolerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueBeanFactoryTest {
	
	private IQueueSpoolerService qss;
	private IQueueBeanFactory qbf;
	
	@Before
	public void setUp() {
		qss = new QueueSpoolerService();
		qbf = qss.getQueueBeanFactory();
		ServicesHolder.setQueueSpoolerService(qss);
	}
	
	@After
	public void tearDown() {
		qss = null;
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
		PositionerAtom positAtomB = new PositionerAtom("setYummy","yummy", 80.0);
		
		//... and these are the bits that are needed to make the atom
		PositionerAtom positAModel = new PositionerAtom("setDummy", true, "dummy", new QueueValue<String>("dummyValue", true));
		PositionerAtom positBModel = new PositionerAtom("setYummy", true, "yummy", new QueueValue<String>("locVal", true));
		qbf.registerAtom(positAModel);
		qbf.registerAtom(positBModel);
		qbf.registerGlobalValue(new QueueValue<>("dummyValue", 10));

		//Try to populate from global values
		PositionerAtom dum = (PositionerAtom)qbf.assembleQueueAtom(new QueueValue<String>("setDummy", true), null); //null since we're after a global value
		assertEquals("PositionerAtom configured from global values is wrong", positAtomA, dum);
		
		//Try to populate from local values
		List<IQueueValue<?>> localValues = Arrays.asList(new QueueValue<Double>("locVal", 80.));
		dum = (PositionerAtom)qbf.assembleQueueAtom(new QueueValue<String>("setYummy", true), localValues);
		assertEquals("PositionerAtom configured from local values is wrong", positAtomB, dum);
	}
		
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
