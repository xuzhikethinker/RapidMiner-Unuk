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
package com.rapidminer.gui.properties;

import groovy.swing.impl.DefaultAction;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.commons.lang.ArrayUtils;

import sun.swing.DefaultLookup;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.PlainArrowDropDownButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * A dialog to create and edit regular expressions. Can be created with a
 * given predefined regular expression (normally a previously set value).
 * A collection of item strings can be given to the dialog which are then
 * available as shortcuts. Additionally, a list shows which of these items
 * match the regular expression. If the item collection is null, both lists
 * will not be visible.
 * 
 * The dialog shows an inline preview displaying where the given pattern matches.
 * It also shows a list of matches, together with their matching groups.
 * 
 * @author Tobias Malbrecht, Dominik Halfkann, Simon Fischer
 */
public class RegexpPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 5396725165122306231L;
	
	private RegexpSearchStyledDocument inlineSearchDocument = null;
	private RegexpReplaceStyledDocument inlineReplaceDocument = null;
	
	private JTabbedPane testExp = null;
	
	private DefaultListModel resultsListModel = new DefaultListModel();
	
	private static String[][] regexpConstructs = {
		{ ".", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.any_character") },
		{ "[]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.bracket_expression") },
		{ "[^]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.not_bracket_expression") },
		{ "()", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.capturing_group") },
		{ "?", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.zero_one_quantifier") },
		{ "*", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.zero_more_quantifier") },
		{ "+", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.one_more_quantifier") },
		{ "{n}", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.exact_quantifier") },
		{ "{min,}", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.min_quantifier") },
		{ "{min,max}", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.min_max_quantifier") },
		{ "|", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.disjunction") },
	};

	// adjust the caret by this amount upon insertion
	private static int[] regexpConstructInsertionCaretAdjustment = {
		0, -1, -1, -1, 0, 0, 0, -1, -2, -1, -5, 0,
	};
	
	// select these construct characters upon insertion
	private static int[][] regexpConstructInsertionSelectionIndices = {
		{ 1, 1 }, { 1, 1 }, { 2, 2 }, { 1, 1 }, { 1, 1 }, { 1, 1 }, { 1, 1 }, { 1, 2 }, { 1, 4 }, { 1, 8 }, { 1, 1 },  
	};
	
	// enclose selected by construct
	private static boolean[] regexpConstructInsertionEncloseSelected = {
		false, true, true, true, false, false, false, false, false, false, false,
	};
	
	private static String[][] regexpShortcuts = {
		{ ".*", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.arbitrary") },
		{ "[a-zA-Z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.letter") },
		{ "[a-z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.lowercase_letter") },
		{ "[A-Z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.uppercase_letter") },
		{ "[0-9]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.digit") },
		{ "\\w", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.word") },
		{ "\\W", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.non_word") },
		{ "\\s", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.whitespace") },
		{ "\\S", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.non_whitespace") },
		{ "[-!\"#$%&'()*+,./:;<=>?@\\[\\\\\\]_`{|}~]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.punctuation") },
	};
	
	private static final String ERROR_MESSAGE = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.error.label");
	
	private static final Icon ERROR_ICON = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.error.icon"));
	
	private static final String NO_ERROR_MESSAGE = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.no_error.label");
	
	private static final Icon NO_ERROR_ICON = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.no_error.icon"));
	
	private final JTextField regexpTextField;
	
	private final JTextField replacementTextField;
	
	private JList itemShortcutsList;
	
	private DefaultListModel matchedItemsListModel;
	
	private final Collection<String> items;
	
	private boolean supportsItems = false;
	
	private final JLabel errorMessage;
	
	private JButton okButton;
	/*
	private class RegExpGroup {
		private String pattern;
		private String match;
		
		public RegExpGroup(String pattern, String match) {
			this.pattern = pattern;
			this.match = match;
		}
		
		public String getPattern() {
			return pattern;
		}
		
		public String getMatch() {
			return match;
		}
		
	}*/
	
	private class RegExpResult {
		private String match;
		private String[] groups;
		private int number;
		private boolean empty = false;

		public RegExpResult(String match, String[] groups, int number) {
			this.match = match;
			this.groups = groups;
			this.number = number;
		}
		
		public RegExpResult() {
			// empty result
			empty = true;
		}
		
		@Override
		public String toString() {
			String output = "";
			if (!empty) {
				output += "<html>"+
						"<span style=\"font-size:11px;margin:2px 0 2px 4px;\">" +
						//"Match "+number+": <b>'"+match+"'</b>" +
						I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.match", number, "<b>'"+match+"'</b>") +
						"</span>";
				if (groups.length > 0) {
					output += 
							"<ol style=\"margin:1px 0 0 24px\">";
					for (int i = 0; i < groups.length; i++) {
						//output += "<li>Group matches: <b>'" + groups[i] +"'</b></li>";
						output += "<li>" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.group_match", "<b>'" + groups[i] +"'</b>") + "</li>";
						
					}
					output += "</ul>";
				}
				output += "</html>";
			} else {
				output += "<html>"+
						"<span style=\"font-size:11px;margin:2px 0 2px 4px;\">" +
						I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.empty") +
						"</span>";
				output += "</html>";
			}
			return output;
		}
	}
	
	private class RegexpSearchStyledDocument extends DefaultStyledDocument {
		private static final long serialVersionUID = 1L;
		
		
		Matcher matcher =
	            Pattern.compile("").matcher("");
		
		Style keyStyle;
		Style rootStyle;
		{
			rootStyle = addStyle("root", null);
		 
			keyStyle = addStyle("key", rootStyle);
			//StyleConstants.setBold(keyStyle, true);
			//StyleConstants.setForeground(keyStyle, Color.RED);
			StyleConstants.setBackground(keyStyle, Color.YELLOW);
			//StyleConstants.setUnderline(keyStyle, true);
		}
		
		public RegexpSearchStyledDocument() {
			super();
			//setText("You can test your regular expression on this text");
		}
		  
		@Override
	    public void insertString(int offs, String str, AttributeSet a)
	    		throws BadLocationException {
			super.insertString(offs, str, a);
	        checkDocument();
	        //copyToReplacement();	        
	    }
	 
	    @Override
	    public void remove(int offs, int len) throws BadLocationException {
	        super.remove(offs, len);
	        checkDocument();
	        //copyToReplacement();
	    }			
		  
		private void checkDocument() {
			
			setCharacterAttributes(0, getLength(), rootStyle, true);
			try {
				matcher.reset(getText(0, getLength()));
				int count = 0;
				resultsListModel.clear();
				StringBuffer sb = new StringBuffer();
				while (matcher.find()) {
					if (matcher.end() <= matcher.start()) continue;
					setCharacterAttributes(matcher.start(), matcher.end()
							- matcher.start(), keyStyle, true);
					
					String[] groups = new String[matcher.groupCount()];
					for (int i=1; i<=matcher.groupCount(); i++) {
						groups[i-1] = matcher.group(i);
				    }
					resultsListModel.addElement(new RegExpResult(
							this.getText(matcher.start(), matcher.end()-matcher.start()), 
							groups, count+1));
					matcher.appendReplacement(sb, replacementTextField.getText()); //Matcher.quoteReplacement(replacementTextField.getText()));
					count++;
				}
				matcher.appendTail(sb);
				
				if (count == 0) {
					// add empty element
					resultsListModel.addElement(new RegExpResult());
				}
				
				testExp.setTitleAt(1,  I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title") + " ("+count+")");
				inlineReplaceDocument.setText(sb.toString());
			} catch (BadLocationException ex) {
				LogService.getRoot().log(Level.WARNING, RegexpPropertyDialog.class.getName()+".bad_location", ex);
			}
	    }	
	      
	    public void updatePattern(String pattern) {
	    	this.matcher = Pattern.compile(pattern).matcher("");
	    	checkDocument();
	    }
	    
	    public void clearResults() {
	    	resultsListModel.clear();
	    	resultsListModel.addElement(new RegExpResult());
	    	testExp.setTitleAt(1, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title") + " (0)");
	    	setCharacterAttributes(0, getLength(), rootStyle, true);
	    }
	      
	}
		
	private class RegexpReplaceStyledDocument extends DefaultStyledDocument {
		private static final long serialVersionUID = 1L;
		

		public RegexpReplaceStyledDocument() {
			super();
		}
	      
		public void setText(String text) {
			try {
				remove(0, getLength());
				insertString(0, text, null);
			} catch (BadLocationException e) {
				LogService.getRoot().log(Level.WARNING, RegexpPropertyDialog.class.getName()+".bad_location", e);
			}			
		}		
	}
	
	public RegexpPropertyDialog(final ParameterTypeRegexp type, String predefinedRegexp, Operator operator) {
		super(type, "regexp");
		this.items = type.getPreviewList();
		this.supportsItems = (items != null);
		
		Dimension size = new Dimension(420,500);
		//this.setSize(size);
		this.setMinimumSize(size);
		//this.setMaximumSize(size);
		this.setPreferredSize(size);
		
		JPanel panel = new JPanel(createGridLayout(1, supportsItems ? 2 : 1));
		
		//panel.setSize(600, 400);
		//panel.setPreferredSize(new Dimension(600, 400));

		// regexp text field
		regexpTextField = new JTextField(predefinedRegexp);
		regexpTextField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.tip"));
		regexpTextField.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {}
			
			public void keyReleased(KeyEvent e) {
				fireRegularExpressionUpdated();
			}

			public void keyTyped(KeyEvent e) {}
			
		});
		regexpTextField.requestFocus();
		
		// replacement text field
		replacementTextField = new JTextField();
		replacementTextField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.tip"));
		replacementTextField.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {}
			
			public void keyReleased(KeyEvent e) {
				fireRegularExpressionUpdated();
			}

			public void keyTyped(KeyEvent e) {}

		});
		
		// constructs table
		final TableModel regexpConstructTableModel = new TableModel() {
			//private static final long serialVersionUID = 3081581916411341948L;

			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}

			public String getColumnName(int columnIndex) {
				switch (columnIndex) {
				case 0:
					return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.constructs_table.construct_header");
				case 1:
					return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.constructs_table.description_header");
				}
				return null;
			}
			
			public int getColumnCount() {
				return 2;
			}

			public int getRowCount() {
				return regexpConstructs.length;
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				return regexpConstructs[rowIndex][columnIndex];
			}

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
			public void setValueAt(Object value, int rowIndex, int columnIndex) {}
		};
		final JTable regexpConstructTable = new JTable(regexpConstructTableModel);
		regexpConstructTable.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.constructs_table.tip"));
		regexpConstructTable.setCellSelectionEnabled(true);
		regexpConstructTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		regexpConstructTable.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = regexpConstructTable.getSelectedRow(); 
					String text = regexpTextField.getText();
					String insertionString = regexpConstructTableModel.getValueAt(row, 0).toString();
					if (regexpConstructInsertionEncloseSelected[row] && regexpTextField.getSelectedText() != null) {
						int selectionStart = regexpTextField.getSelectionStart();
						int selectionEnd = regexpTextField.getSelectionEnd();
						String newText = text.substring(0, selectionStart) +
										 insertionString.substring(0, regexpConstructInsertionSelectionIndices[row][0]) + 
										 text.substring(selectionStart, selectionEnd) +
										 insertionString.substring(regexpConstructInsertionSelectionIndices[row][0], insertionString.length()) +
										 text.substring(selectionEnd, text.length());
						regexpTextField.setText(newText);
						regexpTextField.setCaretPosition(selectionEnd - regexpConstructInsertionCaretAdjustment[row]);
						regexpTextField.setSelectionStart(selectionStart + regexpConstructInsertionSelectionIndices[row][0]);
						regexpTextField.setSelectionEnd(selectionEnd + regexpConstructInsertionSelectionIndices[row][1]);
						regexpTextField.requestFocus();
					} else {
						int cursorPosition = regexpTextField.getCaretPosition();
						String newText = text.substring(0, cursorPosition) + insertionString +
										 (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
						regexpTextField.setText(newText);
						regexpTextField.setCaretPosition(cursorPosition + insertionString.length() + regexpConstructInsertionCaretAdjustment[row]);
						regexpTextField.setSelectionStart(cursorPosition + regexpConstructInsertionSelectionIndices[row][0]);
						regexpTextField.setSelectionEnd(cursorPosition + regexpConstructInsertionSelectionIndices[row][1]);
						regexpTextField.requestFocus();
					}
					fireRegularExpressionUpdated();
				}
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		regexpConstructTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -8024658831923934442L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (column == 1) {
					Component c = super.getTableCellRendererComponent(table, value, false, false, row, column);
					c.setFont(c.getFont().deriveFont(Font.ITALIC));
					return c;
				} else {
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				}
			}		
		});
        for (int i = 0; i < regexpConstructTable.getColumnCount(); i++) {
        	TableColumn column = regexpConstructTable.getColumn(regexpConstructTable.getModel().getColumnName(i));
            int width = 0;
            TableCellRenderer renderer = column.getHeaderRenderer();
             if (renderer == null) {
                renderer = regexpConstructTable.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent(regexpConstructTable, column.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;
 
            // Get maximum width of column data
            for (int r = 0; r < regexpConstructTable.getRowCount(); r++) {
                renderer = regexpConstructTable.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(regexpConstructTable, regexpConstructTable.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            // Set the width
            column.setPreferredWidth(width);
        }
		JScrollPane regexConstructTablePane = new JScrollPane(regexpConstructTable);

		
		// shortcuts table
		final TableModel regexpShortcutsTableModel = new TableModel() {
			//private static final long serialVersionUID = 3081581916411341948L;

			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}

			public String getColumnName(int columnIndex) {
				switch (columnIndex) {
				case 0:
					return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.shortcuts_table.shortcuts_header");
				case 1:
					return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.shortcuts_table.description_header");
				}
				return null;
			}
			
			public int getColumnCount() {
				return 2;
			}

			public int getRowCount() {
				return regexpShortcuts.length;
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				return regexpShortcuts[rowIndex][columnIndex];
			}

			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
			public void setValueAt(Object value, int rowIndex, int columnIndex) {}
		};
		final JTable regexpShortcutsTable = new JTable(regexpShortcutsTableModel);
		regexpShortcutsTable.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.shortcuts_table.tip"));
		regexpShortcutsTable.setCellSelectionEnabled(true);
		regexpShortcutsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		regexpShortcutsTable.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String text = regexpTextField.getText();
					int cursorPosition = regexpTextField.getCaretPosition();
					String insertionString = regexpShortcutsTableModel.getValueAt(regexpShortcutsTable.getSelectedRow(), 0).toString();
					String newText = text.substring(0, cursorPosition) + insertionString +
									 (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
					regexpTextField.setText(newText);
					regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
					regexpTextField.requestFocus();
					fireRegularExpressionUpdated();
				}
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		regexpShortcutsTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -8024658831923934442L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (column == 1) {
					Component c = super.getTableCellRendererComponent(table, value, false, false, row, column);
					c.setFont(c.getFont().deriveFont(Font.ITALIC));
					return c;
				} else {
					return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				}
			}
			
		});
        for (int i = 0; i < regexpShortcutsTable.getColumnCount(); i++) {
        	TableColumn column = regexpShortcutsTable.getColumn(regexpShortcutsTable.getModel().getColumnName(i));
            int width = 0;
            TableCellRenderer renderer = column.getHeaderRenderer();
             if (renderer == null) {
                renderer = regexpShortcutsTable.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent(regexpShortcutsTable, column.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;
 
            // Get maximum width of column data
            for (int r = 0; r < regexpShortcutsTable.getRowCount(); r++) {
                renderer = regexpShortcutsTable.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(regexpShortcutsTable, regexpShortcutsTable.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            // Set the width
            column.setPreferredWidth(width);
        }		
		JScrollPane regexShortcutsTablePane = new JScrollPane(regexpShortcutsTable);

		
		
		// highlight demo
		inlineSearchDocument = new RegexpSearchStyledDocument();
		inlineReplaceDocument = new RegexpReplaceStyledDocument();
		
		// highlight table
		// TODO: Do we have to override the renderer class?
		DefaultListCellRenderer resultCellRenderer = new DefaultListCellRenderer() {
			
			private static final long serialVersionUID = 1L;

			@Override
			 public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				 super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
				 
				 Border border = null;
			        if (cellHasFocus) {
			            if (isSelected) {
			                border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
			            }
			            if (border == null) {
			                border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
			            }
			        } else {
			            border = getNoFocusBorder();
			        }
				setBorder(border);
				return this;
			 }
			
			private Border getNoFocusBorder() {
				Border border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray);
				return border;
		    }
		};
		
		/*RegExpResult[] testdata = new RegExpResult[2];
		testdata[0] = new RegExpResult("abc", "2", "1");
		testdata[1] = new RegExpResult("blubblub", "47", "2");*/
		JList regexpFindingsList = new JList(resultsListModel);
		regexpFindingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		regexpFindingsList.setLayoutOrientation(JList.VERTICAL);
		regexpFindingsList.setCellRenderer(resultCellRenderer);
		
		/*
		final JTable regexpFindingsTable = new JTable(regexpFindingsTableModel);
		// TODO: I18N
		regexpFindingsTable.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.shortcuts_table.tip"));
		regexpFindingsTable.setCellSelectionEnabled(true);
		regexpFindingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		*/
		
		// regexp panel on left side of dialog
		JPanel regexpPanel = new JPanel(new GridBagLayout());
		regexpPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.border")));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		regexpPanel.add(regexpTextField, c);
		
		//ArrowButton regexpShortcutButton = new ArrowButton(ArrowButton.SOUTH);
		
		//JPanel arrowButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		//final Action autoWireAction = new InsertionAction("test", "einf�gen");
		final Action nullAction = new DefaultAction();
		PlainArrowDropDownButton autoWireDropDownButton = PlainArrowDropDownButton.makeDropDownButton(nullAction);
		//autoWireDropDownButton.add(autoWireAction);
		

		for (String[] popupItem : (String[][])ArrayUtils.addAll(regexpConstructs, regexpShortcuts)) {
			String shortcut = popupItem[0].length() > 14 ? popupItem[0].substring(0, 14) + "..." : popupItem[0];
			autoWireDropDownButton.add(new InsertionAction("<html><table border=0 cellpadding=0 cellspacing=0><tr><td width=100>" + shortcut +  "</td><td>"+ popupItem[1] + "</td></tr></table></html>", popupItem[0]));
		}
		//autoWireDropDownButton.add(new InsertionAction("test", "einf"));
		//autoWireDropDownButton.addToToolBar(toolbar, ViewToolBar.RIGHT);
		//autoWireDropDownButton.addToFlowLayoutPanel(arrowButtonPanel);
		c.insets = new Insets(4, 0, 4, 0);
		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		regexpPanel.add(autoWireDropDownButton.getDropDownArrowButton(), c);
		
		
		c.insets = new Insets(4, 0, 4, 4);
		c.gridx = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JButton clearRegexpTextFieldButton = new JButton(SwingTools.createIcon("16/delete2.png"));
		clearRegexpTextFieldButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				regexpTextField.setText("");
				fireRegularExpressionUpdated();
				regexpTextField.requestFocusInWindow();
			}
		});
		

		regexpPanel.add(clearRegexpTextFieldButton, c);
		

		errorMessage = new JLabel(NO_ERROR_MESSAGE, NO_ERROR_ICON, JLabel.LEFT);
		errorMessage.setFocusable(false);
		c.insets = new Insets(4, 8, 4, 4);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		regexpPanel.add(errorMessage, c);
		
		// new box - replacement
		
		JPanel replacementPanel = new JPanel(new GridBagLayout());
		replacementPanel.setBorder(createTitledBorder("Replacement (for preview only)"));
		
		JPanel testerPanel = new JPanel(new GridBagLayout());
		//testerPanel.setBorder(createBorder());
		
		c.insets = new Insets(4, 4, 4, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		replacementPanel.add(replacementTextField, c);
		
		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		JTabbedPane tips = new JTabbedPane();
		tips.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.constructs_table.title"), regexConstructTablePane);
		tips.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.shortcuts_table.title"), regexShortcutsTablePane);
		//tips.setPreferredSize(new Dimension(100,400));
		tips.setMinimumSize(new Dimension(100,222));
		//replacementPanel.add(tips, c);
		
		// create panel for placing the tester textfields
		
		JPanel inlineSearchPanel = new JPanel(new GridBagLayout());
		
		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		inlineSearchPanel.add(new JLabel("Text Search:"), c);
		
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		inlineSearchPanel.add(new JScrollPane(new JTextPane(inlineSearchDocument)), c);
		
		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		inlineSearchPanel.add(new JLabel("Replaced Text:"), c);
		
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 3;
		c.weightx =1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		JTextPane replaceTextPane = new JTextPane(inlineReplaceDocument);
		replaceTextPane.setEditable(false);
		inlineSearchPanel.add(new JScrollPane(replaceTextPane), c);
		
		// create tab panel
		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		testExp = new JTabbedPane();
		testExp.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.inline_search.title"), new JScrollPane(inlineSearchPanel));
		testExp.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title"), new JScrollPane(regexpFindingsList));
		//testExp.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.inline_search.title"), new JScrollPane(new JTextPane(demo)));
		//testExp.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title"), new JScrollPane(regexpFindingsList));
		testerPanel.add(testExp, c);
		
		JPanel groupPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		groupPanel.add(regexpPanel, c);
		
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		groupPanel.add(replacementPanel, c);
		
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		groupPanel.add(testerPanel, c);
		
		panel.add(groupPanel, 1, 0);
		
		if (supportsItems) {
			// item shortcuts list
			itemShortcutsList = new JList(items.toArray());
			itemShortcutsList.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.item_shortcuts.tip"));
			itemShortcutsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			itemShortcutsList.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						String text = regexpTextField.getText();
						int cursorPosition = regexpTextField.getCaretPosition();
						int index = itemShortcutsList.getSelectedIndex();
						if (index > -1 && index < itemShortcutsList.getModel().getSize()) { 
							String insertionString = itemShortcutsList.getModel().getElementAt(index).toString();
							String newText = text.substring(0, cursorPosition) + insertionString +
											 (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
							regexpTextField.setText(newText);
							regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
							regexpTextField.requestFocus();
							fireRegularExpressionUpdated();
						}
					}
				}
				public void mouseEntered(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseReleased(MouseEvent e) {}
			});
			JScrollPane itemShortcutsPane = new JScrollPane(itemShortcutsList);
			itemShortcutsPane.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.item_shortcuts.border")));
			
			// matched items list
			matchedItemsListModel = new DefaultListModel();
			JList matchedItemsList = new JList(matchedItemsListModel);
			matchedItemsList.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.matched_items.tip"));
			// add custom cell renderer to disallow selections
			matchedItemsList.setCellRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = -5795848004756768378L;
	
				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					return super.getListCellRendererComponent(list, value, index, false, false);
				}
			});
			JScrollPane matchedItemsPanel = new JScrollPane(matchedItemsList);
			matchedItemsPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.matched_items.border")));
	
			// item panel on right side of dialog
			JPanel itemPanel = new JPanel(createGridLayout(1, 2));
			itemPanel.add(itemShortcutsPane, 0, 0);
			itemPanel.add(matchedItemsPanel, 0, 1);

			panel.add(itemPanel, 0, 1);
		}

		okButton = makeOkButton();
		fireRegularExpressionUpdated();
		
		layoutDefault(panel, supportsItems ? NORMAL : NARROW, okButton, makeCancelButton());
	}

	private void fireRegularExpressionUpdated() {
		boolean regularExpressionValid = false;
		Pattern pattern = null; 
		try {
			pattern = Pattern.compile(regexpTextField.getText());
			regularExpressionValid = true;
		} catch (PatternSyntaxException e) {
			regularExpressionValid = false;
		}
		if (supportsItems) {
			matchedItemsListModel.clear();
			if (regularExpressionValid && pattern != null) {
				for (String previewString : items) {
					if (pattern.matcher(previewString).matches()) {
						matchedItemsListModel.addElement(previewString);
					}
				}
			}
		}
		if (regularExpressionValid) {
			errorMessage.setText(NO_ERROR_MESSAGE);
			errorMessage.setIcon(NO_ERROR_ICON);
			okButton.setEnabled(true);
			inlineSearchDocument.updatePattern(regexpTextField.getText());
			
		} else {
			errorMessage.setText(ERROR_MESSAGE);
			errorMessage.setIcon(ERROR_ICON);
			okButton.setEnabled(false);
			inlineSearchDocument.clearResults();
		}
	}
	
	private class InsertionAction extends AbstractAction {
		private static final long serialVersionUID = -5185173378762191200L;
		private final String insertionString;
		
		public InsertionAction(String title, String insertionString) {
			putValue(Action.NAME, title);
			this.insertionString = insertionString;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			//super.actionPerformed(e);
			String text = regexpTextField.getText();
			int cursorPosition = regexpTextField.getCaretPosition();
			//String insertionString = regexpShortcutsTableModel.getValueAt(regexpShortcutsTable.getSelectedRow(), 0).toString();
			String newText = text.substring(0, cursorPosition) + insertionString +
							 (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
			regexpTextField.setText(newText);
			regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
			regexpTextField.requestFocus();
			fireRegularExpressionUpdated();
		}
	}
	
	/*
	private class InsertionButtonAction extends DefaultAction {
		private static final long serialVersionUID = -5185173378762191200L;
		private final String insertionString;
		
		public InsertionButtonAction(String title, String insertionString) {
			putValue(Action.NAME, title);
			this.insertionString = insertionString;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			String text = regexpTextField.getText();
			int cursorPosition = regexpTextField.getCaretPosition();
			//String insertionString = regexpShortcutsTableModel.getValueAt(regexpShortcutsTable.getSelectedRow(), 0).toString();
			String newText = text.substring(0, cursorPosition) + insertionString +
							 (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
			regexpTextField.setText(newText);
			regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
			regexpTextField.requestFocus();
			fireRegularExpressionUpdated();
		}
	}*/	
	
	
	public String getRegexp() {
		return regexpTextField.getText();
	}
}
