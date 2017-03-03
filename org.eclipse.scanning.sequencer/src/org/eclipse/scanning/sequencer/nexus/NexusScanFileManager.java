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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;
import org.eclipse.dawnsci.nexus.builder.CustomNexusEntryModification;
import org.eclipse.dawnsci.nexus.builder.NexusEntryBuilder;
import org.eclipse.dawnsci.nexus.builder.NexusFileBuilder;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusScanFile;
import org.eclipse.dawnsci.nexus.builder.data.AxisDataDevice;
import org.eclipse.dawnsci.nexus.builder.data.DataDevice;
import org.eclipse.dawnsci.nexus.builder.data.DataDeviceBuilder;
import org.eclipse.dawnsci.nexus.builder.data.NexusDataBuilder;
import org.eclipse.dawnsci.nexus.builder.data.PrimaryDataDevice;
import org.eclipse.dawnsci.nexus.builder.impl.MapBasedMetadataProvider;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanDataModel;
import org.eclipse.scanning.api.scan.models.ScanDeviceModel;
import org.eclipse.scanning.api.scan.models.ScanDeviceModel.ScanFieldModel;
import org.eclipse.scanning.api.scan.models.ScanMetadata;
import org.eclipse.scanning.api.scan.models.ScanMetadata.MetadataType;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and manages the NeXus file for a scan given a {@link ScanModel}.
 */
public class NexusScanFileManager implements INexusScanFileManager {
	
	private static final Logger logger = LoggerFactory.getLogger(NexusScanFileManager.class);

	private final AbstractRunnableDevice<ScanModel> scanDevice;
	private ScanModel model;
	private NexusScanInfo scanInfo;
	private NexusFileBuilder fileBuilder;
	private NexusScanFile nexusScanFile;
	private SolsticeScanMonitor solsticeScanMonitor;
	
	// we need to cache various things as they are used more than once
	/**
	 * A list of the nexus devices for each category of device. 
	 */
	private Map<ScanRole, Collection<INexusDevice<?>>> nexusDevices = null;
	
	/**
	 * A list of the nexus object providers for each category of device.
	 */
	private Map<ScanRole, List<NexusObjectProvider<?>>> nexusObjectProviders = null;
	
	/**
	 * A map from nexus object provider to the axis data device for that.
	 * This is used for devices added to an NXdata group other than the primary device
	 * (the one that supplies the signal field.) 
	 */
	private Map<NexusObjectProvider<?>, AxisDataDevice<?>> dataDevices = new HashMap<>();
	
	/**
	 * A map from scannable name to the index of the scan for that scannable,
	 * or <code>null</code> if none
	 */
	private Map<String, Integer> defaultAxisIndexForScannable = null;
	
	public NexusScanFileManager(AbstractRunnableDevice<ScanModel> scanDevice) {
		this.scanDevice = scanDevice;
	}
	
	/**
	 * Creates the nexus file for the given {@link ScanModel}. 
	 * The structure of the nexus file is determined by model and the
	 * devices that the model references - these are retrieved from the
	 * {@link IScannableDeviceService}.
	 * 
	 * @param model model of scan
	 * @throws ScanningException
	 */
	public void configure(ScanModel model) throws ScanningException {
		if (fileBuilder != null) {
			throw new IllegalStateException("The nexus file has already been created");
		}
		
		this.model = model;
		
		final List<String> scannableNames = getScannableNames(model.getPositionIterable());
		setMetadataScannables(model, scannableNames);

		this.scanInfo = createScanInfo(model, scannableNames);
		
		// create the scan points writer and add it to the scan as a monitor
		solsticeScanMonitor = createSolsticeScanMonitor(model);

		nexusDevices = extractNexusDevices(model);
		
		// convert this to a map of nexus object providers for each type
		nexusObjectProviders = extractNexusProviders();
		solsticeScanMonitor.setNexusObjectProviders(nexusObjectProviders);
	}
	
	/**
	 * 
	 * @return the paths of all the external files to which we will be writing.
	 */
	public Set<String> getExternalFilePaths() {
		Set<String> paths = new HashSet<>();
		// Big looking loop over small number of things.
		for (List<NexusObjectProvider<?>> provList : nexusObjectProviders.values()) {
			for (NexusObjectProvider<?> prov : provList) {
				paths.addAll(prov.getExternalFileNames());
			}
		}
		return paths;
	}

