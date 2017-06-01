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
package org.eclipse.scanning.event.queues;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.QueueStatus;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.event.queues.remote.QueueResponseCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AtomQueueService provides an implementation of {@link IQueueService}.
 * The service requires a URI and a queueRoot String as configuration. The URI
 * is used to specify the broker which will be used to run create 
 * {@link IEventService} objects and the queueRoot String is used as a starting
 * point to name {@link IQueue} objects.
 * 
 * On starting, the service creates a job-queue {@link IQueue} object which 
 * processes {@link QueueBean}s (i.e. {@link TaskBean}s in the design). The 
 * service has methods to register new active-queue {@link IQueue} object on 
 * the fly, with the names for these based on the queueRoot String and a random
 * number to ensure queue names do not collide.
 * 
 * All queues are configured to share the same heartbeat & command destinations
 * to allow control of the service (these are also based on the queueRoot 
 * String with common suffixes appended).
 * 
 * Users should be able to interact with the service directly, and therefore 
 * the job-queue. However individual active-queues should work autonomously.
 * Interaction with the queue is provided through 
 * {@link IQueueControllerService}.
 * 
 * To start the service, after instantiation a queueRoot & URI should be 
 * provided. init() can then be called, leaving the service in a state where 
 * the start() and stop() methods can be used to activate/deactivate bean 
 * processing. To shutdown the service, call disposeService()  
 * 
 * 
 * @author Michael Wharmby
 *
 */
