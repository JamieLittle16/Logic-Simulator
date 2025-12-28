package uk.ac.cam.jml229.logic.app;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.CustomComponent;
import uk.ac.cam.jml229.logic.io.StorageManager;
import uk.ac.cam.jml229.logic.ui.panels.*;
import uk.ac.cam.jml229.logic.ui.interaction.*;
import uk.ac.cam.jml229.logic.ui.render.*;
import uk.ac.cam.jml229.logic.ui.SimulationController;

public class GuiMain {

  private static boolean isFullScreen = false;
  private static Point prevLocation = null;
  private static Dimension prevSize = null;

  private static CircuitPanel circuitPanel;
  private static ComponentPalette palette;
  private static JFrame frame;
  private static JLabel zoomStatusLabel;

  private static JScrollPane scrollPalette;
  private static JMenuBar menuBar;
  private static JSplitPane splitPane;

  // --- NEW: Simulation Controller ---
  private static SimulationController simController;

  public static void main(String[] args) {
    System.setProperty("sun.java2d.opengl", "true");
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    SwingUtilities.invokeLater(() -> {
      frame = new JFrame("Logic Simulator");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      // --- Init Core Components ---
      circuitPanel = new CircuitPanel();
      CircuitInteraction interaction = circuitPanel.getInteraction();
      CircuitRenderer renderer = circuitPanel.getRenderer();

      palette = new ComponentPalette(interaction, renderer);
      interaction.setPalette(palette);

      // --- Init Simulation Controller ---
      // Pass the circuit and the repaint method
      simController = new SimulationController(circuitPanel.getCircuit(), circuitPanel::repaint);
      simController.start();

      // --- Build Menu Bar ---
      menuBar = new JMenuBar();

      // File Menu
      JMenu fileMenu = new JMenu("File");
      JMenuItem saveItem = new JMenuItem("Save...");
      saveItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      saveItem.addActionListener(e -> performSave());

      JMenuItem loadItem = new JMenuItem("Load...");
      loadItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      loadItem.addActionListener(e -> performLoad());

      fileMenu.add(saveItem);
      fileMenu.add(loadItem);

      // Edit Menu
      JMenu editMenu = new JMenu("Edit");
      JMenuItem undoItem = new JMenuItem("Undo");
      undoItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      undoItem.addActionListener(e -> circuitPanel.undo());

      JMenuItem redoItem = new JMenuItem("Redo");
      redoItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      redoItem.addActionListener(e -> circuitPanel.redo());

      editMenu.add(undoItem);
      editMenu.add(redoItem);
      editMenu.addSeparator();

      JMenuItem cutItem = new JMenuItem("Cut");
      cutItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      cutItem.addActionListener(e -> circuitPanel.cut());

      JMenuItem copyItem = new JMenuItem("Copy");
      copyItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      copyItem.addActionListener(e -> circuitPanel.copy());

      JMenuItem pasteItem = new JMenuItem("Paste");
      pasteItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      pasteItem.addActionListener(e -> circuitPanel.paste());

      editMenu.add(cutItem);
      editMenu.add(copyItem);
      editMenu.add(pasteItem);
      editMenu.addSeparator();

      JMenuItem rotateItem = new JMenuItem("Rotate");
      rotateItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      rotateItem.addActionListener(e -> circuitPanel.rotateSelection());

      JMenuItem deleteItem = new JMenuItem("Delete");
      deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
      deleteItem.addActionListener(e -> circuitPanel.deleteSelection());

      editMenu.add(rotateItem);
      editMenu.add(deleteItem);

      // View Menu
      JMenu viewMenu = new JMenu("View");
      JMenuItem zoomInItem = new JMenuItem("Zoom In");
      zoomInItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      zoomInItem.addActionListener(e -> circuitPanel.zoomIn());

      JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
      zoomOutItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      zoomOutItem.addActionListener(e -> circuitPanel.zoomOut());

      JMenuItem zoomResetItem = new JMenuItem("Reset Zoom");
      zoomResetItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      zoomResetItem.addActionListener(e -> circuitPanel.resetZoom());

      JCheckBoxMenuItem snapGridItem = new JCheckBoxMenuItem("Snap to Grid");
      snapGridItem.setSelected(false);
      snapGridItem.addActionListener(e -> circuitPanel.getInteraction().setSnapToGrid(snapGridItem.isSelected()));

      JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode");
      darkModeItem.addActionListener(e -> {
        Theme.setDarkMode(darkModeItem.isSelected());
        circuitPanel.updateTheme();
        palette.updateTheme();
        if (scrollPalette != null) {
          scrollPalette.setBackground(Theme.PALETTE_BACKGROUND);
          scrollPalette.getViewport().setBackground(Theme.PALETTE_BACKGROUND);
        }
        if (splitPane != null) {
          splitPane.setBackground(Theme.PALETTE_BACKGROUND);
          splitPane.repaint();
        }
        if (menuBar != null) {
          menuBar.setBackground(Theme.isDarkMode ? Theme.PALETTE_BACKGROUND : null);
          for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu m = menuBar.getMenu(i);
            if (m != null)
              m.setForeground(Theme.TEXT_COLOR);
          }
          if (zoomStatusLabel != null)
            zoomStatusLabel.setForeground(Theme.PALETTE_HEADINGS);
        }
      });

      viewMenu.add(zoomInItem);
      viewMenu.add(zoomOutItem);
      viewMenu.addSeparator();
      viewMenu.add(zoomResetItem);
      viewMenu.addSeparator();
      viewMenu.add(snapGridItem);
      viewMenu.add(darkModeItem);

      // Simulation Menu
      JMenu simMenu = new JMenu("Simulation");
      JMenuItem startItem = new JMenuItem("Start");
      startItem.addActionListener(e -> simController.start());

      JMenuItem stopItem = new JMenuItem("Stop");
      stopItem.addActionListener(e -> simController.stop());

      JMenuItem stepItem = new JMenuItem("Step (Manual Tick)");
      stepItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
      stepItem.addActionListener(e -> simController.step());

      // Clock Speed Submenu
      JMenu clockSpeedMenu = new JMenu("Clock Speed");
      ButtonGroup clockGroup = new ButtonGroup();
      addSpeedItem(clockSpeedMenu, clockGroup, "0.5 Hz (Slow)", 2000, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "1 Hz", 1000, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "2 Hz (Default)", 500, true);
      addSpeedItem(clockSpeedMenu, clockGroup, "5 Hz", 200, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "10 Hz", 100, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "50 Hz", 20, false);

      JRadioButtonMenuItem customClockItem = new JRadioButtonMenuItem("Custom...");
      customClockItem.addActionListener(e -> {
        String input = JOptionPane.showInputDialog(frame, "Enter Clock Frequency (Hz):", "Custom Clock Speed",
            JOptionPane.QUESTION_MESSAGE);
        if (input != null) {
          try {
            double hz = Double.parseDouble(input);
            if (hz > 0)
              simController.setClockDelayMs((int) (1000.0 / hz));
          } catch (NumberFormatException ex) {
            /* ignore */ }
        }
      });
      clockGroup.add(customClockItem);
      clockSpeedMenu.add(customClockItem);

      // Logic Speed Submenu
      JMenu logicSpeedMenu = new JMenu("Logic Speed (Propagation)");
      ButtonGroup logicGroup = new ButtonGroup();
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Instant (1000 updates/frame)", 1000, true);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Fast (50 updates/frame)", 50, false);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Visible (5 updates/frame)", 5, false);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Slow Motion (1 update/frame)", 1, false);

