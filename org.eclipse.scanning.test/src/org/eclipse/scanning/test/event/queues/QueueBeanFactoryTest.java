package org.eclipse.scanning.test.event.queues;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;

public class QueueBeanFactoryTest {
	
	private IQueueBeanFactory qbf;
	
	@Before
	public void setUp() {
		qbf = new QueueBeanFactory();
	}
	
	@Test
	public void testPositionerAtomConfig() {
		//Add positioner to the queue atom register
		String atomShrtNm = addBasicDummyPositionerConfig();
		
		List<String> atomReg = qbf.getQueueAtomRegister();
		assertEquals("Should only be one queueable registered in the factory", 1, atomReg.size());
		assertEquals("No atom with the expected short name ("+atomShrtNm+") registered!", atomShrtNm, atomReg.get(0));
		
		//Get the atom and check it's config
		PositionerAtom dum = (PositionerAtom)qbf.getQueueAtom(atomShrtNm);
		assertFalse("No atom with the expected shortName registered", dum == null);
	}
	
	@Test
	public void testSimpleConfigAndBuild() {
		//Add positioner to the queue atom register
		String atomShrtNm = addBasicDummyPositionerConfig();
		
		//Register a simple TaskBean and SubTaskAtom model containing the positioner
		List<String> atoms = Arrays.asList(new String[]{atomShrtNm});
		SubTaskAtomModel simpleSubTask = new SubTaskAtomModel("mvDum", "Move dummy", atoms);
		qbf.registerAtom(simpleSubTask);
		List<String> subTasks = Arrays.asList(new String[]{simpleSubTask.getShortName()});
		TaskBeanModel simpleTask = new TaskBeanModel("Execute move dummy");
		qbf.registerTask(simpleTask);
	}
	
	private String addBasicDummyPositionerConfig() {
		String shortName = "testAtom";
		PositionerAtom positAtom = new PositionerAtom(shortName, "Set dummy to 10", "dummy", 10);
		qbf.registerAtom(positAtom);
		
		return shortName;
	}

}
