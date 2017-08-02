/*-
 *******************************************************************************
 * Copyright (c) 2011, 2014 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.device.ui.composites;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.richbeans.widgets.scalebox.ScaleBox;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class StepModelComposite extends Composite {

	private final ScaleBox    start,stop,step,exposureTime;

	public StepModelComposite(Composite parent, int style) {
		super(parent, style);
		
		setLayout(new GridLayout(2, false));
					
		Label label = new Label(this, SWT.NONE);
		label.setText("Start");
		
		start = new ScaleBox(this, SWT.NONE);
		start.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		start.setUnit("eV");
		start.setMinimum(Integer.getInteger("org.eclipse.scanning.device.ui.composites.stepStartMin", -100000).doubleValue());
		start.setMaximum(Integer.getInteger("org.eclipse.scanning.device.ui.composites.stepStartMax", 100000).doubleValue());
		
		label = new Label(this, SWT.NONE);
		label.setText("Stop");
		
		stop = new ScaleBox(this, SWT.NONE);
		stop.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stop.setUnit("eV");
		stop.setMinimum(start);
		stop.setMaximum(Integer.getInteger("org.eclipse.scanning.device.ui.composites.stepStopMax", 100000).doubleValue());
		
		label = new Label(this, SWT.NONE);
		label.setText("Step");
		
		step = new ScaleBox(this, SWT.NONE);
		step.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		step.setUnit("eV");
		step.setMinimum(Integer.getInteger("org.eclipse.scanning.device.ui.composites.stepMin", -10000).doubleValue());
		step.setMaximum(Integer.getInteger("org.eclipse.scanning.device.ui.composites.stepMax", 10000).doubleValue());

		label = new Label(this, SWT.HORIZONTAL|SWT.SEPARATOR);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		label = new Label(this, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		label.setText("Exposure overrides the detector configuration");
		label.setToolTipText("Exposure overrides the detector configuration.\nIt should be set only if you are sure that the exposure of\nthis part of the scan should be different to usual.\n\nNOTE: If set for one region, all regions set an exposure.");

		label = new Label(this, SWT.NONE);
		label.setText("Exposure");
		
		exposureTime = new ScaleBox(this, SWT.NONE);
		exposureTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		exposureTime.setUnit("s");
		exposureTime.setMinimum(0);
		exposureTime.setMaximum(600);

	}

	public ScaleBox getStart() {
		return start;
	}

	public ScaleBox getStop() {
		return stop;
	}

	public ScaleBox getStep() {
		return step;
	}

	public ScaleBox getExposureTime() {
		return exposureTime;
	}

	public void setScannable(IScannable<Number> scannable) {
		setUnit(scannable.getUnit());
		setBounds(scannable.getMinimum(), scannable.getMaximum());
	}

	private void setBounds(Number minimum, Number maximum) {
		if (Objects.isNull(minimum) || Objects.isNull(maximum)) return;
		start.setMinimum(minimum.doubleValue());
		start.setMaximum(maximum.doubleValue());
		stop.setMinimum(start);
		stop.setMaximum(maximum.doubleValue());		
		step.setMinimum(0.00001);
		step.setMaximum(maximum.doubleValue());
	}

	public void setUnit(String unit) {
		unit = Optional.of(unit).orElse("");
		start.setUnit(unit);
		stop.setUnit(unit);
		step.setUnit(unit);
	}

}
