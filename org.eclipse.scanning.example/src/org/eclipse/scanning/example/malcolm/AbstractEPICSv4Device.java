package org.eclipse.scanning.example.malcolm;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.epics.pvdata.pv.PVStructure;

public abstract class AbstractEPICSv4Device implements IEPICSv4Device {

	protected String recordName = "mydevice";
	protected static int traceLevel = 0;
    protected final CountDownLatch latch = new CountDownLatch(1);
    protected DummyMalcolmRecord pvRecord = null;

    public AbstractEPICSv4Device(String deviceName) {
    	recordName = deviceName;
    }

    @Override
	public String getRecordName() {
		return recordName;
	}

    @Override
	public void stop() {
    	latch.countDown();
    }

    @Override
	public Map<String, PVStructure> getReceivedRPCCalls() {
    	return pvRecord.getReceivedRPCCalls();
    }

}
