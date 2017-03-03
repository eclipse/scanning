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
package org.eclipse.scanning.example.scannable;

import java.text.MessageFormat;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NXslit;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
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
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;

/**
 * A class to generate take a set of scannables which represent simple slits then write to a NeXus file
 * as the positions are set during the scan.
 * 
 * @see {@link MockScannableConfiguration}
 */
public class MockNeXusSlit extends MockScannable implements INexusDevice<NXslit> {

	private ILazyWriteableDataset xLzSet;
	private ILazyWriteableDataset yLzSet;
	private ILazyWriteableDataset xLzValue;
	private ILazyWriteableDataset yLzValue;
	
	private boolean writingOn = true;
	
	public MockNeXusSlit() {
		super();
	}
	
	public MockNeXusSlit(String name, double d, int level) {
		super(name, d, level);
	}

	public MockNeXusSlit(String name, double d, int level, String unit) {
		super(name, d, level, unit);
	}
	
	public boolean isWritingOn() {
		return writingOn;
	}

	public void setWritingOn(boolean writingOn) {
		this.writingOn = writingOn;
	}

	@ScanFinally
	public void nullify() {
		xLzSet   = null;
		xLzValue = null;
		yLzSet   = null;
		yLzValue = null;
	}

	public NexusObjectProvider<NXslit> getNexusProvider(NexusScanInfo info) throws NexusException {
		final NXslit positioner = NexusNodeFactory.createNXslit();

		if (info.getScanRole(getName()) == ScanRole.METADATA) {
			positioner.setX_gapScalar(getPosition().doubleValue());
			positioner.setY_gapScalar(getPosition().doubleValue());
		} else {
			String floatFill = System.getProperty("GDA/gda.nexus.floatfillvalue", "NaN");
			double fill = "NaN".equalsIgnoreCase(floatFill) ? Double.NaN : Double.parseDouble(floatFill);

			xLzSet = positioner.initializeLazyDataset(NXslit.NX_X_GAP, 1, Double.class);
			yLzSet = positioner.initializeLazyDataset(NXslit.NX_Y_GAP, 1, Double.class);
			xLzSet.setFillValue(fill);
			yLzSet.setFillValue(fill);
			xLzSet.setChunking(new int[]{8}); // Faster than looking at the shape of the scan for this dimension because slow to iterate.
			yLzSet.setChunking(new int[]{8}); // Faster than looking at the shape of the scan for this dimension because slow to iterate.
			xLzSet.setWritingAsync(true);
			yLzSet.setWritingAsync(true);

			xLzValue = positioner.initializeLazyDataset(NXslit.NX_X_GAP, info.getRank(), Double.class);
			yLzValue = positioner.initializeLazyDataset(NXslit.NX_Y_GAP, info.getRank(), Double.class);
			xLzValue.setFillValue(fill);
			yLzValue.setFillValue(fill);
			xLzValue.setChunking(info.createChunk(false, 8)); // Might be slow, need to check this
			yLzValue.setChunking(info.createChunk(false, 8)); // Might be slow, need to check this
			xLzValue.setWritingAsync(true);
			yLzValue.setWritingAsync(true);
		}

		registerAttributes(positioner, this);

		NexusObjectWrapper<NXslit> nexusDelegate = new NexusObjectWrapper<>(
				getName(), positioner, NXslit.NX_X_GAP);
		nexusDelegate.setDefaultAxisDataFieldName(NXslit.NX_X_GAP);
		nexusDelegate.setCategory(NexusBaseClass.NX_INSTRUMENT);
		return nexusDelegate;
	}

	public void setPosition(Number initialValue, IPosition position) throws Exception {
		Number value = initialValue;

		if (value!=null) {
			int index = position!=null ? position.getIndex(getName()) : -1;
			if (isRealisticMove()) {
				value = doRealisticMove(value, index, -1);
			}
			this.position = value;
			delegate.firePositionPerformed(-1, new Scalar(getName(), index, value.doubleValue()));
		}

		if (position!=null) {
			write(value, getPosition(), position);
		}
	}

	private void write(Number demand, Number actual, IPosition loc) throws Exception {

		if (xLzValue==null || yLzValue==null) {
			return;
		}
		if (actual!=null) {
			// write actual position
			final Dataset newActualPositionData = DatasetFactory.createFromObject(actual);
			IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(loc);
			SliceND xSliceND = new SliceND(xLzValue.getShape(), xLzValue.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			SliceND ySliceND = new SliceND(yLzValue.getShape(), yLzValue.getMaxShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			if (isWritingOn()) {
				xLzValue.setSlice(null, newActualPositionData, xSliceND);
				yLzValue.setSlice(null, newActualPositionData, ySliceND);
			}
		}

		if (xLzSet==null || yLzSet==null) {
			return;
		}
		if (demand!=null) {
			int index = loc.getIndex(getName());
			if (index<0) {
				throw new Exception("Incorrect data index for scan for value of '"+getName()+"'. The index is "+index);
			}
			final int[] startPos = new int[] { index };
			final int[] stopPos = new int[] { index + 1 };

			// write demand position
			final Dataset newDemandPositionData = DatasetFactory.createFromObject(demand);
			if (isWritingOn()) {
				xLzSet.setSlice(null, newDemandPositionData, startPos, stopPos, null);
				yLzSet.setSlice(null, newDemandPositionData, startPos, stopPos, null);
			}
		}
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
		if (container.getScanAttributeNames()!=null) {
			for(String attrName : container.getScanAttributeNames()) {
				try {
					nexusObject.setField(attrName, container.getScanAttribute(attrName));
				} catch (Exception e) {
					throw new NexusException(MessageFormat.format(
							"An exception occurred attempting to get the value of the attribute ''{0}'' for the device ''{1}''",
							container.getName(), attrName), e);
				}
			}
		}
	}
}
