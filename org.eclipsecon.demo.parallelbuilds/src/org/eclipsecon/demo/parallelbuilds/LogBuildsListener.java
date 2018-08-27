package org.eclipsecon.demo.parallelbuilds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;

public class LogBuildsListener extends JobChangeAdapter implements IResourceChangeListener {

	public static final LogBuildsListener SINGLETON = new LogBuildsListener();

	private static final int LINE_HEIGHT = 40;
	private static final float RATIO = (float)0.1; // 1 pixel == 10ms;
	private static final int PROJECT_NAME_COLUMN_WIDTH = 40;

	private static final Color[] COLORS = new Color[] {
		Color.BLUE,
		Color.GRAY,
		Color.MAGENTA,
		Color.ORANGE,
		Color.GREEN,
		Color.PINK,
		Color.YELLOW
	};

	private BufferedImage bImg;
	private Graphics2D cg;
	private List<IProject> allProjects;
	private long originTime;

	private Map<IProject, Long> jobStartTimes;
	private Map<IProject, Long> jobScheduledTimes;
	private Map<IProject, Long> builderStartTimes;
	private Map<IProject, Long> builderEndTimes;

	private LogBuildsListener() {
	}

	@Override public void resourceChanged(IResourceChangeEvent event) {
		if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD || event.getBuildKind() == IncrementalProjectBuilder.AUTO_BUILD) {
			return;
		}
		if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
			workspaceBuildStart();
		} else if (event.getType() == IResourceChangeEvent.POST_BUILD) {
			workspaceBuildComplete();
		}
	}

	private void workspaceBuildStart() {
		Display.getDefault().asyncExec(() -> {
			try {
				File pngGraphDependency = GenerateDependencyGraph.getPngGraphDependency();
				IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR, pngGraphDependency.getName(), pngGraphDependency.getName(), pngGraphDependency.getName());
				browser.openURL(pngGraphDependency.toURI().toURL());
			} catch (PartInitException | MalformedURLException e) {
				e.printStackTrace();
			}
		});

		originTime = System.currentTimeMillis();
		allProjects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		builderStartTimes = Collections.synchronizedMap(new HashMap<>(allProjects.size()));
		builderEndTimes = Collections.synchronizedMap(new HashMap<>(allProjects.size()));
		jobStartTimes = Collections.synchronizedMap(new HashMap<>(allProjects.size()));
		jobScheduledTimes = Collections.synchronizedMap(new HashMap<>(allProjects.size()));
	}

	public void preBuild(IProject project) {
		if (builderStartTimes != null) {
			builderStartTimes.put(project, System.currentTimeMillis());
		}
	}

	public void postBuild(IProject project) {
		builderEndTimes.put(project, System.currentTimeMillis());
	}

	private void createImageAndDrawCaption() {
		long finalTime = System.currentTimeMillis();
		int width = (int) ((finalTime - originTime) * RATIO);
		bImg = new BufferedImage(PROJECT_NAME_COLUMN_WIDTH + width + 100, yForProject(allProjects.get(allProjects.size() - 1)) + LINE_HEIGHT, ColorSpace.TYPE_RGB);
		cg = bImg.createGraphics();
		cg.setBackground(Color.WHITE);
		cg.setColor(Color.BLACK);
		cg.clearRect(0, 0, bImg.getWidth(), bImg.getHeight());
		int y = 15;
		// total duration
		cg.drawString("total duration=" + (finalTime - originTime)/1000 + "s", 5, y);
		y += LINE_HEIGHT;
		// draw legend
		cg.drawOval(5, y, LINE_HEIGHT / 2, LINE_HEIGHT / 2);
		cg.drawString("Job scheduled", 5 + LINE_HEIGHT/2 + 5, y + LINE_HEIGHT / 2);
		y += LINE_HEIGHT;
		cg.drawPolygon(createLosange(10, y));
		cg.drawString("Job start", 5 + LINE_HEIGHT/2 + 5, y + LINE_HEIGHT / 2);
		y += LINE_HEIGHT;
		cg.fillRect(5, y, LINE_HEIGHT, LINE_HEIGHT / 2);
		cg.drawString("Build running", 5 + LINE_HEIGHT + 5, y + LINE_HEIGHT / 2);
		y += LINE_HEIGHT;
		cg.drawLine(5, y, 105, y);
		cg.drawString("100px = " + 100/RATIO + "ms", 110, y);
		// project name colum
		allProjects.forEach(p -> {
			cg.setColor(colorForProject(p));
			cg.drawString(p.getName(), 2, yForProject(p) + 15);
		});
	}

	private void workspaceBuildComplete() {
		createImageAndDrawCaption();
		for (Entry<IProject, Long> entry : builderStartTimes.entrySet()) {
			Color colorForProject = colorForProject(entry.getKey());
			int projectStartX = xForTime(builderStartTimes.get(entry.getKey()));
			int projectDX = xForTime(builderEndTimes.get(entry.getKey())) - projectStartX;
			cg.setColor(colorForProject);
			cg.fillRect(projectStartX, yForProject(entry.getKey()), projectDX, LINE_HEIGHT / 2);
		}
		for (Entry<IProject, Long> entry : jobStartTimes.entrySet()) {
			drawBuildJobStart(entry.getKey(), entry.getValue());
		}
		for (Entry<IProject, Long> entry : jobScheduledTimes.entrySet()) {
			drawScheduled(entry.getKey(), entry.getValue());
		}
		File targetFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), originTime + "-gantt.png");
		try (OutputStream output = new FileOutputStream(targetFile)) {
			ImageIO.write(bImg, "png", output);
			cg.dispose();
			Display.getDefault().asyncExec(() -> {
				try {
					IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR, targetFile.getName(), targetFile.getName(), targetFile.getName());
					browser.openURL(targetFile.toURI().toURL());
				} catch (PartInitException | MalformedURLException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		SortedMap<IPath, byte[]> map = this.contentHashDigest.apply(ResourcesPlugin.getWorkspace());
		File hashOutput = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), originTime + "-hash.txt");
		try (OutputStream outputStream = new FileOutputStream(hashOutput)) {
			for (Entry<IPath, byte[]> hash : map.entrySet()) {
				outputStream.write(hash.getKey().toString().getBytes());
				outputStream.write(' ');
				for (byte b : hash.getValue()) {
					outputStream.write(String.format("%02x",b).getBytes());
				}
				outputStream.write('\n');
			}
			Display.getDefault().asyncExec(() -> {
				try {
					IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), EFS.getStore(hashOutput.toURI()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int yForProject(IProject p) {
		return 6 * LINE_HEIGHT + allProjects.indexOf(p) * LINE_HEIGHT;
	}

	private int xForTime(long timestamp) {
		long deltaToOrigin = timestamp - originTime;
		return (int) (PROJECT_NAME_COLUMN_WIDTH + deltaToOrigin * RATIO);
	}

	public static Color colorForProject(IProject p) {
		int index = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).indexOf(p);
		return COLORS[index % COLORS.length];
	}

	@Override public void scheduled(IJobChangeEvent event) {
		if (isProjectBuildJob(event.getJob())) {
			jobScheduledTimes.put(getProject(event), System.currentTimeMillis());
		}
	}

	private void drawScheduled(IProject project, long scheduledTime) {
		int x = xForTime(scheduledTime);
		int y = yForProject(project);
		Color colorForProject = colorForProject(project);
		synchronized (cg) {
			cg.setColor(colorForProject);
			cg.drawOval(x - LINE_HEIGHT / 4, y , LINE_HEIGHT / 2, LINE_HEIGHT / 2);
		}
	}

	@Override public void aboutToRun(IJobChangeEvent event) {
		if (isProjectBuildJob(event.getJob())) {
			jobStartTimes.put(getProject(event), System.currentTimeMillis());
		}
	}

	protected void drawBuildJobStart(IProject project, long jobStartTime) {
		int x = xForTime(jobStartTime);
		int y = yForProject(project);
		Color colorForProject = colorForProject(project);
		Polygon polygon = createLosange(x, y);
		cg.setColor(colorForProject);
		cg.drawPolygon(polygon);
	}

	protected Polygon createLosange(int centerX, int topY) {
		Polygon polygon = new Polygon();
		polygon.addPoint(centerX - LINE_HEIGHT / 4, topY + LINE_HEIGHT / 4);
		polygon.addPoint(centerX, topY);
		polygon.addPoint(centerX + LINE_HEIGHT / 4, topY + LINE_HEIGHT / 4);
		polygon.addPoint(centerX, topY + LINE_HEIGHT / 2);
		return polygon;
	}

	protected IProject getProject(IJobChangeEvent event) {
		IProject project = null;
		String buildConfigToString = event.getJob().getName();
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
				if (p.isAccessible() && p.getActiveBuildConfig().toString().equals(buildConfigToString)) {
					project = p;
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return project;
	}

	private boolean isProjectBuildJob(Job job) {
		return job.getJobGroup() != null && job.getJobGroup().getName().equals(Workspace.class.getName().toString());
	}

	private final static Function<IWorkspace, SortedMap<IPath, byte[]>> contentHashDigest = new Function<IWorkspace, SortedMap<IPath, byte[]>>() {

		private byte[] hashCodeFileContent(IFile file) {
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				try (InputStream is = file.getContents();
				     DigestInputStream dis = new DigestInputStream(is, md))
				{
					while (dis.read() >= 0) {}
				}
				return md.digest();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public SortedMap<IPath, byte[]> apply(IWorkspace workspace) {
			SortedMap<IPath, byte[]> res = Collections.synchronizedSortedMap(new TreeMap<>((path1, path2) -> path1.toString().compareTo(path2.toString())));
			try {
				Set<IProject> rootProjects = new HashSet<>();
				SortedSet<IProject> projects = new TreeSet<IProject>(Comparator.comparing(p -> p.getLocation().toFile().getAbsolutePath()));
				projects.addAll(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
				Iterator<IProject> iterator = projects.iterator();
				IProject current = iterator.next();
				rootProjects.add(current);
				while (iterator.hasNext()) {
					IProject p = iterator.next();
					if (!current.getLocation().isPrefixOf(p.getLocation())) {
						current = p;
						rootProjects.add(p);
					}
				}
				rootProjects.forEach(rootProject -> {
					try {
						rootProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						rootProject.accept(resource -> {
							if (resource.getType() == IResource.FILE) {
								res.put(resource.getFullPath(), hashCodeFileContent((IFile)resource));
							}
							return true;
						});
					} catch (CoreException e) {
						throw new RuntimeException(e);
					}
				});
				return res;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	};
}
