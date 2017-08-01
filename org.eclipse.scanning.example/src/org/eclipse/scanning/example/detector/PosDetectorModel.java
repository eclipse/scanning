package org.eclipse.scanning.example.detector;

import org.eclipse.scanning.api.device.models.AbstractDetectorModel;

public class PosDetectorModel extends AbstractDetectorModel {
	
	private int numExposuresPerPoint;
	
	public PosDetectorModel() {
		this(1);
	}
	
	public PosDetectorModel(int numExposuresPerPoint) {
		this.numExposuresPerPoint = numExposuresPerPoint;
		setName("posDetector");
	}
	
	public void setNumExposuresPerPoint(int numExposuresPerPoint) {
		this.numExposuresPerPoint = numExposuresPerPoint;
	}
	
	public int getNumExposuresPerPoint() {
		return this.numExposuresPerPoint;
	}

}
