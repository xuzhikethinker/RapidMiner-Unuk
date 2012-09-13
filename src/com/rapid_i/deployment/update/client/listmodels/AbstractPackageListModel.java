/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2012 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapid_i.deployment.update.client.listmodels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import com.rapid_i.deployment.update.client.PackageDescriptorCache;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;

/**
 * Abstract Class for PackageListModels as used in the Rapidminer Update Dialog.
 * @author Dominik Halfkann
 *
 */
public abstract class AbstractPackageListModel extends AbstractListModel {

	private static final long serialVersionUID = 1L;

	protected PackageDescriptorCache cache;
	
	protected boolean updatedOnce = false;

	protected boolean fetching = false;
	protected int completed = 0;

	protected List<String> packageNames = new ArrayList<String>();
	
	public AbstractPackageListModel(PackageDescriptorCache cache) {
		this.cache = cache;
	}
	
	public synchronized void update() {
		if (shouldUpdate()) {
			fetching = true;
			new ProgressThread("fetching_updates", false) {
				@Override
				public void run() {
					try {
						getProgressListener().setTotal(100);
						setCompleted(getProgressListener(), 5);
						packageNames = fetchPackageNames();
						setCompleted(getProgressListener(), 25);
	
						int a = 0;
						Iterator<String> it = packageNames.iterator();
						int size = packageNames.size();
						while(it.hasNext()) {
							String packageName = it.next();
							PackageDescriptor desc = cache.getPackageInfo(packageName, "ANY");
							a++;
							setCompleted(getProgressListener(), 30 + 70 * a / size);
							if (desc == null) it.remove();
						}
						modifyPackageList();
						updatedOnce = true;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								fetching = false;
								fireContentsChanged(this, 0, packageNames.size() > 0 ? packageNames.size() : 1);						
							}					
						});	
					} finally {
						fetching = false;
						getProgressListener().complete();
					}
				}
			}.start();
		}
	}
	
	protected boolean shouldUpdate() {
		return !updatedOnce;
	}
	
	private void setCompleted(ProgressListener listener, int progress) {
		listener.setCompleted(progress);
		this.completed = progress;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fireContentsChanged(this, 0, packageNames.size() > 0 ? packageNames.size() : 1);
			}
		});
	}

	public abstract List<String> fetchPackageNames();
	
	public void modifyPackageList() {
		return;
	}

	@Override
	public int getSize() {
		if (fetching) {
			return 1;
		} else {
			return packageNames.size() > 0 ? packageNames.size() : 1;	
		}		
	}

	@Override
	public Object getElementAt(int index) {
		if (fetching) return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.loading", completed);
		if (packageNames.size() == 0) return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.no_packages"); 
		return cache.getPackageInfo(packageNames.get(index), "ANY");
	}

	public void update(PackageDescriptor descr) {
		int index = packageNames.indexOf(descr.getPackageId());
		fireContentsChanged(this, index, index);
	}
	
	public void add(PackageDescriptor desc) {
		packageNames.add(desc.getPackageId());
		fireIntervalAdded(this, packageNames.size()-1, packageNames.size()-1);
	}
	
}