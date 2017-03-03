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
package org.eclipse.scanning.device.ui.model;

import java.io.Serializable;
import java.text.MessageFormat;

import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.richbeans.widgets.internal.GridUtils;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.ui.auto.IModelDialog;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.PlotUtil;
import org.eclipse.scanning.device.ui.util.ScanRegions;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a Model Table on an additional popup for those
 * fields which are an almagom of values. For instance BoundingBox.
 * 
 * @author Matthew Gerring
 *
 */
public class ModelCellEditor extends CellEditor {

	private static final Logger logger = LoggerFactory.getLogger(ModelCellEditor.class);

	/**
	 * Image registry key for three dot image (value <code>"cell_editor_dots_button_image"</code>).
	 */
	public static final String CELL_EDITOR_IMG_DOTS_BUTTON = "cell_editor_dots_button_image";//$NON-NLS-1$
	
	static {
		ImageRegistry reg = JFaceResources.getImageRegistry();
		reg.put(CELL_EDITOR_IMG_DOTS_BUTTON, ImageDescriptor.createFromFile(
				DialogCellEditor.class, "images/dots_button.png"));//$NON-NLS-1$
	}

	/**
	 * The area control.
	 */
	private Composite area;

	/**
	 * The current contents.
	 */
	private Control contents;

	/**
	 * The label that gets reused by <code>updateLabel</code>.
	 */
	private Text defaultLabel;

	// Data
	private FieldValue fvalue;
	private Object     value;

	// UI 
	private ILabelProvider labelProv;

	public ModelCellEditor(Composite      parent, 
			               FieldValue     value, 
			               ILabelProvider labelProv) {
		super();
		this.fvalue     = value;
		this.labelProv = labelProv;
		create(parent);
	}

    /**
     * Default DialogCellEditor style
     */
    private static final int defaultStyle = SWT.NONE;

     /**
     * Creates the button for this cell area under the given parent control.
     * <p>
     * The default implementation of this framework method creates the button
     * display on the right hand side of the dialog cell area. Subclasses
     * may extend or reimplement.
     * </p>
     *
     * @param parent the parent control
     * @return the new button control
     */
    protected Button createButton(Composite parent) {
        Button result = new Button(parent, SWT.DOWN);
        result.setText("..."); //$NON-NLS-1$
        GridData gdata = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gdata.heightHint = 22;
        result.setLayoutData(gdata);
        return result;
    }


    @Override
	protected Control createControl(Composite parent) {

        Font font = parent.getFont();
        Color bg = parent.getBackground();

        area = new Composite(parent, getStyle());
        area.setFont(font);
        area.setBackground(bg);
        area.setLayout(new GridLayout(3, false));
        GridUtils.removeMargins(area);

        contents = createTextContents(area);
        updateContents(value);
        
        KeyListener exit = new KeyAdapter() {
        	@Override
        	public void keyReleased(KeyEvent e) {
        		if (e.character == '\u001b') { // Escape
        			fireCancelEditor();
        		}
        	}
        };
        
        if (fvalue.getModel() instanceof IBoundingBoxModel) {
            Button rbutton = new Button(area, SWT.DOWN);
            rbutton.setImage(Activator.getImageDescriptor("icons/ProfileBox.png").createImage());
            rbutton.setToolTipText("Take bounding box from plot or create a new one if none exist");
            rbutton.addKeyListener(exit);
            rbutton.addSelectionListener(new SelectionAdapter() {

            	@Override
            	public void widgetSelected(SelectionEvent event) {
            		try {
            			createBoundingBox();
            		} catch (GeneratorException e) {
            			logger.error("Unable to get the box from the plot", e);
            		}
            	}
            });
            GridData gdata = new GridData(SWT.CENTER, SWT.CENTER, false, false);
            gdata.heightHint = 22;
            rbutton.setLayoutData(gdata);
        }

        Button button = createButton(area);
        button.setFont(font);

        button.addKeyListener(exit);

        button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
 
            	Object newValue = openDialogBox(area);

            	if (newValue != null) {
                    boolean newValidState = isCorrect(newValue);
                    if (newValidState) {
                        markDirty();
                        doSetValue(newValue);
                    } else {
                        // try to insert the current value into the error message.
                        setErrorMessage(MessageFormat.format(getErrorMessage(),
                                new Object[] { newValue.toString() }));
                    }
                    fireApplyEditorValue();
                }
            }
        });

        setValueValid(true);

        return area;
    }
    
    /**
     * Creates the controls used to show the value of this cell area.
     * <p>
     * The default implementation of this framework method creates
     * a label widget, using the same font and background color as the parent control.
     * </p>
     * <p>
     * Subclasses may reimplement.  If you reimplement this method, you
     * should also reimplement <code>updateContents</code>.
     * </p>
     *
     * @param cell the control for this cell area
     * @return the underlying control
     */
    protected Control createTextContents(Composite cell) {
    	Text txt = new Text(cell, SWT.LEFT);
    	txt.setFont(cell.getFont());
    	txt.setBackground(cell.getBackground());
    	txt.setEditable(false);
    	GridData gData = new GridData(SWT.FILL, SWT.FILL, true, true);
    	gData.widthHint = 100;
    	txt.setLayoutData(gData);
    	defaultLabel = txt;
        return txt;
    }

    protected void createBoundingBox() throws GeneratorException {
    	this.value = ScanRegions.createBoxFromPlot();
    	if (value!=null) {
	     	setValueValid(true);
	    	fireApplyEditorValue();
    	} else {
    		try {
				ScanRegions.createRegion(PlotUtil.getRegionSystem(), RegionType.BOX, null);
				ViewUtil.showTip(new ToolTip(defaultLabel.getShell(), SWT.BALLOON), "Drag a box in the '"+PlotUtil.getRegionSystem().getPlotName()+"' to create a scan region.");
			} catch (Exception e) {
				logger.error("Unable to create region!", e);
			}
    	}
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

    @Override
	protected Object doGetValue() {
        return value;
    }

    @Override
	protected void doSetFocus() {
        defaultLabel.setFocus();
    }


    @Override
	protected void doSetValue(Object value) {
        this.value = value;
        updateContents(value);
    }

    /**
     * Opens a dialog box under the given parent control and returns the
     * dialog's value when it closes, or <code>null</code> if the dialog
     * was canceled or no selection was made in the dialog.
     * <p>
     * This framework method must be implemented by concrete subclasses.
     * It is called when the user has pressed the button and the dialog
     * box must pop up.
     * </p>
     *
     * @param cellEditorWindow the parent control cell area's window
     *   so that a subclass can adjust the dialog box accordingly
     * @return the selected value, or <code>null</code> if the dialog was
     *   canceled or no selection was made in the dialog
     */
	protected Object openDialogBox(Control cellEditorWindow) {
		
		try {
			final IModelDialog<Serializable> dialog = ServiceHolder.getInterfaceService().createModelDialog(cellEditorWindow.getShell()); // extends BeanDialog
			dialog.create();
			dialog.setSize(550,450); // As needed
			dialog.setText("Edit "+fvalue.getAnnotation().label());
		
			dialog.setModel((Serializable)fvalue.get(true));
	        final int ok = dialog.open();
	        if (ok == IModelDialog.OK) {
	            return dialog.getModel();
	        }
		} catch (Exception ne) {
			logger.error("Problem editing model!", ne);
		}
		return null;
	}

	protected void updateContents(Object value) {
		if ( defaultLabel == null) {
			return;
		}
		if (value == null ) return;
		defaultLabel.setText(labelProv.getText(value));
	}

}
