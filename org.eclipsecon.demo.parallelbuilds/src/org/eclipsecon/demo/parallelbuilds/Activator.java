package org.eclipsecon.demo.parallelbuilds;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public Activator() {
	}

	private IWorkspace workspace;
	private static Activator INSTANCE;

	public static Activator getPlugin() {
		return INSTANCE;
	}

	@Override public void start(BundleContext context) throws Exception {
		INSTANCE = this;
		super.start(context);
		workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(LogBuildsListener.SINGLETON, IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		Job.getJobManager().addJobChangeListener(LogBuildsListener.SINGLETON);
	}

	@Override public void stop(BundleContext context) throws Exception {
		workspace.removeResourceChangeListener(LogBuildsListener.SINGLETON);
		Job.getJobManager().removeJobChangeListener(LogBuildsListener.SINGLETON);
		super.stop(context);
	}
}
