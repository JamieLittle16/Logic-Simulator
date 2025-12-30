package uk.ac.cam.jml229.logic.app;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Theme {

  public static boolean isDarkMode = false;

  // --- Core Colours ---
  public static Color BACKGROUND = Color.WHITE;
  public static Color GRID_MAJOR = new Color(220, 220, 220);
  public static Color GRID_MINOR = new Color(240, 240, 240);
  public static Color TEXT_COLOR = Color.BLACK;
  public static Color TEXT_INVERTED = Color.WHITE; // Text on dark headers

  // --- Aliases ---
  public static Color GRID_COLOR = GRID_MAJOR;
  public static Color PANEL_BACKGROUND = BACKGROUND;
  public static Color SELECTION_FILL = new Color(0, 120, 255, 50);

  public static Color WIRE_ON = new Color(0, 150, 0);
  public static Color WIRE_OFF = new Color(100, 100, 100);
  public static Color WIRE_SELECTED = new Color(0, 120, 255);
  public static Color WIRE_HANDLE_FILL = Color.WHITE;

  public static Color COMP_BORDER = Color.BLACK;
  public static Color COMP_FILL_GRADIENT_1 = new Color(240, 240, 255);
  public static Color COMP_FILL_GRADIENT_2 = new Color(200, 200, 255);
  public static Color SELECTION_BORDER = new Color(0, 120, 255);
  public static Color GATE_BUBBLE_FILL = Color.WHITE;

  public static Color GENERIC_BOX_FILL = new Color(230, 230, 230);
  public static Color GENERIC_HEADER_FILL = new Color(100, 100, 255);

  // --- Switch Styling ---
  public static Color SWITCH_FILL = new Color(200, 200, 200);
  public static Color SWITCH_ON = new Color(0, 150, 0);
  public static Color SWITCH_OFF = new Color(220, 220, 220);
  public static Color SWITCH_SHADOW = new Color(50, 50, 50, 100);
  public static Color SWITCH_HIGHLIGHT = new Color(255, 255, 255, 100);

  // --- LED / Probe Styling ---
  public static Color LED_ON = new Color(255, 220, 0);
  public static Color LED_OFF = new Color(50, 50, 50);
  public static Color LED_REFLECTION = new Color(255, 255, 255, 100);

  // --- 7-Seg / Hex Display Styling ---
  public static Color DISPLAY_HOUSING = new Color(20, 20, 20);
  public static Color DISPLAY_SCREEN = new Color(40, 40, 40);
  public static Color DISPLAY_BORDER = Color.GRAY;
  public static Color SEGMENT_ON = new Color(255, 50, 50);
  public static Color SEGMENT_OFF = new Color(60, 0, 0);

  public static Color CLOCK_BACKGROUND = new Color(40, 40, 40);

  public static Color PIN_COLOR = new Color(0, 0, 150);
  public static Color STUB_COLOR = Color.GRAY;
  public static Color HOVER_COLOR = new Color(255, 100, 100);

  public static Color PALETTE_BACKGROUND = new Color(245, 245, 245);
  public static Color PALETTE_HEADINGS = new Color(80, 80, 80);
  public static Color BUTTON_BACKGROUND = Color.WHITE;
  public static Color BUTTON_BORDER = new Color(200, 200, 200);

  public static Color BUTTON_HOVER = new Color(200, 225, 255);
  public static Color SCROLL_TRACK = new Color(245, 245, 245);
  public static Color SCROLL_THUMB = new Color(200, 200, 200);

  public static void loadTheme(String themeName) {
    Properties props = new Properties();
    try {
      File userFile = new File(System.getProperty("user.home") + "/.logik/themes/" + themeName + ".properties");
      if (userFile.exists()) {
        try (InputStream in = new FileInputStream(userFile)) {
          props.load(in);
        }
      } else {
        try (InputStream in = Theme.class.getResourceAsStream("/themes/" + themeName + ".properties")) {
          if (in != null)
            props.load(in);
          else {
            setDarkMode(themeName.toLowerCase().contains("dark"));
            return;
          }
        }
      }
      applyProperties(props);
    } catch (Exception e) {
      System.err.println("Failed to load theme: " + themeName);
      setDarkMode(false);
    }
  }

  private static void applyProperties(Properties p) {
    isDarkMode = Boolean.parseBoolean(p.getProperty("isDark", "false"));

    BACKGROUND = parseColor(p, "background", BACKGROUND);
    GRID_MAJOR = parseColor(p, "grid", GRID_MAJOR);
    GRID_MINOR = parseColor(p, "gridMinor", GRID_MINOR);
    TEXT_COLOR = parseColor(p, "text", TEXT_COLOR);
    TEXT_INVERTED = parseColor(p, "textInverted", TEXT_INVERTED);

    WIRE_ON = parseColor(p, "wireOn", WIRE_ON);
    WIRE_OFF = parseColor(p, "wireOff", WIRE_OFF);
    WIRE_SELECTED = parseColor(p, "wireSelected", WIRE_SELECTED);
    WIRE_HANDLE_FILL = parseColor(p, "wireHandleFill", WIRE_HANDLE_FILL);

    COMP_BORDER = parseColor(p, "compBorder", COMP_BORDER);
    COMP_FILL_GRADIENT_1 = parseColor(p, "compFillGradient1", COMP_FILL_GRADIENT_1);
    COMP_FILL_GRADIENT_2 = parseColor(p, "compFillGradient2", COMP_FILL_GRADIENT_2);
    SELECTION_BORDER = parseColor(p, "selectionBorder", SELECTION_BORDER);
    GATE_BUBBLE_FILL = parseColor(p, "gateBubbleFill", GATE_BUBBLE_FILL);

    PIN_COLOR = parseColor(p, "pin", PIN_COLOR);
    STUB_COLOR = parseColor(p, "stub", STUB_COLOR);
    HOVER_COLOR = parseColor(p, "hover", HOVER_COLOR);
    CLOCK_BACKGROUND = parseColor(p, "clockBackground", new Color(40, 40, 40));

    PALETTE_BACKGROUND = parseColor(p, "paletteBackground", PALETTE_BACKGROUND);
    PALETTE_HEADINGS = parseColor(p, "paletteHeadings", PALETTE_HEADINGS);
    BUTTON_BACKGROUND = parseColor(p, "buttonBackground", BUTTON_BACKGROUND);
    BUTTON_BORDER = parseColor(p, "buttonBorder", BUTTON_BORDER);

    Color defHover = isDarkMode ? new Color(60, 60, 60) : new Color(200, 225, 255);
    Color defScrollTrack = isDarkMode ? PALETTE_BACKGROUND : new Color(245, 245, 245);
    Color defScrollThumb = isDarkMode ? new Color(80, 80, 80) : new Color(200, 200, 200);

    BUTTON_HOVER = parseColor(p, "buttonHover", defHover);
    SCROLL_TRACK = parseColor(p, "scrollTrack", defScrollTrack);
    SCROLL_THUMB = parseColor(p, "scrollThumb", defScrollThumb);

    SWITCH_ON = parseColor(p, "switchOn", WIRE_ON);
    SWITCH_OFF = parseColor(p, "switchOff", SWITCH_OFF);

    LED_ON = parseColor(p, "ledOn", LED_ON);
    LED_OFF = parseColor(p, "ledOff", LED_OFF);

    SEGMENT_ON = parseColor(p, "segmentOn", SEGMENT_ON);
    SEGMENT_OFF = parseColor(p, "segmentOff", SEGMENT_OFF);
    DISPLAY_HOUSING = parseColor(p, "displayHousing", DISPLAY_HOUSING);
    DISPLAY_SCREEN = parseColor(p, "displayScreen", DISPLAY_SCREEN);

    Color defBox = isDarkMode ? COMP_FILL_GRADIENT_1 : new Color(230, 230, 230);
    Color defHeader = isDarkMode ? COMP_BORDER : new Color(100, 100, 255);
    Color defSwitch = isDarkMode ? COMP_FILL_GRADIENT_2 : new Color(200, 200, 200);

    GENERIC_BOX_FILL = parseColor(p, "genericBoxFill", defBox);
    GENERIC_HEADER_FILL = parseColor(p, "genericHeaderFill", defHeader);
    SWITCH_FILL = parseColor(p, "switchFill", defSwitch);

    GRID_COLOR = GRID_MAJOR;
    PANEL_BACKGROUND = BACKGROUND;
    SELECTION_FILL = new Color(SELECTION_BORDER.getRed(), SELECTION_BORDER.getGreen(), SELECTION_BORDER.getBlue(), 50);
  }

  private static Color parseColor(Properties p, String key, Color defaultColor) {
    String val = p.getProperty(key);
    if (val == null)
      return defaultColor;
    try {
      return Color.decode(val);
    } catch (NumberFormatException e) {
      return defaultColor;
    }
  }

  public static void setDarkMode(boolean dark) {
    isDarkMode = dark;
    if (dark) {
      // --- DARK MODE ---
      BACKGROUND = new Color(30, 30, 30);
      GRID_MAJOR = new Color(50, 50, 50);
      GRID_MINOR = new Color(40, 40, 40);
      TEXT_COLOR = new Color(220, 220, 220);
      TEXT_INVERTED = new Color(220, 220, 220); // White text on dark BG

      WIRE_ON = new Color(46, 204, 113);
      WIRE_OFF = new Color(85, 90, 100);
      WIRE_SELECTED = new Color(52, 152, 219);
      WIRE_HANDLE_FILL = new Color(45, 45, 48); // Dark handle

      SWITCH_ON = new Color(46, 204, 113);
      SWITCH_OFF = new Color(80, 80, 80); // Darker grey switch

      COMP_BORDER = new Color(120, 120, 120);
      COMP_FILL_GRADIENT_1 = new Color(45, 45, 48);
      COMP_FILL_GRADIENT_2 = new Color(37, 37, 38);
      GATE_BUBBLE_FILL = new Color(45, 45, 48); // Matches component body

      SELECTION_BORDER = new Color(52, 152, 219);
      PIN_COLOR = new Color(100, 200, 255);
      STUB_COLOR = new Color(100, 100, 100);
      HOVER_COLOR = new Color(255, 100, 100);

      CLOCK_BACKGROUND = new Color(25, 25, 25);

      PALETTE_BACKGROUND = new Color(37, 37, 38);
      PALETTE_HEADINGS = new Color(200, 200, 200);
      BUTTON_BACKGROUND = new Color(50, 50, 50);
      BUTTON_BORDER = new Color(80, 80, 80);
      BUTTON_HOVER = new Color(70, 70, 70);

      SCROLL_TRACK = new Color(37, 37, 38);
      SCROLL_THUMB = new Color(80, 80, 80);

      GENERIC_BOX_FILL = COMP_FILL_GRADIENT_1;
      GENERIC_HEADER_FILL = COMP_BORDER;
      SWITCH_FILL = COMP_FILL_GRADIENT_2;

      // Displays
      DISPLAY_HOUSING = new Color(20, 20, 20);
      DISPLAY_SCREEN = new Color(30, 30, 30);
      DISPLAY_BORDER = new Color(80, 80, 80);
      SEGMENT_ON = new Color(255, 60, 60);
      SEGMENT_OFF = new Color(60, 20, 20);

    } else {
      // --- LIGHT MODE ---
      BACKGROUND = new Color(250, 250, 250);
      GRID_MAJOR = new Color(225, 225, 225);
      GRID_MINOR = new Color(245, 245, 245);
      TEXT_COLOR = new Color(30, 30, 30);
      TEXT_INVERTED = Color.WHITE;

      WIRE_ON = new Color(0, 150, 0);
      WIRE_OFF = new Color(160, 160, 170);
      WIRE_SELECTED = new Color(0, 120, 255);
      WIRE_HANDLE_FILL = Color.WHITE;

      SWITCH_ON = new Color(0, 150, 0);
      SWITCH_OFF = new Color(220, 220, 220); // Light grey switch

      COMP_BORDER = new Color(50, 50, 50);
      COMP_FILL_GRADIENT_1 = new Color(225, 245, 255);
      COMP_FILL_GRADIENT_2 = new Color(200, 230, 255);
      GATE_BUBBLE_FILL = Color.WHITE;

      SELECTION_BORDER = new Color(0, 120, 255);
      PIN_COLOR = new Color(0, 80, 180);
      STUB_COLOR = new Color(150, 150, 150);
      HOVER_COLOR = new Color(255, 80, 80);

      CLOCK_BACKGROUND = new Color(40, 40, 40);

      PALETTE_BACKGROUND = new Color(240, 242, 245);
      PALETTE_HEADINGS = new Color(100, 100, 100);
      BUTTON_BACKGROUND = Color.WHITE;
      BUTTON_BORDER = new Color(210, 210, 210);
      BUTTON_HOVER = new Color(200, 225, 255);

      SCROLL_TRACK = PALETTE_BACKGROUND;
      SCROLL_THUMB = new Color(200, 200, 210);

      GENERIC_BOX_FILL = new Color(235, 235, 235);
      GENERIC_HEADER_FILL = new Color(100, 100, 255);
      SWITCH_FILL = new Color(220, 220, 220);

      // Displays
      DISPLAY_HOUSING = new Color(20, 20, 20);
      DISPLAY_SCREEN = new Color(40, 40, 40);
      DISPLAY_BORDER = Color.GRAY;
      SEGMENT_ON = new Color(255, 50, 50);
      SEGMENT_OFF = new Color(60, 0, 0);
    }

    GRID_COLOR = GRID_MAJOR;
    PANEL_BACKGROUND = BACKGROUND;
    SELECTION_FILL = new Color(SELECTION_BORDER.getRed(), SELECTION_BORDER.getGreen(), SELECTION_BORDER.getBlue(), 50);
  }
}
