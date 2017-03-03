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
package org.eclipse.scanning.connector.epics;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.connector.MessageGenerator;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;
import org.eclipse.scanning.api.malcolm.event.MalcolmEvent;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.malcolm.message.Type;
import org.epics.pvaClient.PvaClient;
import org.epics.pvaClient.PvaClientChannel;
import org.epics.pvaClient.PvaClientChannelStateChangeRequester;
import org.epics.pvaClient.PvaClientGet;
import org.epics.pvaClient.PvaClientGetData;
import org.epics.pvaClient.PvaClientMonitor;
import org.epics.pvaClient.PvaClientMonitorData;
import org.epics.pvaClient.PvaClientMonitorRequester;
import org.epics.pvaClient.PvaClientPut;
import org.epics.pvaClient.PvaClientPutData;
import org.epics.pvaClient.PvaClientRPC;
import org.epics.pvaClient.PvaClientUnlistenRequester;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class uses EpicsV4 class to connect to an Epics V4 endpoint.
 * It provides the ability to get a pv, set a pv, call a method, and subsribe and unsubscribe to a pv.
 * 
 * @author Matt Taylor
 *
 */
public class EpicsV4ConnectorService implements IMalcolmConnectorService<MalcolmMessage> {

	static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    static final double REQUEST_TIMEOUT = 1.0;
	
	private static final Logger logger = LoggerFactory.getLogger(EpicsV4ConnectorService.class);
	
	private EpicsV4MessageMapper mapper;
	
	private PvaClient pvaClient;
    
    private Map<Long, Collection<EpicsV4MonitorListener>> listeners;
    
    public EpicsV4ConnectorService() {
		mapper = new EpicsV4MessageMapper();
		this.listeners = new Hashtable<Long, Collection<EpicsV4MonitorListener>>(7);
		pvaClient = PvaClient.get("pva"); // Should this be "pva" or the no-argument one?
	}
    
	@Override
	public void connect(URI malcolmUri) throws MalcolmDeviceException {
		// don't need uri as no centralised connection is needed for Malcolm Devices
	}

	@Override
	public void disconnect() throws MalcolmDeviceException {
        //pvaClient.destroy();
 	}
	
	public PVStructure pvMarshal(Object anyObject) throws Exception {
		return mapper.pvMarshal(anyObject);
	}

	public <U> U pvUnmarshal(PVStructure anyObject, Class<U> beanClass) throws Exception {
		return mapper.pvUnmarshal(anyObject, beanClass);
	}

	@Override
	public MalcolmMessage send(IMalcolmDevice<?> device, MalcolmMessage message) throws MalcolmDeviceException {
		
		MalcolmMessage result = new MalcolmMessage();
				
		try {

			switch (message.getType()) {
			case CALL:
				result = sendCallMessage(device, message);
				break;
			case GET:
				result = sendGetMessage(device, message);
				break;
			case PUT:
				result = sendPutMessage(device, message);
				break;
			default:
				throw new Exception("Unexpected MalcolmMessage type: " + message.getType());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setEndpoint(message.getEndpoint());
			result.setId(message.getId());
			result.setMessage("Error sending message " + message.getEndpoint() + ": " + e.getMessage());
			result.setType(Type.ERROR);
		}
		return result;
	}

	@Override
	public void subscribe(IMalcolmDevice<?> device, MalcolmMessage msg, IMalcolmListener<MalcolmMessage> listener)
			throws MalcolmDeviceException {

		try {
			EpicsV4ClientMonitorRequester monitorRequester = new EpicsV4ClientMonitorRequester(listener, msg);
			PvaClientChannel pvaChannel = pvaClient.createChannel(device.getName(),"pva");
	        pvaChannel.issueConnect();
	        Status status = pvaChannel.waitConnect(REQUEST_TIMEOUT);
	        if(!status.isOK()) {
	        	String errMEssage = "Failed to connect to device '" + device.getName() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	logger.error(errMEssage);
	        	throw new Exception(errMEssage);
	        }
			
	        PvaClientMonitor monitor = pvaChannel.monitor(msg.getEndpoint(),monitorRequester,monitorRequester);
	        
	        Collection<EpicsV4MonitorListener> ls = listeners.get(msg.getId());
			if (ls == null) {
				ls = new Vector<EpicsV4MonitorListener>(3);
				listeners.put(msg.getId(), ls);
			}
			
			EpicsV4MonitorListener monitorListener = new EpicsV4MonitorListener(listener, monitor);
			ls.add(monitorListener);
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex.getMessage());
			throw new MalcolmDeviceException(device, ex.getMessage());
		}
	}

