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
package org.eclipse.scanning.device.ui.util;

import java.util.Arrays;



/**
 * @author Matthew Gerring
 *
 */
public class StringUtils {
	

	/**
	 * Returns a StringBuilder with only the digits and . contained
	 * in the original string.
	 * 
	 * @param text
	 * @param decimalPlaces 
	 * @return StringBuilder
	 */
	public static final StringBuilder keepDigits(final String text,
			                                           int    decimalPlaces) {
		
		// Used to make algorithm below simpler, bit of a hack.
		if (decimalPlaces==0) decimalPlaces = -1;
		
		final StringBuilder buf = new StringBuilder();
		// Remove non digits
		final char [] ca   = text.toCharArray();		
		int decCount = 0;
		for (int i =0;i<ca.length;++i) {
			if (i==0&&ca[i]=='-') {
				buf.append(ca[i]);
				continue;
			}
	        if (StringUtils.isDigit(ca[i])) {
				if ('.'==ca[i]||decCount>0) {
					++decCount;
				}
	        	if (decCount<=decimalPlaces+1) buf.append(ca[i]);
	        } else {
	        	break;
	        }
		}
        return buf;
	}
	
	/**
	 * Returns true if digit or .
	 * @param c
	 * @return boolean
	 */
	public static final boolean isDigit(final char c) {
		if (Character.isDigit(c)) return true;
		if ('.'==c) return true;
		return false;
	}


	/**
	 * Deals with primitive arrays
	 * @param value
	 */
	public static String toString(Object value) {
		
		if (value==null) return null;
		
        if (value instanceof short[]) {
        	return Arrays.toString((short[])value);
        	
        } else if  (value instanceof int[]) {
        	return Arrays.toString((int[])value);
        	
        } else if  (value instanceof long[]) {
        	return Arrays.toString((long[])value);
        	
        } else if  (value instanceof char[]) {
        	return Arrays.toString((char[])value);
        	
        } else if  (value instanceof float[]) {
        	return Arrays.toString((float[])value);
        	
        } else if  (value instanceof double[]) {
        	return Arrays.toString((double[])value);
        	
        } else if  (value instanceof boolean[]) {
        	return Arrays.toString((boolean[])value);
        	
        } else if  (value instanceof byte[]) {
        	return Arrays.toString((byte[])value);
        	
        } else if  (value instanceof Object[]) {
        	return Arrays.toString((Object[])value);
        }
        
        return value.toString();
	}

}

	
