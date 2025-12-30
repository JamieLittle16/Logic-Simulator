package uk.ac.cam.jml229.logic.app;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import java.net.URL;
import java.awt.Taskbar;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.CustomComponent;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.io.SettingsManager;
import uk.ac.cam.jml229.logic.io.StorageManager;
import uk.ac.cam.jml229.logic.ui.panels.*;
import uk.ac.cam.jml229.logic.ui.interaction.*;
import uk.ac.cam.jml229.logic.ui.render.*;
import uk.ac.cam.jml229.logic.ui.SimulationController;
import uk.ac.cam.jml229.logic.ui.timing.SignalMonitor;
import uk.ac.cam.jml229.logic.ui.FlatIcons;
import uk.ac.cam.jml229.logic.ui.SettingsDialog;
import uk.ac.cam.jml229.logic.ui.AppMenuBar;
import uk.ac.cam.jml229.logic.ui.timing.TimingContainer;

public class GuiMain {

  private static boolean isFullScreen = false;
  private static Point prevLocation = null;
  private static Dimension prevSize = null;

  private static CircuitPanel circuitPanel;
  private static ComponentPalette palette;
  private static JFrame frame;

  // --- UI Elements ---
  private static TimingContainer timingContainer;
  private static JScrollPane scrollPalette;
  private static AppMenuBar appMenuBar;

  // Two Split Panes for Layout
  private static JSplitPane mainSplit; // Horizontal: Palette (Left) vs Editor (Right)
  private static JSplitPane editorSplit; // Vertical: Circuit (Top) vs Timing (Bottom)

  private static SimulationController simController;

  public static void main(String[] args) {
    System.setProperty("sun.java2d.opengl", "true");
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    try {
      URL iconUrl = GuiMain.class.getResource("/images/icon.png");
      if (iconUrl != null) {
        Image icon = ImageIO.read(iconUrl);
        frame.setIconImage(icon);
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
          Taskbar.getTaskbar().setIconImage(icon);
        }
      }
    } catch (Exception ex) {
      System.err.println("Warning: Failed to set app icon: " + ex.getMessage());
    }

