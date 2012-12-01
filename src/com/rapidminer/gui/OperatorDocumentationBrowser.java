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

package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.Process;
import com.rapidminer.gui.actions.SaveAction;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.plugin.Plugin;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * 
 * This class contains methods that generate an item that shows a help text eiter from an XML file if provided or from the description contained by the operator itself.
 * The actual document is generated by the {@link OperatorDocToHtmlConverter}.
 * 
 * @author Philipp Kersting, Marco Boeck
 * 
 */
public class OperatorDocumentationBrowser extends JPanel implements Dockable, ProcessEditor {

	final ExtendedHTMLJEditorPane editor = new ExtendedHTMLJEditorPane("text/html", "<html>-</html>");

	private ExtendedJScrollPane scrollPane = new ExtendedJScrollPane();

	public static final String DOCUMENTATION_ROOT = "core/";

	public Operator displayedOperator = null;

	public URL currentResourceURL = null;

	private boolean ignoreSelections = false;

	public static final String OPERATOR_HELP_DOCK_KEY = "operator_help";

	private final DockKey DOCK_KEY = new ResourceDockKey(OPERATOR_HELP_DOCK_KEY);

	private static final long serialVersionUID = 1L;

	private UpdateQueue documentationUpdateQueue = new UpdateQueue("documentation_update_queue");

	/**
	 * Prepares the dockable and its elements.
	 */
	public OperatorDocumentationBrowser() {
		setLayout(new BorderLayout());

		//Instantiate Editor and set Settings
		editor.installDefaultStylesheet();
		editor.addHyperlinkListener(new ExampleProcessLinkListener());
		editor.setEditable(false);
		HTMLEditorKit hed = new HTMLEditorKit();
		hed.setStyleSheet(createStyleSheet());
		editor.setEditorKit(hed);

		//add editor to scrollPane		
		scrollPane = new ExtendedJScrollPane(editor);

		scrollPane.setMinimumSize(new Dimension(100, 100));
		scrollPane.setPreferredSize(new Dimension(100, 100));

		//add scrollPane to Dockable		
		scrollPane.setBorder(null);
		this.add(scrollPane, BorderLayout.CENTER);
		this.setVisible(true);
		this.validate();

		documentationUpdateQueue.start();
	}

	@Override
	public void processChanged(Process process) {
		// not needed
	}

	/**
	 * This method gets called if the user clicks on an operator that has been placed in the process.
	 */
	@Override
	public void setSelection(List<Operator> selection) {
		if (!selection.get(0).equals(displayedOperator) && !ignoreSelections) {
			displayedOperator = selection.get(0);
			assignDocumentation();
		}
	}

	/**
	 * This is called by the {@link #setSelection(List)} method.
	 * It creates an absolute path that indicates the corresponding documentation XML file.
	 */
	private void assignDocumentation() {
		boolean isPlugin = displayedOperator.getOperatorDescription().getProvider()!=null;
		String documentationRoot = isPlugin
							? displayedOperator.getOperatorDescription().getProvider().getPrefix() + "/"
							: DOCUMENTATION_ROOT;
		String groupPath = ((displayedOperator.getOperatorDescription().getGroup()).replace(".", "/"));
		String key = displayedOperator.getOperatorDescription().getKeyWithoutPrefix();
		
		String opDescXMLResourcePath = documentationRoot + groupPath + "/" + key + ".xml";
		URL resourceURL = Plugin.getMajorClassLoader().getResource(opDescXMLResourcePath);
		changeDocumentation(resourceURL);
		currentResourceURL = resourceURL;
	}

