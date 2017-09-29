package org.eclipse.scanning.test.epics;

import java.util.concurrent.CountDownLatch;

import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.connector.epics.EpicsV4ConnectorService;

public class HangingGetConnectorService extends EpicsV4ConnectorService {

	private CountDownLatch latch;

	public HangingGetConnectorService() {
		super();
		this.latch = new CountDownLatch(1);
	}

	@Override
	protected MalcolmMessage sendGetMessage(IMalcolmDevice<?> device, MalcolmMessage message) throws Exception {
		latch.await();
        return null;
	}

	@Override
	public void disconnect() throws MalcolmDeviceException {
		latch.countDown();
		super.disconnect();
	}
}
