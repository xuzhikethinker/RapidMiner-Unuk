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

package com.rapidminer.gui.tools.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.sun.awt.AWTUtilities;
import com.vlsolutions.swing.docking.DockableState;

/**
 * This class creates a speech bubble-shaped JDialog, which can be attache to
 * Buttons, either by using its ID or by passing a reference. 
 * The bubble triggers two events which are obserable by the {@link BubbleListener};
 * either if the close button was clicked, or if the corresponding button was used.
 * The keys for the title and the text must be of format gui.bubble.XXX.body or gui.bubble.XXX.title .
 * 
 * @author Philipp Kersting
 *
 */

public class BubbleWindow extends JDialog {

	private static final long serialVersionUID = -6369389148455099450L;

	public static interface BubbleListener {

		public void bubbleClosed(BubbleWindow bw);

		public void actionPerformed(BubbleWindow bw);
	}

	private List<BubbleListener> listeners = new LinkedList<BubbleListener>();

	/** Used to define the position of the pointer of the bubble. (Discribes exspacilly the corner in which the pointer starts)*/
	public enum Alignment {
		TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT, LEFTTOP, LEFTBOTTOM, RIGHTTOP, RIGHTBOTTOM
	}

	private static RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);
	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	private static final int CORNER_RADIUS = 20;
	private static final int WINDOW_WIDTH = 200;

	/** Shape used for setting the shape of the window and for rendering the outline. */
	private Shape shape;
	private Alignment alignment;
	private ActionListener listener;

	private ComponentAdapter movementListener;
	private AbstractButton button;
	private Component component;
	private WindowAdapter windowListener;
	private Window owner;

	public BubbleWindow(Window owner, final Alignment preferedAlignment, String i18nKey, String buttonKeyToAttach) {
		this(owner, preferedAlignment, i18nKey, (Component) findButton(buttonKeyToAttach, RapidMinerGUI.getMainFrame()));
		this.attachToButton(buttonKeyToAttach);
	}

	public BubbleWindow(Window owner, final Alignment preferedAlignment, String i18nKey, AbstractButton buttonToAttach) {
		this(owner, preferedAlignment, i18nKey, (Component) buttonToAttach);
		this.attachToButton(buttonToAttach);
	}

	/**
	 * 
	 * @param owner
	 * @param favoriatedAlignment offer for alignment but the Class will calculate by itself whether the position is usable
	 * @param i18nKey
	 */
	public BubbleWindow(Window owner, final Alignment preferedAlignment, String i18nKey, Component toAttach) {
		super(owner);
		this.owner = owner;
		this.component = toAttach;
		if (toAttach == null)
			throw new IllegalArgumentException("the given Component is null !!!");
		this.alignment = this.calculateAlignment(preferedAlignment, toAttach);
		this.component = toAttach;
		setLayout(new BorderLayout());
		setUndecorated(true);

		String title = I18N.getGUIBundle().getString("gui.bubble." + i18nKey + ".title");
		String text = I18N.getGUIBundle().getString("gui.bubble." + i18nKey + ".body");

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				shape = createShape(alignment);
				AWTUtilities.setWindowShape(BubbleWindow.this, shape);
			}
		});

		GridBagLayout gbl = new GridBagLayout();
		JPanel bubble = new JPanel(gbl) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics gr) {
				super.paintComponent(gr);
				Graphics2D g = (Graphics2D) gr;
				g.setColor(SwingTools.LIGHT_BROWN_FONT_COLOR);
				g.setStroke(new BasicStroke(3));
				g.setRenderingHints(HI_QUALITY_HINTS);
				g.draw(AffineTransform.getTranslateInstance(-.5, -.5).createTransformedShape(getShape()));
			}
		};
		bubble.setBackground(SwingTools.LIGHTEST_YELLOW);
		bubble.setSize(getSize());
		getContentPane().add(bubble, BorderLayout.CENTER);

		GridBagConstraints c = new GridBagConstraints();
		Insets insets = new Insets(10, 10, 10, 10);
		Insets lInsets = new Insets(0, 10, 10, 10);
		switch (alignment) {
			case TOPLEFT:
				insets = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case TOPRIGHT:
				insets = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case LEFTTOP:
				insets = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				lInsets = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case LEFTBOTTOM:
				insets = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				lInsets = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case BOTTOMRIGHT:
				insets = new Insets(10, 10, 10, 10);
				lInsets = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case BOTTOMLEFT:
				insets = new Insets(10, 10, 10, 10);
				lInsets = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case RIGHTTOP:
				insets = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				lInsets = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			case RIGHTBOTTOM:
				insets = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				lInsets = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
		}
		c.insets = insets;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		JLabel label = new JLabel(title);
		bubble.add(label, c);
		label.setMinimumSize(new Dimension(WINDOW_WIDTH, 12));
		label.setPreferredSize(new Dimension(WINDOW_WIDTH, 12));
		label.setFont(label.getFont().deriveFont(Font.BOLD));

		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = insets;
		JButton close = new JButton("x");
		close.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BubbleWindow.this.dispose();
				fireEventCloseClicked();
				unregister();
			}

		});
		close.setMargin(new Insets(0, 5, 0, 5));
		bubble.add(close, c);

		ExtendedHTMLJEditorPane mainText = new ExtendedHTMLJEditorPane("text/html", "<div style=\"width:" + WINDOW_WIDTH + "px\">" + text + "</div>");
		mainText.setMargin(new Insets(0, 0, 0, 0));
		mainText.installDefaultStylesheet();
		mainText.setOpaque(false);
		mainText.setEditable(false);
		mainText.setFont(mainText.getFont().deriveFont(Font.PLAIN));
		mainText.setMinimumSize(new Dimension(150, 20));
		//mainText.setPreferredSize(new Dimension(200, 200));
		mainText.setMaximumSize(new Dimension(WINDOW_WIDTH, 800));
		c.insets = lInsets;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 1;
		bubble.add(mainText, c);

		pack();

		positionRelative();
	}

	/**
	 * 
	 * Adds a BubbleListener.
	 * 
	 * @param l The listener
	 */
	public void addBubbleListener(BubbleListener l) {
		listeners.add(l);
	}

	public void removeBubbleListener(BubbleListener l) {
		listeners.remove(l);
	}

	/**
	 * Creates a speech bubble-shaped Shape.
	 * 
	 * @param a The alignment of the pointer.
	 * 
	 * @return A speech-bubble <b>Shape</b>.
	 */
	public Shape createShape(Alignment a) {
		int w = getSize().width - 2 * CORNER_RADIUS;
		int h = getSize().height - 2 * CORNER_RADIUS;
		int o = CORNER_RADIUS;

		GeneralPath gp = new GeneralPath();
		switch (a) {
			case TOPLEFT:
				gp.moveTo(0, 0);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), (2 * o));
				gp.quadTo(w + (2 * o), o, w + o, o);
				gp.lineTo(o, o);
				gp.lineTo(0, 0);
				break;
			case TOPRIGHT:
				gp.moveTo(0, 2 * o);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), 0);
				gp.lineTo((w + o), o);
				gp.lineTo(o, o);
				gp.quadTo(0, o, 0, (2 * o));
				break;
			case BOTTOMLEFT:
				gp.moveTo(0, o);
				gp.lineTo(0, h + (2 * o));
				gp.lineTo(o, h + o);
				gp.lineTo(w + o, h + o);
				gp.quadTo(w + (2 * o), h + o, w + (2 * o), h);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case BOTTOMRIGHT:
				gp.moveTo(0, o);
				gp.lineTo(0, h);
				gp.quadTo(0, (h + o), o, (h + o));
				gp.lineTo(w + o, (h + o));
				gp.lineTo(w + (2 * o), h + (2 * o));
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case LEFTBOTTOM:
				gp.moveTo(0, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo((2 * o), 0);
				gp.quadTo(o, 0, o, o);
				gp.lineTo(o, h + o);
				gp.closePath();
				break;
			case LEFTTOP:
				gp.moveTo(0, 0);
				gp.lineTo(o, o);
				gp.lineTo(o, (h + o));
				gp.quadTo(o, h + (2 * o), (2 * o), h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(0, 0);
				break;
			case RIGHTBOTTOM:
				gp.moveTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + (2 * o), h + (2 * o));
				gp.lineTo(w + o, h + o);
				gp.lineTo(w + o, o);
				gp.quadTo(w + o, 0, w, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, h + o);
				break;
			case RIGHTTOP:
				gp.moveTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, (h + o));
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w, h + (2 * o));
				gp.quadTo((w + o), h + (2 * o), (w + o), (h + o));
				gp.lineTo((w + o), o);
				gp.lineTo(w + (2 * o), 0);
				gp.lineTo(o, 0);
				break;
		}
		AffineTransform tx = new AffineTransform();
		return gp.createTransformedShape(tx);
	}

	public void positionRelativeTo(Component component) {
		this.component = component;

		pointAtComponent(component);

		registerMovementListener();
	}

	public void positionRelative() {
		//TODO: Check
		pointAtComponent(this.component);

		registerMovementListener();
	}

	private void pointAtComponent(Component component) {
		int x = (int) component.getLocationOnScreen().getX();
		int y = (int) component.getLocationOnScreen().getY();
		int h = component.getHeight();
		int w = component.getWidth();
		double targetx = 0;
		double targety = 0;
		Point target = new Point(0, 0);
		switch (alignment) {
			case TOPLEFT:
				targetx = x + 0.5 * w;
				targety = y + h;
				break;
			case TOPRIGHT:
				targetx = (x + 0.5 * w) - getWidth();
				targety = y + h;
				break;
			case LEFTBOTTOM:
				targetx = x + w;
				targety = (y + 0.5 * h) - getHeight();
				break;
			case LEFTTOP:
				targetx = x + w;
				targety = (y + 0.5 * h);
				break;
			case RIGHTBOTTOM:
				targetx = x - getWidth();
				targety = (y + 0.5 * h) - getHeight();
				break;
			case RIGHTTOP:
				targetx = x - getWidth();
				targety = (y + 0.5 * h);
				break;
			case BOTTOMLEFT:
				targetx = x + 0.5 * w;
				targety = y - getHeight();
				break;
			case BOTTOMRIGHT:
				targetx = x + 0.5 * w - getWidth();
				targety = y - getHeight();
				break;
		}

		target = new Point((int) Math.round(targetx), (int) Math.round(targety));
		setLocation(target);
	}

	public static Component positionRelativeToDockable(String dockableKey) {
		DockableState[] dockables = RapidMinerGUI.getMainFrame().getDockingDesktop().getDockables();
		for (DockableState ds : dockables) {
			System.out.println("Found: " + ds.getDockable().getDockKey().getKey());
			if (ds.getDockable().getDockKey().getKey().equals(dockableKey)) {
				//TODO:deletePart
//				component = ds.getDockable().getComponent().getParent().getParent();
//				positionRelativeTo(component);
				return ds.getDockable().getComponent().getParent().getParent();
//				Location location = ds.getLocation();
//				RelativeDockablePosition position = ds.getPosition();				
			}
		}
		throw new IllegalArgumentException("No such dockable: " + dockableKey);
	}

	/** Positions the window such that the pointer points to the given button. 
	 *  In addition to that, adds an {@link ActionListener} to the button which
	 *  closes the BubbleWindow as soon as the button is pressed and one that 
	 *  makes sure that the pointer always points at the right position. */
	public void attachToButton(AbstractButton button) {
		//TODO: delete
		//positionRelativeTo(button);
		this.button = button;
		this.component = button;
		listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BubbleWindow.this.dispose();
				fireEventActionPerformed();
				unregister();
			}
		};
		button.addActionListener(listener);

	}

	public void attachToButton(String i18nKeyOfButton) {
		AbstractButton button = findButton(i18nKeyOfButton, RapidMinerGUI.getMainFrame());
		if (button != null) {
			// attach
			attachToButton(button);
		} else {
			// TODO: handle missing case
		}
	}

	public static AbstractButton findButton(String name, Component searchRoot) {
		if (searchRoot instanceof AbstractButton) {

			AbstractButton b = (AbstractButton) searchRoot;
			if (b.getAction() instanceof ResourceAction) {
				String id = (String) b.getAction().getValue("rm_id");
				if (name.equals(id)) {
					return b;
				}
			} else {}
		} else {}
		if (searchRoot instanceof Container) {
			Component[] all = ((Container) searchRoot).getComponents();
			for (Component child : all) {
				AbstractButton result = findButton(name, child);
				if (result != null) {
					return result;

				}
			}
		}
		return null;
	}

