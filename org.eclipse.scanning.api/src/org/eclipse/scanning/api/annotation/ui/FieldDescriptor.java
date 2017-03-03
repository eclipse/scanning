/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.scanning.api.annotation.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By default all fields in the model are editable. This annotation can be used to 
 * mark fields as invisible or just read only. This is used in the GUI to determine which
 * fields of the model should be editable in the UI.
 * 
 * @author Matthew Gerring
 * @see org.eclipse.scanning.scanning.ui.ScanningPerspective This perspective ties in with
 * the models and the scan command processing. It produces a general scan user interface which
 * is interoperable with queues and the jython command line.
 * 
 * TODO Perhaps break this up into separate annotations, however that would be slower to read.
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldDescriptor {

	/**
	 * 
	 * @return true if the field is visible in the UI
	 */
	public boolean visible() default true;
	
	/**
	 * 
	 * @return true if the field is editable or false for read only.
	 */
	public boolean editable() default true;
	
	/**
	 * If this field represents a particular sort of device, they
	 * should only be able to choose that device.
	 * 
	 * @return
	 */
	public DeviceType device() default DeviceType.NONE;
	
	/**
	 * 
	 * @return
	 */
	public FieldRole role() default FieldRole.ALL;
	
	/**
	 * Used to specify how the edit of the value should happen.
	 * @return
	 */
	public EditType edit() default EditType.DIRECT;
	
	/**
	 * 
	 * @return a string expression which if true, enables the field.
	 * For instance a field called 'status' exists which is an enum of OK, FAILED, ...
	 * The expression for enableif on another field of the model might be  'status==OK'
	 */
	public String enableif() default "";
	
	/**
	 * 
	 * @return an expression of other fields that should be evaluated and if it returns
	 * true, the value entered allowed. Otherwise the foreground will colour red.
	 */
	public String validif() default "";

	/**
	 * The label attribute. If unset, uses the name of the field for the label.
	 */
	public String label() default "";
	
	/**
	 * The value for the value if this field is compound. If set this
	 * is used as a pattern using ${} notation. For instance this field
	 * is an object with two fields x and y. The compoundLabel might be
	 * "x=${x}, y=${y}". This expression will be evaluated with the values 
	 * of x and y at render time.
	 * @return
	 */
	public String compoundLabel() default "";
	
	/**
	 * If scannable is set, this value overrides the maximum of the scannble, otherwise the scannable is used.
	 * @return maximum allowed legal value for field
	 */
	public double maximum() default Double.POSITIVE_INFINITY;
	
	/**
	 * If scannable is set, this value overrides the minimum of the scannble, otherwise the scannable is used.
	 * @return minimum allowed legal value for field
	 */
	public double minimum() default Double.NEGATIVE_INFINITY;
	
	/**
	 * If scannable is set, this value overrides the unit of the scannble, otherwise the scannable unit is used.
	 * @return the unit that the fields value should be in.
	 */
	public String unit() default "";
		
	/**
	 * Used to specify if a field is linked to a scannable
	 * and if so which field contains the scannable name.
	 * If it is set, the units of the
	 * scannable are read using the device connector and 
	 * presented in the user interface when the value of
	 * this annotated field is shown.
	 * 
	 * @return name of the field that references a scannable to use for user interface presentation.
	 */
	public String scannable() default "";
	
	/**
	 * 
	 * @return the string hint which is shown to the user when they first edit the value.
	 */
	public String hint() default "";
	
	/**
	 * If the field is a String, java.io.File, java.nio.file.Path or IResource
	 * you may use this annotation to define the type of checking which will be done.
	 * 
	 * If this field is not used and your field is a File for instance, the NEW_FILE
	 * option will be the default, rather than NONE
	 * 
	 * @return the file type.
	 */
	public FileType file() default FileType.NONE;
	
	/**
	 * Used to specify a dataset from a specific file
	 * 
	 * @return the field name the corresponds to file path in the model
	 */
	public String dataset() default "";
	
	/**
	 * used to show this value corresponds to an x or y axis range
	 * 
	 * @return the range type
	 */
	public RangeType rangevalue() default RangeType.NONE;
	
	/**
	 * The number format to format a field editing a number
	 */
	public String numberFormat() default "";
	
	/**
	 * The position of the field in the list, providing a way to override the default alphabetic sorting of fields
	 */
	public int fieldPosition() default Integer.MAX_VALUE;

	/**
	 * Set to make a validation regular expression that runs on a string field.
	 * @return
	 */
	public String regex() default "";
}
