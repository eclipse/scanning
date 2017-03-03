package org.eclipse.scanning.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * This is all the suites which is just a convenient way
 * to run a lot of tests locally.
 * 
 * @author Matthew Gerring
 *
 */
@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({
	
	org.eclipse.scanning.test.annot.Suite.class,
	org.eclipse.scanning.test.command.Suite.class,
	org.eclipse.scanning.test.epics.Suite.class,
	org.eclipse.scanning.test.event.queues.api.Suite.class,
	org.eclipse.scanning.test.event.queues.beans.Suite.class,
	org.eclipse.scanning.test.event.queues.processes.Suite.class,
	org.eclipse.scanning.test.event.queues.Suite.class,
	org.eclipse.scanning.test.fakespring.Suite.class,
	org.eclipse.scanning.test.filter.Suite.class,
	org.eclipse.scanning.test.malcolm.real.Suite.class,
	org.eclipse.scanning.test.messaging.Suite.class,
	org.eclipse.scanning.test.points.Suite.class,
	org.eclipse.scanning.test.remote.Suite.class,
	org.eclipse.scanning.test.scan.nexus.Suite.class,
	org.eclipse.scanning.test.scan.preprocess.Suite.class,
	org.eclipse.scanning.test.scan.servlet.Suite.class,
	org.eclipse.scanning.test.scan.Suite.class,
	org.eclipse.scanning.test.stashing.Suite.class,
	org.eclipse.scanning.test.validation.Suite.class
	
})
public class All {

}
