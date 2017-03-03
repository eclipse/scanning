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
package org.eclipse.scanning.device.ui.model;

import java.io.Serializable;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.richbeans.annot.DOEUtils;
import org.eclipse.scanning.api.ui.auto.IModelDialog;
import org.eclipse.scanning.api.ui.auto.InterfaceInvalidException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModelDialog<T extends Serializable> extends Dialog implements IModelDialog<T>  {

	private static final Logger logger = LoggerFactory.getLogger(ModelDialog.class);
	
	private ModelViewer<T>   modelEditor;

	private String preamble;
	
	public ModelDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	public Control createDialogArea(Composite parent) {

		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		try {
			if (preamble!=null) {
				Label label = new Label(main, SWT.WRAP);
				label.setText(preamble);
				label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			modelEditor = new ModelViewer<T>();
			Control created = modelEditor.createPartControl(main);
			created.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		} catch (Exception e) {
			logger.error("Cannot create model viewer!", e);
		}
		
		return main;

	}
	
	protected void okPressed() {
		modelEditor.applyEditorValue();
		super.okPressed();
	}

	public T getModel() {
		return modelEditor.getModel();
	}

	public void setModel(T model) throws InterfaceInvalidException {
		try {
			model = DOEUtils.deepClone(model);
			modelEditor.setModel(model);
		} catch (InterfaceInvalidException ne) {
			throw ne;
		} catch (Exception other) {
			throw new InterfaceInvalidException(other);
		}
	}

	public void setPreamble(String message) {
		this.preamble = message;
	}

	@Override
	public void setSize(int width, int height) {
		getShell().setSize(width, height);
	}

	@Override
	public void setText(String label) {
		getShell().setText(label);
	}

	@Override
	public T getControl() {
		return (T)getShell();
	}

}
