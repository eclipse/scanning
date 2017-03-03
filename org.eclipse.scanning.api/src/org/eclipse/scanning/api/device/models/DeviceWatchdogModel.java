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
package org.eclipse.scanning.api.device.models;

/**
 * 
 * Model for confuring a watchdog.
 * 
<pre>
Topup watchdogs should watch the following PV:

SR-CS-FILL-01:COUNTDOWN: this is a float-valued counter that runs to zero
at the start of TopUp and remains there until the fill is complete when 
it resets to time before next TopUp fill,
 Example XML configuration
    <pre>
    {@literal <!--  Watchdog Example -->}
	{@literal <bean id="topupModel" class="org.eclipse.scanning.api.device.models.DeviceWatchdogModel">}
	{@literal 	<property name="countdownName"          value="topup"/>}
	{@literal 	<property name="cooloff"                value="4000"/>}
	{@literal 	<property name="warmup"                 value="5000"/>} 

    {@literal   <!-- Optional, recommended but not compulsory a scannable linked to SR-CS-RING-01:MODE, checks the mode is right -->}
    {@literal 	<property name="modeName"               value="mode"/>}

	{@literal   <!-- Optional, do not usually need to set -->}
    {@literal 	<property name="period"                 value="600000"/>}
	{@literal 	<property name="topupTime"              value="15000"/>}
	{@literal   <!-- End optional, do not usually need to set -->}

    {@literal   <property name="bundle"               value="org.eclipse.scanning.api" /> <!-- Delete for real spring? -->}
	{@literal </bean>}
	{@literal <bean id="topupWatchdog"    class="org.eclipse.scanning.sequencer.watchdog.TopupWatchdog" init-method="activate">}
	{@literal 	<property name="model"    ref="topupModel"/>}
    {@literal   <property name="bundle"   value="org.eclipse.scanning.sequencer" /> <!-- Delete for real spring? -->}
	{@literal </bean>}
    </pre>


<h3>Calculation of scannable parts of topup  </h3>  
    <pre>
    
    |<-w->|
    |.
    |  .
    |    .
    |      .
    |        .
    |          .
    |            .
    |              .|<-   c  ->|
    |                .
    |                  .
    |                    .
    |                      .
    |                        . |<-Tf->|
    |                          ........    t
    |                            
    |__________________________________(time)
    
    |<-              p              ->|
    
    w  - warmup
    c  - cooloff
    t  - time until next topup next occurs
    Tf - Topup fill time (variable but max 15s in normal mode)
    p  - Period of cycle, usually 10mins or so.
    
    In order for scanning to run, all of the following conditions must be satisfied:
    
    1. Mode is normal (8)
    2. t > c
    3. t < (p-Tf)-w
    
    </pre>
    
<h3>Ring Mode</h3>

The "Ring Mode" PV is SR-CS-RING-01:MODE.

This PV has various states: 
<img src="./doc/modes.png" /> 

In brief though, the only one you need to care about is state 8 = VMX. This is "normal" mode now that we've installed the new VMX (AKA DDBA) components.

If this PV = 8, then we're in normal mode. If this PV is anything else, then we're in some other state.

 * @author Matthew Gerring
 *
 */
public class DeviceWatchdogModel {

	/**
	 * This is an expression of scannables which are monitored and the expression
	 * reevaluated if they change during a scan. Each variable in the scan *must*
	 * be a scannable name. The expression will be evaluated using JEXL which is
	 * available through the IExpressionService. Any expression
	 */
	private String expression; // e.g. 'beamcurrent >= 1.0 && !portshutter.equalsIgnoreCase("Closed")'
	private String message;
	
	// t
	private String countdownName; // e.g. "topup", "countdown" PV likely to be SR-CS-FILL-01:COUNTDOWN which is in s
	
	// c in ms
	private long   cooloff;       // time in ms before topup for which the scan should be paused.
	
	// w in ms
	private long   warmup;        // time in ms after topup the scan should wait before starting.
	
	// p in ms
	private long   period = 10*60*1000; // Period in ms, default is 10min
	
	// Tf in ms
	private long   topupTime = 15*1000; // The time that a topup takes. This is varible but in normal mode <= 15s
	
	// The name of the mode pv, if any. 
	private String modeName;            // If this is set the PV will be checked to ensure that the topup mode is as expected.
	
	public String getCountdownName() {
		return countdownName;
	}
	public void setCountdownName(String monitorName) {
		this.countdownName = monitorName;
	}
	public long getCooloff() {
		return cooloff;
	}
	public void setCooloff(long cooloff) {
		this.cooloff = cooloff;
	}
	public long getWarmup() {
		return warmup;
	}
	public void setWarmup(long warmup) {
		this.warmup = warmup;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getExpression() {
		return expression;
	}
	public void setExpression(String expression) {
		this.expression = expression;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (cooloff ^ (cooloff >>> 32));
		result = prime * result + ((countdownName == null) ? 0 : countdownName.hashCode());
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + (int) (warmup ^ (warmup >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeviceWatchdogModel other = (DeviceWatchdogModel) obj;
		if (cooloff != other.cooloff)
			return false;
		if (countdownName == null) {
			if (other.countdownName != null)
				return false;
		} else if (!countdownName.equals(other.countdownName))
			return false;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (warmup != other.warmup)
			return false;
		return true;
	}
	public long getPeriod() {
		return period;
	}
	public void setPeriod(long period) {
		this.period = period;
	}
	public long getTopupTime() {
		return topupTime;
	}
	public void setTopupTime(long topupTime) {
		this.topupTime = topupTime;
	}
	public String getModeName() {
		return modeName;
	}
	public void setModeName(String modeName) {
		this.modeName = modeName;
	}
	
}