	public String createNexusFile(boolean async) throws ScanningException {
		// We use the new nexus framework to join everything up into the scan
		// Create a builder
		fileBuilder = ServiceHolder.getFactory().newNexusFileBuilder(model.getFilePath());
		try {
			createEntry(fileBuilder);
			// create the file from the builder and open it
			nexusScanFile = fileBuilder.createFile(async);
			nexusScanFile.openToWrite();
			return model.getFilePath();
		} catch (NexusException e) {
			throw new ScanningException("Cannot create nexus file", e);
		}
	}
	
	/**
	 * Flushes the wrapped nexus file.
	 * @throws ScanningException if the nexus file could not be flushed for any reason
	 */
	public void flushNexusFile() throws ScanningException {
		try {
			int code = nexusScanFile.flush();
			if (code < 0) {
				logger.warn("Problem flushing during scan! Flush code is "+code);
			}
		} catch (NexusException e) {
			throw new ScanningException("Cannot create nexus file", e);
		}
	}
	
	/**
	 * Writes scan finished and closes the wrapped nexus file.
	 * @throws ScanningException
	 */
	public void scanFinished() throws ScanningException {
		solsticeScanMonitor.scanFinished();
		try {
			nexusScanFile.close();
		} catch (NexusException e) {
			throw new ScanningException("Could not close nexus file", e);
		}
	}
	
	public boolean isNexusWritingEnabled() {
		return true;
	}
	
	public NexusScanInfo getNexusScanInfo() {
		return scanInfo;
	}

