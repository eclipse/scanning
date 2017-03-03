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
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.ui.IRerunHandler;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.eclipse.swt.widgets.Display;

public class RerunHandler extends XcenHandler implements IRerunHandler<XcenBean> {

	@Override
	public boolean run(XcenBean copy) throws Exception {
		
        boolean ok = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Rerun '"+copy.getName()+"'", 
                        "Would you like to rerun X-Ray Centering?\n\n"+
                        "The bean was:\n"+copy);
        if (ok) {      	
            ISubmitter<XcenBean> submitter = eventService.createSubmitter(conf.getUri(), conf.getSubmissionQueue());
            copy.setName("Copy of "+copy.getName());
            submitter.submit(copy, true);
            submitter.disconnect(); // Not really required for submitters.
        }
        return true; // We handled it
	}

}
