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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ExportWorkspaceSignatureWizard extends Wizard implements IExportWizard {

	private ExportWorkspaceSignatureWizardPage page;

	public ExportWorkspaceSignatureWizard() {
		// TODO Auto-generated constructor stub
	}

	@Override public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override public void addPages() {
		page = new ExportWorkspaceSignatureWizardPage();
		addPage(page);
	}

	@Override public boolean performFinish() {
		LogBuildsListener.generateWorkspaceContentSignature(page.getTargetSignaturesFile(), page.getTargetMarkersFile());
		return true;
	}

}
