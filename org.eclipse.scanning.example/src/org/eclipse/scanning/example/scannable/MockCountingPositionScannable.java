package org.eclipse.scanning.example.scannable;

import org.eclipse.scanning.api.points.IPosition;

public class MockCountingPositionScannable extends MockScannable {

	private final boolean returnPosition;

	public MockCountingPositionScannable(String name, double position, boolean returnPosition) {
    	super(name, position);
    	this.returnPosition = returnPosition;
	}

	@Override
	public Number setPosition(Number value, IPosition loc) throws Exception {
        count(Thread.currentThread().getStackTrace());
        Number ret = super.setPosition(value, loc);
        return returnPosition ? ret : null;
	}
	

	@Override
	public Number getPosition() {
        count(Thread.currentThread().getStackTrace());
        return super.getPosition();
	}
}
