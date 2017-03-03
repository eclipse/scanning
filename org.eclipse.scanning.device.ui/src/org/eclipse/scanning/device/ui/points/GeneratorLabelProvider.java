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
package org.eclipse.scanning.device.ui.points;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.richbeans.widgets.table.SeriesItemLabelProvider;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

final class GeneratorLabelProvider extends SeriesItemLabelProvider implements IStyledLabelProvider {


	public GeneratorLabelProvider(int column) {
		super(column);
	}

	@Override
	public StyledString getStyledText(Object element) {

		if(!(element instanceof GeneratorDescriptor)) return new StyledString();

		final StyledString ret = new StyledString(getText(element));
		if (column==0) {
			GeneratorDescriptor des = (GeneratorDescriptor)element;
			ret.append("    ");
	        ret.append(des.getCategoryLabel(), StyledString.DECORATIONS_STYLER);
		}
		return ret;
	}

	@Override
	public String getText(Object element) {
		
		if(!(element instanceof GeneratorDescriptor)) return super.getText(element);
		
		GeneratorDescriptor des = (GeneratorDescriptor)element;
		
		// Other columns
		if (column>0) {
			try {
				switch (column) {
				case 1:
					return "TODOField1";
				case 2:
					return "TODOField2";
				}
			} catch (Exception ne) {
				return ne.getMessage();
			}
		}
		
		StringBuilder buf = new StringBuilder(" ");

		try {
			IPointGenerator<?> gen = des.getSeriesObject();
			
			buf.append(gen.getLabel());


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return buf.toString();
		
	}
	
	private Font italicFont;
	public Font getFont(Object element) {
		
		if(!(element instanceof GeneratorDescriptor)) return super.getFont(element);
		
		GeneratorDescriptor des = (GeneratorDescriptor)element;
		
		try {
			IPointGenerator<?> gen = des.getSeriesObject();
			if (!gen.isEnabled()) {
				if (italicFont == null) {
					final FontData shellFd = Display.getDefault().getActiveShell().getFont().getFontData()[0];
					FontData fd      = new FontData(shellFd.getName(), shellFd.getHeight(), SWT.ITALIC);
					italicFont = new Font(null, fd);
				}
				return italicFont;
			}
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	public Image getImage(Object element) {
		if (column>0) return null;
		if(!(element instanceof GeneratorDescriptor)) return super.getImage(element);
		GeneratorDescriptor des = (GeneratorDescriptor)element;
		return des.getImage();
	}

	public void dispose() {
		super.dispose();
		if (italicFont!=null) {
			italicFont.dispose();
			italicFont = null;
		}
	}

}
