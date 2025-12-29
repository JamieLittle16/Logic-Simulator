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

  // --- Aliases ---
  public static Color GRID_COLOR = GRID_MAJOR;
  public static Color PANEL_BACKGROUND = BACKGROUND;
  public static Color SELECTION_FILL = new Color(0, 120, 255, 50);

  public static Color WIRE_ON = new Color(0, 200, 0);
  public static Color WIRE_OFF = new Color(100, 100, 100);
  public static Color WIRE_SELECTED = new Color(0, 120, 255);

  public static Color COMP_BORDER = Color.BLACK;
  public static Color COMP_FILL_GRADIENT_1 = new Color(240, 240, 255);
  public static Color COMP_FILL_GRADIENT_2 = new Color(200, 200, 255);
  public static Color SELECTION_BORDER = new Color(0, 120, 255);

  public static Color GENERIC_BOX_FILL = new Color(230, 230, 230);
  public static Color GENERIC_HEADER_FILL = new Color(100, 100, 255);
  public static Color SWITCH_FILL = new Color(200, 200, 200);
  public static Color CLOCK_BACKGROUND = new Color(40, 40, 40);

  public static Color PIN_COLOR = new Color(0, 0, 150);
  public static Color STUB_COLOR = Color.GRAY;
  public static Color HOVER_COLOR = new Color(255, 100, 100);

  public static Color PALETTE_BACKGROUND = new Color(245, 245, 245);
  public static Color PALETTE_HEADINGS = new Color(80, 80, 80);
  public static Color BUTTON_BACKGROUND = Color.WHITE;
  public static Color BUTTON_BORDER = new Color(200, 200, 200);

  // --- Theme Loading Logic ---

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

    WIRE_ON = parseColor(p, "wireOn", WIRE_ON);
    WIRE_OFF = parseColor(p, "wireOff", WIRE_OFF);
    WIRE_SELECTED = parseColor(p, "wireSelected", WIRE_SELECTED);

    COMP_BORDER = parseColor(p, "compBorder", COMP_BORDER);
    COMP_FILL_GRADIENT_1 = parseColor(p, "compFillGradient1", COMP_FILL_GRADIENT_1);
    COMP_FILL_GRADIENT_2 = parseColor(p, "compFillGradient2", COMP_FILL_GRADIENT_2);
    SELECTION_BORDER = parseColor(p, "selectionBorder", SELECTION_BORDER);

    PIN_COLOR = parseColor(p, "pin", PIN_COLOR);
    STUB_COLOR = parseColor(p, "stub", STUB_COLOR);
    HOVER_COLOR = parseColor(p, "hover", HOVER_COLOR);
    CLOCK_BACKGROUND = parseColor(p, "clockBackground", new Color(40, 40, 40));

    PALETTE_BACKGROUND = parseColor(p, "paletteBackground", PALETTE_BACKGROUND);
    PALETTE_HEADINGS = parseColor(p, "paletteHeadings", PALETTE_HEADINGS);
    BUTTON_BACKGROUND = parseColor(p, "buttonBackground", BUTTON_BACKGROUND);
    BUTTON_BORDER = parseColor(p, "buttonBorder", BUTTON_BORDER);

    // Calculate the default values based on Dark Mode first...
    Color defBox = isDarkMode ? COMP_FILL_GRADIENT_1 : new Color(230, 230, 230);
    Color defHeader = isDarkMode ? COMP_BORDER : new Color(100, 100, 255);
    Color defSwitch = isDarkMode ? COMP_FILL_GRADIENT_2 : new Color(200, 200, 200);

    // ...then try to read specific overrides from the file
    GENERIC_BOX_FILL = parseColor(p, "genericBoxFill", defBox);
    GENERIC_HEADER_FILL = parseColor(p, "genericHeaderFill", defHeader);
    SWITCH_FILL = parseColor(p, "switchFill", defSwitch);

    // Finalise Aliases
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
      BACKGROUND = new Color(30, 30, 30);
      GRID_MAJOR = new Color(50, 50, 50);
      GRID_MINOR = new Color(40, 40, 40);
      TEXT_COLOR = new Color(220, 220, 220);
      WIRE_ON = new Color(0, 255, 0);
      WIRE_OFF = new Color(80, 80, 80);
      WIRE_SELECTED = new Color(0, 150, 255);
      COMP_BORDER = new Color(180, 180, 180);
      COMP_FILL_GRADIENT_1 = new Color(60, 60, 60);
      COMP_FILL_GRADIENT_2 = new Color(40, 40, 40);
      SELECTION_BORDER = new Color(50, 150, 255);
      PIN_COLOR = new Color(100, 200, 255);
      STUB_COLOR = new Color(100, 100, 100);
      HOVER_COLOR = new Color(255, 100, 100);
      CLOCK_BACKGROUND = new Color(20, 20, 20);
      PALETTE_BACKGROUND = new Color(37, 37, 38);
      PALETTE_HEADINGS = new Color(200, 200, 200);
      BUTTON_BACKGROUND = new Color(45, 45, 45);
      BUTTON_BORDER = new Color(80, 80, 80);

      // Default Derived
      GENERIC_BOX_FILL = COMP_FILL_GRADIENT_1;
      GENERIC_HEADER_FILL = COMP_BORDER;
      SWITCH_FILL = COMP_FILL_GRADIENT_2;

    } else {
      BACKGROUND = Color.WHITE;
      GRID_MAJOR = new Color(220, 220, 220);
      GRID_MINOR = new Color(240, 240, 240);
      TEXT_COLOR = Color.BLACK;
      WIRE_ON = new Color(0, 200, 0);
      WIRE_OFF = new Color(100, 100, 100);
      WIRE_SELECTED = new Color(0, 120, 255);
      COMP_BORDER = Color.BLACK;
      COMP_FILL_GRADIENT_1 = new Color(240, 240, 255);
      COMP_FILL_GRADIENT_2 = new Color(200, 200, 255);
      SELECTION_BORDER = new Color(0, 120, 255);
      PIN_COLOR = new Color(0, 0, 150);
      STUB_COLOR = Color.GRAY;
      HOVER_COLOR = new Color(255, 100, 100);
      CLOCK_BACKGROUND = new Color(40, 40, 40);
      PALETTE_BACKGROUND = new Color(245, 245, 245);
      PALETTE_HEADINGS = new Color(80, 80, 80);
      BUTTON_BACKGROUND = Color.WHITE;
      BUTTON_BORDER = new Color(200, 200, 200);

      // Default Derived
      GENERIC_BOX_FILL = new Color(230, 230, 230);
      GENERIC_HEADER_FILL = new Color(100, 100, 255);
      SWITCH_FILL = new Color(200, 200, 200);
    }

    // Final Aliases
    GRID_COLOR = GRID_MAJOR;
    PANEL_BACKGROUND = BACKGROUND;
    SELECTION_FILL = new Color(SELECTION_BORDER.getRed(), SELECTION_BORDER.getGreen(), SELECTION_BORDER.getBlue(), 50);
  }
}
