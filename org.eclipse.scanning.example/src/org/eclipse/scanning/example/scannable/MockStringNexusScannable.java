package org.eclipse.scanning.example.scannable;

import java.text.MessageFormat;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.IScanAttributeContainer;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;

public class MockStringNexusScannable extends MockStringScannable implements INexusDevice<NXpositioner> {

	private ILazyWriteableDataset lzValue;
	
	public MockStringNexusScannable(String name, String pos, String... permittedValues) {
		super(name, pos, permittedValues);
	}

	@Override
	public NexusObjectProvider<NXpositioner> getNexusProvider(NexusScanInfo info) throws NexusException {
		final NXpositioner positioner = NexusNodeFactory.createNXpositioner();
		positioner.setNameScalar(getName());
		
		if (info.getScanRole(getName()) == ScanRole.MONITOR_PER_SCAN) {
			try {
				// note: assume this scannable is a monitor, so no set value dataset created 
				positioner.setValue(DatasetFactory.createFromObject(getPosition()));
			} catch (Exception e) {
				throw new NexusException("Could not get value for scannable " + getName(), e);
			}
		} else {
			lzValue = positioner.initializeLazyDataset(NXpositioner.NX_VALUE, info.getRank(), String.class);
			lzValue.setChunking(info.createChunk(false, 8));
			lzValue.setWritingAsync(true);
		}
		
		registerAttributes(positioner, this);
		
		return new NexusObjectWrapper<>(getName(), positioner, NXpositioner.NX_VALUE);
	}
	
	public void setPosition(String value, IPosition position) throws Exception {
		if (position != null) {
			write(value, getPosition(), position);
		}
	}
	
	private void write(String demand, String actual, IPosition pos) throws Exception {
		if (actual != null) {
			final Dataset newActualValueData = DatasetFactory.createFromObject(actual);
			IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(pos);
			SliceND sliceND = new SliceND(lzValue.getShape(), lzValue.getMaxShape(), rslice.getStop(), rslice.getStep());
			lzValue.setSlice(null,  newActualValueData, sliceND);
		}
		// NOTE: we don't write the demand position as this class is currently only used as a monitor
	}
	
	/**
	 * Add the attributes for the given attribute container into the given nexus object.
	 * @param positioner
	 * @param container
	 * @throws NexusException if the attributes could not be added for any reason 
	 */
	private static void registerAttributes(NXobject nexusObject, IScanAttributeContainer container) throws NexusException {
		// We create the attributes, if any
		nexusObject.setField("name", container.getName());
		if (container.getScanAttributeNames()!=null) for(String attrName : container.getScanAttributeNames()) {
			try {
				nexusObject.setField(attrName, container.getScanAttribute(attrName));
			} catch (Exception e) {
				throw new NexusException(MessageFormat.format(
						"An exception occurred attempting to get the value of the attribute ''{0}'' for the device ''{1}''",
						container.getName(), attrName));
			}
		}
	}
	
}