	@Override
	public void processUpdated(Process process) {
		// not needed
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	/**
	 * This is the method that actually gets the Content of this Dockable.
	 * The conversion takes place in the class {@link OperatorDocToHtmlConverter}.
	 * @param xmlStream
	 */
	private String parseXmlAndReturnHtml(InputStream xmlStream) {
		try {
			return OperatorDocToHtmlConverter.convert(xmlStream, displayedOperator);
		} catch (MalformedURLException e) {
			LogService.getRoot().warning("Failed to load documentation. Reason: " + e.getLocalizedMessage());
			return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.operator_documentation_error.message", e.getLocalizedMessage());
		} catch (IOException e) {
			LogService.getRoot().warning("Failed to load documentation. Reason: " + e.getLocalizedMessage());
			return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.operator_documentation_error.message", e.getLocalizedMessage());
		}
	}

	/**
	 * Event handler that handles clicking on a link to a tutorial process.
	 */
	private class ExampleProcessLinkListener implements HyperlinkListener {

		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType().equals(EventType.ACTIVATED)) {
				if(e.getDescription().startsWith("tutorial:")) {
					// ask for confirmation before stopping the currently running process and opening another one!
	            	if (RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_RUNNING || 
	            			RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_PAUSED) {
	            		if (SwingTools.showConfirmDialog("close_running_process", ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.NO_OPTION) {
	            			return;
	            		}
	            	}
	            	
					// ask user if he wants to save his current process because the example process will replace his current process
					if (RapidMinerGUI.getMainFrame().isChanged()) {
						// current process is flagged as unsaved
						int returnVal = SwingTools.showConfirmDialog("save_before_show_tutorial_process", ConfirmDialog.YES_NO_CANCEL_OPTION);
						if (returnVal == ConfirmDialog.CANCEL_OPTION) {
							return;
						} else if (returnVal == ConfirmDialog.YES_OPTION) {
							SaveAction.save(RapidMinerGUI.getMainFrame().getProcess());
						}
					} else {
						// current process is not flagged as unsaved
						if (SwingTools.showConfirmDialog("show_tutorial_process", ConfirmDialog.OK_CANCEL_OPTION) == ConfirmDialog.CANCEL_OPTION) {
							return;
						}
					}
				
					try {
						if (currentResourceURL == null) {
							// should not happen, because then there would be no link in the first place
							return;
						}
						Document document = XMLTools.parse(WebServiceTools.openStreamFromURL(currentResourceURL));

						int index = Integer.parseInt(e.getDescription().substring("tutorial:".length()));
						
						NodeList nodeList = document.getElementsByTagName("tutorialProcess");
						Node processNode = nodeList.item(index - 1);
						Node process = null;
						int i = 0;
						while (i < processNode.getChildNodes().getLength()) {
							if (processNode.getChildNodes().item(i).getNodeName().equals("process")) {
								process = processNode.getChildNodes().item(i);
							}
							i++;
						}

						StringWriter buffer = new StringWriter();
						DOMSource processSource = new DOMSource(process);
						Transformer t = TransformerFactory.newInstance().newTransformer();
						t.transform(processSource, new StreamResult(buffer));
						Process exampleProcess = new Process(buffer.toString());
						Operator formerOperator = displayedOperator;
						ignoreSelections = true;
						RapidMinerGUI.getMainFrame().setProcess(exampleProcess, true);
						Collection<Operator> displayedOperators = RapidMinerGUI.getMainFrame().getProcess().getAllOperators();
						for (Operator item : displayedOperators) {
							if (item.getClass().equals(formerOperator.getClass())) {
								RapidMinerGUI.getMainFrame().selectOperator(item);
								ignoreSelections = false;
							}
						}
					} catch (TransformerException e1) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.documentation.ExampleProcess.creating_example_process_error", e1);
					} catch (SAXException e1) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.documentation.ExampleProcess.parsing_xml_error", e1);
					} catch (IOException e1) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.documentation.ExampleProcess.reading_file_error", e1);
					} catch (XMLException e1) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.documentation.ExampleProcess.parsing_xml_error", e1);
					}
				
				} else {
					// open url in default browser
					Desktop desktop = Desktop.getDesktop();
			        if(desktop.isSupported(Desktop.Action.BROWSE)) {
			          URI uri;
						try {
							uri = new java.net.URI(e.getDescription());
							desktop.browse(uri);
						} catch (URISyntaxException e1) {
							LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.desktop.browse.malformed_url", e1);
							return;
						} catch (IOException e1) {
							LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.desktop.browse.open_browser", e1);
							return;
						}
			        } else {
			        	LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.desktop.browse.not_supported");
						return;
			        }
				}
			}
		}
	}

	/**
	 * Refreshes the documentation text.
	 * 
	 * @param resourceURL url to the xml resource
	 */
	private void changeDocumentation(final URL resourceURL) {
		editor.setContentType("text/html");
		editor.setText("<html><div style=\"height:100%;width:100%;text-align:center;vertical-align:middle;margin-top:50px;\"><img src=\"icon:///48/hourglass.png\"/></div></html>"); 
		editor.setCaretPosition(0);
		documentationUpdateQueue.executeBackgroundJob(new ProgressThread("loading_documentation") {

			@Override
			public void run() {
				InputStream xmlStream = null;
				String html;
				try {
					if (resourceURL != null) {
						xmlStream = WebServiceTools.openStreamFromURL(resourceURL);
					}
				} catch (IOException e) {
					// do nothing
				} finally {
					html = parseXmlAndReturnHtml(xmlStream);
					if (xmlStream != null) {
						try {
							xmlStream.close();
						} catch (IOException e) {
							//do nothing
						}
					}
				}
				html = html.replace(" xmlns:rmdoc=\"com.rapidminer.gui.OperatorDocumentationBrowser\"", " ");
				final String finalHtml = html;
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						editor.setContentType("text/html");
						editor.setText("<html>" + finalHtml + "</html>");
						editor.setCaretPosition(0);
					}
				});

			}

		});
	}

	/**
	 * This method creates and returns a stylesheet that makes the documentation look as it's supposed to look.
	 * 
	 * @return the stylesheet
	 */
	private StyleSheet createStyleSheet() {
		StyleSheet css = new HTMLEditorKit().getStyleSheet();
		css.addRule("* {font-family: Arial}");

		css.addRule("p {padding: 0px 20px 1px 20px; font-family: Arial;}");
		css.addRule("ul li {padding-bottom:1ex}");
		css.addRule("hr {color:red; background-color:red}");
		css.addRule("h3 {color: #3399FF}");
		css.addRule("h4 {color: #3399FF; font-size:13pt}");
		css.addRule("h4 img {margin-right:8px;}");
		css.addRule(".typeIcon {height: 10px; width: 10px;}");
		css.addRule("td {vertical-align: top}");
		css.addRule(".lilIcon {padding: 2px 4px 2px 0px}");
		//css.addRule(".HeadIcon {height: 40px; width: 40px}");
		css.addRule("td {font-style: normal}");

		return css;
	}

	/**
	 * Sets the operator for which the operator documentation is shown.
	 * @param operator
	 */
	public void setDisplayedOperator(Operator operator) {
		if (operator != null && !operator.getOperatorDescription().isDeprecated() &&
				(this.displayedOperator == null ||
				(this.displayedOperator != null && !operator.getOperatorDescription().getName().equals(this.displayedOperator.getOperatorDescription().getName())))) {
			this.displayedOperator = operator;
			assignDocumentation();
		}
	}

}
