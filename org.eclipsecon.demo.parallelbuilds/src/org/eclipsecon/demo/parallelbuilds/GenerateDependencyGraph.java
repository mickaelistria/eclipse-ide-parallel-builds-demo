package org.eclipsecon.demo.parallelbuilds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class GenerateDependencyGraph {

	public static File getPngGraphDependency() {
		StringBuilder dotGraph = new StringBuilder();
		dotGraph.append("digraph dependencyGraph {\n");
		dotGraph.append("rankdir=RL;");
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : allProjects) {
			String colorString = "\"#" + String.format("%06X",LogBuildsListener.colorForProject(project).getRGB() & 0xffffff) + "\"";
			dotGraph.append(project.getName() + "[color=" + colorString + " penwidth=3];\n");
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
				dotGraph.append(project.getName());
				dotGraph.append("; ");
			});
		dotGraph.append("}\n");
		// dependnencies
		for (IProject project : allProjects) {
			try {
				for (IProject referencedProject : project.getReferencedProjects()) {
					dotGraph.append(project.getName() + " -> " + referencedProject.getName() + ";\n");
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		// conflicting scheduling rule
		for (int i = 0; i < allProjects.length; i++) {
			IProject project = allProjects[i];
			ISchedulingRule rule = WaitProjectBuilder.getSchedulingRule(project);
			for (int j = i + 1; j < allProjects.length; j++) {
				IProject other = allProjects[j];;
				if (rule.isConflicting(WaitProjectBuilder.getSchedulingRule(other))) {
					dotGraph.append(project.getName() + " -> " + other.getName() + "[dir=none color=red style=dashed];\n");
				}
			}
		}
		dotGraph.append("}");


		File dotFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), "graph" + System.currentTimeMillis() + ".dot");
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
