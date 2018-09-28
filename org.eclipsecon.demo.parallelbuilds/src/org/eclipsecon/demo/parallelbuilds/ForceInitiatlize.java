/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipsecon.demo.parallelbuilds;

import org.eclipse.ui.IStartup;

public class ForceInitiatlize implements IStartup {

	@Override public void earlyStartup() {
		// Just forcing plugin to initialize and workspace listener to be added
	}

}
