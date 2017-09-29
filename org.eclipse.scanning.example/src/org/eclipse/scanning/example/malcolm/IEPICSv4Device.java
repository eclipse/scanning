package org.eclipse.scanning.example.malcolm;

import java.util.Map;

import org.epics.pvdata.pv.PVStructure;

public interface IEPICSv4Device {

	public void start() throws Exception;

	public String getRecordName();

	public void stop();

	public Map<String, PVStructure> getReceivedRPCCalls();
}