    SwingUtilities.invokeLater(() -> {
      frame = new JFrame("LogiK");
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          saveSettingsAndExit();
        }

        @Override
        public void windowOpened(WindowEvent e) {
          // Collapse the timing panel immediately after the window opens
          SwingUtilities.invokeLater(() -> {
            if (editorSplit != null) {
              editorSplit.setDividerLocation(1.0);
            }
          });
        }
      });

      // --- Init Core Components ---
      circuitPanel = new CircuitPanel();
      CircuitInteraction interaction = circuitPanel.getInteraction();
      interaction.setSnapToGrid(SettingsManager.isSnapToGrid());
      CircuitRenderer renderer = circuitPanel.getRenderer();

      palette = new ComponentPalette(interaction, renderer);
      interaction.setPalette(palette);

      // --- Init Timing Panel (Docked) ---
      timingContainer = new TimingContainer(GuiMain::toggleTimingPanel);

      // --- Simulation Controller ---
      simController = new SimulationController(circuitPanel.getCircuit(), () -> {
        circuitPanel.repaint();
        timingContainer.tick();
      });
      simController.start();

      circuitPanel.setOnCircuitChanged(newCircuit -> simController.setCircuit(newCircuit));

      // Hook up interaction listener
      interaction.setOnOpenTiming(selection -> addSelectionToTiming(selection));

      // --- Build Menu Bar ---
      appMenuBar = new AppMenuBar(
          frame,
          circuitPanel,
          simController,
          timingContainer,
          GuiMain::loadAndApplyTheme,
          () -> new SettingsDialog(frame).setVisible(true),
          GuiMain::performSave,
          GuiMain::performLoad,
          GuiMain::toggleTimingPanel);
      frame.setJMenuBar(appMenuBar);

      circuitPanel.setOnZoomChanged(scale -> appMenuBar.updateZoomLabel(scale));

      // --- Main Layout Construction ---
      scrollPalette = new JScrollPane(palette);
      scrollPalette.setBorder(null);
      scrollPalette.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPalette.getViewport().setBackground(Theme.PALETTE_BACKGROUND);

      // Vertical Split: Circuit (Top) vs Timing (Bottom)
      editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, circuitPanel, timingContainer);
      editorSplit.setUI(new BasicSplitPaneUI());
      editorSplit.setBorder(null);
      editorSplit.setResizeWeight(1.0);
      editorSplit.setContinuousLayout(true);
      editorSplit.setBackground(Theme.PALETTE_BACKGROUND);

      // Horizontal Split: Palette (Left) vs Editor Area (Right)
      mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPalette, editorSplit);
      mainSplit.setUI(new BasicSplitPaneUI());
      mainSplit.setBorder(null);
      mainSplit.setDividerLocation(130);
      mainSplit.setContinuousLayout(true);
      mainSplit.setResizeWeight(0.0);
      mainSplit.setBackground(Theme.PALETTE_BACKGROUND);

      frame.add(mainSplit);

      circuitPanel.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_F11)
            toggleFullScreen(frame);
        }
      });

      loadAndApplyTheme(SettingsManager.getThemeName());

      boolean isMax = SettingsManager.isMaximized();
      if (isMax) {
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      } else {
        configureWindowSize();
      }

      frame.setVisible(true);

      // Start with Timing Panel collapsed (Hidden)
      SwingUtilities.invokeLater(() -> editorSplit.setDividerLocation(1.0));

      circuitPanel.requestFocusInWindow();
    });
  }

  // --- Helper Methods ---

  private static void toggleTimingPanel() {
    int height = editorSplit.getHeight();
    int loc = editorSplit.getDividerLocation();

    // If divider is > 90% down, assume it's hidden/collapsed
    if (loc >= height - 50) {
      editorSplit.setDividerLocation(0.7); // Show (30% height)
    } else {
      editorSplit.setDividerLocation(1.0); // Hide (collapse to bottom)
    }
  }

  private static void configureWindowSize() {
    int w = SettingsManager.getWindowWidth();
    int h = SettingsManager.getWindowHeight();
    int x = SettingsManager.getWindowX();
    int y = SettingsManager.getWindowY();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    if (w >= screenSize.width && h >= screenSize.height) {
      w = 1280;
      h = 800;
      x = -1;
    }
    if (w < 400)
      w = 1280;
    if (h < 300)
      h = 800;

    frame.setSize(w, h);
    if (x == -1 || y == -1 || (x == 0 && y == 0)) {
      frame.setLocationRelativeTo(null);
    } else {
      frame.setLocation(x, y);
    }
  }

  private static void addSelectionToTiming(List<Component> selection) {
    if (selection.isEmpty()) {
      JOptionPane.showMessageDialog(frame, "Please select a component (Gate/Switch) to monitor.");
      return;
    }
    boolean added = false;
    for (Component c : selection) {
      if (c.getOutputCount() > 0) {
        Wire w = c.getOutputWire(0);
        if (w != null) {
          timingContainer.addMonitor(new SignalMonitor(
              c.getName(), w, Theme.WIRE_ON, timingContainer.getBufferSize()));
          added = true;
        }
      }
    }
    if (added) {
      // Auto-open if hidden
      if (editorSplit.getDividerLocation() >= editorSplit.getHeight() - 50) {
        editorSplit.setDividerLocation(0.7);
      }
      SwingUtilities.invokeLater(() -> timingContainer.scrollToPresent());
    } else {
      JOptionPane.showMessageDialog(frame, "Selected components have no outputs to monitor.");
    }
  }

  private static void loadAndApplyTheme(String t) {
    String loadName = t.toLowerCase().replace(" ", "_");
    if (t.equals("Default Light"))
      loadName = "light";
    if (t.equals("Default Dark"))
      loadName = "dark";
    SettingsManager.setThemeName(t);
    if (loadName.equals("light") || loadName.equals("dark")) {
      Theme.setDarkMode(loadName.equals("dark"));
    } else {
      Theme.loadTheme(loadName);
    }
    SettingsManager.setDarkMode(Theme.isDarkMode);
    updateUIColors();
    circuitPanel.repaint();
  }

  private static void updateUIColors() {
    circuitPanel.updateTheme();
    palette.updateTheme();
    timingContainer.updateTheme();

    if (Theme.isDarkMode) {
      UIManager.put("CheckBoxMenuItem.checkIcon", new FlatIcons.CheckIcon());
      UIManager.put("RadioButtonMenuItem.checkIcon", new FlatIcons.RadioIcon());
    } else {
      UIManager.put("CheckBoxMenuItem.checkIcon", null);
      UIManager.put("RadioButtonMenuItem.checkIcon", null);
    }

    if (scrollPalette != null) {
      scrollPalette.setBackground(Theme.PALETTE_BACKGROUND);
      scrollPalette.getViewport().setBackground(Theme.PALETTE_BACKGROUND);
      scrollPalette.getVerticalScrollBar().setUI(new ThemedScrollBarUI());
      scrollPalette.getHorizontalScrollBar().setUI(new ThemedScrollBarUI());
    }

    if (mainSplit != null) {
      mainSplit.setBackground(Theme.PALETTE_BACKGROUND);
      mainSplit.repaint();
    }

    if (editorSplit != null) {
      editorSplit.setBackground(Theme.PALETTE_BACKGROUND);
      editorSplit.repaint();
    }

    if (appMenuBar != null) {
      appMenuBar.updateTheme();
    }
  }

  private static void saveSettingsAndExit() {
    int state = frame.getExtendedState();
    boolean isMaximized = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;

    if (!isMaximized) {
      SettingsManager.setWindowBounds(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight(), false);
    } else {
      SettingsManager.setWindowBounds(-1, -1, 1280, 800, true);
    }
    System.exit(0);
  }

  private static void performSave() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Logik Files (.lgk)", "lgk"));
    if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (!file.getName().endsWith(".lgk"))
        file = new File(file.getAbsolutePath() + ".lgk");
      try {
        List<Component> tools = palette.getCustomPrototypes();
        StorageManager.save(file, circuitPanel.getInteraction().getCircuit(), tools);
        JOptionPane.showMessageDialog(frame, "Saved successfully!");
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(frame, "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static void performLoad() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Logik Files (.lgk)", "lgk"));
    if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
      try {
        StorageManager.LoadResult result = StorageManager.load(fc.getSelectedFile());
        circuitPanel.setCircuit(result.circuit());
        circuitPanel.getInteraction().resetHistory();
        for (CustomComponent cc : result.customTools())
          palette.addCustomTool(cc);
        simController.setCircuit(result.circuit());
        timingContainer.clear();
        circuitPanel.repaint();
        JOptionPane.showMessageDialog(frame, "Loaded successfully!");
      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(frame, "Error loading: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static void toggleFullScreen(JFrame frame) {
    if (frame.isVisible())
      frame.dispose();
    isFullScreen = !isFullScreen;
    if (isFullScreen) {
      if (prevLocation == null && frame.isShowing()) {
        prevLocation = frame.getLocation();
        prevSize = frame.getSize();
      }
      frame.setUndecorated(true);
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      if (gd.isFullScreenSupported())
        try {
          gd.setFullScreenWindow(frame);
        } catch (Exception ex) {
          gd.setFullScreenWindow(null);
        }
    } else {
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      gd.setFullScreenWindow(null);
      frame.setUndecorated(false);
      frame.setExtendedState(JFrame.NORMAL);
      if (prevLocation != null)
        frame.setLocation(prevLocation);
      if (prevSize != null)
        frame.setSize(prevSize);
    }
    frame.setVisible(true);
  }
}
