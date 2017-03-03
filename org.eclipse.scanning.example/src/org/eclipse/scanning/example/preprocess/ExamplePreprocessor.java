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
package org.eclipse.scanning.example.preprocess;

import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.process.IPreprocessor;
import org.eclipse.scanning.api.scan.process.ProcessingException;
import org.eclipse.scanning.example.detector.MandelbrotModel;

/**
 * A preprocessor which overwrites the stage names with 'xfred' and 'yfred' in various models.
 * 
 * We are just doing various example things here to the request without purpose. Your implementation
 * should ensure that the correct models for scan path and for detector are defined. For instance a 
 * GridModel might be replaced with a malcolm detector model.
 * 
 * @author Matthew Gerring
 *
 */
public class ExamplePreprocessor implements IPreprocessor {

	@Override
	public String getName() {
		return "example";
	}

	@Override
	public <T> ScanRequest<T> preprocess(ScanRequest<T> req) throws ProcessingException {
	
		for (Object model : req.getCompoundModel().getModels()) {
			if (model instanceof StepModel) {
				((StepModel)model).setName("xfred");
			} if (model instanceof IBoundingBoxModel) {
				((IBoundingBoxModel)model).setFastAxisName("xfred");
				((IBoundingBoxModel)model).setSlowAxisName("yfred");
			}
		}
		
		for (String name : req.getDetectors().keySet()) {
			Object dmodel = req.getDetectors().get(name);
			if (dmodel instanceof MandelbrotModel) {
				MandelbrotModel mmodel = (MandelbrotModel)dmodel;
				mmodel.setRealAxisName("xfred");
				mmodel.setImaginaryAxisName("yfred");
			}
		}
		
		return req;
	}
}
