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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static final Logger logger = LoggerFactory.getLogger(ScanServlet.class);
	
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

		// Debugging makes code messy but switching this on can prove useful.
		// Test used because output message does work.
		debug("Accepting bean", scanBean, response);		
		preprocess(scanBean);
		debug("After processing bean (normally no change)", scanBean, response);		
		
		return new ScanProcess(scanBean, response, isBlocking());
	}

	private void debug(String message, ScanBean scanBean, IPublisher<ScanBean> response) {
		
		if (!logger.isDebugEnabled()) return;
		
		logger.debug("{} : {}", message, scanBean);
		try {
			logger.debug("from request : {}", Services.getEventService().getEventConnectorService().marshal(scanBean.getScanRequest()));
		} catch (Exception e) {
			logger.error("Error printing marshalled debugging scan request!", e);
		}
		logger.debug("at response URI {}", response.getUri());
		
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
