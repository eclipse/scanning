package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.rank.IScanSlice;

public class SliceDeviceContext {

	private IPosition    location;
	private IScanSlice   scanSlice;
	private ILazyDataset data;
	private IDataset     slice;
	
	public SliceDeviceContext() {
		
	}
	public SliceDeviceContext(IPosition loc, IScanSlice rslice, ILazyDataset data, IDataset slice) {
		this.location    = loc;
		this.scanSlice = rslice;
		this.data   = data;
		this.slice  = slice;
	}
	public IPosition getLocation() {
		return location;
	}
	public void setLocation(IPosition loc) {
		this.location = loc;
	}
	public IScanSlice getScanSlice() {
		return scanSlice;
	}
	public void setScanSlice(IScanSlice rslice) {
		this.scanSlice = rslice;
	}
	public ILazyDataset getData() {
		return data;
	}
	public void setData(ILazyDataset data) {
		this.data = data;
	}
	public IDataset getSlice() {
		return slice;
	}
	public void setSlice(IDataset slice) {
		this.slice = slice;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((scanSlice == null) ? 0 : scanSlice.hashCode());
		result = prime * result + ((slice == null) ? 0 : slice.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SliceDeviceContext other = (SliceDeviceContext) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (scanSlice == null) {
			if (other.scanSlice != null)
				return false;
		} else if (!scanSlice.equals(other.scanSlice))
			return false;
		if (slice == null) {
			if (other.slice != null)
				return false;
		} else if (!slice.equals(other.slice))
			return false;
		return true;
	}

}
