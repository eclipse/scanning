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
package org.eclipse.scanning.sequencer;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.scan.IScanService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public final class RunnableDeviceServiceImpl implements IRunnableDeviceService, IScanService {
	
	private static final Logger logger = LoggerFactory.getLogger(RunnableDeviceServiceImpl.class);
	
	/**
	 * The default Malcolm Hostname can be injected by spring. Otherwise
	 * this machine will be used for the malcolm host for instance 'beamline'-control.
	 */
	public static String defaultMalcolmHostname = null;
	
	/**
	 * This service can not be present for some tests which run in OSGi
	 * but mock the test laster.
	 */
	private static IScannableDeviceService deviceConnectorService;	
	
	/**
	 * This service must be present.
	 */
	private static IMalcolmService         malcolmService;
	
	/**
	 * Map of device model class to device class.
	 * NOTE This is not unmodifiable. Entries may be made after service create time. For instance
	 * when the spring files are parsed.
	 */
	private static final Map<Class<?>, Class<? extends IRunnableDevice>> modelledDevices;
	
	/**
	 * Map of device name to created device. Used to avoid
	 * recreating non-virtual devices many times.
	 * 
	 * TODO Should this be populated by spring?
	 */
	private static final Map<String, IRunnableDevice> namedDevices;
	
	
	// This field is used to provide the getActiveScanner() method on the service.
	// It should not be accessed from elsewhere.
	private static IRunnableDevice<?> currentScanningDevice;

	
	// Use a factory pattern to register the types.
	// This pattern can always be extended by extension points
	// to allow point generators to be dynamically registered. 
	static {
		System.out.println("Starting device service");
		modelledDevices = new HashMap<>(7);
		modelledDevices.put(ScanModel.class,         AcquisitionDevice.class);

		namedDevices     = new HashMap<>(3);
	}

	/**
	 * Main constructor used in the running server by OSGi (only)
	 */
	public RunnableDeviceServiceImpl() {
		try {
			readExtensions();
		} catch (CoreException e) {
			logger.error("Problem reading extension points, non-fatal as spring may be used.", e);
		}
	}
	
	// Test, we clear the devices so that each test is clean
	public RunnableDeviceServiceImpl(IScannableDeviceService deviceConnectorService) {
		this();
		RunnableDeviceServiceImpl.deviceConnectorService = deviceConnectorService;	
		modelledDevices.clear();
		modelledDevices.put(ScanModel.class,         AcquisitionDevice.class);
		namedDevices.clear();
	}
	
	
	private static void readExtensions() throws CoreException {
		
		if (Platform.getExtensionRegistry()!=null) {
			final IConfigurationElement[] eles = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.scanning.api.device");
			for (IConfigurationElement e : eles) {
								
				if (e.getName().equals("device")) {
					
					final IRunnableDevice device = (IRunnableDevice)e.createExecutableExtension("class");
					String name = e.getAttribute("name");
					if (name == null) name = e.getAttribute("id");
					device.setName(name);
					
	                // If the model has a name we send it from the extension point.
					final Object     mod = e.createExecutableExtension("model");
	                try {
	                    final Method setName = mod.getClass().getMethod("setName", String.class);
	                    setName.invoke(mod, name);
	                } catch (Exception ignored) {
	                	// getName() is not compulsory in the model
	                }

	                if (!device.getRole().isVirtual()) { // We have to make a good instance which will be used in scanning.
	                	
						final DeviceInformation<?> info   = new DeviceInformation<>();
						info.setLabel(e.getAttribute("label"));
						info.setDescription(e.getAttribute("description"));
						info.setId(e.getAttribute("id"));
						info.setIcon(e.getContributor().getName()+"/"+e.getAttribute("icon"));

						if (device instanceof AbstractRunnableDevice) {
							AbstractRunnableDevice adevice = (AbstractRunnableDevice)device;
							adevice.setDeviceInformation(info);
							
							if (adevice.getModel()==null) adevice.setModel(mod); // Empty Model
						}
	                }
					
					registerDevice(mod.getClass(), device);

				} else {
					throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.scanning.sequencer", "Unrecognized device "+e.getName()));
				}
			}
		}
	}
	
	@Override
	public <T> void register(IRunnableDevice<T> device) {
		registerDevice(device.getModel().getClass(), device);
	}

	private static void registerDevice(Class modelClass, IRunnableDevice device) {
		modelledDevices.put(modelClass, device.getClass());
		if (!device.getRole().isVirtual()) {
			namedDevices.put(device.getName(), device);
		}
	}

	@Override
	public final IPositioner createPositioner() throws ScanningException {
		// Try to set a deviceService if it is null
		if (deviceConnectorService==null) deviceConnectorService = getDeviceConnector();
		return new ScannablePositioner(deviceConnectorService);
	}

	
	@Override
	public final <T> IRunnableDevice<T> createRunnableDevice(T model) throws ScanningException {
        return createRunnableDevice(model, null, true);
	}
	

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model, boolean configure) throws ScanningException {
        return createRunnableDevice(model, null, configure);
	}

	@Override
	public final <T> IRunnableDevice<T> createRunnableDevice(T model, IPublisher<ScanBean> publisher) throws ScanningException {
        return createRunnableDevice(model, publisher, true);
	}
	
	@Override
	public final <T> IRunnableDevice<T> createRunnableDevice(T model, IPublisher<ScanBean> publisher, boolean configure) throws ScanningException {
				
		try {
			if (deviceConnectorService==null) deviceConnectorService = getDeviceConnector();
			
			final IRunnableDevice<T> scanner = createDevice(model);
			if (scanner instanceof AbstractRunnableDevice) {
				AbstractRunnableDevice<T> ascanner = (AbstractRunnableDevice<T>)scanner;
				ascanner.setRunnableDeviceService(this);
                ascanner.setConnectorService(deviceConnectorService);
                ascanner.setPublisher(publisher); // May be null
                
                // If the model has a name for the device, we use
                // it automatically.
                try {
                	String name = null;
                	// Try with INameable first as faster than reflection
                	if (model instanceof INameable) name = ((INameable) model).getName(); 
                	
                	// Try with reflection
                	if (name == null) {
	                	final Method getName = model.getClass().getMethod("getName");
	                    name = (String)getName.invoke(model);
                	}
                    ascanner.setName(name);
                } catch (NoSuchMethodException ignored) {
                	// getName() is not compulsory in the model
                }
			}
			
			if (configure) {
				AnnotationManager manager = new AnnotationManager(SequencerActivator.getInstance());
				manager.addDevices(scanner);
				manager.invoke(PreConfigure.class, model);
				scanner.configure(model);
				manager.invoke(PostConfigure.class, model);
			}
			
			if (!scanner.getRole().isVirtual()) {
				namedDevices.put(scanner.getName(), scanner);
			}
			
			return scanner;
			
		} catch (ScanningException s) {
			throw s;
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
	}
	
	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name) throws ScanningException {
		return getRunnableDevice(name, null);
	}

	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name, IPublisher<ScanBean> publisher) throws ScanningException {
		
		@SuppressWarnings("unchecked")
		IRunnableDevice<T> device = (IRunnableDevice<T>)namedDevices.get(name);
		if (device!=null && publisher!=null && device instanceof AbstractRunnableDevice) {
			AbstractRunnableDevice<T> adevice = (AbstractRunnableDevice<T>)device;
			adevice.setPublisher(publisher); // Now all its moves will be reported by this publisher.
		}
		return device;
	}
	
	private <T> IRunnableDevice<T> createDevice(T model) throws ScanningException, InstantiationException, IllegalAccessException, URISyntaxException, UnknownHostException {
		
		final IRunnableDevice<T> scanner;
		
		if (modelledDevices.containsKey(model.getClass())) {
			@SuppressWarnings("unchecked")
			final Class<IRunnableDevice<T>> clazz = (Class<IRunnableDevice<T>>)modelledDevices.get(model.getClass());
			if (clazz == null) throw new ScanningException("The model '"+model.getClass()+"' does not have a device registered for it!");
			scanner = clazz.newInstance();
			
			// TODO Might have other extension point driven devices
		} else {
			throw new ScanningException("The model '"+model.getClass()+"' does not have a device registered for it!");
		}
		return scanner;
	}

	@Override
	public IScannableDeviceService getDeviceConnectorService() {
		return deviceConnectorService;
	}

	public static void setDeviceConnectorService(IScannableDeviceService connectorService) {
		RunnableDeviceServiceImpl.deviceConnectorService = connectorService;
	}
	
	private BundleContext context;

	public void start(BundleContext context) {
		this.context = context;
	}
	
	public void stop() {
		this.context = null;
	}
	
    /**
     * Try to get the connector service or throw an exception
     * @return
     */
	private IScannableDeviceService getDeviceConnector() throws ScanningException {
		ServiceReference<IScannableDeviceService> ref = context.getServiceReference(IScannableDeviceService.class);
		return context.getService(ref);
	}

	public static IMalcolmService getMalcolmService() {
		return malcolmService;
	}

	public static void setMalcolmService(IMalcolmService malcolmService) {
		RunnableDeviceServiceImpl.malcolmService = malcolmService;
	}

	public static String getDefaultMalcolmHostname() {
		return defaultMalcolmHostname;
	}

	public static void setDefaultMalcolmHostname(String defaultMalcolmHostname) {
		RunnableDeviceServiceImpl.defaultMalcolmHostname = defaultMalcolmHostname;
	}

	/**
	 * Used for testing only
	 * @param model
	 * @param device
	 */
	public void _register(Class<?> model, Class<? extends IRunnableDevice> device) {
		modelledDevices.put(model, device);
	}
	
	/**
	 * Used for testing only
	 * @param model
	 * @param device
	 */
	public void _register(String name, IRunnableDevice<?> device) {
		namedDevices.put(name, device);
	}

	@Override
	public Collection<String> getRunnableDeviceNames() throws ScanningException {
		return namedDevices.keySet();
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation() throws ScanningException {
		return getDeviceInformation(false);
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformationIncludingNonAlive() throws ScanningException {
		return getDeviceInformation(true);
	}

	private Collection<DeviceInformation<?>> getDeviceInformation(boolean getNonAliveDeviceInformation) throws ScanningException {
		
		Collection<DeviceInformation<?>> ret = new ArrayList<>();
		final Collection<String> names = getRunnableDeviceNames();
		for (String name : names) {
			try {
				if (name==null) continue;
	
				IRunnableDevice<Object> device = getRunnableDevice(name);
				if (device==null)  continue;		
				if (!(device instanceof AbstractRunnableDevice)) continue;
				DeviceInformation<?> info;
				if (getNonAliveDeviceInformation) {
					info = ((AbstractRunnableDevice<?>)device).getDeviceInformationIncludeNonAlive(true);
				} else {
					info = ((AbstractRunnableDevice<?>)device).getDeviceInformation();
				}
				
				ret.add(info);
			} catch (Exception ex) {
				logger.warn("Error getting device info for : " + name);
			}
		}
		return ret;
	}
	
	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation(final DeviceRole role) throws ScanningException {
		Collection<DeviceInformation<?>> infos = getDeviceInformation();
		return infos.stream().filter(info -> info.getDeviceRole()==role).collect(Collectors.toList());
	}
	
	@Override
	public DeviceInformation<?> getDeviceInformation(String name) throws ScanningException {
		IRunnableDevice<Object> device = getRunnableDevice(name);
		if (device==null)  return null;		
		if (!(device instanceof AbstractRunnableDevice)) return null;
		return ((AbstractRunnableDevice<?>)device).getDeviceInformation();
	}

	private Collection<Object> participants;
	
	@Override
	public void addScanParticipant(Object device) {
		if (participants==null) participants = Collections.synchronizedSet(new LinkedHashSet<>(7));
		participants.add(device);
	}

	@Override
	public void removeScanParticipant(Object device) {
		participants.remove(device);
	}

	@Override
	public Collection<Object> getScanParticipants() {
		return participants; // May be null
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> IRunnableDevice<T> getActiveScanner() {
		return (IRunnableDevice<T>)RunnableDeviceServiceImpl.currentScanningDevice; // Package private method. Do not use globally!
	}

	/**
	 * Package private, think before stopping this.
	 * @param currentScanningDevice
	 */
	static void setCurrentScanningDevice(IRunnableDevice<?> currentScanningDevice) {
		RunnableDeviceServiceImpl.currentScanningDevice = currentScanningDevice;
	}

}
