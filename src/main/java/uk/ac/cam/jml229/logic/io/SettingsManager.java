package uk.ac.cam.jml229.logic.io;

import java.util.prefs.Preferences;

public class SettingsManager {

  private static final Preferences prefs = Preferences.userNodeForPackage(SettingsManager.class);

  private static final String KEY_THEME = "theme_name";
  private static final String KEY_DARK_MODE = "dark_mode";
  private static final String KEY_SNAP = "snap_to_grid";
  private static final String KEY_WIN_X = "win_x";
  private static final String KEY_WIN_Y = "win_y";
  private static final String KEY_WIN_W = "win_w";
  private static final String KEY_WIN_H = "win_h";
  private static final String KEY_WIN_MAX = "win_max";

  // Propagation Settings
  private static final String KEY_PROP_DELAY_ENABLED = "prop_delay_enabled";
  private static final String KEY_GATE_DELAY = "gate_delay";

  private static final String DEFAULT_THEME = "Default Light";

  public static boolean isPropagationDelayEnabled() {
    return prefs.getBoolean(KEY_PROP_DELAY_ENABLED, true);
  }

  public static void setPropagationDelayEnabled(boolean enabled) {
    prefs.putBoolean(KEY_PROP_DELAY_ENABLED, enabled);
  }

  public static int getGateDelay() {
    return prefs.getInt(KEY_GATE_DELAY, 1);
  }

  public static void setGateDelay(int delay) {
    prefs.putInt(KEY_GATE_DELAY, delay);
  }

  public static String getThemeName() {
    return prefs.get(KEY_THEME, DEFAULT_THEME);
  }

  public static void setThemeName(String name) {
    prefs.put(KEY_THEME, name);
  }

  public static boolean isDarkMode() {
    return prefs.getBoolean(KEY_DARK_MODE, false);
  }

  public static void setDarkMode(boolean dark) {
    prefs.putBoolean(KEY_DARK_MODE, dark);
  }

  public static boolean isSnapToGrid() {
    return prefs.getBoolean(KEY_SNAP, true);
  }

  public static void setSnapToGrid(boolean snap) {
    prefs.putBoolean(KEY_SNAP, snap);
  }

  public static int getWindowX() {
    return prefs.getInt(KEY_WIN_X, -1);
  }

  public static int getWindowY() {
    return prefs.getInt(KEY_WIN_Y, -1);
  }

  public static int getWindowWidth() {
    return prefs.getInt(KEY_WIN_W, 1280);
  }

  public static int getWindowHeight() {
    return prefs.getInt(KEY_WIN_H, 800);
  }

  public static boolean isMaximized() {
    return prefs.getBoolean(KEY_WIN_MAX, false);
  }

  public static void setWindowBounds(int x, int y, int w, int h, boolean max) {
    prefs.putInt(KEY_WIN_X, x);
    prefs.putInt(KEY_WIN_Y, y);
    prefs.putInt(KEY_WIN_W, w);
    prefs.putInt(KEY_WIN_H, h);
    prefs.putBoolean(KEY_WIN_MAX, max);
  }
}
