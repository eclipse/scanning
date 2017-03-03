/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.test.scan.servlet;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.server.servlet.AbstractConsumerServlet;
import org.eclipse.scanning.server.servlet.ScanServlet;
import org.junit.Test;

public class ScanServletTest extends AbstractServletTest {

	protected AbstractConsumerServlet<ScanBean> createServlet() throws EventException, URISyntaxException {
		
		ScanServlet servlet = new ScanServlet();
		servlet.setBroker(uri.toString());
		servlet.setSubmitQueue("org.eclipse.scanning.test.servlet.submitQueue");
		servlet.setStatusSet("org.eclipse.scanning.test.servlet.statusSet");
		servlet.setStatusTopic("org.eclipse.scanning.test.servlet.statusTopic");
		servlet.setPauseOnStart(false);
		servlet.connect(); // Gets called by Spring automatically

		return servlet;
	}
	
	/**
	 * This test mimiks a client submitting a scan. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStepScan() throws Exception {
		
		ScanBean bean = createStepScan();
		runAndCheck(bean, 60);
	}
	
	/**
	 * This test mimiks a client submitting a scan. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStepScanProcessing() throws Exception {
		
		System.setProperty("org.eclipse.scanning.api.preprocessor.name", "example");
		try {
			ScanBean bean = createStepScan();
			List<ScanBean> beans = runAndCheck(bean, 20);
			// We now check that they all had xfred set.
			for (ScanBean scanBean : beans) {
				ScanRequest<?> req = scanBean.getScanRequest();
				
				StepModel step = (StepModel)req.getCompoundModel().getModels().toArray()[0];
				assertEquals("fred", step.getName());
			}
		} finally {
		    System.setProperty("org.eclipse.scanning.api.preprocessor.name", "");
		}
		
	}


	@Test
	public void testGridScan() throws Exception {
		
		ScanBean bean = createGridScan();
		runAndCheck(bean, 20);

	}

	@Test
	public void testStepGridScanNested1() throws Exception {
		
		ScanBean bean = createStepGridScan(1);
		runAndCheck(bean, 100);
	}
	
	@Test
	public void testStepGridScanNested5() throws Exception {
		
		ScanBean bean = createStepGridScan(5);
		runAndCheck(bean, 500);
	}

}
