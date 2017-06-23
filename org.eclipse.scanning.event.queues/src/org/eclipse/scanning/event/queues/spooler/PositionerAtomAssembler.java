package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;

public final class PositionerAtomAssembler extends AbstractBeanAssembler<PositionerAtom> {

	public PositionerAtomAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
	}

	@Override
	public PositionerAtom buildNewBean(PositionerAtom model) throws QueueModelException {
		PositionerAtom atom = new PositionerAtom(model.getShortName(), false);
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		
		/*
		 * Loop through the positionerConfig in the model replacing any 
		 * targets which have references in the localValues or the 
		 * globalValues (see {@link QueueBeanFactory})
		 */
		for (String dev : model.getPositionerNames()) {
			Object devTgt = model.getPositionerTarget(dev);
			if (devTgt instanceof IQueueValue) {
				//It's a probably a value name...
				devTgt = updateIQueueValue((IQueueValue<?>) devTgt).evaluate();
			}
			atom.addPositioner(dev, devTgt);
		}
		
		return atom;
	}

	@Override
	public PositionerAtom setBeanName(PositionerAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
