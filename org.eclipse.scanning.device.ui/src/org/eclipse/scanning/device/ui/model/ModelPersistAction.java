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

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.richbeans.widgets.file.FileSelectionDialog;
import org.eclipse.scanning.api.IModelProvider;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This action can be used to allow scan paths to 
 * be saved and loaded.
 * 
 * @author Matthew Gerring
 *
 */
public class ModelPersistAction<T> extends Action {
	
	private static final Logger logger = LoggerFactory.getLogger(ModelPersistAction.class);

	public static final String[] IDS = new String[]{"SAVE", "LOAD"};
	/**
	 * The action has two types, save and load.
	 * 
	 * @author Matthew Gerring
	 *
	 */
	public static enum PersistType {
		
		SAVE("Save model to file",   "icons/folder-export.png"), 
		LOAD("Load model from file", "icons/folder-import.png");
		
		private final String          label;
		private final String          descriptor;

		PersistType(String label, String descriptor) {
		    this.label = label;
		    this.descriptor = descriptor;
	    }

		public String getLabel() {
			return label;
		}

		public String getDescriptor() {
			return descriptor;
		}
	}

	private final PersistType      type;
	private final IModelProvider<T> provider;
	private       Class<T>       modelClass;
	
	public ModelPersistAction(IModelProvider<T> provider, PersistType type) {
		this(provider, type, null);
	}

	/**
	 * 
	 * @param type
	 */
	public ModelPersistAction(IModelProvider<T> provider, PersistType type, Class<T> modelClass) {
		super(type.getLabel(), Activator.getImageDescriptor(type.getDescriptor()));
		setId(type.toString());
		this.provider = provider;
		this.type     = type;
		this.modelClass = modelClass;
	}
	
	@Override
	public void run() {
		
		try {
			if (type==PersistType.LOAD) {
				doLoad();
			} else if (type==PersistType.SAVE) {
				doSave();
			}
		} catch (Exception neio) {
			logger.error("Cannot read directory for saved files!", neio);
		}
	}

	private void doSave() throws Exception {
		IStashing stash = chooseFile(type);
		if (stash==null) return; // They cancelled it
		stash.save(provider.getModel());
	}

	private void doLoad() throws IOException {
		IStashing stash = chooseFile(type);
		if (stash==null) return; // They cancelled it
		T path = stash.load(modelClass);
		try {
			provider.updateModel(path); // Ensures that validate and events are fired.
		} catch (Exception e) {
			logger.error("Problem setting the model. It might be the wrong model type!", e);
		}
	}

	private static String previousFile = System.getProperty("GDA/gda.var", System.getProperty("user.home"));
	private final static String[] extensions = new String[]{"json", "*.*"};
	private final static String[] files = new String[]{"Model Files (json)", "All Files"};
	
	private static final IStashing chooseFile(PersistType type) throws IOException {
		
		FileSelectionDialog dialog = new FileSelectionDialog(Display.getCurrent().getActiveShell(), previousFile);
		dialog.setExtensions(extensions);
		dialog.setFiles(files);
		dialog.setNewFile(type==PersistType.SAVE);
		dialog.setFolderSelector(false);		
		dialog.create();
		if (dialog.open() != Dialog.OK) return null;
		
		String path = dialog.getPath();
		if (!path.endsWith(extensions[0])) { //should always be saved to .json
			path = path.concat("." + extensions[0]);
		}
	
		final File file = new File(path);
		previousFile    = file.getCanonicalPath();
		IStashing stash = ServiceHolder.getStashingService().createStash(file);
		return stash;
	}

	public Class<T> getModelClass() {
		return modelClass;
	}

	public void setModelClass(Class<T> modelClass) {
		this.modelClass = modelClass;
	}
}
