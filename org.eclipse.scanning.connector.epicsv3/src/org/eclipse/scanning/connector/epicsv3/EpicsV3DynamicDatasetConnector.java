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
package org.eclipse.scanning.connector.epicsv3;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DataEvent;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataListener;
import org.eclipse.january.dataset.IDatasetChangeChecker;
import org.eclipse.january.dataset.IDatasetConnector;
import org.eclipse.january.dataset.ILazyDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosylab.epics.caj.CAJChannel;

import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_Short;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Connects to a remote Epics V3 Array, and uses the data to populate a dataset which dynamically changes
 * whenever the Epics data changes. 
 * @author Matt Taylor
 *
 */
public class EpicsV3DynamicDatasetConnector implements IDatasetConnector {

	private final String arrayDataSuffix = ":ArrayData";
	private final String arraySize0Suffix = ":ArraySize0_RBV";
	private final String arraySize1Suffix = ":ArraySize1_RBV";
	private final String arraySize2Suffix = ":ArraySize2_RBV";
	private final String numDimensionsSuffix = ":NDimensions_RBV";
	private final String colourModeSuffix = ":ColorMode_RBV";
	private final String dataTypeSuffix = ":DataType_RBV";
	
	private final String monoColourMode = "Mono";
	private final String uint8DataType = "UInt8";
	
	private final long frameLimitTimeDifference = 1000 / 20; // 20 FPS
	
	EpicsV3Communicator ec = EpicsV3Communicator.getInstance();
	
	Channel dataChannel = null;
	Channel dim0Ch = null;
	Channel dim1Ch = null;
	Channel dim2Ch = null;
	Channel numDimCh = null;
	Channel colourModeCh = null;
	Channel dataTypeCh = null;
	
	Monitor dataChannelMonitor = null;
	EpicsMonitorListener dataChannelMonitorListener = null;

	LinkedList<IDataListener> listeners = new LinkedList<>();
	
	int height = 0;
	int width = 0;
	int rgbChannels = 0;
	int numDimensions = 0;
	String colourMode = "";
	String dataTypeStr = "";
	
	String arrayPluginName;
	String dataChannelPV = "";
	String dim1PV = "";
	String dim0PV = "";
	String dim2PV = "";
	String numDimensionsPV = "";
	String colourModePV = "";
	String dataTypePV = "";
	
	String currentWidthPV = "";
	String currentHeightPV = "";
	
	DBRType dataType = null;
	
	long lastSystemTime = System.currentTimeMillis();

	private ILazyDataset dataset;
	
	private static final Logger logger = LoggerFactory.getLogger(EpicsV3DynamicDatasetConnector.class);
	
	/**
	 * Constructor, takes the name of the base plugin name
	 * @param arrayPluginName The name of the 'parent' PV endpoint
	 */
	public EpicsV3DynamicDatasetConnector(String arrayPluginName) {
		this.arrayPluginName = arrayPluginName;
		dataChannelPV = arrayPluginName + arrayDataSuffix;      
		dim1PV = arrayPluginName + arraySize1Suffix;            
		dim0PV = arrayPluginName + arraySize0Suffix;            
		dim2PV = arrayPluginName + arraySize2Suffix;            
		numDimensionsPV = arrayPluginName + numDimensionsSuffix;
		colourModePV = arrayPluginName + colourModeSuffix;      
		dataTypePV = arrayPluginName + dataTypeSuffix;          
	}	

	@Override
	public String getPath() {
		// Not applicable
		return null;
	}

	@Override
	public void setPath(String path) {
		// Not applicable
	}

	@Override
	public int[] getMaxShape() {
		// TODO applicable?
		return null;
	}

	@Override
	public void setMaxShape(int... maxShape) {
		// TODO applicable?
	}

	@Override
	public void startUpdateChecker(int milliseconds, IDatasetChangeChecker checker) {
		// TODO applicable?
	}

	@Override
	public void addDataListener(IDataListener l) {
		listeners.add(l);
	}

	@Override
	public void removeDataListener(IDataListener l) {
		listeners.remove(l);
	}

	@Override
	public void fireDataListeners() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getDatasetName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDatasetName(String datasetName) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setWritingExpected(boolean expectWrite) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isWritingExpected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String connect() throws DatasetException {
		return connect(500, TimeUnit.MILLISECONDS);
	}

