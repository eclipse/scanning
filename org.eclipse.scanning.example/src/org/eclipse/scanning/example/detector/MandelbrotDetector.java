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

package org.eclipse.scanning.example.detector;

import java.io.IOException;
import java.util.Random;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.example.Services;

/**
 * A dummy detector which must be set up with references to two Scannables representing X and Y positions. When used in a step scan, this detector generates a
 * value of 0 if the point (x, y) is in the Mandelbrot set, and greater than zero otherwise.
 * <p>
 * Note: values will always be high if used at (x, y) positions more than 2 units away from the origin.
 */
public class MandelbrotDetector extends AbstractRunnableDevice<MandelbrotModel> implements IWritableDetector<MandelbrotModel>, INexusDevice<NXdetector> {

	// Field names to be used in the NeXus file 
	private static final String FIELD_NAME_VALUE = "value";
	private static final String FIELD_NAME_SPECTRUM = "spectrum";
	private static final String FIELD_NAME_SPECTRUM_AXIS = "spectrum_axis";
	private static final String FIELD_NAME_IMAGINARY_AXIS = "imaginary";
	private static final String FIELD_NAME_REAL_AXIS = "real";

	// Data to be passed from run() to write()
	private IDataset image;
	private IDataset spectrum;
	private double value;

	// Writable datasets
	private ILazyWriteableDataset imageData;
	private ILazyWriteableDataset spectrumData;
	private ILazyWriteableDataset valueData;
	private final Random random = new Random();

	public MandelbrotDetector() throws IOException, ScanningException {
		super(Services.getRunnableDeviceService()); // Necessary if you are going to spring it
		this.model = new MandelbrotModel();
		setDeviceState(DeviceState.IDLE);
	}
	
	@ScanFinally
	public void clean() {
		image     = null;
		imageData = null;
		spectrum  = null;
		spectrumData = null;
		valueData = null;
	}

	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		NXdetector detector = createNexusObject(info);
		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<>(getName(), detector);

