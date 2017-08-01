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
package org.eclipse.scanning.sequencer.nexus;

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_CMD;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_DEAD_TIME;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_DEAD_TIME_PERCENT;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_ESTIMATED_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_FINISHED;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_MODELS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_RANK;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_SHAPE;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_UNIQUE_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_SOLSTICE_SCAN;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.PROPERTY_NAME_UNIQUE_KEYS_PATH;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.SCANNABLE_NAME_SOLSTICE_SCAN_MONITOR;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;
import org.eclipse.dawnsci.nexus.builder.DelegatingNexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.IntegerDataset;
import org.eclipse.january.dataset.LazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scan points writer creates and writes to the unique keys and points
 * datasets in a nexus file. The unique keys dataset can be used to track how far the scan has
 * progressed.
 * 
 * Note that this monitor is not added to the list of monitors in the ScanModel, but instead
 * its methods are invoked by the {@link NexusScanFileManager}. This is because it is part of the
 * scan framework itself, and specifically because it must write to the unique keys dataset only
 * after all devices have written to their datasets.
 * 
 * @author Matthew Dickie
 */
public class SolsticeScanMonitor extends AbstractScannable<Object> implements INexusDevice<NXcollection> {
	
	private static final Logger logger = LoggerFactory.getLogger(SolsticeScanMonitor.class);
	
