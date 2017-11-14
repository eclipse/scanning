package org.eclipse.scanning.test.epics;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.eclipse.scanning.example.malcolm.EPICSv4ExampleDevice;
import org.eclipse.scanning.example.malcolm.IEPICSv4Device;

public class DeviceRunner {

	private Exception exception;
	private Class<?> deviceClass;

	public Exception getException() {
		return exception;
	}

	public DeviceRunner() {
		this(EPICSv4ExampleDevice.class);
	}

	public DeviceRunner(Class<? extends IEPICSv4Device> class1) {
		this.deviceClass = class1;
	}

	public IEPICSv4Device start() throws Exception {
		String deviceName = getTestDeviceName();

		Constructor<IEPICSv4Device> constructor = (Constructor<IEPICSv4Device>)deviceClass.getConstructor(String.class);
		IEPICSv4Device device = constructor.newInstance(deviceName);

		Thread worker = new Thread(()->execute(device), "EPICSv4 Runner "+deviceName);
		worker.setPriority(Thread.NORM_PRIORITY-2);
		worker.start();

		return device;
	}

	private void execute(IEPICSv4Device device) {
		try {
			device.start();
		} catch (Exception ne) {
			this.exception = ne;
		}
	}

	private static String getTestDeviceName() {
		String deviceName = "DummyMalcolmDevice";

		Map<String, String> env = System.getenv();
		if (env.containsKey("COMPUTERNAME"))
			deviceName = env.get("COMPUTERNAME");
		else if (env.containsKey("HOSTNAME"))
			deviceName = env.get("HOSTNAME");

		return deviceName.replace('.', ':') + ":malcolmTest";
	}

}