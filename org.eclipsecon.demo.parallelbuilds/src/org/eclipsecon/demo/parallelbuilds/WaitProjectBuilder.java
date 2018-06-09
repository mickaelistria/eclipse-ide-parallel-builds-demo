package org.eclipsecon.demo.parallelbuilds;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class WaitProjectBuilder extends IncrementalProjectBuilder {

	private static final IProject[] NO_PROJECT = new IProject[0];

	public WaitProjectBuilder() {
	}

	@Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		java.util.Properties p = getProperties(getProject());
		int duration = Integer.parseInt(p.getProperty("duration", "1000"));
		LogBuildsListener.SINGLETON.preBuild(getProject());
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LogBuildsListener.SINGLETON.postBuild(getProject());
		return NO_PROJECT;
	}

	@Override public ISchedulingRule getRule(int kind, Map<String, String> args) {
		IProject project = getProject();
		Properties p = getProperties(project);
		String rule = p.getProperty("schedulingRule");
		if (rule == null) {
			return null;
		} else if ("/".equals(rule)) {
			return project.getWorkspace().getRoot();
		} else if ("this".equals(rule)) {
			return project;
		} else {
			return project.getWorkspace().getRoot().getProject(rule);
		}
	}

	private static Properties getProperties(IProject project) {
		Properties res = new Properties();
		IFile file = project.getFile("waitBuilder.properties");
		if (file.isAccessible()) {
			try (InputStream inStream = file.getContents()) {
				res.load(inStream);
			} catch (IOException | CoreException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

}
