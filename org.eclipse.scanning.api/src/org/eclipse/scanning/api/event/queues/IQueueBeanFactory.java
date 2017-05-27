package org.eclipse.scanning.api.event.queues;

import java.util.List;

import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.models.SubTaskAtomModel;
import org.eclipse.scanning.api.event.queues.models.TaskBeanModel;

public interface IQueueBeanFactory {
	
	<Q extends QueueAtom> void registerAtom(Q atom);
	
	void registerAtom(SubTaskAtomModel subTask);
	
	void registerTask(TaskBeanModel task);
	
	/**
	 * Return a list of all the shortNames of the registered queue atoms (both 
	 * {@link QueueAtom} instances such as {@link PositionerAtom} and 
	 * {@link SubTaskAtomModel}). This provides a common way to access any of 
	 * the objects which can create {@link QueueAtom}s.
	 * 
	 * @return List<String> shortNames of all registered atoms
	 */
	List<String> getQueueAtomRegister();
	
	/**
	 * 
	 * @param shortName
	 * @return
	 */
	<Q extends QueueAtom> Q getQueueAtom(String shortName);

}
