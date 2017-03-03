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

import org.eclipse.richbeans.api.generator.IGuiGeneratorService;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.ui.IModifyHandler;
import org.eclipse.scanning.example.xcen.beans.XcenBean;

public class ModifyHandler extends XcenHandler implements IModifyHandler<XcenBean> {
	
	private static IGuiGeneratorService guiservice;

	@Override
	public boolean modify(XcenBean bean) throws Exception {
		
		BeamPosition pos = new BeamPosition(bean.getX(), bean.getY());
		// TODO FIXME Require method to edit 
		//pos = guiservice.openDialog(pos);
		//if (pos == null) return true; // We still looked at it
	
		ISubmitter<XcenBean> submitter = eventService.createSubmitter(conf.getUri(), conf.getSubmissionQueue());
		submitter.replace(bean);
		
		return true; // It was dealt with 
	}

	public static IGuiGeneratorService getGuiservice() {
		return guiservice;
	}

	public static void setGuiservice(IGuiGeneratorService guiservice) {
		ModifyHandler.guiservice = guiservice;
	}

}
