package org.eclipse.scanning.test.event.queues.spooler;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({
	PositionerAtomAssemblerTest.class,
	QueueBeanFactoryTest.class,
	ScanAtomAssemblerTest.class
})
public class Suite {

}
