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
package com.rapidminer.gui.new_plotter.templates.style;

import java.awt.Font;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.rapidminer.gui.new_plotter.templates.style.ColorScheme.ColorRGB;
import com.rapidminer.tools.LogService;

/**
 * The default {@link PlotterStyleProvider}. Lets the user choose a font
 * for the axes, title and legend, and lets him choose a color scheme for the plot.
 * 
 * @author Marco Boeck
 *
 */
public class DefaultPlotterStyleProvider extends PlotterStyleProvider {
	
	private static final String COLOR_B_ATTRIBUTE = "b";

	private static final String COLOR_G_ATTRIBUTE = "g";

	private static final String COLOR_R_ATTRIBUTE = "r";

	private static final String COLOR_ALPHA_ATTRIBUTE = "alpha";

	private static final String COLOR_ELEMENT = "color";

	private static final String COLORS_ELEMENT = "colors";

	private static final String GRADIENT_END_B_ATTRIBUTE = "gradient_end_b";

	private static final String GRADIENT_END_G_ATTRIBUTE = "gradient_end_g";

	private static final String GRADIENT_END_R_ATTRIBUTE = "gradient_end_r";

	private static final String GRADIENT_END_ALPHA_ATTRIBUTE = "gradient_end_alpha";

	private static final String GRADIENT_START_B_ATTRIBUTE = "gradient_start_b";

	private static final String GRADIENT_START_G_ATTRIBUTE = "gradient_start_g";

	private static final String GRADIENT_START_R_ATTRIBUTE = "gradient_start_r";

	private static final String GRADIENT_START_ALPHA_ATTRIBUTE = "gradient_start_alpha";

	private static final String COLOR_SCHEME_ELEMENT = "color_scheme";

	private static final String TITLE_FONT_ELEMENT = "title_font";

	private static final String LEGEND_FONT_ELEMENT = "legend_font";

	private static final String FONT_SIZE_ATTRIBUTE = "size";

	private static final String FONT_STYLE_ATTRIBUTE = "style";

	private static final String FONT_NAME_ATTRIBUTE = "name";

	private static final String AXES_FONT_ELEMENT = "axes_font";
	
	private static final String SCHEME_NAME_ATTRIBUTE = "name";
	

	/** the list containing all {@link ColorScheme}s. */
	private List<ColorScheme> listOfColorSchemes;
	
	/** the list containing all default {@link ColorScheme}s. */
	private List<ColorScheme> listOfDefaultColorSchemes;
	
	/** the synchronized object */
	private Object synchronizeColorSchemeListObject = new Object();
	
	/** the index pointing to the currently used color scheme */
	private int colorSchemeIndex;
	
	/** the current axes font */
	private Font axesFont;
	
	/** the current legend font */
	private Font legendFont;
	
	/** the current title font */
	private Font titleFont;
	
	/** the font size for the font buttons */
	public static final int FONT_SIZE_DEFAULT = 12;
	
	
	
	/**
	 * Creates a new {@link DefaultPlotterStyleProvider}.
	 */
	public DefaultPlotterStyleProvider() {
		listOfColorSchemes = new LinkedList<ColorScheme>();
		listOfDefaultColorSchemes = new LinkedList<ColorScheme>();
		colorSchemeIndex = 0;
		
		/*
		 * default color schemes are defined here
		 */
		List<ColorRGB> listOfColors = new LinkedList<ColorRGB>();
		listOfColors.add(new ColorRGB(222, 217, 26));
		listOfColors.add(new ColorRGB(219, 138, 47));
		listOfColors.add(new ColorRGB(217, 26, 21));
		listOfColors.add(new ColorRGB(83, 70, 255));
		listOfColors.add(new ColorRGB(156, 217, 84));
		ColorScheme cs = new ColorScheme("Colorful", listOfColors);
		listOfColorSchemes.add(cs);
		listOfDefaultColorSchemes.add(cs);
		
		listOfColors = new LinkedList<ColorRGB>();
		listOfColors.add(new ColorRGB(94, 173, 0));
		listOfColors.add(new ColorRGB(255, 188, 10));
		listOfColors.add(new ColorRGB(189, 39, 53));
		listOfColors.add(new ColorRGB(255, 119, 0));
		listOfColors.add(new ColorRGB(81, 17, 84));
		cs = new ColorScheme("Forest", listOfColors);
		listOfColorSchemes.add(cs);
		listOfDefaultColorSchemes.add(cs);
		/*
		 * end default color schemes
		 */
		
		axesFont = new Font("Dialog", Font.PLAIN, 10);
		legendFont = new Font("Dialog", Font.PLAIN, 10);
		titleFont = new Font("Dialog", Font.PLAIN, 10);
	}

	