		// "data" is the name of the primary data field (i.e. the 'signal' field of the default NXdata)
		if (model.isSaveImage()) nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);
		// An additional NXdata group with "spectrum" as the signal to hold the 1D spectrum data
		if (model.isSaveSpectrum() && !model.isSaveImage()) {
			nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);
		} else if (model.isSaveSpectrum()) {
			nexusProvider.addAdditionalPrimaryDataFieldName(FIELD_NAME_SPECTRUM);
		}
		// An additional NXdata group with "value" as the signal to hold the Mandelbrot value
		if (model.isSaveValue() && !model.isSaveImage() && !model.isSaveSpectrum()){
			nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);
		} else if (model.isSaveValue()) {
			nexusProvider.addAdditionalPrimaryDataFieldName(FIELD_NAME_VALUE);
		}

		// Add the axes to the image and spectrum data. scanRank here corresponds to the position
		// in the axes attribute written in the NeXus file (0 based)
		int scanRank = info.getRank();
		if (model.isSaveImage()) {
			nexusProvider.addAxisDataFieldForPrimaryDataField(FIELD_NAME_REAL_AXIS, NXdetector.NX_DATA, scanRank);
			nexusProvider.addAxisDataFieldForPrimaryDataField(FIELD_NAME_IMAGINARY_AXIS, NXdetector.NX_DATA, scanRank + 1);
		}
		
		if (model.isSaveSpectrum())nexusProvider.addAxisDataFieldForPrimaryDataField(FIELD_NAME_SPECTRUM_AXIS, FIELD_NAME_SPECTRUM, scanRank);

		return nexusProvider;
	}

	private NXdetector createNexusObject(NexusScanInfo info) throws NexusException {
		final NXdetector detector = NexusNodeFactory.createNXdetector();

		// add the exposure time to the nexus object
		detector.setCount_timeScalar(model.getExposureTime());

		int scanRank = info.getRank();
		// We add 2 to the scan rank to include the image
		if (model.isSaveImage()) imageData = detector.initializeLazyDataset(NXdetector.NX_DATA, scanRank + 2, Double.class);
		// We add 1 to the scan rank to include the spectrum
		if (model.isSaveSpectrum() && !model.isSaveImage()) {
			spectrumData = detector.initializeLazyDataset(NXdetector.NX_DATA, scanRank + 1, Double.class);
		} else if (model.isSaveSpectrum()){
			spectrumData = detector.initializeLazyDataset(FIELD_NAME_SPECTRUM, scanRank + 1, Double.class);
		}
		// Total is a single scalar value (i.e. zero-dimensional) for each point in the scan
		// Dimensions match that of the scan
		if (model.isSaveValue() && !model.isSaveImage() && !model.isSaveSpectrum()) {
			valueData = detector.initializeLazyDataset(NXdetector.NX_DATA, scanRank, Double.class);
		} else if (model.isSaveValue()) {
			valueData = detector.initializeLazyDataset(FIELD_NAME_VALUE, scanRank, Double.class);
		}

		// Setting chunking is a very good idea if speed is required.
		if (model.isSaveImage()) imageData.setChunking(info.createChunk(model.getRows(), model.getColumns()));
		if (model.isSaveSpectrum())spectrumData.setChunking(info.createChunk(model.getPoints()));

		// Write detector metadata
		detector.setField("exposure_time", model.getExposureTime());
		detector.setAttribute("exposure_time", "units", "seconds");
		detector.setField("escape_radius", model.getEscapeRadius());
		detector.setField("max_iterations", model.getMaxIterations());

		// The axis datasets
		if (model.isSaveImage()) {
			detector.setDataset(FIELD_NAME_REAL_AXIS, DatasetFactory.createLinearSpace(-model.getMaxRealCoordinate(), model.getMaxRealCoordinate(), model.getRows(), Dataset.FLOAT64));
			detector.setDataset(FIELD_NAME_IMAGINARY_AXIS, DatasetFactory.createLinearSpace(-model.getMaxImaginaryCoordinate(), model.getMaxImaginaryCoordinate(), model.getColumns(), Dataset.FLOAT64));
		}
		if (model.isSaveSpectrum()) detector.setDataset(FIELD_NAME_SPECTRUM_AXIS, DatasetFactory.createLinearSpace(0.0, model.getMaxRealCoordinate(), model.getPoints(), Dataset.FLOAT64));

		Attributes.registerAttributes(detector, this);
		
		return detector;
	}

	@Override
	public void configure(MandelbrotModel model) throws ScanningException {
		setDeviceState(DeviceState.CONFIGURING);
		setName(model.getName());
		// super.configure sets device state to ready
		super.configure(model);
	}

	@Override
	public void run(IPosition pos) throws ScanningException, InterruptedException {
		setDeviceState(DeviceState.RUNNING);

		final long startTime = System.nanoTime();
		final long targetDuration = (long) (model.getExposureTime() * 1000000000.0); // nanoseconds

		// Find out where we are in the scan. This is unique to the Mandelbrot
		// detector as it's a dummy in general a detector shouldn't need to get
		// the position in the scan
		double a = 1.0;
		double b = 1.0;
		if (pos.size() > 0) {
			Object da = pos.get(model.getRealAxisName());
			Object db = (Double) pos.get(model.getImaginaryAxisName());
			if (da==null || db==null) {
				throw new ScanningException("One of the axis names in the scan does not match the Mandelbrot model!");
			}
			a = (Double)da;
			b = (Double)db;
		}

		// Calculate the data for the image spectrum and total
		if (model.isSaveImage()) image = calculateJuliaSet(a, b, model.getColumns(), model.getRows());
		if (model.isSaveSpectrum()) spectrum = calculateJuliaSetLine(a, b, 0.0, 0.0, model.getMaxRealCoordinate(), model.getPoints());
		if (model.isSaveValue()) value = mandelbrot(a, b);

		// See if we need to sleep to honour the requested exposure time
		long currentTime = System.nanoTime();
		long duration = currentTime - startTime;
		if (duration < targetDuration) {
			long millisToWait = (targetDuration - duration) / 1000000;
			Thread.sleep(millisToWait);
		}

		// TODO Should device state be set back to ready here? The device has finished acquiring (calculating) but the data is not in the file yet?
	}

	@Override
	public boolean write(IPosition pos) throws ScanningException {

		try {
			if (model.isSaveImage()) {
				IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos, model.getRows(), model.getColumns());
				SliceND sliceND = new SliceND(imageData.getShape(), imageData.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
				imageData.setSlice(null, image, sliceND);
			}

			if (model.isSaveSpectrum()){
				IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos, model.getPoints());
				SliceND sliceND = new SliceND(spectrumData.getShape(), spectrumData.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
				spectrumData.setSlice(null, spectrum, sliceND);
			}

			if (model.isSaveValue()){
				IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos);
				SliceND sliceND = new SliceND(valueData.getShape(), valueData.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
				valueData.setSlice(null, DatasetFactory.createFromObject(value), sliceND);
			}

		} catch (Exception e) {
			// Change state to fault if exception is caught
			setDeviceState(DeviceState.FAULT);
			throw new ScanningException("Failed to write the data to the NeXus file", e);
		}

		// Finished writing set state back to ready
		setDeviceState(DeviceState.READY);
		return true;
	}

	/**
	 * Fill a Julia set around the origin for the value C = a + bi
	 */
	private IDataset calculateJuliaSet(final double a, final double b, int columns, int rows) {
		final double xStart = -model.getMaxRealCoordinate();
		final double xStop = model.getMaxRealCoordinate();
		final double yStart = -model.getMaxImaginaryCoordinate();
		final double yStop = model.getMaxImaginaryCoordinate();
		final double yStep = (yStop - yStart) / (rows - 1);
		double y;
		IDataset juliaSet = DatasetFactory.zeros(rows,columns);
		for (int yIndex = 0; yIndex < rows; yIndex++) {
			y = yStart + yIndex * yStep;
			IDataset line = calculateJuliaSetLine(a, b, y, xStart, xStop, columns);
			for (int x = 0; x < line.getSize(); x++) {
				juliaSet.set(line.getObject(x), yIndex, x);	
			}
		}
		return juliaSet;
	}

	/**
	 * Fill a Julia set line between xStart and xStop at the given y value, for the value C = a + bi
	 */
	private IDataset calculateJuliaSetLine(final double a, final double b, final double y, final double xStart, final double xStop, final int numPoints) {
		final double xStep = (xStop - xStart) / (numPoints - 1);
		double x;
		IDataset juliaSetLine = DatasetFactory.zeros(numPoints);
		for (int xIndex = 0; xIndex < numPoints; xIndex++) {
			x = xStart + xIndex * xStep;
			juliaSetLine.set(julia(x, y, a, b), xIndex);
		}
		return juliaSetLine;
	}

	/**
	 * Iterations of f(z) = z^2 + C, where z = x + yi, C = a + bi, and initial z = 0
	 */
	private double mandelbrot(final double a, final double b) {
		return julia(0.0, 0.0, a, b);
	}

	/**
	 * Iterations of f(z) = z^2 + C, where z = x + yi and C = a + bi
	 */
	private double julia(double x, double y, final double a, final double b) {
		int iteration = 0;
		double xSquared, ySquared, tempX;
		double escapeRadiusSquared = model.getEscapeRadius() * model.getEscapeRadius();
		do {
			xSquared = x * x;
			ySquared = y * y;
			tempX = xSquared - ySquared + a;
			y = 2 * x * y + b;
			x = tempX;
			iteration++;
		} while (iteration < model.getMaxIterations() && xSquared + ySquared < escapeRadiusSquared);

		double modulus = Math.sqrt(x * x + y * y);

		// If modulus > 1.0, normalise the result
		// (Theoretically, I think this should make the value roughly independent of MAX_ITERATIONS and ESCAPE_RADIUS)
		if (modulus > 1.0) {
			return addNoise(iteration - (Math.log(Math.log(modulus)) / Math.log(2.0)));
		}
		// Otherwise just return the iteration count
		return addNoise(iteration);
	}

	/**
	 * Adds random noise to the data if specified by the model.
	 * <p>
	 * First checks if noise is enabled, if not just returns the value. Then checks if the exposure is greater than the
	 * "noise free exposure" if it is, return the value unchanged. Finally if noise is enabled and the exposure time is
	 * below the noise free exposure time then noise will be added proportional to the exposure time.
	 * 
	 * @param value
	 * @return value with noise added, if model specifies it
	 */
	private double addNoise(double value) {
		// If noise is disabled just return the value
		if (!model.isEnableNoise()) {
			return value;
		}
		// If the exposure time is longer than the noise free exposure time just return the value
		else if (model.getExposureTime() >= model.getNoiseFreeExposureTime()) {
			return value;
		}
		// Add noise dependent on the exposure time
		else {
			// noiseFraction is between 0 and 1 where 0 is pure signal and 1 means pure noise
			double noiseFraction = (model.getNoiseFreeExposureTime() - model.getExposureTime())
					/ model.getNoiseFreeExposureTime();
			return value * (1 - noiseFraction) + value * random.nextDouble() * noiseFraction;
		}
	}

	public boolean _isScanFinallyCalled() {
		if (image     != null) return false;
		if (imageData != null) return false;
		if (spectrum  != null) return false;
		if (spectrumData != null) return false;
		if (valueData != null) return false;
		return true;
	}

}
