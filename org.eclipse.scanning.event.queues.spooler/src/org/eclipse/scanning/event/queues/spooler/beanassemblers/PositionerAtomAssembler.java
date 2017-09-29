package org.eclipse.scanning.event.queues.spooler.beanassemblers;

import java.util.stream.Collectors;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

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
		model.getPositionerNames().stream().forEach(dev -> atom.addPositioner(dev, model.getPositionerTarget(dev)));
		return atom;
	}


	@Override
	public void setBeanName(PositionerAtom bean) {
		StringBuffer name = new StringBuffer("Set position of ");
		name.append(bean.getPositionerNames().stream().map(devName -> "'"+devName+"'="+bean.getPositionerConfig().get(devName)).collect(Collectors.joining(", ")));
		bean.setName(name.toString());
	}

	@Override
	public void updateBeanModel(PositionerAtom model, ExperimentConfiguration config) throws QueueModelException {
		replaceMapIQueueValues(model.getPositionerConfig(), config);
	}

}
