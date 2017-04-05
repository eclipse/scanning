package org.eclipse.scanning.points;

import java.util.Iterator;

import org.eclipse.scanning.api.points.IPosition;

public interface ScanPointIterator extends Iterator<IPosition> {
	
	public int size();
	
	public int[] getShape();
	
	public int getRank();

}