	// custom parser for converting durations to times. The only difference between this as
	// DateTimeFormatter.ISO_LOCAL_TIME is that this one always outputs 3 digits for nanoseconds
	private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().
			appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true).toFormatter();

	// Nexus
	private List<NexusObjectProvider<?>> nexusObjectProviders = null;
	private NexusObjectProvider<NXcollection> nexusProvider = null;
	
	// Writing Datasets
	private ILazyWriteableDataset uniqueKeysDataset = null;
	private ILazyWriteableDataset scanFinishedDataset = null;
	private ILazyWriteableDataset scanDurationDataset = null;
	private ILazyWriteableDataset scanDeadTimeDataset = null;
	private ILazyWriteableDataset scanDeadTimePercentDataset = null;

	// State
	private boolean malcolmScan = false;
	private final ScanModel model;
	private Instant scanStartTime = null;
	private int[] scanShape = null;
	private boolean writeAfterMovePerformed = false;
	
	public SolsticeScanMonitor(ScanModel model) {
		this.model = model;
		setName(SCANNABLE_NAME_SOLSTICE_SCAN_MONITOR);
	}

	public void setNexusObjectProviders(Map<ScanRole, List<NexusObjectProvider<?>>> nexusObjectProviderMap) {
		EnumSet<ScanRole> deviceTypes = EnumSet.complementOf(EnumSet.of(ScanRole.MONITOR_PER_SCAN));
		List<NexusObjectProvider<?>> nexusObjectProviderList = nexusObjectProviderMap.entrySet().stream()
			.filter(e -> deviceTypes.contains(e.getKey())) // filter where key is in deviceType set
			.flatMap(e -> e.getValue().stream())  // concatenate value lists into a single stream
			.collect(Collectors.toList());  // collect in a list

		this.nexusObjectProviders = nexusObjectProviderList;
		
		final List<NexusObjectProvider<?>> detectors = nexusObjectProviderMap.get(ScanRole.DETECTOR);
		// we can write the unique key for each position on move if all detectors write their own unique key
		// TODO what about monitors?
		if (detectors != null) {
			writeAfterMovePerformed = detectors.stream().allMatch(n -> n.getPropertyValue(PROPERTY_NAME_UNIQUE_KEYS_PATH) != null);
		}
	}
	
	public void setNexusObjectProviders(List<NexusObjectProvider<?>> nexusObjectProviders) {
		this.nexusObjectProviders = nexusObjectProviders;
	}
	
	public void setMalcolmScan(boolean malcolmScan) {
		this.malcolmScan = malcolmScan;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.dawnsci.nexus.INexusDevice#getNexusProvider(org.eclipse.dawnsci.nexus.NexusScanInfo)
	 */
	@Override
	public NexusObjectProvider<NXcollection> getNexusProvider(NexusScanInfo info) {
		// Note: NexusScanFileManager relies on this method returning the same object each time
		if (nexusProvider == null) {
			nexusProvider = new DelegatingNexusObjectProvider<>(GROUP_NAME_SOLSTICE_SCAN,
							NexusBaseClass.NX_COLLECTION, () -> createNexusObject(info));
		}
		
		return nexusProvider;
	}
	
	public NXcollection createNexusObject(NexusScanInfo info) {
		scanStartTime = Instant.now(); // record the current time
		
		final NXcollection scanPointsCollection = NexusNodeFactory.createNXcollection();
		
		// add a field for the scan rank
		scanPointsCollection.setField(FIELD_NAME_SCAN_RANK, info.getRank());
		
		try {
		    String cmd = ServiceHolder.getParserService().getCommand(model.getBean().getScanRequest(), true);
			scanPointsCollection.setField(FIELD_NAME_SCAN_CMD,  cmd);
		} catch (Exception ne) {
			logger.debug("Unable to write scan command", ne);
		}
		
		try {
			List<?> models = model.getBean().getScanRequest().getCompoundModel().getModels();
			String json = ServiceHolder.getMarshallerService().marshal(models);
			scanPointsCollection.setField(FIELD_NAME_SCAN_MODELS, json);
		} catch (Exception ne) {
			logger.debug("Unable to write point models", ne);
		}

		// create the scan finished dataset and set the initial value to false
//		scanFinished = scanPointsCollection.initializeFixedSizeLazyDataset(
//				FIELD_NAME_SCAN_FINISHED, new int[] { 1 }, Dataset.INT32);
		// TODO: workaround for bug in HD5 loader, do not set size limit 
		scanFinishedDataset = new LazyWriteableDataset(FIELD_NAME_SCAN_FINISHED, Integer.class,
				new int[] { 1 }, new int[] { -1 }, new int[] { 1 }, null);
		scanFinishedDataset.setFillValue(0);
		scanPointsCollection.createDataNode(FIELD_NAME_SCAN_FINISHED, scanFinishedDataset);
		
		// write the scan shape
		scanShape = info.getShape();
		logger.info("Estimated scan shape " + Arrays.toString(scanShape));
		scanPointsCollection.setDataset(FIELD_NAME_SCAN_SHAPE, DatasetFactory.createFromObject(scanShape));
		
		// write the estimated scan duration
		long estimatedScanTimeMillis = model.getScanInformation().getEstimatedScanTime();
		String estimatedScanTimeStr = durationInMillisToString(Duration.ofMillis(estimatedScanTimeMillis));
		logger.info("Estimated scan time " + estimatedScanTimeStr);
		scanPointsCollection.setField(FIELD_NAME_SCAN_ESTIMATED_DURATION, estimatedScanTimeStr);
		
		// create lazy datasets for actual scan duration, dead time and dead time percent
		// these are written to at the end of the scan
		scanDurationDataset = new LazyWriteableDataset(FIELD_NAME_SCAN_DURATION, String.class,
				new int[] { 1 }, new int[] { -1 }, new int[] { 1 }, null);
		scanPointsCollection.createDataNode(FIELD_NAME_SCAN_DURATION, scanDurationDataset);
		scanDeadTimeDataset = new LazyWriteableDataset(FIELD_NAME_SCAN_DEAD_TIME, String.class,
				new int[] { 1 }, new int[] { -1 }, new int[] { 1 }, null);
		scanPointsCollection.createDataNode(FIELD_NAME_SCAN_DEAD_TIME, scanDeadTimeDataset);
		scanDeadTimePercentDataset = new LazyWriteableDataset(FIELD_NAME_SCAN_DEAD_TIME_PERCENT, String.class,
				new int[] { 1 }, new int[] { -1 }, new int[] { 1 }, null);
		scanPointsCollection.createDataNode(FIELD_NAME_SCAN_DEAD_TIME_PERCENT, scanDeadTimePercentDataset);
		
		// create a sub-collection for the unique keys field and keys from each external file
		final NXcollection keysCollection = NexusNodeFactory.createNXcollection();
		scanPointsCollection.addGroupNode(GROUP_NAME_KEYS, keysCollection);
		
		// create the unique keys dataset (not for malcolm scans)
		if (!malcolmScan) {
			uniqueKeysDataset = keysCollection.initializeLazyDataset(FIELD_NAME_UNIQUE_KEYS, info.getRank(), Integer.class);
		}

		// set chunking for lazy datasets
		if (info.getRank() > 0) {
			final int[] chunk = info.createChunk(false, 8);
			if (!malcolmScan) {
				uniqueKeysDataset.setFillValue(0);
				uniqueKeysDataset.setChunking(chunk);
			}
		}
		
		// add external links to the unique key datasets for each external HD5 file
		addLinksToExternalFiles(keysCollection);
		
		return scanPointsCollection;
	}
	
	private static String durationInMillisToString(Duration duration) {
		long days = duration.toDays(); // chop off any days as formatter can't handle them
		duration = duration.minusDays(days);
		
		// convert duration to a time by adding it to midnight
		LocalTime durationAsTime = LocalTime.MIDNIGHT.plus(duration);
		String result = formatter.format(durationAsTime);
		
		if (days > 0) result = String.format("%dd ", days) + result;
		return result;
	}
	
	/**
	 * Called when the scan completes to: 
	 * <ul>
	 * <li>write the scan finished (by writing '1' to the scan finished dataset;</li>
	 * <li>write the scan duration.</li>
	 * </ul>
	 * @throws ScanningException
	 */
	public void scanFinished() throws ScanningException {
		// Note: we don't use scanFinally as that is called after the nexus file is closed.
		final Dataset scanFinishedDataset = DatasetFactory.createFromObject(IntegerDataset.class, 1, null);
		try {
			this.scanFinishedDataset.setSlice(null, scanFinishedDataset,
					new int[] { 0 }, new int[] { 1 }, new int[] { 1 });
		} catch (Exception e) {
			throw new ScanningException("Could not write scanFinished to NeXus file", e);
		}
		
		Duration scanDuration = Duration.between(scanStartTime, Instant.now());
		String scanDurationStr = durationInMillisToString(scanDuration);
		logger.info("Scan finished in " + scanDurationStr);
		
		final Dataset scanDurationDataset = DatasetFactory.createFromObject(scanDurationStr);
		try {
			this.scanDurationDataset.setSlice(null, scanDurationDataset,
					new int[] { 0 }, new int[] { 1 }, new int[] { 1 });
		} catch (Exception e) {
			throw new ScanningException("Could not write scan duration to NeXus file", e);
		}
		
		final long estimatedScanTimeMillis = model.getScanInformation().getEstimatedScanTime();
		Duration scanDeadTime = scanDuration.minus(estimatedScanTimeMillis, ChronoUnit.MILLIS);
		String scanDeadTimeStr = durationInMillisToString(scanDeadTime);
		final Dataset scanDeadTimeDataset = DatasetFactory.createFromObject(scanDeadTimeStr);
		try {
			this.scanDeadTimeDataset.setSlice(null, scanDeadTimeDataset,
					new int[] { 0 }, new int[] { 1 }, new int[] { 1 });
		} catch (Exception e) {
			throw new ScanningException("Could not write scan dead time to NeXus file", e);
		}
		
		double deadTimePercent = ((double) scanDeadTime.toMillis() / scanDuration.toMillis()) * 100.0;
		final String deadTimePercentStr = String.format("%.2f", deadTimePercent);
		final Dataset deadTimePercentDataset = DatasetFactory.createFromObject(deadTimePercentStr);
		try {
			this.scanDeadTimePercentDataset.setSlice(null, deadTimePercentDataset,
					new int[] { 0 }, new int[] { 1 }, new int[] { 1 });
		} catch (Exception e) {
			throw new ScanningException("Could not write scan dead time percent to NeXus file", e);
		}
		
		final ScanInformation scanInfo = model.getScanInformation();
		final String filePath = scanInfo.getFilePath();
		final String shapeStr = Arrays.toString(scanShape);
		final String estimatedTimeStr = durationInMillisToString(Duration.ofMillis(scanInfo.getEstimatedScanTime()));
		logger.info("MScan Details: scan file = {}, shape = {}, estimated time = {}, actual time = {}, dead time = {} ({}%)",
				filePath, shapeStr, estimatedTimeStr, scanDurationStr, scanDeadTimeStr, deadTimePercentStr);
	}

	@Override
	public Object getPosition() throws Exception {
		return null;
	}

	@Override
	public Object setPosition(Object value, IPosition position) {
		return writePosition(position);
	}

	/**
	 * For each device, if that device writes to an external file, create an external link
	 * within the unique keys collection to the unique keys dataset in that file.
	 * The unique keys are required in order for live processing to take place - we need to know
	 * how much data has been written to each file - devices may flush their data to file at
	 * different times.
	 * @param uniqueKeysCollection unique keys collection to add any external links to
	 */
	private void addLinksToExternalFiles(final NXcollection uniqueKeysCollection) {
		if (nexusObjectProviders == null) throw new IllegalStateException("nexusObjectProviders not set");
		
		for (NexusObjectProvider<?> nexusObjectProvider : nexusObjectProviders) {
			String uniqueKeysPath = (String) nexusObjectProvider.getPropertyValue(PROPERTY_NAME_UNIQUE_KEYS_PATH);
			if (uniqueKeysPath != null) {
				for (String externalFileName : nexusObjectProvider.getExternalFileNames()) {
					addLinkToExternalFile(uniqueKeysCollection, externalFileName, uniqueKeysPath);
				}
			}
		}
	}

	/**
	 * Add a link to the unique keys dataset of the external file if not already present.
	 * @param uniqueKeysCollection unique keys collection to add link to
	 * @param externalFileName name of external file
	 * @param uniqueKeysPath path to unique keys dataset in external file
	 */
	private void addLinkToExternalFile(final NXcollection uniqueKeysCollection,
			String externalFileName, String uniqueKeysPath) {
		// we just use the final segment of the file name as the dataset name,
		// This assumes that we won't have files with the same name in different dirs
		// Note: the name doesn't matter for processing purposes 
		String datasetName = new File(externalFileName).getName();
		if (uniqueKeysCollection.getSymbolicNode(datasetName) == null) {
			uniqueKeysCollection.addExternalLink(datasetName, externalFileName,
					uniqueKeysPath);
		}
	}

	/**
	 * Write the given position to the NexusFile.
	 * The unique key of the position is added to the <code>uniqueKeys</code> dataset,
	 * and the position as a string, as returned by {@link IPosition#toString()},
	 * is added to the <code>points</code>
	 * @param position
	 * @throws Exception
	 */
	private Object writePosition(IPosition position) {
		if (!malcolmScan) {
			IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(position);
			SliceND sliceND = new SliceND(uniqueKeysDataset.getShape(), uniqueKeysDataset.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			final int uniqueKey = position.getStepIndex() + 1;
			final Dataset newActualPosition = DatasetFactory.createFromObject(uniqueKey);
			try {
				uniqueKeysDataset.setSlice(null, newActualPosition, sliceND);
			} catch (DatasetException e) {
				logger.error("Could not write unique key");
			}
			return newActualPosition;
		}
		return null;
	}

	public boolean writeAfterMovePerformed() {
		return writeAfterMovePerformed;
	}

}