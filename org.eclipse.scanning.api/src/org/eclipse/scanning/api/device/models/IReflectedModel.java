package org.eclipse.scanning.api.device.models;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * In order for models to be automatically added to the mscan
 * command, they must have a way to get from java object to
 * jython command. Any IDetectorModel will have a name and
 * exposure time generated. A refected model can provide a
 * method for doing this. This method is defaulted to reflecting
 * the fields out of the model. Please override to provide more
 * fine grained behaviour.
 *
 * @author Matthew Gerring
 *
 */
public interface IReflectedModel {

	/**
	 * TODO: this field should really be private, it is used as a constants by {@link #getCommandString(boolean)}
	 * below. Unfortunately interfaces cannot have private member is Java 8. Java 9 may fix this.
	 */
	public static final Set<String> EXCLUDED_FIELD_NAMES = new HashSet<>(Arrays.asList("name", "exposureTime"));

	/**
	 * Implement to provide the command to configure a given detector.
	 * For instance:<p>
	 * <pre>
	 *     detector('processing', -1, detectorName='mandelbrot', processingFile = '/tmp/something')
	 * </pre>
	 * @param verbose
	 * @return
	 */
	default String getCommandString(boolean verbose) throws Exception {

		StringBuilder buf = new StringBuilder("detector('");
		buf.append(ModelReflection.getName(this));
		buf.append("', ");
		buf.append(ModelReflection.getTime(this));

	    if (verbose) {
	    	final List<String> fieldStrings = Arrays.stream(getClass().getDeclaredFields()).
		    	map(Field::getName).
    			filter(name -> !EXCLUDED_FIELD_NAMES.contains(name)).
    			map(name -> ModelReflection.stringifyField(this,name)).
    			filter(Objects::nonNull).
    			collect(toList());
	    	if (!fieldStrings.isEmpty()) buf.append(", ");
	    	buf.append(String.join(", ", fieldStrings));
	    }
	    buf.append(")");
	    return buf.toString();
	}

}