	protected Map<ScanRole, Collection<INexusDevice<?>>> extractNexusDevices(ScanModel model) throws ScanningException {
		final IPosition firstPosition = model.getPositionIterable().iterator().next();
		final Collection<String> scannableNames = firstPosition.getNames();
		
		Map<ScanRole, Collection<INexusDevice<?>>> nexusDevices = new EnumMap<>(ScanRole.class);
		nexusDevices.put(ScanRole.DETECTOR,  getNexusDevices(model.getDetectors()));
		nexusDevices.put(ScanRole.SCANNABLE, getNexusScannables(scannableNames));
		
		if (model.getMonitors()!=null) {
			Collection<IScannable<?>> perPoint = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_POINT).collect(Collectors.toList());
			Collection<IScannable<?>> perScan  = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_SCAN).collect(Collectors.toList());
			nexusDevices.put(ScanRole.MONITOR,   getNexusDevices(perPoint));
			nexusDevices.put(ScanRole.METADATA,  getNexusDevices(perScan));
		}
		
		return nexusDevices;
	}

	protected Map<ScanRole, List<NexusObjectProvider<?>>> extractNexusProviders() throws ScanningException {
		Map<ScanRole, List<NexusObjectProvider<?>>> nexusObjectProviders = new EnumMap<>(ScanRole.class);
		for (ScanRole deviceType: ScanRole.values()) {
			final Collection<INexusDevice<?>> nexusDevicesForType = nexusDevices.get(deviceType);
			final List<NexusObjectProvider<?>> nexusObjectProvidersForType =
					new ArrayList<>(nexusDevicesForType.size());
			for (INexusDevice<?> nexusDevice : nexusDevicesForType) {
				try {
					NexusObjectProvider<?> nexusProvider = nexusDevice.getNexusProvider(scanInfo);
					if (nexusProvider != null) {
						nexusObjectProvidersForType.add(nexusProvider);
					}
				} catch (NexusException e) {
					throw new ScanningException("Cannot create device: " + e);
				}
			}
			
			nexusObjectProviders.put(deviceType, nexusObjectProvidersForType);
		}
		
		return nexusObjectProviders;
	}

	/**
	 * Augments the set of metadata scannables in the model with: <ul>
	 * <li>any scannables from the legacy spring configuration;</li>
	 * <li>the required scannables of any scannables in the scan;</li>
	 * </ul> 
	 * @param model
	 * @throws ScanningException
	 */
	@SuppressWarnings("deprecation")
	private void setMetadataScannables(ScanModel model, Collection<String> scannableNames) throws ScanningException {
		final IScannableDeviceService deviceConnectorService = scanDevice.getConnectorService();
		
		// build up the set of all metadata scannables
		final Set<String> metadataScannableNames = new HashSet<>();
		
		// add the metadata scannables in the model
		if (model.getMonitors()!=null) {
			Collection<IScannable<?>> perScan  = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_SCAN).collect(Collectors.toList());
			metadataScannableNames.addAll(perScan.stream().map(m -> m.getName()).collect(Collectors.toSet()));
		}
		
		// add the global metadata scannables, and the required metadata scannables for
		// each scannable in the scan
		metadataScannableNames.addAll(deviceConnectorService.getGlobalMetadataScannableNames());
		
		// the set of scannable names to check for dependencies
		Set<String> scannableNamesToCheck = new HashSet<>();
		scannableNamesToCheck.addAll(metadataScannableNames);
		scannableNamesToCheck.addAll(scannableNames);
		do {
			// check the given set of scannable names for dependencies
			// each iteration checks the scannable names added in the previous one
			Set<String> requiredScannables = scannableNamesToCheck.stream().flatMap(
					name -> deviceConnectorService.getRequiredMetadataScannableNames(name).stream())
					.filter(name -> !metadataScannableNames.contains(name))
					.collect(Collectors.toSet());
			
			metadataScannableNames.addAll(requiredScannables);
			scannableNamesToCheck = requiredScannables;
		} while (!scannableNamesToCheck.isEmpty());
		
		// remove any scannable names in the scan from the list of metadata scannables
		metadataScannableNames.removeAll(scannableNames);
		
		// get the metadata scannables for the given names
		final List<IScannable<?>> monitors = model.getMonitors() != null ? model.getMonitors() : new ArrayList<>();
		for (String scannableName : metadataScannableNames) {
			IScannable<?> metadataScannable = deviceConnectorService.getScannable(scannableName);
			try {
			    metadataScannable.setMonitorRole(MonitorRole.PER_SCAN);
			} catch (IllegalArgumentException ne) {
				continue;
			}
			monitors.add(metadataScannable);
		}
		
		model.setMonitors(monitors);
	}
	
	private List<String> getScannableNames(Iterable<IPosition> gen) {
		List<String> names = null;
		if (gen instanceof IDeviceDependentIterable) {
			names = ((IDeviceDependentIterable)gen).getScannableNames();
		}
		if (names==null) {
			names = model.getPositionIterable().iterator().next().getNames();
		}
		return names;
	}
	
	private NexusScanInfo createScanInfo(ScanModel scanModel, List<String> scannableNames) throws ScanningException {
		final NexusScanInfo nexusScanInfo = new NexusScanInfo(scannableNames);

		final int scanRank = getScanRank(scanModel);
		nexusScanInfo.setRank(scanRank);
		nexusScanInfo.setShape(scanModel.getScanInformation().getShape());
		
		nexusScanInfo.setDetectorNames(getDeviceNames(scanModel.getDetectors()));

		Collection<IScannable<?>> perPoint = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_POINT).collect(Collectors.toList());
		Collection<IScannable<?>> perScan  = model.getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_SCAN).collect(Collectors.toList());
        nexusScanInfo.setMonitorNames(getDeviceNames(perPoint));
		nexusScanInfo.setMetadataScannableNames(getDeviceNames(perScan));
		
		
		return nexusScanInfo;
	}
	
	protected int getScanRank(ScanModel model) throws ScanningException {
		return getScanRank(model.getPositionIterable());
	}
	
	protected int getScanRank(Iterable<IPosition> gen) {
		int scanRank = -1;
		if (gen instanceof IDeviceDependentIterable) {
			scanRank = ((IDeviceDependentIterable)gen).getScanRank();
		}
		if (scanRank < 0) {
			scanRank = gen.iterator().next().getScanRank();
		}
		if (scanRank < 0) {
			scanRank = 1;
		}
		
		return scanRank;
	}

	private Set<String> getDeviceNames(Collection<? extends INameable> devices) {
		return devices.stream().map(d -> d.getName()).collect(Collectors.toSet());
	}
	
	protected SolsticeScanMonitor createSolsticeScanMonitor(ScanModel scanModel) {
		SolsticeScanMonitor solsticeScanMonitor = new SolsticeScanMonitor(model);
		
		// add the solstice scan monitor to the list of monitors in the scan
		List<IScannable<?>> monitors = scanModel.getMonitors();
		if (monitors == null || monitors.isEmpty()) {
			scanModel.setMonitors(solsticeScanMonitor);
		} else {
			monitors.add(solsticeScanMonitor);
		}
		
		return solsticeScanMonitor;
	}
	
	/**
	 * Creates and populates the {@link NXentry} for the NeXus file.
	 * @param fileBuilder a {@link NexusFileBuilder}
	 * @throws NexusException
	 */
	private void createEntry(NexusFileBuilder fileBuilder) throws NexusException {
		final NexusEntryBuilder entryBuilder  = fileBuilder.newEntry();
		entryBuilder.addDefaultGroups();
		
		addScanMetadata(entryBuilder, model.getScanMetadata());
		
		// add all the devices to the entry. Metadata scannables are added first.
		for (ScanRole deviceType : EnumSet.allOf(ScanRole.class)) {
			addDevicesToEntry(entryBuilder, deviceType);
		}
		
		// create the NXdata groups
		createNexusDataGroups(entryBuilder);
	}
	
	private void addDevicesToEntry(NexusEntryBuilder entryBuilder, ScanRole deviceType) throws NexusException {
		entryBuilder.addAll(nexusObjectProviders.get(deviceType));
		
		List<CustomNexusEntryModification> customModifications =
				nexusDevices.get(deviceType).stream().
				map(d -> d.getCustomNexusModification()).
				filter(Objects::nonNull).
				collect(Collectors.toList());
		for (CustomNexusEntryModification customModification : customModifications) {
			entryBuilder.modifyEntry(customModification);
		}
	}
	
	private void addScanMetadata(NexusEntryBuilder entryBuilder, List<ScanMetadata> scanMetadataList) throws NexusException {
		if (scanMetadataList != null) {
			for (ScanMetadata scanMetadata : scanMetadataList) {
				// convert the ScanMetadata into a MapBasedMetadataProvider and add to the entry builder
				NexusBaseClass category = getBaseClassForMetadataType(scanMetadata.getType());
				MapBasedMetadataProvider metadataProvider = new MapBasedMetadataProvider(category);
				Map<String, Object> metadataFields = scanMetadata.getFields();
				for (String metadataFieldName : metadataFields.keySet()) {
					Object value = scanMetadata.getFieldValue(metadataFieldName);
					metadataProvider.addMetadataEntry(metadataFieldName, value); 
				}
				
				entryBuilder.addMetadata(metadataProvider);
			}
		}
	}
	
	private NexusBaseClass getBaseClassForMetadataType(MetadataType metadataType) {
		if (metadataType == null) {
			return null;
		}
		switch (metadataType) {
			case ENTRY:
				return NexusBaseClass.NX_ENTRY;
			case INSTRUMENT:
				return NexusBaseClass.NX_INSTRUMENT;
			case SAMPLE:
				return NexusBaseClass.NX_SAMPLE;
			case USER:
				return NexusBaseClass.NX_USER;
			default:
				throw new IllegalArgumentException("Unknown metadata type : " + metadataType);
		}
	}
	
	private List<INexusDevice<?>> getNexusDevices(Collection<?> devices) {
		return devices.stream().filter(d -> d instanceof INexusDevice<?>).map(
				d -> (INexusDevice<?>) d).collect(Collectors.toList());
	}
	
	protected Collection<INexusDevice<?>> getNexusScannables(Collection<String> scannableNames) throws ScanningException {
		try {
			return scannableNames.stream().map(name -> getNexusScannable(name)).
					filter(s -> s != null).collect(Collectors.toList());
		} catch (Exception e) {
			if (e.getCause() instanceof ScanningException) {
				throw (ScanningException) e.getCause();
			} else {
				throw e;
			}
		}
	}
	
	protected INexusDevice<?> getNexusScannable(String scannableName) {
		try {
			IScannable<?> scannable = scanDevice.getConnectorService().getScannable(scannableName);
			if (scannable == null) {
				throw new IllegalArgumentException("No such scannable: " + scannableName);
			}
			if (scannable instanceof INexusDevice<?>) {
				return (INexusDevice<?>) scannable;
			}
			
			return null;
		} catch (ScanningException e) {
			throw new RuntimeException("Error getting scannable with name: " + scannableName, e);
		}
	}
	
	/**
	 * Create the {@link NXdata} groups for the scan
	 * @param entryBuilder
	 * @throws NexusException
	 */
	private void createNexusDataGroups(final NexusEntryBuilder entryBuilder) throws NexusException {
		
		Set<ScanRole> deviceTypes = EnumSet.of(ScanRole.DETECTOR, ScanRole.SCANNABLE, ScanRole.MONITOR);
		if (deviceTypes.stream().allMatch(t -> nexusObjectProviders.get(t).isEmpty())) {
			throw new NexusException("The scan must include at least one device in order to write a NeXus file.");
		}
		
		List<NexusObjectProvider<?>> detectors = nexusObjectProviders.get(ScanRole.DETECTOR);
		if (detectors.isEmpty()) {
			// create a NXdata groups when there is no detector
			// (uses first monitor, or first scannable if there is no monitor either)
			createNXDataGroups(entryBuilder, null);
		} else {
			// create NXdata groups for each detector
			for (NexusObjectProvider<?> detector : detectors) {
				createNXDataGroups(entryBuilder, detector);
			}
		}
	}

	private void createNXDataGroups(NexusEntryBuilder entryBuilder, NexusObjectProvider<?> detector) throws NexusException {
		List<NexusObjectProvider<?>> scannables = nexusObjectProviders.get(ScanRole.SCANNABLE);
		List<NexusObjectProvider<?>> monitors = new LinkedList<>(nexusObjectProviders.get(ScanRole.MONITOR));
		monitors.remove(solsticeScanMonitor.getNexusProvider(scanInfo));

		// determine the primary device - i.e. the device whose primary dataset to make the @signal field
		NexusObjectProvider<?> primaryDevice = null;
		final ScanRole primaryDeviceType;
		if (detector != null) {
			// if there's a detector then it is the primary device
			primaryDevice = detector;
			primaryDeviceType = ScanRole.DETECTOR;
		} else if (!monitors.isEmpty()) {
			// otherwise the first monitor is the primary device (and therefore is not a data device)
			primaryDevice = monitors.remove(0);
			primaryDeviceType = ScanRole.MONITOR;
		} else if (!scannables.isEmpty()) {
			// if there are no monitors either (a rare edge case), where we use the first scannable
			// note that this scannable is also added as data device
			for (NexusObjectProvider<?> scannable : scannables) {
				if (scannable.getPrimaryDataFieldName() != null) {
					primaryDevice = scannable;
					break;
				}
				if (primaryDevice == null) {
					// Could not device with a dataset to use as the @signal field
					logger.error("Could not create an NXdata group for the nexus file as no suitable dataset could be found.");
					return;
				}
			}
			primaryDeviceType = ScanRole.SCANNABLE;
		} else {
			// the scan has no devices at all (sanity check as this should already have been checked for) 
			throw new IllegalStateException("There must be at least one device to create a Nexus file.");
		}
		
		// create the NXdata group for the primary data field
		String primaryDeviceName = primaryDevice.getName();
		String primaryDataFieldName = primaryDevice.getPrimaryDataFieldName();		
		createNXDataGroup(entryBuilder, primaryDevice, primaryDeviceType, monitors,
				scannables, primaryDeviceName, primaryDataFieldName);
		
		// create an NXdata group for each additional primary data field (if any)
		for (String dataFieldName : primaryDevice.getAdditionalPrimaryDataFieldNames()) {
			String dataGroupName = primaryDeviceName + "_" + dataFieldName;
			createNXDataGroup(entryBuilder, primaryDevice, primaryDeviceType, monitors,
					scannables, dataGroupName, dataFieldName);
		}
	}

	/**
	 * Create the {@link NXdata} groups for the given primary device.
	 * @param entryBuilder the entry builder to add to
	 * @param primaryDevice the primary device (e.g. a detector or monitor)
	 * @param primaryDeviceType the type of the primary device
	 * @param monitors the monitors
	 * @param scannable the scannables 
	 * @param dataGroupName the name of the {@link NXdata} group within the parent {@link NXentry}
	 * @param primaryDataFieldName the name that the primary data field name
	 *   (i.e. the <code>@signal</code> field) should have within the NXdata group
	 * @throws NexusException
	 */
	private void createNXDataGroup(NexusEntryBuilder entryBuilder,
			NexusObjectProvider<?> primaryDevice,
			ScanRole primaryDeviceType,
			List<NexusObjectProvider<?>> monitors,
			List<NexusObjectProvider<?>> scannables,
			String dataGroupName,
			String primaryDataFieldName)
			throws NexusException {
		if (entryBuilder.getNXentry().containsNode(dataGroupName)) {
			dataGroupName += "_data"; // append _data if the node already exists
		}
		
		// create the data builder and add the primary device
		final NexusDataBuilder dataBuilder = entryBuilder.newData(dataGroupName);
		
		PrimaryDataDevice<?> primaryDataDevice = createPrimaryDataDevice(
				primaryDevice, primaryDeviceType, primaryDataFieldName);
		dataBuilder.setPrimaryDevice(primaryDataDevice);
		
		// add the monitors (excludes the first monitor if the scan has no detectors)
		for (NexusObjectProvider<?> monitor : monitors) {
			dataBuilder.addAxisDevice(getAxisDataDevice(monitor, null));
		}
		
		// Create the map from scannable name to default index of that scannable in the scan
		if (defaultAxisIndexForScannable == null) {
			defaultAxisIndexForScannable = createDefaultAxisMap(scannables);
		}
		
		// add the scannables to the data builder
		Iterator<NexusObjectProvider<?>> scannablesIter = scannables.iterator();
		while (scannablesIter.hasNext()) {
			final NexusObjectProvider<?> scannable = scannablesIter.next(); 
			final Integer defaultAxisForDimensionIndex =
					defaultAxisIndexForScannable.get(scannable.getName());
			dataBuilder.addAxisDevice(getAxisDataDevice(scannable, defaultAxisForDimensionIndex));
		}
	}
	
	/**
	 * Creates a map from scannable names to the index of the scan
	 * (and therefore the index of the signal dataset of each NXdata) that this
	 * scannable is the default axis for.
	 * 
	 * @param scannables list of scannables
	 * @return map from scannable name to index that this scannable is the index for
	 */
	private Map<String, Integer> createDefaultAxisMap(List<NexusObjectProvider<?>> scannables) {
		final Map<String, Integer> defaultAxisIndexForScannableMap = new HashMap<>();
		
		AbstractPosition firstPosition = (AbstractPosition) model.getPositionIterable().iterator().next();
		// A collection of dimension (scannable) names for each index of the scan
		List<Collection<String>> dimensionNames = firstPosition.getDimensionNames();
		
		// Convert the list into a map from scannable name to index in scan, only including
		// scannable names which are the dimension name for exactly one index of the scan
		int dimensionIndex = 0;
		Iterator<Collection<String>> dimensionNamesIter = dimensionNames.iterator();
		while (dimensionNamesIter.hasNext()) {
			Collection<String> dimensionNamesForIndex = dimensionNamesIter.next();
			//need to iterate or the _indices attibute defaults to [0]
			Iterator<String> it = dimensionNamesForIndex.iterator();
			while (it.hasNext()){
				String scannableName = it.next();
				if (defaultAxisIndexForScannableMap.containsKey(scannableName)) {
					// already seen this scannable name for another index,
					// so this scannable should not be the default axis for any index
					// note: we put null instead of removing the entry in case the scannable
					// because we don't want to add it again if the scannable is encountered again
					defaultAxisIndexForScannableMap.put(scannableName, null);
				}else {
					defaultAxisIndexForScannableMap.put(scannableName, dimensionIndex);
				}
			}

			dimensionIndex++;
		}
		
		return defaultAxisIndexForScannableMap;
	}
	
	private <N extends NXobject> PrimaryDataDevice<N> createPrimaryDataDevice(
			NexusObjectProvider<N> nexusObjectProvider,
			ScanRole primaryDeviceType, String signalDataFieldName) throws NexusException {
		
		if (primaryDeviceType == ScanRole.SCANNABLE) {
			// using scannable as primary device as well as a scannable
			// only use main data field (e.g. value for an NXpositioner)
			DataDeviceBuilder<N> dataDeviceBuilder = DataDeviceBuilder.newPrimaryDataDeviceBuilder(
					nexusObjectProvider);
			dataDeviceBuilder.setAxisFields();
			return (PrimaryDataDevice<N>) dataDeviceBuilder.build();
		}

		return DataDeviceBuilder.newPrimaryDataDevice(nexusObjectProvider, signalDataFieldName);
	}
	
	/**
	 * Gets the data device for the given {@link NexusObjectProvider},
	 * creating it if it doesn't exist.
	 * 
	 * @param nexusObjectProvider nexus object provider
	 * @param scannableIndex index in scan for {@link IScannable}s, or <code>null</code>
	 *    if the scannable is being scanned (i.e. is a monitor or metadata scannable).
	 * @param isPrimaryDevice <code>true</code> if this is the primary device for
	 *    the scan, <code>false</code> otherwise
	 * @return the data device
	 * @throws NexusException 
	 */
	private AxisDataDevice<?> getAxisDataDevice(NexusObjectProvider<?> nexusObjectProvider,
			Integer scannableIndex) throws NexusException {
		AxisDataDevice<?> dataDevice = dataDevices.get(nexusObjectProvider);
		if (dataDevice == null) {
			dataDevice = createAxisDataDevice(nexusObjectProvider, scannableIndex);
			// cache the non-primary devices for any other NXdata groups
			dataDevices.put(nexusObjectProvider, dataDevice);
		}
		
		return dataDevice;
	}
	
	/**
	 * Creates the {@link DataDevice} for the given {@link NexusObjectProvider},
	 * @param nexusObjectProvider
	 * @param scannableIndex if the {@link NexusObjectProvider} represents an
	 *    {@link IScannable} then the index of that scannable in the list of scannables,
	 *    otherwise <code>null</code>
	 * @return
	 * @throws NexusException 
	 */
	private <N extends NXobject> AxisDataDevice<N> createAxisDataDevice(
			NexusObjectProvider<N> nexusObjectProvider, Integer scannableIndex) throws NexusException {
		if (model instanceof ScanDataModel) {
			// using a ScanDataModel allows for customization of how the data fields
			// of the device are added to the NXdata
			ScanDeviceModel scanDeviceModel = ((ScanDataModel) model).getScanDevice(
					nexusObjectProvider.getName());
			if (scanDeviceModel != null) {
				createCustomAxisDataDevice(nexusObjectProvider, scanDeviceModel, scannableIndex);
			}
		}
		
		return DataDeviceBuilder.newAxisDataDevice(nexusObjectProvider, scannableIndex); 
	}
	
	/**
	 * Configures the {@link DataDevice} according to the given {@link ScanDeviceModel}.
	 * @param nexusObjectProvider
	 * @param scanDeviceModel scan device model
	 */
	private <N extends NXobject> void createCustomAxisDataDevice(
			NexusObjectProvider<N> nexusObjectProvider, ScanDeviceModel scanDeviceModel,
			Integer scannableIndex) {
		DataDeviceBuilder<N> builder = DataDeviceBuilder.newAxisDataDeviceBuilder(
				nexusObjectProvider, scannableIndex);
		
		// add named fields only means only add fields
		if (scanDeviceModel.getAddNamedFieldsOnly()) {
			builder.clearAxisFields();
		}
		
		// set the default dimension mappings
		builder.setDefaultDimensionMappings(scanDeviceModel.getDefaultDimensionMappings());
		builder.setDefaultAxisDimension(scanDeviceModel.getDefaultAxisDimension());

		// configure the information for any fields
		Map<String, ScanFieldModel> fieldDimensionModels = scanDeviceModel.getFieldDimensionModels();
		for (String sourceFieldName : fieldDimensionModels.keySet()) {
			ScanFieldModel fieldDimensionModel = fieldDimensionModels.get(sourceFieldName);
			builder.addAxisField(sourceFieldName);
			if (fieldDimensionModel != null) {
				// add the field info from the ScanModel to the DataDevice
				
				// the name of the field in the NXdata
				String destinationFieldName = scanDeviceModel.getDestinationFieldName(sourceFieldName);
				if (destinationFieldName != null) {
					builder.setDestinationFieldName(sourceFieldName, destinationFieldName);
				}
				
				// the index of the dimension of the signal field that this field is a default axis for
				Integer fieldDefaultAxisDimension = fieldDimensionModel.getDefaultAxisDimension();
				if (fieldDefaultAxisDimension != null) {
					builder.setDefaultAxisDimension(fieldDefaultAxisDimension);
				}
				
				// the dimension mappings between this field and the signal field 
				int[] dimensionMappings = fieldDimensionModel.getDimensionMappings();
				if (dimensionMappings != null) {
					builder.setDimensionMappings(sourceFieldName, dimensionMappings);
				}
				
			}
		}
	}

}
