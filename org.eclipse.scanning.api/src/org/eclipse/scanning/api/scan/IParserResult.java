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
package org.eclipse.scanning.api.scan;

import java.util.List;
import java.util.Map;

/**
 * Naive long winded usage of the parser API below to show how it should perform.<br>
 * However in fact the parser will be embedded as a subsystem of the scan.<br>
 * There is a utility for creating a compound points generator for<br>
 * any parser. However not putting this method directly on IParserResult<br>
 * means that the parser has a simple and well defined role.<br>
 * <br>
 * <usage><code>
 * IParserService pservice = ...// OSGi<br>
 * <br>
 * // Parse the scan command, throws an exception<br>
 * IParserResult<StepModel> parser = pservice.createParser(...)<br>
 * // e.g. "scan x 0 5 0.1 analyser"<br>
 * <br>
 * // Now use the parser to create a generator<br>
 * IPointGeneratorService gservice = ...// OSGi<br>
 * StepModel model = parser.getModel("x");<br>
 * IPointGenerator<?>     gen = gservice.createGenerator(model)<br>
 * <br>
 * // Now scan the point iterator<br>
 * IDeviceService sservice = ...// OSGi<br>
 * IRunnableDevice<ScanModel> scanner = sservice.createScanner(...);<br>
 * scanner.configure(model);<br>
 * scanner.run();<br>
 * 
 * </code></usage>
 * 
 * @see http://confluence.diamond.ac.uk/pages/viewpage.action?spaceKey=MAP&title=discussion+item%3A+generality+of+continuous+scans
 * @author Matthew Gerring
 *
 */
public interface IParserResult<T> {
	
	/**
	 * The original scan command which this parser is processing.
	 * @return
	 */
	String getCommand();
	
	/**
	 * Get the names of the scannables in order. For instance:
	 * Scan x 0 5 0.1 y 0 5 0.1 analyser
	 * would give:
	 * ["x","y"]
	 * 
	 * @return
	 */
	List<String> getScannableNames();

	/**
	 * Get the names of the detectors in order. For instance:
	 * Scan x 0 5 0.1 y 0 5 0.1 analyser 0.1 det 0.2
	 * would give:
	 * ["analyser","det"]
	 * 
	 * @return
	 */
	List<String> getDetectorNames();
	
	/**
	 * Get the exposures of the detectors, keys are ordered (LinkedHashMap). For instance:
	 * scan x 0 5 0.1 y 0 5 0.1 analyser 0.1 det 0.2
	 * would give the map:
	 * ["analyser":0.1,"det":0.2]
	 * 
	 * @return map of exposures
	 */
	Map<String, Number> getExposures();


	/**
	 * Get the point generation model for a given scannable
	 * For instance in the example:
	 * scan x 0 5 0.1 analyser
	 * Would be getModel("x") giving a StepModel with start stop and step set to  0, 5 and 0.1 respectively.
	 * 
	 * @param scannableName
	 * @return
	 */
	T getModel(String scannableName);
}
