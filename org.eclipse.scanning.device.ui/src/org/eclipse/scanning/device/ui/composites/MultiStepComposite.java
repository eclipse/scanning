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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.richbeans.api.event.ValueAdapter;
import org.eclipse.richbeans.api.event.ValueEvent;
import org.eclipse.richbeans.widgets.selector.VerticalListEditor;
import org.eclipse.richbeans.widgets.wrappers.ComboWrapper;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.filter.IFilterService;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.device.ui.IMultiStepConfiguratorService;
import org.eclipse.scanning.device.ui.util.SortNatural;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiStepComposite extends Composite {

	private static Logger logger = LoggerFactory.getLogger(MultiStepComposite.class);

	// UI
    private final ComboWrapper                  name;
	private final VerticalListEditor<StepModel> steps;
	private IScannableDeviceService             scannableConnectorService;
	private StepModelComposite                  stepComposite;

	public MultiStepComposite(Composite parent, int style) {
		super(parent, style);

		setLayout(new GridLayout(2, false));

		Label label = new Label(this, SWT.NONE);
		label.setText("Scannable");

		this.name = new ComboWrapper(this, SWT.READ_ONLY);
		name.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		name.addValueListener(new ValueAdapter("unitUpdater") {
			@Override
			public void valueChangePerformed(ValueEvent e) {
				updateUnits(e.getValue());
				steps.setBeanConfigurator(IMultiStepConfiguratorService.DEFAULT.getConfigurator((String) e.getValue()));
			}
		});

		// List of ExampleItems
		this.steps = new VerticalListEditor<>(this, SWT.NONE);
		steps.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		steps.setMinItems(0);
		steps.setMaxItems(10);
		steps.setEditorClass(StepModel.class); // Must match generic!

		steps.setBeanConfigurator(IMultiStepConfiguratorService.DEFAULT.getConfigurator(""));
		steps.setListHeight(80);
		steps.setRequireSelectionPack(false);
		steps.setTemplateName("Step");
		steps.setNameField("label");

		stepComposite = new StepModelComposite(this, SWT.NONE);
		stepComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		steps.setEditorUI(stepComposite);

	}

	public boolean updateUnits(Object value) {
		if (value==null) return false;
		if (scannableConnectorService==null) return false;
		try {
			IScannable<Number> scannable = scannableConnectorService.getScannable(value.toString());
			if (scannable!=null) {
				stepComposite.setScannable(scannable);
				return true;
			}
		} catch (Exception ne) {
			logger.error("Cannot setup units!", ne);
		}
		return false;
	}

	@Inject
	public void setScannableService(IScannableDeviceService service, MultiStepModel model) throws ScanningException {

		this.scannableConnectorService = service;
		List<String> items = getScannablesList(true);
		if (model.getName()!= null) {
			if (!items.contains(model.getName())) {
				items = getScannablesList(false);
			}
			populateComboBox(items);
			int index = items.indexOf(model.getName());
			name.select(index);
		} else {
			populateComboBox(items);
		}
		getParent().layout(new Control[]{this});
	}

	private List<String> getScannablesList(boolean usingFilter) throws ScanningException {
		List<String> names = scannableConnectorService.getScannableNames();
		if (usingFilter) {
			names = IFilterService.DEFAULT.filter("org.eclipse.scanning.scannableFilter", names);
		}
		return names;
	}

	private void populateComboBox(List<String> items) {
		Collections.sort(items, new SortNatural<>(false));
		name.setItems(items.toArray(new String[items.size()]));
	}

	public VerticalListEditor<StepModel> getStepModels() {
		return steps;
	}

	public ComboWrapper getName() {
		return name;
	}

	public void setNameComboEnabled(boolean enabled) {
		name.setEnabled(enabled);
	}
}
