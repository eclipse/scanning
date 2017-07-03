package org.eclipse.scanning.test.event.queues.spooler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.event.queues.spooler.PositionerAtomAssembler;
import org.junit.Test;

public class PositionerAtomAssemblerTest {
	
	@Test
	public void testPositionerAtomCreation() throws QueueModelException {
		//This is the positioner atom we want...
		PositionerAtom positAtomA = new PositionerAtom("setDummy","dummy", 10.0);
		positAtomA.setName("Set position of 'dummy'=10.0");
		PositionerAtom positAtomB = new PositionerAtom("setYummy","yummy", 80.0);
		positAtomB.setName("Set position of 'yummy'=80.0");

		//... and these are the bits that are needed to make the atom
		PositionerAtom positAModel = new PositionerAtom("setDummy", true, "dummy", 10.0);
		PositionerAtom positBModel = new PositionerAtom("setYummy", true, "yummy", new QueueValue<String>("locVal", true));
		
		ExperimentConfiguration config = new ExperimentConfiguration(Arrays.asList(new QueueValue<Double>("locVal", 80.)), null, null);
		
		PositionerAtomAssembler posAtAss = new PositionerAtomAssembler(null);
		assertEquals("Simple atom differs from expected", positAtomA, posAtAss.assemble(positAModel, config));
		assertEquals("Atom with localValue to configure differs from expected", positAtomB, posAtAss.assemble(positBModel, config));
	}

	@Test
	public void testSetName() throws QueueModelException {
		PositionerAtom positAtomA = new PositionerAtom("setDummy","dummy", 10.0);
		positAtomA.addPositioner("yummy", 80.0);
		positAtomA.addPositioner("valve", "open");
		
		PositionerAtomAssembler posAtAss = new PositionerAtomAssembler(null);
		assertEquals("Set position of 'dummy'=10.0, 'yummy'=80.0, 'valve'=open", posAtAss.assemble(positAtomA, null).getName());
	}
}
