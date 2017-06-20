package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;
import org.eclipse.scanning.event.queues.QueueBeanFactory;
import org.junit.Before;
import org.junit.Test;

public class QueueBeanFactoryTest {
	
	private IQueueBeanFactory qbf;
	
	@Before
	public void setUp() {
		qbf = new QueueBeanFactory();
	}
	
	@Test
	public void testAddRemoveAtoms() throws QueueModelException {
		//Add positioner to the queue atom register
		String reference = "testAtom";
		PositionerAtom positAtom = new PositionerAtom(reference, "dummy", 10);
		positAtom.setName("Set dummy to 10");
		qbf.registerAtom(positAtom);
		
		List<String> atomReg = qbf.getQueueAtomRegister();
		assertEquals("Should only be one queue atoms registered in the factory", 1, atomReg.size());
		assertEquals("No atom with the expected reference ("+reference+") registered!", reference, atomReg.get(0));
		
		//Get the atom and check it's config
		PositionerAtom dum = (PositionerAtom)qbf.getQueueAtom(reference);
		assertFalse("No atom with the expected reference registered", dum == null);
		
		
		PositionerAtom detXAtom = new PositionerAtom("setDetX", "dummy", 225);
		detXAtom.setName("Set detX to 10");
		qbf.registerAtom(detXAtom);
//TODO?		atomReg = qbf.getQueueAtomRegister();
		assertEquals("Should be two queue atoms registered in the factory", 2, atomReg.size());
		SubTaskAtomModel simpleSubTask = new SubTaskAtomModel("mvDum", "Move dummy & set detector X position", atomReg);
		qbf.registerAtom(simpleSubTask);
//TODO?		atomReg = qbf.getQueueAtomRegister();
		assertEquals("Should be three queue atoms registered in the factory", 3, atomReg.size());
		qbf.unregisterAtom("setDetX");
		assertEquals("Should be two queue atoms registered in the factory", 2, atomReg.size());
		try {
			qbf.getQueueAtom("setDetX");
			fail("Fetching unregistered atom did not throw an exception");
		} catch (QueueModelException qme) {
			//expected
		}

		qbf.unregisterAtom("mvDum");
		assertEquals("Should be one queue atoms registered in the factory", 1, atomReg.size());
		try {
			qbf.getQueueAtom("mvDum");
			fail("Fetching unregistered atom did not throw an exception");
		} catch (QueueModelException qme) {
			//expected
		}
		
		//Register another atom with the same short name (this should throw an exception!)
		try {
			qbf.registerAtom(positAtom);
			fail("Should not be able to add ");
		} catch (QueueModelException qme) {
			//Expected
		}
	}
	
	@Test
	public void testSimpleConfigAndBuild() throws QueueModelException {
		//Add positioner to the queue atom register
		PositionerAtom positAtom = new PositionerAtom("setDummy","dummy", 10);
		positAtom.setName("Set dummy to 10");
		PositionerAtom detXAtom = new PositionerAtom("setDetX", "dummy", 225);
		detXAtom.setName("Set detX to 10");
		qbf.registerAtom(positAtom);
		qbf.registerAtom(detXAtom);
		
		//Register a simple SubTaskAtom model containing the positioner & detX setting
		List<String> atoms = Arrays.asList(new String[]{"setDummy", "setDetX"});
		SubTaskAtomModel simpleSubTask = new SubTaskAtomModel("mvDum", "Move dummy & set detector X position", atoms);
		qbf.registerAtom(simpleSubTask);
		
		SubTaskAtom mvDum = qbf.getQueueAtom("mvDum");
		assertEquals("Name of returned SubTaskAtom is wrong", "Move dummy & set detector X position", mvDum.getName());
		assertEquals("Short name of returned SubTaskAtom is wrong", "mvDum", mvDum.getShortName());
		assertEquals("Unexpected number of atoms in AtomQueue", 2, mvDum.atomQueueSize());
		List<QueueAtom> atomQueue = mvDum.getAtomQueue();
		assertEquals("First atom is not the expected 'setDummy'", positAtom, atomQueue.get(0));
		assertEquals("Second atom is not the expected 'setDetX'", detXAtom, atomQueue.get(1));
		
		//Register a simple TaskBean model containing just the SubTaskAtom
		List<String> subTasks = Arrays.asList(new String[]{simpleSubTask.getShortName()});
		TaskBeanModel simpleTask = new TaskBeanModel("execMvDum", "Execute move dummy & detX", subTasks);
		qbf.registerTask(simpleTask);
		
		TaskBean execDum = qbf.assembleTaskBean("execMvDum");
		assertEquals("Name of returned TaskBean is wrong", "Execute move dummy & detX", execDum.getName());
		assertEquals("Short name of returned TaskBean is wrong", "execMvDum", execDum.getShortName());
		assertEquals("Unexpected number of atoms in AtomQueue", 1, execDum.atomQueueSize());
		SubTaskAtom atomQueueSubTaskAtom = execDum.getAtomQueue().get(0);
		assertEquals("SubTaskAtom is not the expected 'mvDum'", mvDum, atomQueueSubTaskAtom);
		
		TaskBean secondExecDum = qbf.assembleDefaultTaskBean();
		assertEquals("assembleDefault should return the same model as explicitly requested when one model registered", secondExecDum, execDum);
	}
	
}
