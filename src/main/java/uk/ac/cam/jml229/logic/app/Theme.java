package uk.ac.cam.jml229.logic.app;

import java.awt.Color;

public class Theme {
  public static boolean isDarkMode = false;

  // --- UI Colors ---
  public static Color PANEL_BACKGROUND = Color.WHITE;
  public static Color PALETTE_BACKGROUND = new Color(245, 245, 245);
  public static Color TEXT_COLOR = Color.BLACK;
  public static Color PALETTE_HEADINGS = Color.GRAY;
  public static Color BUTTON_BACKGROUND = Color.WHITE;
  public static Color BUTTON_BORDER = new Color(200, 200, 200);

  // --- Canvas Colors ---
  public static Color GRID_COLOR = new Color(235, 235, 235);
  public static Color SELECTION_BORDER = new Color(0, 180, 255);
  public static Color SELECTION_FILL = new Color(0, 180, 255, 40);
  public static Color HOVER_COLOR = new Color(255, 180, 0);

  // --- Component Colors ---
  public static Color COMP_BORDER = Color.BLACK;
  public static Color COMP_FILL_GRADIENT_1 = new Color(70, 120, 200);
  public static Color COMP_FILL_GRADIENT_2 = new Color(120, 160, 240);
  public static Color STUB_COLOR = Color.BLACK;
  public static Color PIN_COLOR = new Color(50, 50, 50);

  // Specific Component overrides
  public static Color SWITCH_FILL = Color.DARK_GRAY;
  public static Color GENERIC_BOX_FILL = new Color(220, 220, 220);
  public static Color GENERIC_HEADER_FILL = new Color(60, 60, 60);

  // --- Wire Colors ---
  public static Color WIRE_OFF = new Color(100, 100, 100);
  public static Color WIRE_ON = new Color(230, 50, 50);

  public static void setDarkMode(boolean dark) {
    isDarkMode = dark;
    if (dark) {
      PANEL_BACKGROUND = new Color(30, 30, 30);
      PALETTE_BACKGROUND = new Color(45, 45, 45);
      TEXT_COLOR = new Color(220, 220, 220);
      PALETTE_HEADINGS = new Color(180, 180, 180);
      BUTTON_BACKGROUND = new Color(60, 60, 60);
      BUTTON_BORDER = new Color(100, 100, 100);

      GRID_COLOR = new Color(50, 50, 50);

      COMP_BORDER = new Color(180, 180, 180);
      // Darker blue gradients for gates
      COMP_FILL_GRADIENT_1 = new Color(40, 80, 160);
      COMP_FILL_GRADIENT_2 = new Color(80, 120, 200);

      STUB_COLOR = new Color(180, 180, 180);
      PIN_COLOR = new Color(200, 200, 200);

      SWITCH_FILL = new Color(80, 80, 80);
      GENERIC_BOX_FILL = new Color(80, 80, 80);
      GENERIC_HEADER_FILL = new Color(40, 40, 40);

      WIRE_OFF = new Color(120, 120, 120);
    } else {
      // Restore Defaults
      PANEL_BACKGROUND = Color.WHITE;
      PALETTE_BACKGROUND = new Color(245, 245, 245);
      TEXT_COLOR = Color.BLACK;
      PALETTE_HEADINGS = Color.GRAY;
      BUTTON_BACKGROUND = Color.WHITE;
      BUTTON_BORDER = new Color(200, 200, 200);

      GRID_COLOR = new Color(235, 235, 235);

      COMP_BORDER = Color.BLACK;
      COMP_FILL_GRADIENT_1 = new Color(70, 120, 200);
      COMP_FILL_GRADIENT_2 = new Color(120, 160, 240);

      STUB_COLOR = Color.BLACK;
      PIN_COLOR = new Color(50, 50, 50);

      SWITCH_FILL = Color.DARK_GRAY;
      GENERIC_BOX_FILL = new Color(220, 220, 220);
      GENERIC_HEADER_FILL = new Color(60, 60, 60);

      WIRE_OFF = new Color(100, 100, 100);
    }
  }
}
