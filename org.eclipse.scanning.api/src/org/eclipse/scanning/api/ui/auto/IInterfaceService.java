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
package org.eclipse.scanning.api.ui.auto;

import java.io.Serializable;

/**
 * 
 * An OSGi service for automatically generating user interfaces from annotated beans.
 * 
 * Currently the service is backed by an SWT generator.
 * 
 * @author Matthew Gerring
 *
 */
public interface IInterfaceService {
	
	/**
	 * Creates a model viewer.
	 * 
	 * The model viewer has a createPartControl(...) for creating content to edit the bean
	 * which is the parametized type 'O'.
	 * 
	 * @param parent
	 * @return
	 * @throws InterfaceInvalidException
	 */
	<O> IModelViewer<O> createModelViewer() throws InterfaceInvalidException;

	/**
	 * T is the type of the shell for instance swt.Shell for an SWT generator
	 * O is the type of the bean. The bean may be any proper bean class and may have fields annotated with @FieldDescriptor
	 * 
	 * This method creates a ModelViewer on a dialog so is the same as createModelViewer(...) 
	 * 
	 * @param shell
	 * @return Dialog a class which can be used to edit any model.
	 * @throws InterfaceInvalidException, for instance if T is a user interface type which is unsupported e.g. FXDialog
	 */
	<T,O extends Serializable> IModelDialog<O> createModelDialog(T shell) throws InterfaceInvalidException;
}
