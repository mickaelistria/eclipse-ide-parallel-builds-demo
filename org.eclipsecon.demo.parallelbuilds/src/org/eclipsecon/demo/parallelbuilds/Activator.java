package org.eclipsecon.demo.parallelbuilds;

import java.io.PrintStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.debug.DebugOptions;
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
		// track workspace builds
		workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(LogBuildsListener.SINGLETON, IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.POST_BUILD);
		Job.getJobManager().addJobChangeListener(LogBuildsListener.SINGLETON);
		// enable tracking JDT Builder execution
		DebugOptions options = context.getService(context.getServiceReference(DebugOptions.class));
		options.setOption("org.eclipse.jdt.core/debug", Boolean.toString(true));
		options.setOption("org.eclipse.jdt.core/debug/builder", Boolean.toString(true));
		System.setOut(new PrintStream(System.out) {
			@Override public void println(String x) {
				if (x.startsWith("\nJavaBuilder: Starting build of ")) {
					String projectName = x.substring("\nJavaBuilder: Starting build of ".length());
					projectName = projectName.substring(0, projectName.indexOf('@'));
					projectName = projectName.substring(0, projectName.length() - 1);
					IProject project = workspace.getRoot().getProject(projectName);
					LogBuildsListener.SINGLETON.preBuild(project);
				} else if (x.startsWith("JavaBuilder: Finished build of ")) {
					String projectName = x.substring("JavaBuilder: Finished build of ".length());
					projectName = projectName.substring(0, projectName.indexOf('@'));
					projectName = projectName.substring(0, projectName.length() - 1);
					IProject project = workspace.getRoot().getProject(projectName);
					LogBuildsListener.SINGLETON.postBuild(project);
				}
				super.println(x);
			}
		});
	}

	@Override public void stop(BundleContext context) throws Exception {
		workspace.removeResourceChangeListener(LogBuildsListener.SINGLETON);
		Job.getJobManager().removeJobChangeListener(LogBuildsListener.SINGLETON);
		super.stop(context);
	}
}
