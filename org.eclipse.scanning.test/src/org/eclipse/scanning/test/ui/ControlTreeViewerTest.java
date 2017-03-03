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
package org.eclipse.scanning.test.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.richbeans.test.ui.ShellTest;
import org.eclipse.scanning.api.ISpringParser;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.device.scannable.ControlTreeViewer;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class ControlTreeViewerTest extends ShellTest {

	
	@BeforeClass
	public static void createServices() throws Exception {	
		UISuite.createTestServices(true);
	}
	
	@AfterClass
	public static void disposeServices() throws Exception {	
		UISuite.disposeTestServices();
	}

	private ControlTree controlTree;
	private ControlTreeViewer viewer;

	@Override
	protected Shell createShell(Display display) throws Exception {
		
		this.controlTree = getControlTree("control_tree.xml");
		
		this.viewer = new ControlTreeViewer(controlTree, Services.getConnector());
		viewer.setUseFilteredTree(false);

		Shell shell = new Shell(display);
		shell.setText("Control Tree");
		shell.setLayout(new GridLayout(1, false));
        viewer.createPartControl(shell, controlTree);
		
		shell.pack();
		shell.setSize(500, 500);
		shell.open();

		return shell;
	}

	@Test
	public void checkShell() throws Exception {
		assertNotNull(bot.shell("Control Tree"));
	}
	
	@Test
	public void checkTree() throws Exception {
		assertNotNull(bot.tree(0));
	}
	
	@Test
	public void checkDefaultValues() throws Exception {
		
		assertEquals(2, bot.tree(0).columnCount());
		assertEquals(2, bot.tree(0).rowCount());
		
		assertEquals("Translations", bot.tree(0).cell(0, 0));
		
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Translations");
		List<String> children = item.getNodes();
		assertEquals(Arrays.asList("Stage X", "Stage Y", "Stage Z"), children);
		
		assertEquals("Stage X",   item.cell(0, 0));
		assertEquals("0.0    mm", item.cell(0, 1));	
		assertEquals("Stage Y",   item.cell(1, 0));
		assertEquals("0.0    mm", item.cell(1, 1));
		assertEquals("Stage Z",   item.cell(2, 0));
		assertEquals("2.0    mm", item.cell(2, 1));
		
		
		assertEquals("Experimental Conditions",  bot.tree(0).cell(1, 0));
		item = bot.tree(0).getTreeItem("Experimental Conditions");
		children = item.getNodes();
		assertEquals(Arrays.asList("Temperature"), children);
		assertEquals("Temperature",   item.cell(0, 0));
		assertEquals("295.0    K", item.cell(0, 1));	
	}
	
	@Test
	public void checkValuesTree2() throws Exception {
		
		ControlTree ct = getControlTree("control_tree2.xml");
		bot.getDisplay().syncExec(()->viewer.setControlTree(ct));
		
		assertEquals(2, bot.tree(0).columnCount());
		assertEquals(2, bot.tree(0).rowCount());
		
		assertEquals("Translations", bot.tree(0).cell(0, 0));
		
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Translations");
		List<String> children = item.getNodes();
		assertEquals(Arrays.asList("X", "Y", "Z"), children);
		
		assertEquals("X",   item.cell(0, 0));
		assertEquals("10.0    mm", item.cell(0, 1));	
		assertEquals("Y",   item.cell(1, 0));
		assertEquals("10.0    mm", item.cell(1, 1));
		assertEquals("Z",   item.cell(2, 0));
		assertEquals("10.0    mm", item.cell(2, 1));
		
		
		assertEquals("Experimental Conditions",  bot.tree(0).cell(1, 0));
		item = bot.tree(0).getTreeItem("Experimental Conditions");
		children = item.getNodes();
		assertEquals(Arrays.asList("Temperature"), children);
		assertEquals("Temperature",   item.cell(0, 0));
		assertEquals("295.0    K", item.cell(0, 1));	
	}
	
	@Test
	public void checkValuesTree3() throws Exception {
		
		ControlTree ct = getControlTree("control_tree3.xml");
		bot.getDisplay().syncExec(()->viewer.setControlTree(ct));
		
		assertEquals(2, bot.tree(0).columnCount());
		assertEquals(1, bot.tree(0).rowCount());
		
		assertEquals("Machine", bot.tree(0).cell(0, 0));
		
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Machine");
		List<String> children = item.getNodes();
		assertEquals(Arrays.asList("Current"), children);
		
		assertEquals("Current",   item.cell(0, 0));
		assertEquals("5.0    mA", item.cell(0, 1));	
	}
	@Ignore("Cannot get the click to work...")
	@Test
	public void checkValuesTree4() throws Exception {
		
		ControlTree ct = getControlTree("control_tree4.xml");
		bot.getDisplay().syncExec(()->viewer.setControlTree(ct));
		
		assertEquals(2, bot.tree(0).columnCount());
		assertEquals(1, bot.tree(0).rowCount());
		
		assertEquals("Hutch", bot.tree(0).cell(0, 0));
		
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Hutch");
		List<String> children = item.getNodes();
		assertEquals(Arrays.asList("Port Shutter"), children);
		
		assertEquals("Port Shutter",   item.cell(0, 0));
		assertEquals("Open",           item.cell(0, 1));
		
		SWTBotTreeItem node = item.getNode("Port Shutter");
		node.click(1); // Cannot get the click to work...

		SWTBotCCombo combo = bot.ccomboBox(0);
		combo.setSelection(1); // Closed
		
		bot.getDisplay().syncExec(()->viewer.applyEditorValue());
		
		assertEquals("Closed", item.cell(0, 1));

		node.click(1);
		combo = bot.ccomboBox(0);
		combo.setSelection(0); // Open
		
		bot.getDisplay().syncExec(()->viewer.applyEditorValue());
		
		assertEquals("Open", item.cell(0, 1));

	}
	
	@Test
	public void checkSetStageXValue() throws Exception {
				
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Translations");
		assertEquals("Stage X",   item.cell(0, 0));

		SWTBotTreeItem node = item.getNode("Stage X");
	   
		node.click(1);
	    setEditorValue("10.0");
		assertEquals("10.0    mm", item.cell(0, 1));	
		
	    node.click(1);
	    setEditorValue("0.0");
		assertEquals("0.0    mm", item.cell(0, 1));	
	}
	
	@Test
	public void checkSetTemperatureValue() throws Exception {
				
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Experimental Conditions");
		assertEquals("Temperature",   item.cell(0, 0));

		SWTBotTreeItem node = item.getNode("Temperature");
	   
		node.click(1);
	    setEditorValue("290.0");
		assertEquals("290.0    K", item.cell(0, 1));	
		
	    node.click(1);
	    setEditorValue("295.0");
		assertEquals("295.0    K", item.cell(0, 1));	
	}
	
	@Ignore("Travis does not like this one, rather a shame that")
	@Test
	public void addANumericScannable() throws Exception {
				
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Experimental Conditions");
	    item.select();
	    
		bot.getDisplay().syncExec(()->viewer.addNode());
    
		SWTBotCCombo combo = bot.ccomboBox(0);
		assertNotNull(combo);
		combo.setSelection("a");
		
		assertEquals("a",   item.cell(1, 0));
		assertEquals("10.0    mm", item.cell(1, 1));

	}

	@Ignore("Travis does not like this one, rather a shame that")
	@Test
	public void addAStringScannable() throws Exception {
				
	    SWTBotTreeItem item = bot.tree(0).getTreeItem("Experimental Conditions");
	    item.select();
	    
		bot.getDisplay().syncExec(()->viewer.addNode());
    
		SWTBotCCombo combo = bot.ccomboBox(0);
		assertNotNull(combo);
		combo.setSelection("portshutter");
		
		assertEquals("portshutter",   item.cell(1, 0));
		assertEquals("Open", item.cell(1, 1));

	}

	/**
	 * Bit of funny logic for setting value because the tree editor
	 * requires the user to press enter in order to take the value.
	 * 
	 * @param string
	 * @throws InterruptedException 
	 */
    private void setEditorValue(String value) throws InterruptedException {
    	
	    bot.text(0).setText(value);
	    bot.getDisplay().syncExec(()->{
	    	bot.text(0).widget.traverse(SWT.TRAVERSE_RETURN);
	    	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	viewer.applyEditorValue();
	    });
	    Thread.sleep(200); // Wait briefly for mock motor to simulate moving to this position.
	}
    
	@Test
	public void checkSettingScannableValue() throws Exception {
		
		Services.getConnector().getScannable("stage_x").setPosition(1.0d);
		Services.getConnector().getScannable("stage_y").setPosition(2.0d);
		Thread.sleep(500);
		
		try {
		
		    SWTBotTreeItem item = bot.tree(0).getTreeItem("Translations");
			List<String> children = item.getNodes();
			assertEquals(Arrays.asList("Stage X", "Stage Y", "Stage Z"), children);
			
			assertEquals("Stage X",   item.cell(0, 0));
			assertEquals("1.0    mm", item.cell(0, 1));	
			assertEquals("Stage Y",   item.cell(1, 0));
			assertEquals("2.0    mm", item.cell(1, 1));
			
		} finally {
			
			Services.getConnector().getScannable("stage_x").setPosition(0.0d);
			Services.getConnector().getScannable("stage_y").setPosition(0.0d);
			Thread.sleep(500);

		}
	}


	/**
     * 
     * @param path
     * @throws Exception 
     * @throws UnsupportedOperationException 
     */
	public ControlTree getControlTree(String name) throws UnsupportedOperationException, Exception {
		InputStream in = getClass().getResourceAsStream(name);
		ISpringParser parser = ServiceHolder.getSpringParser();
		Map<String, Object> map = parser.parse(in);
		if (!map.isEmpty()) return (ControlTree)map.get("Control_Factory");
		return null;
	}

}