	@Override
	public void subscribeToConnectionStateChange(IMalcolmDevice<?> device, IMalcolmListener<Boolean> listener)
			throws MalcolmDeviceException  {

		try {
			PvaClientChannel pvaChannel = pvaClient.createChannel(device.getName(),"pva");
	        pvaChannel.issueConnect();
	        Status status = pvaChannel.waitConnect(0); // Wait forever for this connection.
	        if(!status.isOK()) {
	        	String errMEssage = "Failed to connect to device '" + device.getName() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	logger.error(errMEssage);
	        	throw new Exception(errMEssage);
	        }
	        pvaChannel.setStateChangeRequester(new StateChangeRequester(listener));
	        
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex.getMessage());
			throw new MalcolmDeviceException(device, ex.getMessage());
		}
	}

	@Override
	public MalcolmMessage unsubscribe(IMalcolmDevice<?> device, MalcolmMessage msg, IMalcolmListener<MalcolmMessage>... removeListeners)
			throws MalcolmDeviceException {
		
		MalcolmMessage result = new MalcolmMessage();
		result.setType(Type.RETURN);
		result.setId(msg.getId());
		
		try {
			if (removeListeners==null) { // Kill every subscriber
				
				for (EpicsV4MonitorListener monitorListener : listeners.get(msg.getId()))
				{
					monitorListener.getMonitor().stop();
				}
				listeners.remove(msg.getId());
			} else {
				Collection<EpicsV4MonitorListener> ls = listeners.get(msg.getId());
				if (ls!=null) {
					
					ArrayList<EpicsV4MonitorListener> toRemove = new ArrayList<EpicsV4MonitorListener>();
					
					for (EpicsV4MonitorListener monitorListener : ls)
					{
						if (Arrays.asList(removeListeners).contains(monitorListener.getMalcolmListener()))
						{
							toRemove.add(monitorListener);
							monitorListener.getMonitor().stop();
						}
					}
					
					ls.removeAll(toRemove);
				}
			}
		
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			result.setMessage("Error unsubscribing from message Id " + msg.getId() + ": " + e.getMessage());
			result.setType(Type.ERROR);
			throw new MalcolmDeviceException(device, result.getMessage());
		}
		return result;
	}
	
	private MalcolmMessage sendGetMessage(IMalcolmDevice<?> device, MalcolmMessage message) {

		MalcolmMessage returnMessage = new MalcolmMessage();
		PvaClientChannel pvaChannel = null;
		try {
			PVStructure pvResult = null;
			pvaChannel = pvaClient.createChannel(device.getName(),"pva");
	        pvaChannel.issueConnect();
	        Status status = pvaChannel.waitConnect(REQUEST_TIMEOUT);
	        if(!status.isOK()) {
	        	String errMEssage = "Failed to connect to device '" + device.getName() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	        }
			
			String requestString = message.getEndpoint();
			logger.debug("Get '" + requestString + "'");
	        PvaClientGet pvaGet = pvaChannel.createGet(requestString);
	        pvaGet.issueConnect();
	        status = pvaGet.waitConnect();
	        if(!status.isOK()) {
	        	String errMEssage = "CreateGet failed for '" + requestString + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	    	}
	        PvaClientGetData pvaData = pvaGet.getData();
			pvResult = pvaData.getPVStructure();
			logger.debug("Get response = \n" + pvResult + "\nEND");
	        returnMessage = mapper.convertGetPVStructureToMalcolmMessage(pvResult, message);
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			returnMessage.setType(Type.ERROR);
			returnMessage.setMessage(ex.getMessage());
		}
		
		if (pvaChannel != null) {
			pvaChannel.destroy();
		}
		
        return returnMessage;
	}
	
	private MalcolmMessage sendPutMessage(IMalcolmDevice<?> device, MalcolmMessage message) {
		
		MalcolmMessage returnMessage = new MalcolmMessage();
        returnMessage.setType(Type.RETURN);
        returnMessage.setId(message.getId());
        
        if (message.getValue() == null) {
			returnMessage.setType(Type.ERROR);
			returnMessage.setMessage("Unable to set field value to null: " + message.getEndpoint());
        }

		PvaClientChannel pvaChannel = null;
		
		try {
			String requestString = message.getEndpoint();
			
			pvaChannel = pvaClient.createChannel(device.getName(),"pva");
			pvaChannel.issueConnect();
	        Status status = pvaChannel.waitConnect(REQUEST_TIMEOUT);
	        if(!status.isOK()) {
	        	String errMEssage = "Failed to connect to device '" + device.getName() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	        }
	        PvaClientPut pvaPut = pvaChannel.createPut(requestString);
	        pvaPut.issueConnect();
	        status = pvaPut.waitConnect();
	        if(!status.isOK()) {
	        	String errMEssage = "CreatePut failed for '" + requestString + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	    	}
	        PvaClientPutData putData = pvaPut.getData();
	        PVStructure pvStructure = putData.getPVStructure();
	        
	        mapper.populatePutPVStructure(pvStructure, message);
	        
	        pvaPut.put();
        
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			ex.printStackTrace();
			returnMessage.setType(Type.ERROR);
			returnMessage.setMessage("Error putting value into field " + message.getEndpoint() + ": " + ex.getMessage());
		}
		
		if (pvaChannel != null) {
			pvaChannel.destroy();
		}
        
        return returnMessage;
	}
	
	private MalcolmMessage sendCallMessage(IMalcolmDevice<?> device, MalcolmMessage message) {
		
		MalcolmMessage returnMessage = new MalcolmMessage();
		PvaClientChannel pvaChannel = null;
		
		try {
			PVStructure pvResult = null;
			PVStructure pvRequest = mapper.convertMalcolmMessageToPVStructure(message);

			// Mapper outputs two nested structures, one for the method, one for the parameters 
			PVStructure methodStructure = pvRequest.getStructureField("method");
			PVStructure parametersStructure = pvRequest.getStructureField("parameters");
			
			pvaChannel = pvaClient.createChannel(device.getName(),"pva");
			pvaChannel.issueConnect();
	        Status status = pvaChannel.waitConnect(REQUEST_TIMEOUT);
	        if(!status.isOK()) {
	        	String errMEssage = "Failed to connect to device '" + device.getName() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	        }

			logger.debug("Call method = \n" + methodStructure + "\nEND");
	        PvaClientRPC rpc = pvaChannel.createRPC(methodStructure);
	        rpc.issueConnect();
	        status = rpc.waitConnect();
	        if(!status.isOK()) {
	        	String errMEssage = "CreateRPC failed for '" + message.getMethod() + "' (" + status.getType() + ": " + status.getMessage() + ")";
	        	throw new Exception(errMEssage);
	    	}
			logger.debug("Call param = \n" + parametersStructure + "\nEND");
	        pvResult = rpc.request(parametersStructure);
			logger.debug("Call response = \n" + pvResult + "\nEND");
			returnMessage = mapper.convertCallPVStructureToMalcolmMessage(pvResult, message);
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			ex.printStackTrace();
			returnMessage.setType(Type.ERROR);
			returnMessage.setMessage(ex.getMessage());
		}
		
		if (pvaChannel != null) {
			pvaChannel.destroy();
		}
		
        return returnMessage;		
	}
	
	public MessageGenerator<MalcolmMessage> createDeviceConnection(IMalcolmDevice<?> device) throws MalcolmDeviceException {
		return (MessageGenerator<MalcolmMessage>) new EpicsV4MalcolmMessageGenerator(device, this);
	}

	@Override
	public MessageGenerator<MalcolmMessage> createConnection() {
		return (MessageGenerator<MalcolmMessage>) new EpicsV4MalcolmMessageGenerator(this);
	}
		
	class EpicsV4ClientMonitorRequester implements PvaClientMonitorRequester, PvaClientUnlistenRequester {
		private IMalcolmListener<MalcolmMessage> listener;
		private MalcolmMessage subscribeMessage;
		
		public EpicsV4ClientMonitorRequester(IMalcolmListener<MalcolmMessage> listener, MalcolmMessage subscribeMessage) {
			this.listener = listener;
			this.subscribeMessage = subscribeMessage;
		}

		@Override
		public void event(PvaClientMonitor monitor) {
			while (monitor.poll()) {
				PvaClientMonitorData monitorData = monitor.getData();
				
				MalcolmMessage message = new MalcolmMessage();
				try {
					message = mapper.convertSubscribeUpdatePVStructureToMalcolmMessage(monitorData.getPVStructure(), subscribeMessage);
				} catch (Exception ex) {
					logger.error(ex.getMessage());
					message.setType(Type.ERROR);
					message.setMessage("Error converting subscription update: " + ex.getMessage());
				}
				listener.eventPerformed(new MalcolmEvent<MalcolmMessage>(message));
				monitor.releaseEvent();
			}
		}

		@Override
		public void unlisten(PvaClientMonitor arg0) {
			// TODO What to do when unlisten is called?
		}
	}

	class StateChangeRequester implements PvaClientChannelStateChangeRequester
    {
		private IMalcolmListener<Boolean> listener;

		public StateChangeRequester(IMalcolmListener<Boolean> listener) {
			this.listener = listener;
		}
		
		@Override
		public void channelStateChange(PvaClientChannel channel, boolean isConnected) {
			listener.eventPerformed(new MalcolmEvent<Boolean>(isConnected));
		}
    }
}
