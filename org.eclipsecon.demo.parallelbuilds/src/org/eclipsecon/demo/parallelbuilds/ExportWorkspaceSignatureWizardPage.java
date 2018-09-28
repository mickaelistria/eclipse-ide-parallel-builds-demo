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

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ExportWorkspaceSignatureWizardPage extends WizardPage implements IWizardPage {

	private Text signatureText;
	private Text problemsText;

	protected ExportWorkspaceSignatureWizardPage() {
		super(ExportWorkspaceSignatureWizardPage.class.getName());
	}

	@Override public void createControl(Composite parent) {
		Composite res = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		long timestamp = System.currentTimeMillis();
		new Label(res, SWT.NONE).setText("Signatures file");
		signatureText = new Text(res, SWT.NONE);
		signatureText.setText(new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), "signatures-" + timestamp + ".txt").getAbsolutePath());
		new Label(res, SWT.NONE).setText("Signatures file");
		problemsText = new Text(res, SWT.NONE);
		problemsText.setText(new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), "markets-" + timestamp + ".txt").getAbsolutePath());
		setControl(res);
	}

	File getTargetSignaturesFile() {
		return new File(signatureText.getText());
	}

	File getTargetMarkersFile() {
		return new File(problemsText.getText());
	}

}
