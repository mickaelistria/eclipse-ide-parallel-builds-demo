package org.eclipsecon.demo.parallelbuilds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

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
	private Map<IProject, Long> startTimes;
	private long startTime;

	private LogBuildsListener() {
	}

	@Override public void resourceChanged(IResourceChangeEvent event) {
		if (event.getBuildKind() == IncrementalProjectBuilder.AUTO_BUILD) {
			return;
		}
		if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
			workspaceBuildStart();
		} else if (event.getType() == IResourceChangeEvent.POST_BUILD) {
			workspaceBuildComplete();
		}
	}

	private void workspaceBuildStart() {
		File pngGraphDependency = GenerateDependencyGraph.getPngGraphDependency();
		Display.getDefault().asyncExec(() -> {
			try {
				IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR, pngGraphDependency.getName(), pngGraphDependency.getName(), pngGraphDependency.getName());
				browser.openURL(pngGraphDependency.toURI().toURL());
			} catch (PartInitException | MalformedURLException e) {
				e.printStackTrace();
			}
		});

		startTime = System.currentTimeMillis();
		allProjects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		startTimes = Collections.synchronizedMap(new HashMap<>(allProjects.size()));
		bImg = new BufferedImage(PROJECT_NAME_COLUMN_WIDTH + allProjects.size() * 100 + 100, yForProject(allProjects.get(allProjects.size() - 1)) + LINE_HEIGHT, ColorSpace.TYPE_RGB);
		cg = bImg.createGraphics();
		cg.setBackground(Color.WHITE);
		cg.setColor(Color.BLACK);
		cg.clearRect(0, 0, bImg.getWidth(), bImg.getHeight());
		// draw legend
		int y = 15;
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
		File targetFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), System.currentTimeMillis() + ".png");
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
	}

	public void preBuild(IProject project) {
		startTimes.put(project, System.currentTimeMillis());
	}

	public void postBuild(IProject project) {
		Color colorForProject = colorForProject(project);
		int projectStartX = xForTime(startTimes.get(project));
		int projectDX = xForTime(System.currentTimeMillis()) - projectStartX;
		synchronized (cg) {
			cg.setColor(colorForProject);
			cg.fillRect(projectStartX, yForProject(project), projectDX, LINE_HEIGHT / 2);
		}
	}

	private int yForProject(IProject p) {
		return 5 * LINE_HEIGHT + allProjects.indexOf(p) * LINE_HEIGHT;
	}

	private int xForTime(long timestamp) {
		long deltaToOrigin = timestamp - startTime;
		return (int) (PROJECT_NAME_COLUMN_WIDTH + deltaToOrigin * RATIO);
	}

	public static Color colorForProject(IProject p) {
		int index = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()).indexOf(p);
		return COLORS[index];
	}

	@Override public void scheduled(IJobChangeEvent event) {
		if (isProjectBuildJob(event.getJob())) {
			IProject project = getProject(event);
			int x = xForTime(System.currentTimeMillis());
			int y = yForProject(project);
			Color colorForProject = colorForProject(project);
			synchronized (cg) {
				cg.setColor(colorForProject);
				cg.drawOval(x - LINE_HEIGHT / 4, y , LINE_HEIGHT / 2, LINE_HEIGHT / 2);
			}
		}
	}

	@Override public void aboutToRun(IJobChangeEvent event) {
		if (isProjectBuildJob(event.getJob())) {
			IProject project = getProject(event);
			int x = xForTime(System.currentTimeMillis());
			int y = yForProject(project);
			Color colorForProject = colorForProject(project);
			Polygon polygon = createLosange(x, y);
			synchronized (cg) {
				cg.setColor(colorForProject);
				cg.drawPolygon(polygon);
			}
		}
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
				if (p.getActiveBuildConfig().toString().equals(buildConfigToString)) {
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
}
