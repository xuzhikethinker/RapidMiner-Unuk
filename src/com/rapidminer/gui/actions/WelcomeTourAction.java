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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tour.RapidMinerTour;
import com.rapidminer.tools.I18N;

/**
 * Start the corresponding action.
 * 
 * @author Marco Boeck
 */
public class WelcomeTourAction extends AbstractAction {

	private static final long serialVersionUID = 1L;
	
	private static Icon icon = null;
	
	static {
		icon = SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.icon"));
	}
	
		
	private MainFrame mainFrame;
	
	
	public WelcomeTourAction(MainFrame mainFrame) {
		super(I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.label"), icon);
		putValue(SHORT_DESCRIPTION, I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.tip"));
		this.mainFrame = mainFrame;
	}

	public void actionPerformed(ActionEvent e) {
		this.mainFrame.getPerspectives().showPerspective("design");
		new RapidMinerTour().startTour(0);
	}
}
