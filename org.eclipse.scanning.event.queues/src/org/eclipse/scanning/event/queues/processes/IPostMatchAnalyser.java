package org.eclipse.scanning.event.queues.processes;

import org.eclipse.scanning.api.event.EventException;
/**
 * Interface to provide methods to handle different behaviours of concrete 
 * instances of {@link QueueProcess} at end of processing. Methods are calls 
 * by the the postMatchAnalysis method of {@link QueueProcess} depending on 
 * processed bean state/percent complete.
 *  
 * {@link QueueProcess} declares it implements IPostMatchAnalyser, but as an 
 * abstract class, does not provide implementation. Thus different behaviours 
 * can be encoded in different concrete classes. 
 * 
 * @author Michael Wharmby
 *
 */
public interface IPostMatchAnalyser {
	
	/**
	 * When the processed bean has a percentComplete of 99.5% or more.
	 * @throws EventException if event infrastructure clean-up fails.
	 */
	default void postMatchCompleted() throws EventException {
		//Implement as required
	};
	
	/**
	 * When the queueProcess has been marked terminated=true
	 * @throws EventException if event infrastructure clean-up fails.
	 */
	default void postMatchTerminated() throws EventException {
		//Implement as required
	};
	
	/**
	 * When the state of the processed bean is unclear (e.g. not marked 
	 * terminated or >99.5% complete).
	 * @throws EventException if event infrastructure clean-up fails.
	 */
	default void postMatchFailed() throws EventException {
		//Implement as required
	};

}