public class QueueService extends QueueControllerService implements IQueueService {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueService.class);
	
	private static Map<String, IQueue<QueueAtom>> activeQueueRegister;

	
	private String heartbeatTopicName, commandSetName, commandTopicName, jobQueueID;
	private boolean active = false, stopped = false, runOnce = false;

	/*
	 * uriConstruct is only set during the constructor and used by unit tests to 
	 * specify the URI.
	 */
	private final String queueRoot, uriConstruct;
	private URI uri;
	private IQueue<QueueBean>         jobQueue;
	private IResponder<QueueRequest>  queueResponder;

	
	private final ReentrantReadWriteLock queueControlLock = new ReentrantReadWriteLock();
	
	static {
		System.out.println("Created " + IQueueService.class.getSimpleName());
	}
	
	/**
	 * No argument constructor for OSGi
	 * @throws EventException if a URI cannot be constructed for the service.
	 */
	public QueueService() {
		this(getQueueRootFromProperty(), CommandConstants.getScanningBrokerUri());
	}
	
	/**
	 * Used by tests directly
	 * @throws EventException if a URI cannot be constructed for the service.
	 */
	public QueueService(String uri) {
		this(getQueueRootFromProperty(), uri);
	}
	
	/**
	 * Used by tests directly.
	 * @throws EventException if a URI cannot be constructed for the service.
	 */
	public QueueService(String queueRoot, String uriConstruct) {
		this.queueRoot = queueRoot;
		this.uriConstruct = uriConstruct;
		
		//uriString & queueRoot are already set, so we need to set their dependent fields
		heartbeatTopicName = queueRoot+HEARTBEAT_TOPIC_SUFFIX;
		commandSetName = queueRoot+COMMAND_SET_SUFFIX;
		commandTopicName = queueRoot+COMMAND_TOPIC_SUFFIX;

		//Create the active-queues map
		if (activeQueueRegister==null) activeQueueRegister = new ConcurrentHashMap<>();
	}
	
	private static final String getQueueRootFromProperty() {
		String root = System.getProperty("GDA/gda.event.queues.queue.root");
		if (root==null) root = System.getProperty("org.eclipse.scanning.event.queues.queue.root", DEFAULT_QUEUE_ROOT);
		return root;
	}

	@Override
	public void init() throws EventException {
		/*
		 * We need the URI set by this point. Before here, we couldn't rely on 
		 * SPRING to have set a value, so we check one hasn't been passed in 
		 * by tests (as uriConstruct) and if not, get a URI via the SPRING 
		 * config. 
		 */
		logger.debug("Initialising QueueService...");
		runOnce = true; //Indicate that initialisation has been attempted
		try {
			if (uriConstruct == null) {
				uri = new URI(CommandConstants.getScanningBrokerUri());
			} else {
				uri = new URI(uriConstruct);
			}
		} catch (URISyntaxException uSEx) {
			throw new IllegalArgumentException("Failed to set QueueService URI", uSEx);
		}
		
		//Now we can set up the job-queue
		logger.debug("Creating job-queue...");
		jobQueueID = queueRoot+JOB_QUEUE_SUFFIX;
		jobQueue = new Queue<QueueBean>(jobQueueID, uri, 
				heartbeatTopicName, commandSetName, commandTopicName);

		//Add responder
		logger.debug("Creating queueResponder...");
		IEventService evServ = ServicesHolder.getEventService();
		queueResponder = evServ.createResponder(uri, QUEUE_REQUEST_TOPIC, QUEUE_RESPONSE_TOPIC);
		queueResponder.setBeanClass(QueueRequest.class);
		queueResponder.setResponseCreator(new QueueResponseCreator());

		//Mark initialised
		init = true;
		
		//With QueueService parts initialised, we can initialise the QueueController parts
		// - this also makes calls to the IEventService!
		super.init();
		logger.debug("QueueService initialised with queue-root: "+queueRoot);
	}
	
	@Override
	public void disposeService() throws EventException {
		if (!init && runOnce) {
			logger.warn("Queue service has already been disposed or was never initialised.");
			return;
		}
		
		//Stop the job queue if service is up
		logger.debug("Force-stopping active-queue(s)...");
		if (active) stop(true);
		
		//Shutdown the responder
		logger.debug("Disconnecting queueResponder...");
		queueResponder.disconnect();

		//Dispose the job queue
		disconnectAndClear(jobQueue);
		jobQueue = null;
		jobQueueID = null;
		
		//Mark the service not initialised
		init = false;
		logger.debug("QueueService disposed");
	}

	@Override
	public void start() throws EventException {
		logger.debug("Starting QueueService...");
		if (!init) {
			logger.error("Could not start the QueueService - service has not been initialised.");
			throw new EventException("QueueService not initialised. Cannot start");
		}
		//Check the job-queue is in a state to start
		if (!jobQueue.getStatus().isStartable()) {
			throw new EventException("Job queue not startable - Status: " + jobQueue.getStatus());
		} else if (isActive() || jobQueue.getStatus().isActive()) {
			logger.warn("Job queue is already active.");
			return;
		}
		//Start the job-queue if it can be
		logger.debug("Starting job-queue...");
		jobQueue.start();
		
		//Mark service as up & reset stopped (if needed)
		active = true;
		stopped = false;
		logger.info("QueueService started. Job-queue ready to receive QueueBeans...");
	}

	@Override
	public void stop(boolean force) throws EventException {
		logger.debug("Stopping job-queue...");
		if (!isActive()) return;//On two lines since stopped service with null jobQueue causes NPE
		if (!jobQueue.getStatus().isActive()) {
			logger.warn("Job-queue is not active.");
			return;
		}

		try {
			//Barge to the front of the queue to get the lock & start stopping things. TODO writelock?
			queueControlLock.writeLock().tryLock();

			//Deregister all existing active queues.
			if (!activeQueueRegister.isEmpty()) {
				//Create a new HashSet here as the deRegister method changes activeQueues
				Set<String> qIDSet = new HashSet<String>(activeQueueRegister.keySet());

				for (String qID : qIDSet) {
					//Stop the queue
					stopActiveQueue(qID, force);

					//Deregister the queue
					deRegisterActiveQueue(qID);
				}
			}

			logger.debug("Stopping job-queue (force="+force+")...");
			//Kill/stop the job queuebroker
			if (force) {
				killQueue(jobQueueID, true, false, false);
			} else {
				jobQueue.stop();
			}

			//Mark service as down & that it was stopped
			active = false;
			stopped = true;
		} finally {
			queueControlLock.writeLock().unlock();
		}
		logger.info("QueueService stopped");
	}
	
	@Override
	public String registerNewActiveQueue() throws EventException {
		if (!active) throw new IllegalStateException("Queue service not started.");
		
		//Generate the random name of the queue
		Random randNrGen = new Random();
		String randInt = String.format("%03d", randNrGen.nextInt(999));
		String aqID = queueRoot+ACTIVE_QUEUE_PREFIX+activeQueueRegister.size()+"-"+randInt+ACTIVE_QUEUE_SUFFIX;;
		try {
			//As we start interacting with the register, lock it so it doesn't change...
			queueControlLock.readLock().lockInterruptibly();
			//...and really make sure we don't get any name collisions
			while (activeQueueRegister.containsKey(aqID)) {
				aqID = queueRoot+ACTIVE_QUEUE_PREFIX+activeQueueRegister.size()+"-"+randInt+ACTIVE_QUEUE_SUFFIX;
			}

			//Create active-queue, add to register & return the active-queue ID
			logger.debug("Creating new active-queue... (ID: "+aqID+")");
			IQueue<QueueAtom> activeQueue = new Queue<>(aqID, uri, 
					heartbeatTopicName, commandSetName, commandTopicName);
			activeQueue.clearQueues();
			try {
				//We need to get the write lock to protect the register for us
				queueControlLock.readLock().unlock();
				queueControlLock.writeLock().lockInterruptibly();
				activeQueueRegister.put(aqID, activeQueue);
				return aqID;
			} finally {
				queueControlLock.readLock().lockInterruptibly();
				queueControlLock.writeLock().unlock();
				logger.debug("Active-queue successfully registered");
			}
			
		} catch (InterruptedException iEx) {
			logger.error("Active-queue registration interrupted: "+iEx.getMessage());
			throw new EventException(iEx);
		} finally{
			if (queueControlLock.isWriteLockedByCurrentThread()) {
				queueControlLock.writeLock().unlock();
			} else {
				queueControlLock.readLock().unlock();
			}
		}
	}
	
	@Override
	public void deRegisterActiveQueue(String queueID) throws EventException {
		//Are we in a state where we can deregister?
		if (stopped) throw new EventException("Queue service is stopped");
		if (!active) throw new EventException("Queue service not started.");
		logger.debug("Deregistering active-queue (ID: "+queueID+")");
		try {
			//Acquire a readlock to make sure other processes don't mess with the register
			queueControlLock.readLock().lockInterruptibly();

			//Get the queue and check that it's not started
			IQueue<QueueAtom> activeQueue = getActiveQueue(queueID);
			if (activeQueue.getStatus().isActive()) {
				throw new EventException("Active-queue " + queueID +" still running - cannot deregister.");
			}

			//Queue disposal happens here
			disconnectAndClear(activeQueue);

			try {
				//Lock the queue register & remove queueID requested
				queueControlLock.readLock().unlock();
				queueControlLock.writeLock().lockInterruptibly();

				//Remove remaining queue processes from map
				activeQueueRegister.remove(queueID);
			} finally {
				queueControlLock.readLock().lockInterruptibly();
				queueControlLock.writeLock().unlock();
				logger.debug("Active-queue successfully deregistered");
			}
		} catch (InterruptedException iEx){
			logger.error("Deregistration of active-queue "+queueID+" was interrupted.");
			throw new EventException(iEx);
		} finally {
			queueControlLock.readLock().unlock();
		}
	}
	
	private void disconnectAndClear(IQueue<? extends Queueable> queue) throws EventException {
		//Clear queues: in previous iteration found that...
		queue.clearQueues(); //...status queue clear, but submit not...
		queue.disconnect();
		boolean isClear = queue.clearQueues();//... submit queue now clear.
		if (!isClear) throw new EventException("Failed to clear queues when disposing "+queue.getQueueID());
		logger.debug(queue.getQueueID()+" successfully disconnected and cleared");
	}
	
	@Override
	public boolean isActiveQueueRegistered(String queueID) {
		//Use lock to make sure the register isn't being changed by another process
		try {
			queueControlLock.readLock().lock();
			return activeQueueRegister.containsKey(queueID);
		} finally {
			queueControlLock.readLock().unlock();
		}
		
	}
	
	@Override
	public void startActiveQueue(String queueID) throws EventException {
		try {
			//Get read lock to & check active-queue is not already running
			queueControlLock.readLock().lockInterruptibly();
			IQueue<QueueAtom> activeQueue = getActiveQueue(queueID);
			if (!activeQueue.getStatus().isStartable()) {
				throw new EventException("Active-queue "+queueID+" is not startable - Status: " + activeQueue.getStatus());
			}
			if (activeQueue.getStatus().isActive()) {
				logger.warn("Active-queue "+queueID+" is already active.");
				return;
			}

			try {
				//We're ready to write the new queue to the register, so get the write lock
				queueControlLock.readLock().unlock();
				queueControlLock.writeLock().lockInterruptibly();
				activeQueue.start();
			} finally {
				queueControlLock.readLock().lockInterruptibly();
				queueControlLock.writeLock().unlock();
				logger.debug("Active-queue successfully started (ID: "+queueID+")");
			}
		} catch (InterruptedException iEx) {
			logger.error("Starting of active-queue "+queueID+" stopping was interrupted.");
			throw new EventException(iEx);
		} finally {
			queueControlLock.readLock().unlock();
		}
	}
	
	@Override
	public void stopActiveQueue(String queueID, boolean force) 
			throws EventException {
		if (stopped) throw new EventException("stopped");
		try {
			//Lock the register against changes & check active-queue is running
			queueControlLock.readLock().lockInterruptibly();
			IQueue<QueueAtom> activeQueue = getActiveQueue(queueID);

			//Is the Queue actually stoppable?
			if (activeQueue.getStatus() == QueueStatus.STOPPED) {
				logger.warn("Active-queue "+queueID+" already stopped.");
				return;
			} else if (!activeQueue.getStatus().isActive()) {
				logger.warn("Active-queue "+queueID+" is not active.");
				return;
			}

			//Upgrade to write lock while we stop/kill the requested active-queue
			try {
				queueControlLock.readLock().unlock();
				queueControlLock.writeLock().lockInterruptibly();
				if (force) {
					killQueue(queueID, true, false, false);
				}
				//Whatever happens we need to mark the queue stopped
				//TODO Does this need to wait for the kill call to be completed?
				activeQueue.stop();
			} finally {
				queueControlLock.readLock().lockInterruptibly();
				queueControlLock.writeLock().unlock();
				logger.debug(queueID+" successfully stopped");
			}
		} catch (InterruptedException iEx){
			logger.error("Stopping of active-queue "+queueID+" stopping was interrupted.");
			throw new EventException(iEx);
		} finally {
			queueControlLock.readLock().unlock();
		}
	}
	
	@Override
	public Set<String> getAllActiveQueueIDs() {
		//Use lock to make sure the register isn't being changed by another process
		try {
			queueControlLock.readLock().lock();
			return activeQueueRegister.keySet();
		} finally {
			queueControlLock.readLock().unlock();
		}
		
	}
	
	@Override
	public IQueue<? extends Queueable> getQueue(String queueID) throws EventException {
		if (queueID.equals(jobQueueID)) {
			return (IQueue<? extends Queueable>) getJobQueue();
		} else {
			if (isActiveQueueRegistered(queueID)) {
				return (IQueue<? extends Queueable>) getActiveQueue(queueID);
			} else {
				throw new EventException("QueueID does not match any registered queue");
			}
		}
	}

	@Override
	public IQueue<QueueBean> getJobQueue() {
		return jobQueue;
	}
	
	@Override
	public IQueue<QueueAtom> getActiveQueue(String queueID) throws EventException {
		//Use lock to make sure the register isn't being changed by another process
		try {
			queueControlLock.readLock().lock();
			if (isActiveQueueRegistered(queueID)) return activeQueueRegister.get(queueID);
			throw new EventException("Queue ID "+queueID+" not found in registry");
		} finally {
			queueControlLock.readLock().unlock();
		}
	}
	
	@Override
	public String getJobQueueID() {
		return jobQueueID;
	}

	@Override
	public String getQueueRoot() {
		return queueRoot;
	}

	@Override
	public String getHeartbeatTopicName() {
		return heartbeatTopicName;
	}

	@Override
	public String getCommandSetName() {
		return commandSetName;
	}

	@Override
	public String getCommandTopicName() {
		return commandTopicName;
	}

	@Override
	public URI getURI() {
		return uri;
	}
	
	@Override
	public boolean isInitialized() {
		return init;
	}

	@Override
	public boolean isActive() {
		return active;
	}

}
