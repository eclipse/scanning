package org.eclipse.scanning.api.event.queues.models;

import java.util.List;

public class SubTaskAtomModel extends QueueableModel {

	public SubTaskAtomModel(String stShrtNm, String name, List<String> atomQueue) {
		super(stShrtNm, name, atomQueue);
	}
}
