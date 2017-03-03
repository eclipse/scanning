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
package org.eclipse.scanning.device.ui.device.scannable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.scanning.api.IScannable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ControlValueJob<T> extends Job {
	
	private static Logger logger = LoggerFactory.getLogger(ControlValueJob.class);

	
	// Data
	private IScannable<T> scannable;
	private T             value;

    // UI
	private ControlValueCellEditor editor;

	public ControlValueJob() { // TODO Float spinner?
		this(null);
	}

	public ControlValueJob(ControlValueCellEditor editor) { // TODO Float spinner?
		super("Set Value");
		this.editor = editor;
	}
	

	public void setPosition(IScannable<T> scannable, T value) {
		cancel();
		this.scannable = scannable;
		this.value     = value;
		setName("Set '"+scannable.getName()+"' to "+value);
		schedule();
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			if (editor!=null) editor.setSafeEnabled(false);
		    scannable.setPosition(value); // Blocking call
		    if (editor!=null && value instanceof Number) {
		    	editor.setSafeValue(((Number)value).doubleValue());
		    }
		    return Status.OK_STATUS;
		    
		} catch (Exception e) {
			editor.setSafeText(e.getMessage());
			logger.error("Cannot set position!", e);
		    return Status.CANCEL_STATUS;
			
		} finally {
			editor.setSafeEnabled(true);
		}
	}

}
