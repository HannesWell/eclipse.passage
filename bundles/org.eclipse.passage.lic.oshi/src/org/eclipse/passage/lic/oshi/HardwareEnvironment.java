/*******************************************************************************
 * Copyright (c) 2020, 2022 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ArSysOp - initial API and implementation
 *     IILS mbH (Hannes Wellmann) - rewrite to use OSHI 6 and to compute properties lazy
 *******************************************************************************/
package org.eclipse.passage.lic.oshi;

import java.util.Objects;

import org.eclipse.passage.lic.api.EvaluationType;
import org.eclipse.passage.lic.api.LicensingException;
import org.eclipse.passage.lic.api.inspection.EnvironmentProperty;
import org.eclipse.passage.lic.api.inspection.RuntimeEnvironment;

/**
 * @since 1.1
 */
public final class HardwareEnvironment implements RuntimeEnvironment {

	static final EvaluationType TYPE = new EvaluationType.Hardware();

	private State state;

	@Override
	public EvaluationType id() {
		return TYPE;
	}

	private synchronized State getState() {
		if (state == null) { // reuse state instance to cache static values
			state = new State();
		}
		return state;
	}

	@Override
	public boolean isAssuptionTrue(EnvironmentProperty property, String assumption) throws LicensingException {
		Objects.requireNonNull(property, "HardwareEnvironment::isAssuptionTrue::property"); //$NON-NLS-1$
		Objects.requireNonNull(assumption, "HardwareEnvironment::isAssuptionTrue::assumption"); //$NON-NLS-1$
		return getState().hasMatchingValue(property, assumption);
	}

	@Override
	public String state() throws LicensingException {
		return getState().getGlance();
	}

}
