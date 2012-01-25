/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2011 by Rapid-I and the contributors
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

package com.rapidminer.gui.new_plotter.gui.dnd;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import com.rapidminer.gui.new_plotter.listener.DragListener;

/**
 * Abstract class for PlotConfiguration DnD {@link TransferHandler}s. Extends the original
 * TransferHandler and allows to set DnD Icons by overwriting the original
 * {@link SwingDragGestureRecognizer} and {@link DragHandler}. Furthermore it informs all
 * {@link DragListener}s when starting a Drag Action.
 * 
 * @author Nils Woehler
 * 
 */
public abstract class AbstractPatchedTransferHandler extends TransferHandler implements Serializable {

	//TODO Occasionally check if Bug has been fixed: http://bugs.sun.com/view_bug.do?bug_id=4816922
	
	private final int HIDE_DELAY = 100;
	private final int SHOW_DELAY = 500;

	private static class SwingDragGestureRecognizer extends DragGestureRecognizer {

		private static final long serialVersionUID = 1L;

		SwingDragGestureRecognizer() {
			super(DragSource.getDefaultDragSource(), null, NONE, null);
		}

		void gestured(JComponent c, MouseEvent e, int srcActions, int action) {
			setComponent(c);
			setSourceActions(srcActions);
			appendEvent(e);
			fireDragGestureRecognized(action, e.getPoint());
		}

		/**
		 * register this DragGestureRecognizer's Listeners with the Component
		 */
		protected void registerListeners() {
		}

		/**
		 * unregister this DragGestureRecognizer's Listeners with the Component
		 * <p/>
		 * subclasses must override this method
		 */
		protected void unregisterListeners() {
		}

	}

	/**
	 * This is the default drag handler for drag and drop operations that use the
	 * <code>TransferHandler</code>.
	 */
	private class DragHandler implements DragGestureListener, DragSourceListener {

		private boolean scrolls;

		/*
		 * ******************** LISTENERS ************
		 */

		// --- DragGestureListener methods -----------------------------------

		/**
		 * a Drag gesture has been recognized
		 */
		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			JComponent c = (JComponent) dge.getComponent();
			AbstractPatchedTransferHandler th = (AbstractPatchedTransferHandler) c.getTransferHandler();
			Transferable t = th.createTransferable(c);
			if (t != null) {
				scrolls = c.getAutoscrolls();
				c.setAutoscrolls(false);
				try {
					// dge.startDrag(null, t, this);

					Image img = null;
					Icon icn = th.getVisualRepresentation(t);
					if (icn != null) {
						if (icn instanceof ImageIcon) {
							img = ((ImageIcon) icn).getImage();
						} else {
							img = new BufferedImage(icn.getIconWidth(), icn.getIconWidth(), BufferedImage.TYPE_4BYTE_ABGR);
							Graphics g = img.getGraphics();
							icn.paintIcon(c, g, 0, 0);
						}
					}

					fireDragStart(t); // calls method from AbstractPatchedTransferHandler
					if (img == null) {
						dge.startDrag(null, t, this);
					} else {
						Cursor cursor = c.getToolkit().createCustomCursor(img, new Point(0, 0), "usr");
						dge.startDrag(cursor, t, this);
					}
					return;
				} catch (RuntimeException re) {
					c.setAutoscrolls(scrolls);
				}
			}
			th.exportDone(c, t, NONE);
		}

		// --- DragSourceListener methods -----------------------------------

		/**
		 * as the hotspot enters a platform dependent drop site
		 */
		public void dragEnter(DragSourceDragEvent dsde) {
		}

		/**
		 * as the hotspot moves over a platform dependent drop site
		 */
		public void dragOver(DragSourceDragEvent dsde) {
		}

		/**
		 * as the hotspot exits a platform dependent drop site
		 */
		public void dragExit(DragSourceEvent dsde) {
		}

		/**
		 * as the operation completes
		 */
		public void dragDropEnd(DragSourceDropEvent dsde) {
			DragSourceContext dsc = dsde.getDragSourceContext();
			JComponent c = (JComponent) dsc.getComponent();
			if (dsde.getDropSuccess()) {
				((AbstractPatchedTransferHandler) c.getTransferHandler()).exportDone(c, dsc.getTransferable(), dsde.getDropAction());
			} else {
				((AbstractPatchedTransferHandler) c.getTransferHandler()).exportDone(c, dsc.getTransferable(), NONE);
			}
			c.setAutoscrolls(scrolls);
			fireDragEnded();
		}

