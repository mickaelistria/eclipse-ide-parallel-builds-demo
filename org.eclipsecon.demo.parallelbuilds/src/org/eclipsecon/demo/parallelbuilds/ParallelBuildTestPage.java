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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class ParallelBuildTestPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ParallelBuildTestPage() {
		super(FieldEditorPreferencePage.FLAT);
	}

	@Override public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getPlugin().getPreferenceStore());
	}

	@Override protected void createFieldEditors() {
		BooleanFieldEditor jdtPref = new BooleanFieldEditor("useNullSchedulingRule", "JDT null scheduling rule", getFieldEditorParent()) {
			@Override public void setPreferenceStore(IPreferenceStore store) {
				if (getPreferenceStore() == null || store == null) {
					super.setPreferenceStore(store);
				}
			}
		};
		jdtPref.setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.jdt.core"));
		addField(jdtPref);

		BooleanFieldEditor m2ePref = new BooleanFieldEditor("builderUsesNullSchedulingRule", "M2E null scheduling rule", getFieldEditorParent()) {
			@Override public void setPreferenceStore(IPreferenceStore store) {
				if (getPreferenceStore() == null || store == null) {
					super.setPreferenceStore(store);
				}
			}
		};
		m2ePref.setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.m2e.core"));
		addField(m2ePref);

		Composite fieldEditorParent = getFieldEditorParent();
		GridDataFactory.fillDefaults().indent(0, 80).applyTo(fieldEditorParent);
		BooleanFieldEditor generateGraphPref = new BooleanFieldEditor(GenerateDependencyGraph.PREF_ID, "Generate dependency graph before build", fieldEditorParent);
		addField(generateGraphPref);

		BooleanFieldEditor generateGantt = new BooleanFieldEditor(LogBuildsListener.PREF_GENERATE_GANTT, "Generate Gantt chart (shows scheduling info and JDT build duration)", getFieldEditorParent());
		addField(generateGantt);

		BooleanFieldEditor generateHash = new BooleanFieldEditor(LogBuildsListener.PREF_GENERATE_HASH, "Generate report with signature for all workspace files", getFieldEditorParent());
		addField(generateHash);
	}

}
