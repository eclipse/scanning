package org.eclipse.scanning.test.scan.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.ConsumerStatus;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.server.servlet.AbstractConsumerServlet;
import org.eclipse.scanning.server.servlet.ScanServlet;
import org.junit.Test;

/**
 * 
 * When the scan servlet is started:
 * 1. If there are things in the queue it should pause.
 * 2. If the queue is empty it should not pause.
 * 
 * @author Matthew Gerring
 *
 */
public class StartServerTest extends AbstractServletTest {

	@Override
	protected AbstractConsumerServlet<ScanBean> createServlet() throws EventException, URISyntaxException {
		return null;
	}

	
	@Test
	public void runServletEmptyQueue() throws Exception {
		
		ScanServlet servlet = new ScanServlet();
		try {
			servlet.setBroker(uri.toString());
			servlet.connect(); // Gets called by Spring automatically
			
			assertTrue(servlet.getConsumer().isActive());
			
		} finally {
			servlet.disconnect();
		}
		
	}
	
	@Test
	public void runServletSomethingInQueue() throws Exception {
		
		// We do not start it!
		ScanServlet servlet = new ScanServlet();
		servlet.setBroker(uri.toString());
 		
		// Now there is something in the queue
		submit(servlet, createGridScan());
		
		try {
			servlet.connect(); // Gets called by Spring automatically
			servlet.getConsumer().awaitStart();
			
			assertEquals(ConsumerStatus.PAUSED, servlet.getConsumer().getConsumerStatus());
			
		} finally {
			servlet.getConsumer().cleanQueue(servlet.getSubmitQueue());
			servlet.getConsumer().cleanQueue(servlet.getStatusSet());
			servlet.disconnect();
		}
		
	}

}
