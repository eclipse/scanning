package org.eclipse.scanning.example.scannable;

import java.util.Collections;
import java.util.List;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * A scannable that returns a nexus object created by combining the nexus objects of other scannables.
 * 
 * @author Matthew Dickie
 *
 * @param <T> the type of object returned by {@link #getPosition()}. Not normally used by this class
 * @param <N> the type of nexus object created for this scannable.
 */
public class CompositeNexusScannable<T, N extends NXobject> extends AbstractScannable<T> implements INexusDevice<N> {
	
	private NexusBaseClass nexusClass;
	private NexusBaseClass nexusCategory;
	private List<String> scannableNames;
	
	@Override
	public T getPosition() throws Exception {
		throw new UnsupportedOperationException("A CompositeNexusScannable should only be used as a per-scan monitor");
	}

	@Override
	public void setPosition(T value, IPosition position) throws Exception {
		throw new UnsupportedOperationException("A CompositeNexusScannable should only be used as a per-scan monitor");
	}
	
	public NexusBaseClass getNexusClass() {
		return nexusClass;
	}
	
	public void setNexusClass(NexusBaseClass nexusClass) {
		this.nexusClass = nexusClass;
	}
	
	public NexusBaseClass getNexusCategory() {
		return nexusCategory;
	}
	
	public void setNexusCategory(NexusBaseClass nexusCategory) {
		this.nexusCategory = nexusCategory;
	}

	@Override
	public NexusObjectProvider<N> getNexusProvider(NexusScanInfo info) throws NexusException {
		N nexusObject = buildNexusObject(info);
		NexusObjectWrapper<N> nexusWrapper = new NexusObjectWrapper<N>(getName(), nexusObject);
		nexusWrapper.setCategory(nexusCategory);
		return nexusWrapper;
	}

	private N buildNexusObject(NexusScanInfo info) throws NexusException {
		@SuppressWarnings("unchecked")
		N nexusObject = (N) NexusNodeFactory.createNXobjectForClass(nexusClass);
		
		IScannableDeviceService scannableDeviceService = getScannableDeviceService();
		for (String scannableName : getScannableNames()) {
			try {
				IScannable<?> scannable = scannableDeviceService.getScannable(scannableName);
				if (scannable instanceof INexusDevice<?>) {
					NexusObjectProvider<?> nexusProvider = ((INexusDevice<?>) scannable).getNexusProvider(info);
					nexusObject.addGroupNode(nexusProvider.getName(), nexusProvider.getNexusObject());
				}
			} catch (ScanningException e) {
				throw new NexusException(e);
			}
		}
		
		return nexusObject;
	}

	public List<String> getScannableNames() {
		return scannableNames == null ? Collections.emptyList() : scannableNames;
	}

	public void setScannableNames(List<String> scannableNames) {
		this.scannableNames = scannableNames;
	}

}
