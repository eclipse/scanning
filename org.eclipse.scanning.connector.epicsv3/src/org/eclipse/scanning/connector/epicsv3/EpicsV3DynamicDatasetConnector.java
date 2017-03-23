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

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_Short;
import gov.aps.jca.dbr.DBR_String;
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
	
	int dim0,
		dim1,
		dim2;

	int height = 0;
	int width = 0;
	int rgbChannels = 0;

	int numDimensions = 0;
	String colourMode = "";
	String dataTypeStr = "";
	
	String arrayPluginName;
	String dataChannelPV = "";
	String dim0PV = "";
	String dim1PV = "";
	String dim2PV = "";
	String numDimensionsPV = "";
	String colourModePV = "";
	String dataTypePV = "";
	
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
		dim0PV = arrayPluginName + arraySize0Suffix;            
		dim1PV = arrayPluginName + arraySize1Suffix;            
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
			dim0 = ec.cagetInt(dim0Ch);
			dim1 = ec.cagetInt(dim1Ch);
			dim2 = ec.cagetInt(dim2Ch);
			numDimensions = ec.cagetInt(numDimCh);
			colourMode = ec.cagetString(colourModeCh);
			dataTypeStr = ec.cagetString(dataTypeCh);
			
			int dataSize = calculateAndUpdateDataSize();
			// Without specifying data size in cagets, they will always try to get max data size, which could be >> actual data, causing timeouts.

			DBR dbr = dataChannel.get(dataType, dataSize);
			
			if (dataType.equals(DBRType.BYTE)) {
				ec.cagetByteArray(dataChannel, dataSize); // Without doing this, the dbr isn't populated with the actual data
				handleByte(dbr);
			} else if (dataType.equals(DBRType.SHORT)) {
				ec.cagetShortArray(dataChannel, dataSize); // Without doing this, the dbr isn't populated with the actual data
				handleShort(dbr);
			} else if (dataType.equals(DBRType.INT)) {
				ec.cagetIntArray(dataChannel, dataSize); // Without doing this, the dbr isn't populated with the actual data
				handleInt(dbr);
			} else if (dataType.equals(DBRType.FLOAT)) {
				ec.cagetFloatArray(dataChannel, dataSize); // Without doing this, the dbr isn't populated with the actual data
				handleFloat(dbr);
			} else if (dataType.equals(DBRType.DOUBLE)) {
				ec.cagetDoubleArray(dataChannel, dataSize); // Without doing this, the dbr isn't populated with the actual data
				handleDouble(dbr);
			} else {
				logger.error("Unknown DBRType - " + dataType);
			}
			
			dataChannelMonitorListener = new EpicsMonitorListener();
			dataChannelMonitor = ec.setMonitor(dataChannel, dataChannelMonitorListener, dataSize);
			ec.setMonitor(dim0Ch, new EpicsMonitorListener());
			ec.setMonitor(dim1Ch, new EpicsMonitorListener());
			ec.setMonitor(dim2Ch, new EpicsMonitorListener());
			ec.setMonitor(colourModeCh, new EpicsMonitorListener());
			ec.setMonitor(numDimCh, new EpicsMonitorListener());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new DatasetException(e.getMessage());
		}
		return null;
	}

	@Override
	public void disconnect() throws DatasetException {
		if (   null != dataChannel) {
			ec.destroy(dataChannel);
		}
		if (   null != dim0Ch)	{
			ec.destroy(dim0Ch);
		}
		if (   null != dim1Ch) {
			ec.destroy(dim1Ch);
		}
		if (   null != dim2Ch) {
			ec.destroy(dim2Ch);
		}
		if (   null != colourModeCh) {
			ec.destroy(colourModeCh);
		}
		if (null != numDimCh) {
			ec.destroy(numDimCh);
		}
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
	
	private int calculateAndUpdateDataSize() {
		String currentWidthPV;
		String currentHeightPV;
		String rgbChannelsPV;

		if (colourMode.equals(monoColourMode)) {

			width = dim0;
			height = dim1;
			rgbChannels = 1;

			currentWidthPV = dim0PV;
			currentHeightPV = dim1PV;
			rgbChannelsPV = dim2PV;

		} else {
			if (colourMode.equals("RGB2")) {

				width = dim0;
				height = dim2;
				rgbChannels = dim1;

				currentWidthPV = dim0PV;
				currentHeightPV = dim2PV;
				rgbChannelsPV = dim1PV;

			} else if (colourMode.equals("RGB3")) {

				width = dim0;
				height = dim1;
				rgbChannels = dim2;

				currentWidthPV = dim0PV;
				currentHeightPV = dim1PV;
				rgbChannelsPV = dim2PV;

			} else {

				width = dim1;
				height = dim2;
				rgbChannels = dim0;

				currentWidthPV = dim1PV;
				currentHeightPV = dim2PV;
				rgbChannelsPV = dim0PV;
			} 
		}
		// Ensure that we never return a data size less than 1.

		// EPICS takes a data size of 0 as maximum possible size, which causes it to ask for more data than is available. 

		if (width <= 1) {
			logger.warn("Image width {} from {}, assuming a width of at least 1. Check that plugin is receiving images.", width, currentWidthPV);
			width = 1;
		}
		if (height <= 1) {
			logger.warn("Image height {} from {}, assuming a height of at least 1. Check that plugin is receiving images.", height, currentHeightPV);
			height = 1;
		}
		if (rgbChannels < 1) {
			logger.warn("Image channels {} from {}, assuming a at least 1 channel. Check that plugin is receiving images.", rgbChannels, rgbChannelsPV);
			rgbChannels = 1;
		}
		return height * width * rgbChannels;
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
			int dataSize = calculateAndUpdateDataSize();

			if (latestData.length != dataSize) {
				if (dataSize > latestData.length) {
					logger.warn("Warning: Image size is larger than data array size");
				}
				latestData = Arrays.copyOf(latestData, dataSize);
			}
			if (numDimensions == 2) {
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
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
			int dataSize = calculateAndUpdateDataSize();

			if (latestData.length != dataSize) {
				if (dataSize > latestData.length) {
					logger.warn("Warning: Image size is larger than data array size");
				}
				latestData = Arrays.copyOf(latestData, dataSize);
			}
			if (numDimensions == 2) {
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
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
			int dataSize = calculateAndUpdateDataSize();

			if (latestData.length != dataSize) {
				if (dataSize > latestData.length) {
					logger.warn("Warning: Image size is larger than data array size");
				}
				latestData = Arrays.copyOf(latestData, dataSize);
			}
			if (numDimensions == 2) {
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
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
			int dataSize = calculateAndUpdateDataSize();

			if (latestData.length != dataSize) {
				if (dataSize > latestData.length) {
					logger.warn("Warning: Image size is larger than data array size");
				}
				latestData = Arrays.copyOf(latestData, dataSize);
			}
			if (numDimensions == 2) {
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
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
			int dataSize = calculateAndUpdateDataSize();

			if (latestData.length != dataSize) {
				if (dataSize > latestData.length) {
					logger.warn("Warning: Image size is larger than data array size");
				}
				latestData = Arrays.copyOf(latestData, dataSize);
			}
			if (numDimensions == 2) {
				dataset = DatasetFactory.createFromObject(latestData, new int[]{height, width});
			} else {
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
					DBR dbr = arg0.getDBR();
					
					if (channelName.equalsIgnoreCase(dataChannelPV)) {
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
					} else if (channelName.equalsIgnoreCase(dim0PV)) {
						DBR_Int dbri = (DBR_Int)dbr;
						int value = dbri.getIntValue()[0];
						if (dim0 != value) {
							dim0 = value;
							logger.debug("New dim0PV value {} for {}", value, dim0PV);
							updateDataChannelMonitor();
						}
					} else if (channelName.equalsIgnoreCase(dim1PV)) {
						DBR_Int dbri = (DBR_Int)dbr;
						int value = dbri.getIntValue()[0];
						if (dim1 != value) {
							dim1 = value;
							logger.debug("New dim1PV value {} for {}", value, dim1PV);
							updateDataChannelMonitor();
						}
					} else if (channelName.equalsIgnoreCase(dim2PV)) {
						DBR_Int dbri = (DBR_Int)dbr;
						int value = dbri.getIntValue()[0];
						if (dim2 != value) {
							dim2 = value;
							logger.debug("New dim2PV value {} for {}", value, dim2PV);
							updateDataChannelMonitor();
						}
					} else if (channelName.equalsIgnoreCase(numDimensionsPV)) {
						DBR_Int dbri = (DBR_Int)dbr;
						int value = dbri.getIntValue()[0];
						if (numDimensions != value) {
							numDimensions = value;
							logger.debug("New numDimensionsPV value {} from {}", value, numDimensionsPV);
							updateDataChannelMonitor();
						}
					} else if (channelName.equalsIgnoreCase(colourModePV)) {
						DBR_String dbrs = (DBR_String)dbr;
						String value = dbrs.getStringValue()[0];
						if (colourMode != value) {
							colourMode = value;
							logger.debug("New colourModePV value '{} 'from {}", value, colourModePV);
							updateDataChannelMonitor();
						}
					} else {
						logger.debug("New value from {}, ignoring: {}", channelName, dbr);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}

		private void updateDataChannelMonitor() throws CAException, InterruptedException {
			int[] were = {height, width, rgbChannels};
			dataChannelMonitor.removeMonitorListener(dataChannelMonitorListener);
			int dataSize = calculateAndUpdateDataSize();
			logger.debug("New value for height {} width {} channels {} numDimensions {} or colourMode {} "+
					"(height, width & channels were {})", height, width, rgbChannels, numDimensions, colourMode, were);
			dataChannelMonitor = ec.setMonitor(dataChannel, dataChannelMonitorListener, dataSize);
		}
	}
}