	@Override
	public String connect(long time, TimeUnit unit) throws DatasetException {
		try {
			dataChannel = ec.createChannel(dataChannelPV);
			dim0Ch = ec.createChannel(dim0PV);
			dim1Ch = ec.createChannel(dim1PV);
			dim2Ch = ec.createChannel(dim2PV);
			numDimCh = ec.createChannel(numDimensionsPV);
			colourModeCh = ec.createChannel(colourModePV);
			dataTypeCh = ec.createChannel(dataTypePV);
			
			dataType = dataChannel.getFieldType();
			
			numDimensions = ec.cagetInt(numDimCh);

			colourMode = ec.cagetString(colourModeCh);

			dataTypeStr = ec.cagetString(dataTypeCh);
			
			int dataSize = 0;
			
			if (colourMode.equals(monoColourMode)) {
				width = ec.cagetInt(dim0Ch);
				height = ec.cagetInt(dim1Ch);
				
				currentWidthPV = dim0PV;
				currentHeightPV = dim1PV;
				
				dataSize = height * width;
			} else {
				if (colourMode.equals("RGB2")) {
					rgbChannels = ec.cagetInt(dim1Ch);
					width = ec.cagetInt(dim0Ch);
					height = ec.cagetInt(dim2Ch);
					currentWidthPV = dim0PV;
					currentHeightPV = dim2PV;
				} else if (colourMode.equals("RGB3")) {
					rgbChannels = ec.cagetInt(dim2Ch);
					width = ec.cagetInt(dim0Ch);
					height = ec.cagetInt(dim1Ch);
					currentWidthPV = dim0PV;
					currentHeightPV = dim1PV;
				} else {
					rgbChannels = ec.cagetInt(dim0Ch);
					width = ec.cagetInt(dim1Ch);
					height = ec.cagetInt(dim2Ch);
					currentWidthPV = dim1PV;
					currentHeightPV = dim2PV;
				} 
				
				dataSize = height * width * rgbChannels;
			}

			DBR dbr = dataChannel.get(dataType, dataSize);
			
			if (dataType.equals(DBRType.BYTE)) {
				ec.cagetByteArray(dataChannel); // Without doing this, the dbr isn't populated with the actual data
				handleByte(dbr);
			} else if (dataType.equals(DBRType.SHORT)) {
				ec.cagetShortArray(dataChannel); // Without doing this, the dbr isn't populated with the actual data
				handleShort(dbr);
			} else if (dataType.equals(DBRType.INT)) {
				ec.cagetIntArray(dataChannel); // Without doing this, the dbr isn't populated with the actual data
				handleInt(dbr);
			} else if (dataType.equals(DBRType.FLOAT)) {
				ec.cagetFloatArray(dataChannel); // Without doing this, the dbr isn't populated with the actual data
				handleFloat(dbr);
			} else if (dataType.equals(DBRType.DOUBLE)) {
				ec.cagetDoubleArray(dataChannel); // Without doing this, the dbr isn't populated with the actual data
				handleDouble(dbr);
			} else {
				logger.error("Unknown DBRType - " + dataType);
			}
			
			dataChannelMonitorListener = new EpicsMonitorListener();
			dataChannelMonitor = ec.setMonitor(dataChannel, dataChannelMonitorListener, dataSize);
			ec.setMonitor(dim0Ch, new EpicsMonitorListener());
			ec.setMonitor(dim1Ch, new EpicsMonitorListener());
			ec.setMonitor(dim2Ch, new EpicsMonitorListener());
				
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new DatasetException(e.getMessage());
		}
		return null;
	}

	@Override
	public void disconnect() throws DatasetException {
		ec.destroy(dataChannel);
		ec.destroy(dim0Ch);
		ec.destroy(dim1Ch);
	}

	@Override
	public ILazyDataset getDataset() {
		return dataset;
	}

	@Override
	public boolean resize(int... newShape) {
		// TODO ?
		return false;
	}

	@Override
	public boolean refreshShape() {
		// TODO ?
		return false;
	}
	
	/**
	 * Handles a byte DBR, updating the dataset with the data from the DBR
	 * @param dbr
	 */
	private void handleByte(DBR dbr) {

		DBR_Byte dbrb = (DBR_Byte)dbr;
		byte[] rawData = dbrb.getByteValue();
		short[] latestData = new short[rawData.length];
		
		if (dataTypeStr.equalsIgnoreCase(uint8DataType)) {
			for (int i = 0; i < rawData.length; i++) {
				latestData[i] = (short)(rawData[i] & 0xFF);
			}
		} else {
			for (int i = 0; i < rawData.length; i++) {
				latestData[i] = rawData[i];
			}
		}
		
		if (latestData != null) {

			if (numDimensions == 2) {
				int dataSize = height * width;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
				int dataSize = height * width * rgbChannels;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(Dataset.RGB, latestData, new int[]{height, width});
			}
		}
	}
	
	/**
	 * Handles a short DBR, updating the dataset with the data from the DBR
	 * @param dbr
	 */
	private void handleShort(DBR dbr) {

		DBR_Short dbrb = (DBR_Short)dbr;
		short[] latestData = Arrays.copyOf(dbrb.getShortValue(), dbrb.getShortValue().length);
		
		if (latestData != null) {
			

			if (numDimensions == 2) {
				int dataSize = height * width;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
				int dataSize = height * width * rgbChannels;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(Dataset.RGB, latestData, new int[]{height, width});
			}
		}
	}
	
