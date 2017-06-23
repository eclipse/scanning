package org.eclipse.scanning.event.queues.spooler;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.IHasAtomQueue;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class  AtomQueueBuilder<P extends IHasAtomQueue<T>, T extends QueueAtom> {

	private static final Logger logger = LoggerFactory.getLogger(AtomQueueBuilder.class);
	
	private IQueueBeanFactory qbf;
	private P modelInstance, realInstance;
	private Map<QueueValue<String>, IQueueValue<?>> localValues;
	
	/**
	 * Construct an {@link AtomQueueBuilder} to construct the given model.
	 * @param modelInstance {@link IHasAtomQueue} instance containing atom list
	 * @param realInstance {@link IHasAtomQueue} instance to be supplied with 
	 *        atoms
	 * @param localValues Map containing {@link IQueueValue}s used to set 
	 *        parameters in the atoms
	 * @param qbf {@link IQueueBeanFactory} containing the atom registry
	 */
	protected AtomQueueBuilder(P modelInstance, P realInstance, Map<QueueValue<String>, IQueueValue<?>> localValues, IQueueBeanFactory qbf) {
		this.modelInstance = modelInstance;
		this.realInstance = realInstance;
		this.localValues = localValues;
		this.qbf = qbf;
	}

	/**
	 * Used by {@link TaskBeanAssembler}/{@link SubTaskAtomAssembler}s to get 
	 * atoms in the queueAtomShortNames Lists of a given model and put 
	 * them into a new, real atomQueue in an instance of {@link TaskBean} or 
	 * {@link SubTaskAtom} (respectively).
	 * @param clazz Class of bean being created (for error reporting)
	 * @throws QueueModelException if an atom was not present in the registry
	 */
	protected P populateAtomQueue(Class<P> clazz) throws QueueModelException {
		
		for (QueueValue<String> stShrtNm : modelInstance.getQueueAtomShortNames()) {
			try {
				T at = qbf.assembleQueueAtom(stShrtNm, localValues);
				realInstance.addAtom(at);
			} catch (QueueModelException qme) {
				logger.error("Could not assemble "+clazz.getSimpleName()+" due to missing child atom: "+qme.getMessage());
				throw new QueueModelException("Could not assemble "+clazz.getSimpleName()+": "+qme.getMessage(), qme);
			}
		}
		return realInstance;
	}

}
