package org.eclipse.scanning.api.malcolm.attributes;

/**
 * Interface for device attributes. At the time of writing this is only used by
 * Malcolm devices.
 *
 * @author Matthew Dickie
 * @param <T> the type of the attribute's value
 */
public interface IDeviceAttribute<T> {

	/**
	 * @return the name of this attribute
	 */
	public String getName();

	/**
	 * @return a description of this attribute
	 */
	public String getDescription();

	/**
	 * @return tags for this attribute (possibly empty)
	 */
	public String[] getTags();

	/**
	 * @return a human-readable label for this attribute
	 */
	public String getLabel();

	/**
	 * @return <code>true</code> if this attribute is writeable, <code>false</code> otherwise
	 */
	public boolean isWriteable();

	/**
	 * @return the value of this attribute
	 */
	public T getValue();

}