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
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.dry.DryRunProcess;
import org.eclipse.scanning.api.event.scan.ScanBean;

/**
 * A servlet to do a dry run. Used in the XcenExample
 * 
 * @see config.xml
 * 
     Spring config started, for instance:
    <pre>
    
    {@literal <bean id="dryRun" class="org.eclipse.scanning.server.servlet.DryRunServlet" init-method="connect">}
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
public class DryRunServlet extends AbstractConsumerServlet<ScanBean> {
	
	@Override
	public String getName() {
		return "Scan Consumer";
	}

	@Override
	public IConsumerProcess<ScanBean> createProcess(ScanBean scanBean, IPublisher<ScanBean> response) throws EventException {
		return new DryRunProcess<ScanBean>(scanBean, response, true);
	}
}
