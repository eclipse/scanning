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
package org.eclipse.scanning.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.IParserResult;

class ParserImpl implements IParserResult<StepModel> {
	
	static {
		System.out.println("Started Parser Service");
	}

	private final String command;

	private Map<String, StepModel> scannables;

	private Map<String, Number> detectorExposures;

	ParserImpl(String command) {
		this.command = command;
	}

	protected void setScannables(LinkedHashMap<String, StepModel> scannables) {
		this.scannables = scannables;
	}

	protected void setDetectors(LinkedHashMap<String, Number> detectorExposures) {
		this.detectorExposures = detectorExposures;
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public List<String> getScannableNames() {
		return new ArrayList<String>(scannables.keySet());
	}

	@Override
	public List<String> getDetectorNames() {
		return new ArrayList<String>(detectorExposures.keySet());
	}

	@Override
	public Map<String, Number> getExposures() {
		return detectorExposures;
	}

	@Override
	public StepModel getModel(String scannableName) {
		return scannables.get(scannableName);
	}

}
