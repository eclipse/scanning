package org.eclipse.scanning.device.composite;

import java.util.Collections;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.scan.DeviceValueMultiPosition;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A scannable that returns a nexus object created by combining the nexus objects of other scannables.
 *
 * This scannable should only be used as a per scan monitor, attempts to use it as a per point monitor will fail.
 *
 * @author Matthew Dickie
 *
 * @param <T> the type of object returned by {@link #getPosition()}. Not normally used by this class
 * @param <N> the type of nexus object created for this scannable.
 */
public class CompositeNexusScannable<N extends NXobject> extends AbstractScannable<DeviceValueMultiPosition> implements INexusDevice<N> {

	private static Logger logger = LoggerFactory.getLogger(CompositeNexusScannable.class);

	private NexusBaseClass nexusClass = NexusBaseClass.NX_COLLECTION;
	private NexusBaseClass nexusCategory;
	private List<ChildNode> childNodes;

	public CompositeNexusScannable() {
		super(Services.getScannableDeviceService());
	}

	@Override
	public DeviceValueMultiPosition getPosition() throws Exception {
		DeviceValueMultiPosition position = new DeviceValueMultiPosition();

		IScannableDeviceService scannableDeviceService = getScannableDeviceService();
		for (ChildNode childNode : getChildNodes()) {
			final String scannableName = childNode.getScannableName();
			try {
				final IScannable<?> scannable = scannableDeviceService.getScannable(scannableName);
				if (scannable instanceof CompositeNexusScannable) {
					((CompositeNexusScannable<?>) scannable).getPosition().getValues().forEach((k,v) -> position.put(scannableName+"."+k, v));
				} else {
					position.put(scannableName, (double)scannable.getPosition());
				}
			} catch (ScanningException e) {
				throw new NexusException(e);
			}
		}
		return position;
	}

	@Override
	public DeviceValueMultiPosition setPosition(DeviceValueMultiPosition value, IPosition position) throws Exception {
		logger.warn("setPosition({}, {}) called on {}", value, position, this);
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
		for (ChildNode childNode : getChildNodes()) {
			final String scannableName = childNode.getScannableName();
			try {
				final IScannable<?> scannable = scannableDeviceService.getScannable(scannableName);
				if (info.getScanRole(scannable.getName()) != ScanRole.NONE) {
					// Cannot include scannables that are already in the scan - this would mean the nexus object is duplicated.
					throw new NexusException("The scannable " + scannable.getName() + " is already in the scan.");
				}
				if (scannable instanceof INexusDevice<?>) {
					final NexusObjectProvider<?> nexusProvider = ((INexusDevice<?>) scannable).getNexusProvider(info);
					final NXobject nexusObj = nexusProvider.getNexusObject();
					if (childNode instanceof ChildGroupNode) {
						final String groupName = ((ChildGroupNode) childNode).getGroupName();
						nexusObject.addGroupNode(groupName, nexusObj);
					} else if (childNode instanceof ChildFieldNode) {
						final ChildFieldNode fieldNode = (ChildFieldNode) childNode;
						DataNode dataNode = nexusObj.getDataNode("value");
						if (dataNode == null) {
							throw new NullPointerException("No such dataset: " + fieldNode.getSourceFieldName());
						}
						nexusObject.addDataNode(fieldNode.getDestinationFieldName(), dataNode);
					}
				}
			} catch (ScanningException e) {
				throw new NexusException(e);
			}
		}
		return nexusObject;
	}

	public List<ChildNode> getChildNodes() {
		return childNodes == null ? Collections.emptyList() : childNodes;
	}

	public void setChildNodes(List<ChildNode> childNodes) {
		this.childNodes = childNodes;
	}
}
