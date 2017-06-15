package org.eclipse.scanning.event.queues;

import javax.annotation.PostConstruct;

import org.eclipse.scanning.api.event.EventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On creation by spring, class calls the init() method of the OSGi configured 
 * {@link IQueueService}. This is necessary since the remote control service 
 * ({@link IQueueControllerService}) needs the {@link IResponder} in 
 * {@link QueueService} to be present (it is instantiated in the init() 
 * method).
 * 
 * Why this class?
 * --
 * The {@link QueueService} is instantiated by OSGi. To initialise the 
 * {@link QueueService}, the {@link IEventService} (also OSGi) needs to be 
 * configured and the broker URI needs to be set (set by spring). As the 
 * {@link QueueService} is currently set up we can't call it from spring after 
 * creating the OSGi service as this would create a second non-OSGi instance 
 * of the service. We also can't just use OSGi to set it up, since the URI is 
 * not set at that stage.
 * 
 * Essentially, we need this class to allow the {@link QueueService} to be 
 * auto-configured in preparation to start, when it is called to do so.
 * 
 * So this class is a bit of a fudge. To get round it, the 
 * {@link QueueService} would need to be refactored, but at this stage it's 
 * more important to get everything working - 05.06.2017 M. Wharmby
 * 
 * @author Michael Wharmby
 *
 */
public class QueueServiceSpringInitializer {
	
	private static Logger logger = LoggerFactory.getLogger(QueueServiceSpringInitializer.class);
	
	@PostConstruct
	public void initialiseQueueService() {
		try {
			ServicesHolder.getQueueService().init();
		} catch (EventException e) {
			logger.error("Failed to initialise queue service");
		}
	}

}