	/**
	 * Handles an int DBR, updating the dataset with the data from the DBR
	 * @param dbr
	 */
	private void handleInt(DBR dbr) {

		DBR_Int dbrb = (DBR_Int)dbr;
		int[] latestData = Arrays.copyOf(dbrb.getIntValue(), dbrb.getIntValue().length);
		
		if (latestData != null) {

			if (numDimensions == 2) {
				int dataSize = height * width;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
				int dataSize = height * width * rgbChannels;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(Dataset.RGB, latestData, new int[]{height, width});
			}
		}
	}
	
	/**
	 * Handles a float DBR, updating the dataset with the data from the DBR
	 * @param dbr
	 */
	private void handleFloat(DBR dbr) {

		DBR_Float dbrb = (DBR_Float)dbr;
		float[] latestData = Arrays.copyOf(dbrb.getFloatValue(), dbrb.getFloatValue().length);
		
		if (latestData != null) {

			if (numDimensions == 2) {
				int dataSize = height * width;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
				int dataSize = height * width * rgbChannels;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(Dataset.RGB, latestData, new int[]{height, width});
			}
		}
	}
	
	/**
	 * Handles a double DBR, updating the dataset with the data from the DBR
	 * @param dbr
	 */
	private void handleDouble(DBR dbr) {

		DBR_Double dbrb = (DBR_Double)dbr;
		double[] latestData = Arrays.copyOf(dbrb.getDoubleValue(), dbrb.getDoubleValue().length);
		
		if (latestData != null) {

			if (numDimensions == 2) {
				int dataSize = height * width;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size"); 
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
				int dataSize = height * width * rgbChannels;
				if (latestData.length != dataSize) {
					if (dataSize > latestData.length) {
						logger.warn("Warning: Image size is larger than data array size");
					}
					latestData = Arrays.copyOf(latestData, dataSize);
				}
				dataset = DatasetFactory.createFromObject(Dataset.RGB, latestData, new int[]{height, width});
			}
		}
	}
	
	/**
	 * Private class used to perform actions based on events sent from the Epics PVs	 *
	 */
	private class EpicsMonitorListener implements MonitorListener {

		@Override
		public void monitorChanged(MonitorEvent arg0) {
			try {
				
				Object source = arg0.getSource();
				
				if (source instanceof CAJChannel) {
					CAJChannel chan = (CAJChannel) source;
					String channelName = chan.getName();
					
					if (channelName.equalsIgnoreCase(dataChannelPV)) {
						DBR dbr = arg0.getDBR();
						
						if (dataType.equals(DBRType.BYTE)) {
							handleByte(dbr);
						} else if (dataType.equals(DBRType.SHORT)) {
							handleShort(dbr);
						} else if (dataType.equals(DBRType.INT)) {
							handleInt(dbr);
						} else if (dataType.equals(DBRType.FLOAT)) {
							handleFloat(dbr);
						} else if (dataType.equals(DBRType.DOUBLE)) {
							handleDouble(dbr);
						} else {
							logger.error("Unknown DBRType - " + dataType);
						}
						
						// Only notify of data update at certain FPS
						long timeNow = System.currentTimeMillis();
						if (timeNow - lastSystemTime > frameLimitTimeDifference) {
							for (IDataListener listener : listeners) {
								int[] shape = new int[]{height, width};
								DataEvent evt = new DataEvent("", shape);
								listener.dataChangePerformed(evt);
							}
							lastSystemTime = timeNow;
						}
						
					} else if (channelName.equalsIgnoreCase(currentWidthPV)) {
						DBR dbr = arg0.getDBR();
						DBR_Int dbri = (DBR_Int)dbr;
						if (width != dbri.getIntValue()[0]) {
							width = dbri.getIntValue()[0];
							dataChannelMonitor.removeMonitorListener(dataChannelMonitorListener);
							int dataSize = height * width;
							if (numDimensions == 3) {
								dataSize *= rgbChannels;
							}
							dataChannelMonitor = ec.setMonitor(dataChannel, dataChannelMonitorListener, dataSize);
						}
					} else if (channelName.equalsIgnoreCase(currentHeightPV)) {
						DBR dbr = arg0.getDBR();
						DBR_Int dbri = (DBR_Int)dbr;
						if (height != dbri.getIntValue()[0]) {
							height = dbri.getIntValue()[0];
							dataChannelMonitor.removeMonitorListener(dataChannelMonitorListener);
							int dataSize = height * width;
							if (numDimensions == 3) {
								dataSize *= rgbChannels;
							}
							dataChannelMonitor = ec.setMonitor(dataChannel, dataChannelMonitorListener, dataSize);
						}
					}
				}
				
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
		
	}
}
