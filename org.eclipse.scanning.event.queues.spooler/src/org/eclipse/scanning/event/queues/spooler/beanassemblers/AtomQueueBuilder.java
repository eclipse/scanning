package org.eclipse.scanning.event.queues.spooler.beanassemblers;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.IHasAtomQueue;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class  AtomQueueBuilder<P extends IHasAtomQueue<T>, T extends QueueAtom> {

	private static final Logger logger = LoggerFactory.getLogger(AtomQueueBuilder.class);
	
	private IQueueBeanFactory qbf;
	private Class<P> clazz;
	
	/**
	 * Construct an {@link AtomQueueBuilder} to construct the given model.
	 * @param modelInstance {@link IHasAtomQueue} instance containing atom list
	 * @param realInstance {@link IHasAtomQueue} instance to be supplied with 
	 *        atoms
	 * @param config Map containing {@link IQueueValue}s used to set 
	 *        parameters in the atoms
	 * @param qbf {@link IQueueBeanFactory} containing the atom registry
	 */
	protected AtomQueueBuilder(IQueueBeanFactory qbf, Class<P> clazz) {
		this.qbf = qbf;
		this.clazz = clazz;
	}

	/**
	 * Used by {@link TaskBeanAssembler}/{@link SubTaskAtomAssembler}s to get 
	 * atoms in the queueAtomShortNames Lists of a given model and put 
	 * them into a new, real atomQueue in an instance of {@link TaskBean} or 
	 * {@link SubTaskAtom} (respectively).
	 * @param clazz Class of bean being created (for error reporting)
	 * @throws QueueModelException if an atom was not present in the registry
	 */
	protected P populateAtomQueue(P modelInstance, P realInstance, ExperimentConfiguration config) 
			throws QueueModelException {
		
		for (QueueValue<String> stShrtNm : modelInstance.getQueueAtomShortNames()) {
			try {
				T at = qbf.assembleQueueAtom(stShrtNm, config);
				realInstance.addAtom(at);
			} catch (QueueModelException qme) {
				logger.error("Could not assemble "+clazz.getSimpleName()+" due to missing child atom: "+qme.getMessage());
				throw new QueueModelException("Could not assemble "+clazz.getSimpleName()+": "+qme.getMessage(), qme);
			}
		}
		return realInstance;
	}

}
