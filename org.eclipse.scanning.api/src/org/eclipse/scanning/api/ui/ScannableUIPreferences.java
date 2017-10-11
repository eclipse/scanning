/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.api.ui;

import org.eclipse.scanning.api.INameable;

/**
 * Default values in UI elements are invariably arbitrary. Use this class to tweak these for a particular scannable in Spring.
 * Fields not configured will revert to default values.
 * <p>
 * Spring will register this object with IScannableUIPreferencesService.DEFAULT; UI elements can therefore call e.g.
 * IScannableUIPreferencesService.DEFAULT.getPreferences(scannableName).getNudgeValue()
 * <p>
 * Note: the name must match the name of the scannable.
 * <p>Example configuration:
 * <pre>
{@literal		<bean id="stage1_z_uiPrefs" class="org.eclipse.scanning.api.ui.ScannableUIPreferences" init-method="register" >}
{@literal			<property name="name" value="stage1_z" />}
{@literal			<property name="nudgeValue" value="0.05" />}
{@literal		</bean>}

{@literal		<bean id="dcm_enrg_uiPrefs" class="org.eclipse.scanning.api.ui.ScannableUIPreferences" init-method="register" >}
{@literal			<property name="name" value="dcm_enrg" />}
{@literal			<property name="stepModelStart" value="5.0" />}
{@literal			<property name="stepModelWidth" value="15.0" />}
{@literal			<property name="stepModelStep" value="0.001" />}
{@literal		</bean>}
   </pre>
 */
public class ScannableUIPreferences implements INameable {

	// DEFAULT VALUES
	private static final double DEFAULT_NUDGE = 0.1;
	private static final double DEFAULT_START = 10.0;
	private static final double DEFAULT_WIDTH = 10.0;
	private static final double DEFAULT_STEP = 1.0;

	private String name;
	private double nudgeValue;
	private double stepModelStart;
	private double stepModelWidth;
	private double stepModelStep;

	public ScannableUIPreferences() {
		loadDefaults();
	}

	private void loadDefaults() {
		setName("Default Scannable UI preferences");
		setNudgeValue(DEFAULT_NUDGE);
		setStepModelStart(DEFAULT_START);
		setStepModelWidth(DEFAULT_WIDTH);
		setStepModelStep(DEFAULT_STEP);
	}

	/**
	 * init-method for Spring
	 */
	public void register() {
		IScannableUIPreferencesService.DEFAULT.register(this);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name must match scannable name as configured in Spring
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * A sensible value to move scannable by with a single click
	 * Used in e.g. ControlTreeViewer to display ControlNodes with increments
	 * @param nudgeValue
	 */
	public void setNudgeValue(double nudgeValue) {
		this.nudgeValue = nudgeValue;
	}

	public double getNudgeValue() {
		return nudgeValue;
	}

	/**
	 * Used by MultiStepComposite to add default StepModel values
	 * @param stepModelStart
	 */
	public double getStepModelStart() {
		return stepModelStart;
	}

	public void setStepModelStart(double stepModelStart) {
		this.stepModelStart = stepModelStart;
	}

	/**
	 * Used by MultiStepComposite to add default StepModel values
	 * @param stepModelWidth i.e. stop = stepModelStart + stepModelWidth
	 */
	public double getStepModelWidth() {
		return stepModelWidth;
	}

	public void setStepModelWidth(double stepModelWidth) {
		this.stepModelWidth = stepModelWidth;
	}

	/**
	 * Used by MultiStepComposite to add default StepModel values
	 * @param stepModelStep
	 */
	public double getStepModelStep() {
		return stepModelStep;
	}

	public void setStepModelStep(double stepModelStep) {
		this.stepModelStep = stepModelStep;
	}

}
