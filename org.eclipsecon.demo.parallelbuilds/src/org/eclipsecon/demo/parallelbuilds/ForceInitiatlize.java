package org.eclipsecon.demo.parallelbuilds;

import org.eclipse.ui.IStartup;

public class ForceInitiatlize implements IStartup {

	@Override public void earlyStartup() {
		// Just forcing plugin to initialize and workspace listener to be added
	}

}