//	public void attachToOperator(ProcessSetupListener l, Component component){
//		positionRelativeTo(component);
//		this.processListener = l;
//		
////		DockableState dockableState = RapidMinerGUI.getMainFrame().getDockingDesktop().getDockables()[0];
////		dockableState.getDockable().getComponent();
//		
//		RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(processListener);		
//	}

	public Shape getShape() {
		if (shape == null) {
			shape = createShape(alignment);
		}
		return shape;
	}

	private void registerMovementListener() {
		movementListener = new ComponentAdapter() {

			@Override
			public void componentMoved(ComponentEvent e) {
				super.componentMoved(e);
				BubbleWindow.this.pointAtComponent(BubbleWindow.this.component);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				super.componentResized(e);
				BubbleWindow.this.pointAtComponent(BubbleWindow.this.component);
			}

			@Override
			public void componentShown(ComponentEvent e) {
				super.componentShown(e);
				BubbleWindow.this.pointAtComponent(BubbleWindow.this.component);
				BubbleWindow.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				super.componentHidden(e);
				BubbleWindow.this.setVisible(false);
			}

		};
		windowListener = new WindowAdapter() {

			@Override
			public void windowIconified(WindowEvent e) {
				super.windowIconified(e);
				BubbleWindow.this.setVisible(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				super.windowDeiconified(e);
				BubbleWindow.this.setVisible(true);
				BubbleWindow.this.pointAtComponent(component);
			}

		};
		RapidMinerGUI.getMainFrame().addComponentListener(movementListener);
		RapidMinerGUI.getMainFrame().addWindowStateListener(windowListener);
	}

	private void unregister() {
		if ((listener != null) && (button != null)) {
			button.removeActionListener(listener);
		}
	}

	private void unregisterMovementListener() {
		RapidMinerGUI.getMainFrame().removeComponentListener(movementListener);
		RapidMinerGUI.getMainFrame().removeWindowStateListener(windowListener);
	}

	public void triggerFire() {
		fireEventActionPerformed();
		dispose();
	}

	protected void fireEventCloseClicked() {
		LinkedList<BubbleListener> listenerList = new LinkedList<BubbleWindow.BubbleListener>(listeners);
		for (BubbleListener l : listenerList) {
			l.bubbleClosed(this);
			unregisterMovementListener();
		}
	}

	protected void fireEventActionPerformed() {
		LinkedList<BubbleListener> listenerList = new LinkedList<BubbleWindow.BubbleListener>(listeners);
		for (BubbleListener l : listenerList) {
			l.actionPerformed(this);
			unregisterMovementListener();
		}
	}

	protected Alignment calculateAlignment(Alignment preferedAlignment, Component component) {
		//get Mainframe size
		int xframe = owner.getWidth();
		int yframe = owner.getHeight();

		//location of Component
		Point location = component.getLocationOnScreen();
		double xloc = location.getX();
		double yloc = location.getY();
		//size of Component
		int xSize = component.getWidth();
		int ySize = component.getHeight();

		if (yloc < (yframe / 2)) {
			if (xloc < (xframe / 2)) {
				//corner up-left
				if (!(preferedAlignment == Alignment.TOPLEFT || preferedAlignment == Alignment.LEFTTOP) || (ySize > (yframe / 3))) {
//					return Alignment.TOPLEFT;
					return Alignment.LEFTTOP;
				}
			} else {
				//corner up-right
				if (!(preferedAlignment == Alignment.TOPRIGHT || preferedAlignment == Alignment.RIGHTTOP) || (ySize > (yframe / 3))) {
//					return Alignment.TOPRIGHT;
					return Alignment.RIGHTTOP;
				}
			}
		} else {
			if (xloc < (xframe / 2)) {
				//corner down-left
				if (!(preferedAlignment == Alignment.BOTTOMLEFT || preferedAlignment == Alignment.LEFTBOTTOM) || (xSize > (xloc / 3))) {
					return Alignment.BOTTOMLEFT;
//					return Alignment.LEFTBOTTOM;
				}
			} else {
				//corner down-right
				if (!(preferedAlignment == Alignment.BOTTOMRIGHT || preferedAlignment == Alignment.RIGHTBOTTOM)) {
//					return Alignment.RIGHTBOTTOM;
					return Alignment.TOPRIGHT;
				}
			}
		}
		return preferedAlignment;
	}
}
