package org.eclipse.scanning.api.annotation.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * If a given model should not be edited by the default table of 
 * FieldDescriptors but the whole type has its own editor. This is 
 * defined by annotating the type with a TypeDescriptor
 * 
 * @author Matthew Gerring
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeDescriptor {

	/**
	 * This is the fully qualified class name to the class required to edit the
	 * bean which is annotated by this annotation.
	 * 
	 * As this is likely to be an SWT composite, the system looks for a constructor
	 * of argument list "Composite parent, int style". However if that constructor
	 * is not found then a no-argument one is searched. This allows editors which 
	 * are not SWT Composites to be made available. However the recommended approach
	 * is to provide an SWT Composite for the editor. 
	 * 
	 * @return
	 */
	public String editor() default "";
	
	/**
	 * This is the bundle id. If the editor class is not available to the
	 * UI rendering bundle (this is org.eclipse.scanning.device.ui when
	 * this comment is written) then specify bundle to provide a class loader
	 * to load the required editor class.
	 * 
	 * @return
	 */
	public String bundle() default "";
	
	/**
	 * This is the property shown in the UI to name the thing which we are editing 
	 * with the editor we have referenced. For instance "scans" or "xanes".
	 */
	public String label() default "";
}
