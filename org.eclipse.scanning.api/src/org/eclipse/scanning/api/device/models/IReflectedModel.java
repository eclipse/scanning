package org.eclipse.scanning.api.device.models;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
		
	    final Field[] fields = getClass().getDeclaredFields();
	    List<String> names = Arrays.stream(fields).map(field->field.getName()).collect(Collectors.toList());
	    names.remove("name");
	    names.remove("exposureTime");
	    
	    if (verbose) {
	 	    if (names.size()>0) {
				buf.append(", ");
			    for (Iterator<String> it = names.iterator(); it.hasNext();) {
					String name = it.next();
					buf.append(name);
					buf.append("=");
					buf.append(ModelReflection.stringify(ModelReflection.getValue(this, name)));
					if (it.hasNext()) buf.append(", ");
				}
		    }
	    }
	    buf.append(")");
	    return buf.toString();
	}
	

}
