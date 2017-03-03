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
package org.eclipse.scanning.server.servlet;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.scan.process.IPreprocessor;
import org.eclipse.scanning.api.scan.process.ProcessingException;

/**
 * A servlet to do any scan type based on the information provided
 * in a ScanBean.
 * 
 * @see example.xml
 * 
     Spring config started, for instance:
    <pre>
    
    {@literal <bean id="scanServlet" class="org.eclipse.scanning.server.servlet.ScanServlet" init-method="connect">}
    {@literal    <property name="broker"      value="tcp://p45-control:61616" />}
    {@literal    <property name="submitQueue" value="uk.ac.diamond.p45.submitQueue" />}
    {@literal    <property name="statusSet"   value="uk.ac.diamond.p45.statusSet"   />}
    {@literal    <property name="statusTopic" value="uk.ac.diamond.p45.statusTopic" />}
    {@literal    <property name="durable"     value="true" />}
    {@literal </bean>}
     
    </pre>
    
    FIXME Add security via activemq layer. Anyone can run this now.

 * 
 * @author Matthew Gerring
 *
 */
public class ScanServlet extends AbstractConsumerServlet<ScanBean> {
	
	public ScanServlet() {
		setPauseOnStart(true);
	}
	
	@Override
	public String getName() {
		return "Scan Consumer";
	}

	@Override
	public ScanProcess createProcess(ScanBean scanBean, IPublisher<ScanBean> response) throws EventException {
		
		if (scanBean.getScanRequest()==null) throw new EventException("The scan must include a request to run something!");
		preprocess(scanBean);
		return new ScanProcess(scanBean, response, isBlocking());
	}

	private void preprocess(ScanBean scanBean) throws ProcessingException {
		ScanRequest<?> req = scanBean.getScanRequest();
		if (req.isIgnorePreprocess()) {
			return;
		}
		for (IPreprocessor processor : Services.getPreprocessors()) {
			req = processor.preprocess(req);
		}
		scanBean.setScanRequest(req);
	}
}
