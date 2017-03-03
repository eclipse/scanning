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
package org.eclipse.scanning.example.xcen.ui.handlers;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.scanning.api.ui.IResultHandler;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.eclipse.swt.widgets.Display;

public class OpenHandler extends XcenHandler implements IResultHandler<XcenBean> {

	@Override
	public boolean open(XcenBean bean) throws Exception {
		
	   MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Position '"+bean.getName()+"'", 
                   "The center was [x,y]: "+bean.getX()+","+bean.getY());
	   return true; // We handled it
	}

}
