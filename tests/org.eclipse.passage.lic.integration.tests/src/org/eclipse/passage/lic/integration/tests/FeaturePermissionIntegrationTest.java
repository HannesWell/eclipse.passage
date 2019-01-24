/*******************************************************************************
 * Copyright (c) 2018-2019 ArSysOp
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ArSysOp - initial API and implementation
 *******************************************************************************/
package org.eclipse.passage.lic.integration.tests;

import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.eclipse.passage.lic.runtime.FeaturePermission;
import org.junit.Test;

public class FeaturePermissionIntegrationTest extends LicIntegrationBase {

	@Test
	public void testEvaluateConditionsNegative() {
		Iterable<FeaturePermission> permissionsNull = accessManager.evaluateConditions(null, null);
		assertFalse(permissionsNull.iterator().hasNext());

		Iterable<FeaturePermission> permissionsEmpty = accessManager.evaluateConditions(Collections.emptyList(), null);
		assertFalse(permissionsEmpty.iterator().hasNext());

	}

}
