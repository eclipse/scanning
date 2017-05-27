package org.eclipse.scanning.api.event.queues.models;

import java.util.List;

public class TaskBeanModel extends QueueableModel {
	
	public TaskBeanModel(String tbShrtNm, String name, List<String> atomQueue) {
		super(tbShrtNm, name, atomQueue);
	}

}
