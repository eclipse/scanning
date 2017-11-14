package org.eclipse.scanning.api.database;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * This class represents one or more Ids as returned
 * by an operation on the database.
 *
 * It is desirable to have operations which do more than
 * one thing, therefore we allow compound operations.
 * Accordingly there is more than one return code for operations
 * like this.
 *
 * @author Matthew Gerring
 *
 */
public class Id {

	public static final Id NONE = new Id(0L);

	private Map<String, Long>   ids;
	private Map<String, String> errors;

	public Id() {
		ids    = new LinkedHashMap<>();
		errors = new LinkedHashMap<>();
	}

	public Id(Long id) {
		this();
		ids.put(null, id);
	}
	public Id(String name, Long id) {
		this();
		ids.put(name, id);
	}

    /**
     *
     * @return if this id contains one or more real ids
     */
	public boolean is() {
		if (ids.isEmpty()) return false;
		for (String key : ids.keySet()) {
			if (ids.get(key)>0) return true;
		}
		return false;
	}
	public Long get() {
		return ids.get(null);
	}

	/**
	 * Get named Id
	 * @param name
	 * @return
	 */
	public Long get(String name) {
		return ids.get(name);
	}

	/**
	 * Add an id
	 * @param name
	 * @param id
	 * @return
	 */
	public Long put(String name, Long id) {
		return ids.put(name, id);
	}

	public Long put(String name, Id id) {
		if (id.size()!=1) throw new IllegalArgumentException("Only one id may be added at a time!");
		return ids.put(name, id.get());
	}

	/**
	 * Get named Id
	 * @param name
	 * @return
	 */
	public String getError(String name) {
		return errors.get(name);
	}

	/**
	 * Add an id
	 * @param name
	 * @param id
	 * @return
	 */
	public String putError(String name, String message) {
		return errors.put(name, message);
	}

	public boolean isErrors() {
		return !errors.isEmpty();
	}

	public Map<String, Long> getIds() {
		return ids;
	}

	public void setIds(Map<String, Long> ids) {
		this.ids = ids;
	}

	public Map<String, String> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, String> errors) {
		this.errors = errors;
	}

	public int size() {
		return ids.size();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((errors == null) ? 0 : errors.hashCode());
		result = prime * result + ((ids == null) ? 0 : ids.hashCode());
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
		Id other = (Id) obj;
		if (errors == null) {
			if (other.errors != null)
				return false;
		} else if (!errors.equals(other.errors))
			return false;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!ids.equals(other.ids))
			return false;
		return true;
	}

}
