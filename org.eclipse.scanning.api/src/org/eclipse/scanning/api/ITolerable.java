package org.eclipse.scanning.api;

public interface ITolerable<T> {

	/**
	 * The tolerance is a value +/- position which
	 * when the position is still within this tolerance
	 * means that a setPosition(...) does nothing. 
	 * 
	 * This principle of tolerance allows jittery motors
	 * not to have value set when their current value is
	 * within acceptable accuracy or tolerance.
	 * 
	 * @return
	 */
	default T getTolerance() {
		return null;
	}
	
	/**
	 * Set the tolerance which when set will allow setPosition(...)
	 * on the device to check if a position set is required.
	 * 
	 * The tolerance is a value +/- position which
	 * when the position is still within this tolerance
	 * means that a setPosition(...) does nothing. 
	 *
	 * @param toleranceValue
	 * @return The previous value including null if tolerance is being set for the first time.
	 */
	default T setTolerance(T toleranceValue) {
		throw new IllegalArgumentException("Tolerance is not implemented for "+getClass().getSimpleName());
	}

}
