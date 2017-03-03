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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.ui.services.IDisposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModelFieldEditingSupport extends EditingSupport implements IDisposable {

	private static final Logger logger = LoggerFactory.getLogger(ModelFieldEditingSupport.class);

	/**
	 * 
	 */
	private final ModelViewer<?> modelViewer;
	private final ModelFieldEditorFactory factory;
	
	public ModelFieldEditingSupport(ModelViewer<?> modelViewer, ColumnViewer viewer, ColumnLabelProvider prov) {
		super(viewer);
		this.modelViewer = modelViewer;
		this.factory = new ModelFieldEditorFactory(prov);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		try {
			return factory.createEditor((FieldValue)element, this.modelViewer.getTable());
		} catch (ScanningException e) {
			logger.error("Cannot create an editor! Internal error!", e);
			return null;
		}
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		return ((FieldValue)element).get();
	}

	@Override
	protected void setValue(Object element, Object value) {
		try {
			FieldValue field = (FieldValue)element;
			field.set(value); // Changes model value, getModel() will now return a model with the value changed.
			this.modelViewer.refresh();
			this.modelViewer.setSelection(new StructuredSelection(field));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		factory.dispose();
	}
}