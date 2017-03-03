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
package org.eclipse.scanning.api.device;

/**
 * 
   <h3>Welcome to Watchdogs</h3>
   <i>The new standard in scan monitoring</i>
   <p>
   
 * This service holds available watchdogs and if they are
 * active will start them for a given IRunnableDevice.
 * 
 * Multiple watchdogs may be created and activated in spring.
 * These will be started if any scan is run and their annotations
 * called at different points of the scan so that they can monitor
 * the scan.
 * 
    <h3>Adding in Spring</h3>
    <pre>
    {@literal <!--  Watchdog Example -->}
	{@literal <bean id="topupModel" class="org.eclipse.scanning.api.device.models.DeviceWatchdogModel">}
	{@literal 	<property name="countdownName"          value="topup"/>}
	{@literal 	<property name="periodName"             value="period"/>}
	{@literal 	<property name="cooloff"                value="4000"/>}
	{@literal 	<property name="warmup"                 value="5000"/>}
    {@literal     <property name="bundle"                 value="org.eclipse.scanning.api" /> <!-- Delete for real spring? -->}
	{@literal </bean>}
	{@literal <bean id="topupWatchdog" class="org.eclipse.scanning.sequencer.watchdog.TopupWatchdog" init-method="activate">}
	{@literal 	<property name="model"             ref="topupModel"/>}
    {@literal     <property name="bundle"            value="org.eclipse.scanning.sequencer" /> <!-- Delete for real spring? -->}
	{@literal </bean>}
    </pre>
    
    <p>
    
    <h3>Controlling from Jython</h3>
<ul>
    <li>Get the service from one of the holders e.g. <code>wservice = org.eclipse.scanning.sequencer.ServiceHolder.getWatchdogService();</code></li>
    <li>Set the watchdog required to disabled. <code>wservice.getWatchdog("topup").setEnabled(false);</code></li>
</ul>

 * 
 * @author Matthew Gerring
 *
 */
public interface IDeviceWatchdogService {

	/**
	 * Call to add a watchdog to a scan
	 * @param dog
	 */
	void register(IDeviceWatchdog dog);
	
	/**
	 * Call to remove a watchdog from a scan
	 * @param dog
	 */
	void unregister(IDeviceWatchdog dog);
	
	/**
	 * Initiate a list of dogs to run with 
	 * the device. These then implement @ScanStart, @PointStart, @ScanFinally
	 * as required to participate themselves in the running of the device
	 * and watch the process.
	 * 
	 * This process actually makes new dogs from the active list which means that
	 * the IRunnableDevice<?> passed in is unique to that list.
	 * 
	 * @param device
	 * @return list of objects which may be added to the scan and will be processed
	 * by their annotations.
	 */
	IDeviceController create(IPausableDevice<?> device);
	
	/**
	 * Get the watchdog by name.
	 * 
	 * @param name
	 * @return the watchdog with this name
	 */
	IDeviceWatchdog getWatchdog(String name);

}