		public void dropActionChanged(DragSourceDragEvent dsde) {
		}
	}

	protected class hidePopupAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			hideDropDeniedTooltip();
		}
	}

	protected class showPopupAction implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			showPopupAtMousePosition();
		}
	}

	private static final long serialVersionUID = 1L;
	private static SwingDragGestureRecognizer recognizer = null;
	private Popup tipWindow;
	private int mouseX;
	private int mouseY;
	private Timer hideTimer, showTimer;
	private String reason;

	public AbstractPatchedTransferHandler() {
		hideTimer = new Timer(HIDE_DELAY, new hidePopupAction());
		hideTimer.setRepeats(false);
		showTimer = new Timer(SHOW_DELAY, new showPopupAction());
		showTimer.setRepeats(false);
	}

	@Override
	public void exportAsDrag(JComponent comp, InputEvent e, int action) {
		int srcActions = getSourceActions(comp);

		// only mouse events supported for drag operations
		if (!(e instanceof MouseEvent)
		// only support known actions
				|| !(action == COPY || action == MOVE || action == LINK)
				// only support valid source actions
				|| (srcActions & action) == 0) {

			action = NONE;
		}

		if (action != NONE && !GraphicsEnvironment.isHeadless()) {
			if (recognizer == null) {
				recognizer = new SwingDragGestureRecognizer();

			}
			DragHandler dgl = new DragHandler();
			try {
				recognizer.addDragGestureListener(dgl);
			} catch (TooManyListenersException e1) {
				e1.printStackTrace();
			}
			recognizer.gestured(comp, (MouseEvent) e, srcActions, action);
			recognizer.removeDragGestureListener(dgl);
		} else {
			exportDone(comp, null, NONE);
		}
	}

	@Override
	abstract public Icon getVisualRepresentation(Transferable t);

	/*
	 * ******************** LISTENERS ************
	 */

	private final List<DragListener> listeners = new LinkedList<DragListener>();
	private JComponent popupSource;

	private void fireDragStart(Transferable t) {
		for (DragListener l : listeners) {
			l.dragStarted(t);
		}
	}

	private void fireDragEnded() {
		for (DragListener l : listeners) {
			l.dragEnded();
		}
	}

	/**
	 * Hides popup and stops hide and show time if they are running.
	 */
	private void hideDropDeniedTooltip() {
		if (tipWindow != null) {
			tipWindow.hide();
			tipWindow = null;
			if (hideTimer.isRunning()) {
				hideTimer.stop();
			}
		}
		if (showTimer.isRunning()) {
			showTimer.stop();
		}
	}

	private void showPopupAtMousePosition() {

		// get mouse location
		Point screenLocation = MouseInfo.getPointerInfo().getLocation();
		screenLocation.x += 26;
		screenLocation.y += 10;

		// if tooltip is shown
		if (tipWindow != null) {

			// check if mouse has moved
			if (mouseX != screenLocation.x || mouseY != screenLocation.x) {
				// hide tooltip
				hideDropDeniedTooltip();
			} else {
				// otherwise restart hide timer
				hideTimer.restart();
				return;
			}
		}

		Point componentLocation = (Point) screenLocation.clone();
		SwingUtilities.convertPointFromScreen(componentLocation, popupSource);
		if (tipWindow == null && popupSource.contains(componentLocation)) {
			JToolTip tip = popupSource.createToolTip();
			tip.setTipText(reason);
			PopupFactory popupFactory = PopupFactory.getSharedInstance();

			mouseX = screenLocation.x;
			mouseY = screenLocation.y;

			tipWindow = popupFactory.getPopup(popupSource, tip, mouseX, mouseY);
			tipWindow.show();
			hideTimer.restart();
		}
	}

	protected void updateDropDeniedTooltip(JComponent comp, String deniedReason) {

		// if there is a tooltip to show
		if (deniedReason != null) {

			// if tooltip is shown
			if (tipWindow != null) {

				// check if reason has changed
				if (!deniedReason.equals(reason)) {

					popupSource = (JComponent) comp;
					reason = deniedReason;
					hideDropDeniedTooltip();
					showTimer.restart();
				} else {
					// check if mouse position has changed
					showPopupAtMousePosition();
				}

			} else {
				// if tooltip is not shown check if show timer is running already. if not start it
				if (!showTimer.isRunning()) {
					popupSource = (JComponent) comp;
					reason = deniedReason;
					showTimer.restart();
				} else if (!deniedReason.equals(reason)) {
					// if timer is running already check if reason has changed an restart if it has changed
					popupSource = (JComponent) comp;
					reason = deniedReason;
					showTimer.restart();
				}
			}
		} else {

			// reason is null. stop timers and hide tooltip
			hideDropDeniedTooltip();
			reason = null;
		}
	}

	public void addDragStartListener(DragListener l) {
		listeners.add(l);
	}

	public void removeDragStartListener(DragListener l) {
		listeners.remove(l);
	}
}