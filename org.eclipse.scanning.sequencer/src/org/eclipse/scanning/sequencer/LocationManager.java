package org.eclipse.scanning.sequencer;

import java.util.Iterator;

import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;

/**
 * 
 * This class manages the location of
 * various parts of the scan as the scan runs.
 * 
 * It maintains a count of levels and creates a 
 * 
 * @author Matthew Gerring
 *
 */
public final class LocationManager {


	/**
	 * Variables used to monitor progress of inner scans
	 */
	private int outerSize  = 0;
	private int outerCount = 0;
	private int innerSize  = 0;
	private int totalSize  = 0;
	private int stepNumber = -1;
	
	// External data
	private final ScanBean bean;
	private final ScanModel model;
	private final AnnotationManager manager;
	
	public LocationManager(ScanBean bean, ScanModel model, AnnotationManager manager) {
		this.bean    = bean;
		this.model   = model;
		this.manager = manager;
		manager.addDevices(this);
	}
	
	public int getOuterSize() {
		return outerSize;
	}
	public void setOuterSize(int outerSize) {
		this.outerSize = outerSize;
	}
	public int getOuterCount() {
		return outerCount;
	}
	public void setOuterCount(int outerCount) {
		this.outerCount = outerCount;
	}
	public int getInnerSize() {
		return innerSize;
	}
	public void setInnerSize(int innerSize) {
		this.innerSize = innerSize;
	}
	public int getTotalSize() {
		return totalSize;
	}
	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}
	public int getStepNumber() {
		return stepNumber;
	}
	public void setStepNumber(int stepNumber) {
		this.stepNumber = stepNumber;
	}
	
	/**
	 * Called during the scan to increment counts.
	 */
	@PointEnd
	public void increment() {
    	outerCount++;
		stepNumber+=Math.max(innerSize, 1);
	}

	/**
	 * Method used to generate an iterator for the scan.
	 * It sets counts which are incremented during the scan.
	 * 
	 * @return
	 * @throws ScanningException
	 */
	public Iterator<IPosition> createPositionIterator() throws ScanningException {
		
		CompoundModel<?> cmodel = bean.getScanRequest()!=null ? bean.getScanRequest().getCompoundModel() : null;
		SubscanModerator moderator = new SubscanModerator(model.getPositionIterable(), cmodel, model.getDetectors(), ServiceHolder.getGeneratorService());
		manager.addContext(moderator);
		
		try {
			stepNumber = 0;
			outerSize  = getEstimatedSize(moderator.getOuterIterable());
			innerSize  = getEstimatedSize(moderator.getInnerIterable());
			totalSize  = getEstimatedSize(model.getPositionIterable());
		} catch (GeneratorException se) {
			throw new ScanningException("Cannot create the position iterator!", se);
		}

		return moderator.getOuterIterable().iterator();
	}


	private int getEstimatedSize(Iterable<IPosition> gen) throws GeneratorException {
		
		int size=0;
		if (gen instanceof IDeviceDependentIterable) {
			size = ((IDeviceDependentIterable)gen).size();
			
		} else if (gen instanceof IPointGenerator<?>) {
			size = ((IPointGenerator<?>)gen).size();
			
		} else if (gen!=null) {
		    for (IPosition unused : gen) size++; // Fast even for large stuff providing they do not check hardware on the next() call.
		}
		return size;   		
	}

	/**
	 * Seek within the iterator for the given location.
	 * @param location
	 * @param iterator
	 * @return null if position not found.
	 */
	public IPosition seek(int location, Iterator<IPosition> iterator) {
		
		stepNumber=0;
		/*
		 * IMPORTANT We do not keep the positions in memory because there can be millions.
		 * Running over them is fast however.
		 */
		while(iterator.hasNext()) {
			IPosition pos = iterator.next();
        	pos.setStepIndex(stepNumber);
			if (stepNumber == location) return pos;
			stepNumber+=Math.max(innerSize, 1);
		}
		return null;
	}

	public boolean isInnerScan() {
		return outerSize > 0 && innerSize > 0;
	}

	public int getOverallCount() {
		return (outerCount * innerSize) + getStepNumber() + 1; 
	}

	/**
	 * TODO This code is copied from AcquisitionDevice but 
	 * it is not clear if/how it works. The stepnumber was
	 * used however this is the global position in the scan
	 * so presumably the maths are wrong.
	 * 
	 * @return
	 */
	public double getOuterPercent() {
		
		double innerPercentComplete = 0;
		if (stepNumber > -1) {
			innerPercentComplete = (double) (stepNumber + 1) / innerSize;
		}
		double outerPercentComplete = 0;
		if (outerCount > -1) {
			outerPercentComplete = ((double) (outerCount) / outerSize) * 100;
		}
		double innerPercentOfOuter = 100 / (double) outerSize;
		innerPercentOfOuter *= innerPercentComplete;
		outerPercentComplete += innerPercentOfOuter;
		
		return outerPercentComplete;
	}

}