      JRadioButtonMenuItem customLogicItem = new JRadioButtonMenuItem("Custom...");
      customLogicItem.addActionListener(e -> {
        String input = JOptionPane.showInputDialog(frame, "Enter Logic Updates per Frame:", "Custom Logic Speed",
            JOptionPane.QUESTION_MESSAGE);
        if (input != null) {
          try {
            int steps = Integer.parseInt(input);
            if (steps > 0)
              simController.setLogicStepsPerFrame(steps);
          } catch (NumberFormatException ex) {
            /* ignore */ }
        }
      });
      logicGroup.add(customLogicItem);
      logicSpeedMenu.add(customLogicItem);

      simMenu.add(startItem);
      simMenu.add(stopItem);
      simMenu.add(stepItem);
      simMenu.addSeparator();
      simMenu.add(clockSpeedMenu);
      simMenu.add(logicSpeedMenu);

      menuBar.add(fileMenu);
      menuBar.add(editMenu);
      menuBar.add(viewMenu);
      menuBar.add(simMenu);

      menuBar.add(Box.createHorizontalGlue());
      zoomStatusLabel = new JLabel("Zoom: 100%  ");
      zoomStatusLabel.setForeground(Color.GRAY);
      menuBar.add(zoomStatusLabel);

      frame.setJMenuBar(menuBar);

      circuitPanel.setOnZoomChanged(scale -> {
        int pct = (int) (scale * 100);
        zoomStatusLabel.setText("Zoom: " + pct + "%  ");
      });

      // Layout
      scrollPalette = new JScrollPane(palette);
      scrollPalette.setBorder(null);
      scrollPalette.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPalette.getViewport().setBackground(Theme.PALETTE_BACKGROUND);

      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPalette, circuitPanel);
      splitPane.setUI(new BasicSplitPaneUI());
      splitPane.setBorder(null);
      splitPane.setDividerLocation(130);
      splitPane.setContinuousLayout(true);
      splitPane.setResizeWeight(0.0);
      splitPane.setBackground(Theme.PALETTE_BACKGROUND);

      frame.add(splitPane);

      circuitPanel.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_F11)
            toggleFullScreen(frame);
        }
      });

      frame.setSize(1280, 800);
      frame.setLocationRelativeTo(null);
      toggleFullScreen(frame);
      circuitPanel.requestFocusInWindow();
    });
  }

  // --- Helper Helpers ---
  private static void addSpeedItem(JMenu menu, ButtonGroup group, String label, int delayMs, boolean selected) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
    item.setSelected(selected);
    item.addActionListener(e -> simController.setClockDelayMs(delayMs));
    group.add(item);
    menu.add(item);
  }

  private static void addLogicSpeedItem(JMenu menu, ButtonGroup group, String label, int steps, boolean selected) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
    item.setSelected(selected);
    item.addActionListener(e -> simController.setLogicStepsPerFrame(steps));
    group.add(item);
    menu.add(item);
  }

  private static void performSave() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Logik Files (.lgk)", "lgk"));
    if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (!file.getName().endsWith(".lgk"))
        file = new File(file.getAbsolutePath() + ".lgk");
      try {
        List<Component> empty = new ArrayList<>();
        StorageManager.save(file, circuitPanel.getInteraction().getCircuit(), empty);
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

        // --- NEW: Update Simulation Controller ---
        simController.setCircuit(result.circuit());

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