	@Override
	public JPanel getStyleProviderPanel() {
		return new DefaultPlotterStyleProviderGUI(this);
	}

	@Override
	public Font getAxesFont() {
		return axesFont;
	}
	
	/**
	 * Sets the axes {@link Font}.
	 * @param axesFont
	 */
	public void setAxesFont(Font axesFont) {
		if (axesFont == null) {
			throw new IllegalArgumentException("axesFont must not be null!");
		}
		
		this.axesFont = axesFont;
		
		setChanged();
		notifyObservers();
	}

	@Override
	public Font getLegendFont() {
		return legendFont;
	}
	
	/**
	 * Sets the legend {@link Font}.
	 * @param legendFont
	 */
	public void setLegendFont(Font legendFont) {
		if (legendFont == null) {
			throw new IllegalArgumentException("legendFont must not be null!");
		}
		
		this.legendFont = legendFont;
		
		setChanged();
		notifyObservers();
	}

	@Override
	public Font getTitleFont() {
		return titleFont;
	}
	
	/**
	 * Sets the title {@link Font}.
	 * @param titleFont
	 */
	public void setTitleFont(Font titleFont) {
		if (titleFont == null) {
			throw new IllegalArgumentException("titleFont must not be null!");
		}
		
		this.titleFont = titleFont;
		
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Set the index of the currently selected {@link ColorScheme}.
	 * @param index
	 */
	public void setSelectedColorSchemeIndex(int index) {
		synchronized(synchronizeColorSchemeListObject) {
			if (index < 0 || index >= listOfColorSchemes.size()) {
				throw new IllegalArgumentException("index must be >= 0 and <= number of available color schemes!");
			}
		}
		
		colorSchemeIndex = index;
		
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Sets the selected {@link ColorScheme}. If the given {@link ColorScheme} is not in the current list of schemes,
	 * it will be appended at the end.
	 * @param colorScheme the newly selected {@link ColorScheme}
	 */
	public void setSelectedColorScheme(ColorScheme colorScheme) {
		if (colorScheme == null) {
			throw new IllegalArgumentException("colorScheme must not be null!");
		}
		
		synchronized(synchronizeColorSchemeListObject) {
			if (!listOfColorSchemes.contains(colorScheme)) {
				addColorScheme(colorScheme);
			}
			setSelectedColorSchemeIndex(listOfColorSchemes.indexOf(colorScheme));
		}
	}
	
	/**
	 * Returns the index of the currently selected {@link ColorScheme}.
	 * @return
	 */
	public int getSelectedColorSchemeIndex() {
		return colorSchemeIndex;
	}
	
	/**
	 * Returns the list of available {@link ColorScheme}s. Notice that this list is a copy
	 * so direct modification of the {@link DefaultPlotterStyleProvider} is not possible.
	 * @return
	 */
	public List<ColorScheme> getColorSchemes() {
		List<ColorScheme> newList;
		synchronized(synchronizeColorSchemeListObject) {
			newList = new LinkedList<ColorScheme>(listOfColorSchemes);
		}
		return newList;
	}
	
	/**
	 * Appends the given {@link ColorScheme} to the list of available {@link ColorScheme}s.
	 * Duplicates are not allowed and will result in an {@link IllegalArgumentException}.
	 * @param colorScheme
	 */
	public void addColorScheme(ColorScheme colorScheme) {
		addColorScheme(colorScheme, listOfColorSchemes.size());
	}
	
	/**
	 * Adds the given {@link ColorScheme} to the list of available {@link ColorScheme}s at the specified index.
	 * Note that inserting before the default ColorSchemes is not supported and will result in an {@link IllegalArgumentException}.
	 * Duplicates are not allowed and will result in an {@link IllegalArgumentException}.
	 * @param colorScheme
	 */
	public void addColorScheme(ColorScheme colorScheme, int index) {
		if (colorScheme == null) {
			throw new IllegalArgumentException("colorScheme must not be null!");
		}
		if (index < listOfDefaultColorSchemes.size()) {
			throw new IllegalArgumentException("Cannot add a ColorScheme before the default ColorSchemes!");
		}
		index = Math.min(listOfColorSchemes.size(), index);
		
		synchronized(synchronizeColorSchemeListObject) {
			if (!listOfColorSchemes.contains(colorScheme)) {
				listOfColorSchemes.add(index, colorScheme);
			} else {
				throw new IllegalArgumentException("duplicate colorScheme not allowed!");
			}
		}
		
		setChanged();
		notifyObservers();
	}
	
	/**
	 * Removes the given {@link ColorScheme} with the given index.
	 * Will throw an {@link IllegalArgumentException} if trying to remove a default {@link ColorScheme}.
	 * @param index
	 */
	public void removeColorScheme(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("index must not be < 0!");
		}
		if (index < listOfDefaultColorSchemes.size()) {
			throw new IllegalArgumentException("Cannot remove a default ColorScheme!");
		}
		
		synchronized(synchronizeColorSchemeListObject) {
			listOfColorSchemes.remove(index);
		}
	}
	
	/**
	 * Removes the given {@link ColorScheme}.
	 * Will throw an {@link IllegalArgumentException} if trying to remove a default {@link ColorScheme}.
	 * @param colorScheme
	 */
	public void removeColorScheme(ColorScheme colorScheme) {
		synchronized(synchronizeColorSchemeListObject) {
			if (listOfDefaultColorSchemes.contains(colorScheme)) {
				throw new IllegalArgumentException("Cannot remove a default ColorScheme!");
			}
			
			listOfColorSchemes.remove(colorScheme);
		}
	}

	@Override
	public ColorScheme getColorScheme() {
		ColorScheme scheme;
		synchronized(synchronizeColorSchemeListObject) {
			scheme = listOfColorSchemes.get(colorSchemeIndex);
		}
		return scheme;
	}
	
	@Override
	public Element createXML(Document document) {
		Element styleElement = document.createElement(STYLE_ELEMENT);
		
		// store axes font
		Element axesFont = document.createElement(AXES_FONT_ELEMENT);
		axesFont.setAttribute(FONT_NAME_ATTRIBUTE, String.valueOf(getAxesFont().getName()));
		axesFont.setAttribute(FONT_STYLE_ATTRIBUTE, String.valueOf(getAxesFont().getStyle()));
		axesFont.setAttribute(FONT_SIZE_ATTRIBUTE, String.valueOf(getAxesFont().getSize()));
		styleElement.appendChild(axesFont);
		
		// store legend font
		Element legendFont = document.createElement(LEGEND_FONT_ELEMENT);
		legendFont.setAttribute(FONT_NAME_ATTRIBUTE, String.valueOf(getLegendFont().getName()));
		legendFont.setAttribute(FONT_STYLE_ATTRIBUTE, String.valueOf(getLegendFont().getStyle()));
		legendFont.setAttribute(FONT_SIZE_ATTRIBUTE, String.valueOf(getLegendFont().getSize()));
		styleElement.appendChild(legendFont);
		
		// store title font
		Element titleFont = document.createElement(TITLE_FONT_ELEMENT);
		titleFont.setAttribute(FONT_NAME_ATTRIBUTE, String.valueOf(getTitleFont().getName()));
		titleFont.setAttribute(FONT_STYLE_ATTRIBUTE, String.valueOf(getTitleFont().getStyle()));
		titleFont.setAttribute(FONT_SIZE_ATTRIBUTE, String.valueOf(getTitleFont().getSize()));
		styleElement.appendChild(titleFont);
		
		// store currently selected color scheme
		Element selectedColorSchemeElement = document.createElement(COLOR_SCHEME_ELEMENT);
		selectedColorSchemeElement.setAttribute(SCHEME_NAME_ATTRIBUTE, String.valueOf(getColorScheme().getName()));
		selectedColorSchemeElement.setAttribute(GRADIENT_START_ALPHA_ATTRIBUTE, String.valueOf(getColorScheme().getGradientStartColor().getAlpha()));
		selectedColorSchemeElement.setAttribute(GRADIENT_START_R_ATTRIBUTE, String.valueOf(getColorScheme().getGradientStartColor().getR()));
		selectedColorSchemeElement.setAttribute(GRADIENT_START_G_ATTRIBUTE, String.valueOf(getColorScheme().getGradientStartColor().getG()));
		selectedColorSchemeElement.setAttribute(GRADIENT_START_B_ATTRIBUTE, String.valueOf(getColorScheme().getGradientStartColor().getB()));
		selectedColorSchemeElement.setAttribute(GRADIENT_END_ALPHA_ATTRIBUTE, String.valueOf(getColorScheme().getGradientEndColor().getAlpha()));
		selectedColorSchemeElement.setAttribute(GRADIENT_END_R_ATTRIBUTE, String.valueOf(getColorScheme().getGradientEndColor().getR()));
		selectedColorSchemeElement.setAttribute(GRADIENT_END_G_ATTRIBUTE, String.valueOf(getColorScheme().getGradientEndColor().getG()));
		selectedColorSchemeElement.setAttribute(GRADIENT_END_B_ATTRIBUTE, String.valueOf(getColorScheme().getGradientEndColor().getB()));
		Element colorsElement = document.createElement(COLORS_ELEMENT);
		for (ColorRGB color : getColorScheme().getColors()) {
			Element colorElement = document.createElement(COLOR_ELEMENT);
			colorElement.setAttribute(COLOR_ALPHA_ATTRIBUTE, String.valueOf(color.getAlpha()));
			colorElement.setAttribute(COLOR_R_ATTRIBUTE, String.valueOf(color.getR()));
			colorElement.setAttribute(COLOR_G_ATTRIBUTE, String.valueOf(color.getG()));
			colorElement.setAttribute(COLOR_B_ATTRIBUTE, String.valueOf(color.getB()));
			colorsElement.appendChild(colorElement);
		}
		selectedColorSchemeElement.appendChild(colorsElement);
		styleElement.appendChild(selectedColorSchemeElement);
		
		return styleElement;
	}
	
	@Override
	public void loadFromXML(Element styleElement) {
		for (int i=0; i<styleElement.getChildNodes().getLength(); i++) {
			Node node = styleElement.getChildNodes().item(i);
			if (node instanceof Element) {
				Element setting = (Element) node;
				
				if (setting.getNodeName().equals(COLOR_SCHEME_ELEMENT)) {
					try {
						// load currently selected color scheme
						List<ColorRGB> listOfColors = new LinkedList<ColorRGB>();
						for (int j=0; j<setting.getChildNodes().getLength(); j++) {
							Node colorSchemeNode = setting.getChildNodes().item(j);
							if (colorSchemeNode instanceof Element) {
								Element colorsElement = (Element) colorSchemeNode;

								if (colorsElement.getNodeName().equals(COLORS_ELEMENT)) {
									// load currently selected color scheme

									for (int k=0; k<colorsElement.getChildNodes().getLength(); k++) {
										Node colorNode = colorsElement.getChildNodes().item(k);
										if (colorNode.getNodeName().equals(COLOR_ELEMENT)) {
											Element colorElement = (Element) colorNode;
											int alpha = Integer.parseInt(colorElement.getAttribute(COLOR_ALPHA_ATTRIBUTE));
											int r = Integer.parseInt(colorElement.getAttribute(COLOR_R_ATTRIBUTE));
											int g = Integer.parseInt(colorElement.getAttribute(COLOR_G_ATTRIBUTE));
											int b = Integer.parseInt(colorElement.getAttribute(COLOR_B_ATTRIBUTE));
											ColorRGB colorRGB = new ColorRGB(r, g, b, alpha);
											listOfColors.add(colorRGB);
										}
									}

								}
							}
						}
						String name = setting.getAttribute(SCHEME_NAME_ATTRIBUTE);
						int alpha = Integer.parseInt(setting.getAttribute(GRADIENT_START_ALPHA_ATTRIBUTE));
						int r = Integer.parseInt(setting.getAttribute(GRADIENT_START_R_ATTRIBUTE));
						int g = Integer.parseInt(setting.getAttribute(GRADIENT_START_G_ATTRIBUTE));
						int b = Integer.parseInt(setting.getAttribute(GRADIENT_START_B_ATTRIBUTE));
						ColorRGB gradientStart = new ColorRGB(r, g, b, alpha);
						alpha = Integer.parseInt(setting.getAttribute(GRADIENT_END_ALPHA_ATTRIBUTE));
						r = Integer.parseInt(setting.getAttribute(GRADIENT_END_R_ATTRIBUTE));
						g = Integer.parseInt(setting.getAttribute(GRADIENT_END_G_ATTRIBUTE));
						b = Integer.parseInt(setting.getAttribute(GRADIENT_END_B_ATTRIBUTE));
						ColorRGB gradientEnd = new ColorRGB(r, g, b, alpha);
						ColorScheme colorScheme = new ColorScheme(name, listOfColors, gradientStart, gradientEnd);
						setSelectedColorScheme(colorScheme);
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore color scheme for style provider!");
					}
				} else if (setting.getNodeName().equals(AXES_FONT_ELEMENT)) {
					// load axes font
					try {
						String name = setting.getAttribute(FONT_NAME_ATTRIBUTE);
						int style = Integer.parseInt(setting.getAttribute(FONT_STYLE_ATTRIBUTE));
						int size = Integer.parseInt(setting.getAttribute(FONT_SIZE_ATTRIBUTE));
						Font axesFont = new Font(name, style, size);
						setAxesFont(axesFont);
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore axes font for style provider!");
					}
				} else if (setting.getNodeName().equals(LEGEND_FONT_ELEMENT)) {
					try {
						// load legend font
						String name = setting.getAttribute(FONT_NAME_ATTRIBUTE);
						int style = Integer.parseInt(setting.getAttribute(FONT_STYLE_ATTRIBUTE));
						int size = Integer.parseInt(setting.getAttribute(FONT_SIZE_ATTRIBUTE));
						Font legendFont = new Font(name, style, size);
						setLegendFont(legendFont);
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore legend font for style provider!");
					}
				} else if (setting.getNodeName().equals(TITLE_FONT_ELEMENT)) {
					try {
						// load title font
						String name = setting.getAttribute(FONT_NAME_ATTRIBUTE);
						int style = Integer.parseInt(setting.getAttribute(FONT_STYLE_ATTRIBUTE));
						int size = Integer.parseInt(setting.getAttribute(FONT_SIZE_ATTRIBUTE));
						Font titleFont = new Font(name, style, size);
						setTitleFont(titleFont);
					} catch (NumberFormatException e) {
						LogService.getRoot().warning("Could not restore title font for style provider!");
					}
				}
			}
		}
	}
}
