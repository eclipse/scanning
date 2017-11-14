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
package org.eclipse.scanning.command.factory;

import java.util.Collection;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.command.ParserServiceImpl;

class ScanRequestExpresser extends PyModelExpresser<ScanRequest<?>> {

	@SuppressWarnings("squid:S3776")
	@Override
	String pyExpress(ScanRequest<?> request, boolean verbose) throws Exception {


		// TODO Fragment should be a StringBuilder, it is more efficient.
		String fragment = "mscan(";
		boolean scanRequestPartiallyWritten = false;

		PyExpressionFactory factory = new PyExpressionFactory();
		if (request.getCompoundModel().getModels() != null
				&& request.getCompoundModel().getModels().size() > 0) {

			if (verbose) { fragment += "path="; }

			if (verbose || request.getCompoundModel().getModels().size() > 1) fragment += "[";
			boolean listPartiallyWritten = false;

			for (Object model : request.getCompoundModel().getModels()) {  // Order is important.
				if (listPartiallyWritten) fragment += ", ";
				Collection<IROI> rois = (Collection<IROI>) ParserServiceImpl.getPointGeneratorService().findRegions(model, request.getCompoundModel().getRegions());

				String smodel = factory.pyExpress(model, rois, verbose);
				fragment += smodel;
				listPartiallyWritten |= true;
			}

			if (verbose || request.getCompoundModel().getModels().size() > 1) fragment += "]";
			scanRequestPartiallyWritten |= true;
		}

		if (request.getMonitorNamesPerPoint() != null
				&& request.getMonitorNamesPerPoint().size() > 0) {

			if (scanRequestPartiallyWritten) fragment += ", ";
			if (verbose || !scanRequestPartiallyWritten) { fragment += "monitorsPerPoint="; }

			if (verbose || request.getMonitorNamesPerPoint().size() > 1) fragment += "[";
			boolean listPartiallyWritten = false;

			for (String monitorName : request.getMonitorNamesPerPoint()) {
				if (listPartiallyWritten) fragment += ", ";
				fragment += "'"+monitorName+"'";
				listPartiallyWritten |= true;
			}

			if (verbose || request.getMonitorNamesPerPoint().size() > 1) fragment += "]";
			scanRequestPartiallyWritten |= true;
		}

		if (request.getMonitorNamesPerScan() != null
				&& request.getMonitorNamesPerScan().size() > 0) {

			if (scanRequestPartiallyWritten) fragment += ", ";
			if (verbose || !scanRequestPartiallyWritten) { fragment += "monitorsPerScan="; }

			if (verbose || request.getMonitorNamesPerScan().size() > 1) fragment += "[";
			boolean listPartiallyWritten = false;

			for (String monitorName : request.getMonitorNamesPerScan()) {
				if (listPartiallyWritten) fragment += ", ";
				fragment += "'"+monitorName+"'";
				listPartiallyWritten |= true;
			}

			if (verbose || request.getMonitorNamesPerScan().size() > 1) fragment += "]";
			scanRequestPartiallyWritten |= true;
		}

		if (request.getDetectors() != null
				&& request.getDetectors().size() > 0) {

			if (scanRequestPartiallyWritten) fragment += ", ";
			if (verbose || !scanRequestPartiallyWritten) { fragment += "det="; }

			if (verbose || request.getDetectors().size() > 1) fragment += "[";

			boolean listPartiallyWritten = false;
			for (String detectorName : request.getDetectors().keySet()) {
				if (listPartiallyWritten) fragment += ", ";
				Object model = request.getDetectors().get(detectorName);
				fragment += factory.pyExpress(model, verbose);
				listPartiallyWritten |= true;
			}

			if (verbose || request.getDetectors().size() > 1) fragment += "]";
			scanRequestPartiallyWritten |= true;
		}

		fragment += ")";
		return fragment;

	}
}
