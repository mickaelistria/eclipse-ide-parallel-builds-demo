package org.eclipsecon.demo.parallelbuilds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class GenerateDependencyGraph {

	public static final String PREF_ID = "generateDependencyGraph";

	public static File buildPngDependencyGraph(File parentDirectory) {
		if (!parentDirectory.exists()) {
			parentDirectory.mkdirs();
		} else if (!parentDirectory.isDirectory()) {
			throw new IllegalArgumentException("expected a directory");
		}
		StringBuilder dotGraph = new StringBuilder();
		dotGraph.append("digraph dependencyGraph {\n");
		dotGraph.append("rankdir=RL;");
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : allProjects) {
			String colorString = "\"#" + String.format("%06X",LogBuildsListener.colorForProject(project).getRGB() & 0xffffff) + "\"";
			dotGraph.append('"' + project.getName() + "\" [color=" + colorString + " penwidth=3];\n");
		}
		dotGraph.append("{rank = same; ");
		Arrays.stream(allProjects)
			.filter(project -> {
				try {
					return project.getReferencedProjects().length == 0;
				} catch (CoreException e1) {
					e1.printStackTrace();
					return false;
				}
			})
			.forEach(project -> {
				dotGraph.append('"' + project.getName() + '"');
				dotGraph.append("; ");
			});
		dotGraph.append("}\n");
		// dependnencies
		for (IProject project : allProjects) {
			try {
				for (IProject referencedProject : project.getReferencedProjects()) {
					dotGraph.append('"' + project.getName() + "\" -> \"" + referencedProject.getName() + "\";\n");
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		// conflicting scheduling rule
		Map<IProject, ISchedulingRule> rulesForProject = new LinkedHashMap<>(allProjects.length);
		BuildManager manager = null;
		try {
			Field field = Workspace.class.getDeclaredField("buildManager");
			field.setAccessible(true);
			manager = (BuildManager) field.get(ResourcesPlugin.getWorkspace());
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
			return null;
		}
		for (IProject project : allProjects) {
			if (!project.isAccessible()) {
				continue;
			}
			try {
				for (ICommand command : project.getDescription().getBuildSpec()) {
					rulesForProject.put(project,
							manager.getRule(project.getActiveBuildConfig(), IncrementalProjectBuilder.FULL_BUILD, command.getBuilderName(), Collections.emptyMap()));
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		for (int i = 0; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			ISchedulingRule rule = rulesForProject.get(project);
			if (rule != null) {
				for (int j = i + 1; j < allProjects.length; j++) {
					IProject other = allProjects[j];
					ISchedulingRule otherRule = rulesForProject.get(other);
					if (otherRule != null) {
						if (rule.isConflicting(otherRule)) {
							dotGraph.append(project.getName() + " -> " + other.getName() + "[dir=none color=red style=dashed];\n");
						}
					}
				}
			}
		}
		dotGraph.append("}");


		File dotFile = new File(parentDirectory, "graph" + System.currentTimeMillis() + ".dot");
		try (FileOutputStream outputStream = new FileOutputStream(dotFile)) {
			outputStream.write(dotGraph.toString().getBytes(), 0, dotGraph.length());
			File pngFile = new File(dotFile.getParentFile(), dotFile.getName() + ".png");
			new ProcessBuilder("dot", "-Tpng", "-O" + pngFile.getAbsolutePath(), dotFile.getAbsolutePath()).start().waitFor();
			return pngFile;
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
