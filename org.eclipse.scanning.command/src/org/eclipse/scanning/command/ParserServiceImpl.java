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

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.IParserResult;
import org.eclipse.scanning.api.scan.IParserService;
import org.eclipse.scanning.api.scan.ParsingException;
import org.eclipse.scanning.command.factory.PyExpressionFactory;

public class ParserServiceImpl implements IParserService {
	
	static {
		System.out.println("Starting Parser Service");
	}

	// TODO clarify valid scan command names and valid scannable/detector names
	private static final String IDENTIFIER_REGEX = "[a-zA-Z][a-zA-Z0-9_]*";

	private static final String DOUBLE_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

	private static final String GROUP_NAME_SCANNABLES = "scannables";

	private static final String GROUP_NAME_DETECTORS = "detectors";

	private final Pattern scanCommandPattern;

	private final Pattern scannablePattern;

	private final Pattern detectorsPattern;

	public ParserServiceImpl() {

		// a scannable takes exactly 3 doubles as arguments - TODO sometimes none
		// TODO match a single space or multiple, or even general whitespace?
		final String scannableRegex = IDENTIFIER_REGEX + "( " + DOUBLE_REGEX + "){3}";
		scannablePattern = Pattern.compile(scannableRegex);
		final String scannableGroups = "( " + scannableRegex + ")+";
		final String allScannablesGroup = asNamedGroup(GROUP_NAME_SCANNABLES, scannableGroups);

		// a detector takes exactly 1 double as an argument
		final String detectorRegex = IDENTIFIER_REGEX + " " + DOUBLE_REGEX;
		detectorsPattern = Pattern.compile(detectorRegex);
		final String detectorGroups = "( " + detectorRegex + ")+";
		final String allDetectorsGroup = asNamedGroup(GROUP_NAME_DETECTORS, detectorGroups);

		final StringBuilder commandRegexBuilder = new StringBuilder();
		commandRegexBuilder.append("\\A"); // anchor at start of input, otherwise match could be anywhere in the input
		commandRegexBuilder.append(IDENTIFIER_REGEX); // the scan command, e.g. scan
		commandRegexBuilder.append(allScannablesGroup);
		commandRegexBuilder.append(allDetectorsGroup);
		commandRegexBuilder.append("\\z"); // anchor at end of input
		final String commandRegex = commandRegexBuilder.toString();
		scanCommandPattern = Pattern.compile(commandRegex);
	}

	private static String asNamedGroup(String groupName, String groupRegex) {
		return "(?<" + groupName + ">" + groupRegex + ")";
	}

	private LinkedHashMap<String, StepModel> parseScannables(final String scannablesString) throws ParsingException {
		final LinkedHashMap<String, StepModel> scannables = new LinkedHashMap<>();

		final Matcher scannableMatcher = scannablePattern.matcher(scannablesString);
		while (scannableMatcher.find()) {
			final String scannableStr = scannableMatcher.group();
			final String[] strs = scannableStr.split(" ");
			final String scannableName = strs[0];
			if (strs.length != 4) { // sanity check -- TODO scannables with no args
				throw new ParsingException("Scannable must have exacty 3 arguments: " + scannableName);
			}

			final double[] numericArgs = getNumericArgs(strs, "scannable");
			final StepModel scannableModel = new StepModel(scannableName, numericArgs[0], numericArgs[1], numericArgs[2]);
			scannables.put(scannableName, scannableModel);
		}

		return scannables;
	}

	private LinkedHashMap<String, Number> parseDetectors(final String detectorsString) throws ParsingException {
		final LinkedHashMap<String, Number> detectors = new LinkedHashMap<>();

		final Matcher detectorMatcher = detectorsPattern.matcher(detectorsString);
		while (detectorMatcher.find()) {
			final String detectorStr = detectorMatcher.group();
			final String[] strs = detectorStr.split(" ");
			final String detectorName = strs[0];
			if (strs.length != 2) { // sanity check
				throw new ParsingException("Detector must have exacty 1 argument: " + detectorName);
			}

			final double[] numericArgs = getNumericArgs(strs, "detector");
			detectors.put(detectorName, numericArgs[0]);
		}

		return detectors;
	}

	private double[] getNumericArgs(String[] command, String deviceType) throws ParsingException {
		// note, always skip the first element, this is the device name
		final double[] numericArgs = new double[command.length - 1];

		for (int i = 1; i < command.length; i++) {
			try {
				numericArgs[i - 1] = Double.parseDouble(command[i]);
			} catch (final NumberFormatException e) {
				final String errorMessage = MessageFormat.format("Error parsing argument {0} of {1} ''{2}''",
						i, deviceType, command[0]);
				throw new ParsingException(errorMessage);
			}
		}

		return numericArgs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IParserResult<T> createParser(String scan) throws ParsingException {

		// TODO trim trailing whitespace first?
		final Matcher scanCommandMatcher = scanCommandPattern.matcher(scan);
		final boolean matchFound = scanCommandMatcher.find();
		if (!matchFound) {
			// TODO: using a single regex to match the whole scan command
			// doesn't appear to make it very easy to say what's wrong with the command
			throw new ParsingException("Invalid scan command: " + scan);
		}

		// TODO other models besides StepModel? what determines type T?
		final ParserImpl parser = new ParserImpl(scan);

		// parse the scannables string
		final String scannablesString = scanCommandMatcher.group(GROUP_NAME_SCANNABLES);
		final LinkedHashMap<String, StepModel> scannableModels = parseScannables(scannablesString);
		parser.setScannables(scannableModels);

		// parser the detectors string
		final String detectorsString = scanCommandMatcher.group(GROUP_NAME_DETECTORS);
		final LinkedHashMap<String, Number> detectorExposures = parseDetectors(detectorsString);
		parser.setDetectors(detectorExposures);

		return (IParserResult<T>) parser;
	}

	@Override
	public <T> String getCommand(ScanRequest<T> req, boolean verbose) throws Exception {
		PyExpressionFactory factory = new PyExpressionFactory();
		return factory.pyExpress(req, verbose);
	}

	
	private static IPointGeneratorService pointGeneratorService;

	public static IPointGeneratorService getPointGeneratorService() {
		return pointGeneratorService;
	}

	public static void setPointGeneratorService(IPointGeneratorService pointGeneratorService) {
		ParserServiceImpl.pointGeneratorService = pointGeneratorService;
	}

}
