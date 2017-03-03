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

import org.eclipse.scanning.api.event.scan.ScanRequest;

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
 * IRunnableDevice scanner = sservice.createScanner();<br>
 * scanner.configure(...);<br>
 * scanner.run();<br>
 * 
 * </code></usage>
 * 
 * @see http://confluence.diamond.ac.uk/pages/viewpage.action?spaceKey=MAP&title=discussion+item%3A+generality+of+continuous+scans
 * @author Matthew Gerring
 * @author Nic Bricknell
 *
 */
public interface IParserService {

	/**
	 * Create a parser for a give scan, optionally parametixing the
	 * model returned. Some scans will consist of multiple models,
	 * for instance a Compound generator scan like "scan dcm 1 5 1 spiral(loops=720,expand%=10,rot=ccw,start=perip) detector 0.1"
	 * will generate different models for dcm and spiral. In this case the
	 * model for the position generation is undefined. Therefore the parser would simply be:
	 * IParserResult<?> parser = pservice.createParser("scan dcm 1 5 1 spiral(loops=720,expand%=10,rot=ccw,start=perip) detector 0.1");
	 * Object xmodel = parser.getModel("dcm");
	 * Object smodel = parser.getModel("spiral");
	 * 
	 * @param scan
	 * @return
	 */
	<T> IParserResult<T> createParser(String scan) throws ParsingException;
	
	/**
	 * Get the scan command for a given scan request which would be submitted to the server.
	 * @param req
	 * @param verbose
	 * @return a command that should work in the python console.
	 * @throws an exception. This can often happen if features of the scan not supported in the python command are used. 
	 * For instance generators which do not have a working alternative.
	 */
	<T> String getCommand(ScanRequest<T> req, boolean verbose) throws Exception;
}
