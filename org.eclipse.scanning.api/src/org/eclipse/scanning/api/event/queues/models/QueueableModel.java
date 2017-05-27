package org.eclipse.scanning.api.event.queues.models;

import java.util.List;

public abstract class QueueableModel {
	
	private String name;
	private String shortName;
	private long runTime;
	
	private List<String> queueAtomShortNames;

	protected QueueableModel(String shortName, String name, List<String> atomQueueShortNames) {
		this.shortName = shortName;
		this.name = name;
		this.queueAtomShortNames = atomQueueShortNames;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	public List<String> getQueueAtomShortNames() {
		return queueAtomShortNames;
	}

	public void setQueueAtomShortNames(List<String> queueAtomShortNames) {
		this.queueAtomShortNames = queueAtomShortNames;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queueAtomShortNames == null) ? 0 : queueAtomShortNames.hashCode());
		result = prime * result + (int) (runTime ^ (runTime >>> 32));
		result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueueableModel other = (QueueableModel) obj;
		if (queueAtomShortNames == null) {
			if (other.queueAtomShortNames != null)
				return false;
		} else if (!queueAtomShortNames.equals(other.queueAtomShortNames))
			return false;
		if (runTime != other.runTime)
			return false;
		if (shortName == null) {
			if (other.shortName != null)
				return false;
		} else if (!shortName.equals(other.shortName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String clazzName = this.getClass().getSimpleName();
		return clazzName +" [shortName=" + shortName + "(name=" + name + "), runTime=" + runTime + ", queueAtomShortNames="
				+ queueAtomShortNames + "]";
	}

}
