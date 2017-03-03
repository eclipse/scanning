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
package org.eclipse.scanning.sequencer.watchdog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.expressions.IExpressionEngine;
import org.eclipse.dawnsci.analysis.api.expressions.IExpressionService;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.device.models.DeviceWatchdogModel;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.sequencer.expression.ServerExpressionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Monitors an expression of scannables and if one of the values changes, reevaluates the
 * expression.
 * 
 
  Example XML configuration
    <pre>
    {@literal <!--  Watchdog Expression Example -->}
	{@literal <bean id="expressionModel" class="org.eclipse.scanning.api.device.models.DeviceWatchdogModel">}
	{@literal 	<property name="expression"   value="beamcurrent >= 1.0 &amp;&amp; !portshutter.equalsIgnoreCase(&quot;Closed&quot;)"/>}
	{@literal 	<property name="message"      value="Beam has been lost"/>}
    {@literal   <property name="bundle"       value="org.eclipse.scanning.api" /> <!-- Delete for real spring? -->}
	{@literal </bean>}
	{@literal <bean id="expressionWatchdog" class="org.eclipse.scanning.sequencer.watchdog.ExpressionWatchdog" init-method="activate">}
	{@literal 	<property name="model"        ref="expressionModel"/>}
    {@literal   <property name="bundle"       value="org.eclipse.scanning.sequencer" /> <!-- Delete for real spring? -->}
	{@literal </bean>}

 * 
 * @author Matthew Gerring
 *
 */
public class ExpressionWatchdog extends AbstractWatchdog implements IPositionListener {
	
	private static Logger logger = LoggerFactory.getLogger(ExpressionWatchdog.class);

	private IExpressionEngine engine;
	private IPosition         lastCompletedPoint;


	private List<IScannable<?>>       scannables;
	private static IExpressionService expressionService;

	public ExpressionWatchdog() {
		super();
	}
	public ExpressionWatchdog(DeviceWatchdogModel model) {
		super(model);
	}

	String getId() {
		return model.getExpression();
	}

	/**
	 * Called on a thread when the position changes.
	 */
	public void positionChanged(PositionEvent evt) {
		checkPosition(evt.getPosition());
	}
	public void positionPerformed(PositionEvent evt) {
		checkPosition(evt.getPosition());
	}

	private void checkPosition(IPosition pos) {
		try {
			if (engine==null) return;
			
			if (pos.getNames().size()!=1) return;
			String name = pos.getNames().get(0);
			engine.addLoadedVariable(name, pos.get(name));
			checkExpression(true);
					
		} catch (Exception ne) {
			logger.error("Cannot process position "+pos, ne);
		}	
	}
	
	private boolean checkExpression(boolean requirePause) throws Exception {
		Boolean ok = engine.evaluate();
		
		if (requirePause) {
			
			if (!ok) {
			    controller.pause(getId(), model); // Will not pause if already paused.
				
			} else {
				if (lastCompletedPoint!=null) {
					controller.seek(getId(), lastCompletedPoint.getStepIndex());
				}
				controller.resume(getId()); // Will not resume unless paused by us
			}
		}
		return ok;
	}
	
	@ScanStart
	public void start(ScanBean bean, IPosition firstPosition) throws ScanningException {
		
		logger.debug("Expression Watchdog starting on "+controller.getName());
		try {
		    this.engine = getExpressionService().getExpressionEngine();
		    
		    engine.createExpression(model.getExpression()); // Parses expression, may send exception on syntax
		    Collection<String> names = engine.getVariableNamesFromExpression();
		    this.scannables = new ArrayList<>(names.size());
		    for (String name : names) {
				IScannable<?> scannable = getScannable(name);
				scannables.add(scannable);
				
			    if (!(scannable instanceof IPositionListenable)) throw new ScanningException(name+" is not a position listenable!");

				engine.addLoadedVariable(scannable.getName(), scannable.getPosition());
		    }
		    
		    // Check it
		    boolean ok = checkExpression(false);
		    if (!ok) throw new ScanningException(model.getMessage()+". The expression '"+model.getExpression()+"' is false and a scan may not be run!");
		    
		    // Listen to it
		    for (IScannable<?> scannable : scannables) {
			    ((IPositionListenable)scannable).addPositionListener(this);
			}
		    
		    checkPosition(firstPosition);
		    
		} catch (ScanningException ne) {
			throw ne; // If there is something badly wrong a proper scanning exception will be prepared and thrown
		} catch (Exception ne) {
			logger.error("Cannot start watchdog!", ne);
		}
		logger.debug("Expression Watchdog started on "+controller.getName());
	} 
	
	@PointEnd
	public void pointEnd(IPosition done) {
		this.lastCompletedPoint = done;
	}
	
	@ScanFinally
	public void stop() {
		logger.debug("Expression Watchdog stopping on "+controller.getName());
		try {
			if (scannables!=null) for (IScannable<?> scannable : scannables) {
		    	((IPositionListenable)scannable).removePositionListener(this);
			}
			scannables.clear();
			engine = null;
		    
		} catch (Exception ne) {
			logger.error("Cannot stop watchdog!", ne);
		}
		logger.debug("Expression Watchdog stopped on "+controller.getName());
	}
	
	
	private BundleContext bcontext;
	
	public IExpressionService getExpressionService() {
		if (expressionService==null) {
			ServiceReference<IExpressionService> ref = bcontext.getServiceReference(IExpressionService.class);
			if (ref!=null) expressionService = bcontext.getService(ref);
		}
		return expressionService;
	}
	public void setExpressionService(IExpressionService eservice) {
		ExpressionWatchdog.expressionService = eservice;
	}
	public static void setTestExpressionService(ServerExpressionService eservice) {
		expressionService = eservice;
	}
	
	public void start(ComponentContext context) {
		this.bcontext = context.getBundleContext();
	}

}
