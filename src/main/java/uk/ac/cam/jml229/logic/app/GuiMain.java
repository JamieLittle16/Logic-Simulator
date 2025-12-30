package uk.ac.cam.jml229.logic.app;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import uk.ac.cam.jml229.logic.ui.AutoLayout;
import uk.ac.cam.jml229.logic.ui.timing.TimingPanel;
import uk.ac.cam.jml229.logic.ui.timing.SignalMonitor;

public class GuiMain {

  private static boolean isFullScreen = false;
  private static Point prevLocation = null;
  private static Dimension prevSize = null;

  private static CircuitPanel circuitPanel;
  private static ComponentPalette palette;
  private static JFrame frame;
  private static JLabel zoomStatusLabel;

  // --- Timing Window UI Elements ---
  private static JFrame timingFrame;
  private static TimingPanel timingPanel;
  private static JScrollPane timingScroll;
  private static JToolBar timingTools;

  private static JScrollPane scrollPalette;
  private static JMenuBar menuBar;
  private static JSplitPane splitPane;

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
      });

      // --- Init Core Components ---
      circuitPanel = new CircuitPanel();
      CircuitInteraction interaction = circuitPanel.getInteraction();
      CircuitRenderer renderer = circuitPanel.getRenderer();

      palette = new ComponentPalette(interaction, renderer);
      interaction.setPalette(palette);

      // --- Init Timing Window ---
      timingPanel = new TimingPanel();
      timingFrame = new JFrame("Timing Diagram");
      timingFrame.setSize(900, 500);
      timingFrame.setLayout(new BorderLayout());

      // Toolbar
      timingTools = new JToolBar();
      timingTools.setFloatable(false);

      JButton playPauseBtn = new JButton("Pause");
      playPauseBtn.addActionListener(e -> {
        timingPanel.togglePause();
        playPauseBtn.setText(timingPanel.isPaused() ? "Resume" : "Pause");
      });
      JButton zoomInBtn = new JButton("Zoom In (+)");
      zoomInBtn.addActionListener(e -> timingPanel.zoomIn());
      JButton zoomOutBtn = new JButton("Zoom Out (-)");
      zoomOutBtn.addActionListener(e -> timingPanel.zoomOut());
      JButton clearBtn = new JButton("Clear History");
      JButton skipBtn = new JButton("Present");
      skipBtn.setToolTipText("Skip to Present");
      skipBtn.addActionListener(e -> timingPanel.scrollToPresent());
      clearBtn.addActionListener(e -> timingPanel.clear());

      timingTools.add(playPauseBtn);
      timingTools.addSeparator();
      timingTools.add(zoomInBtn);
      timingTools.add(zoomOutBtn);
      timingTools.addSeparator();
      timingTools.add(clearBtn);
      timingTools.addSeparator();
      timingTools.add(skipBtn);

      timingFrame.add(timingTools, BorderLayout.NORTH);

      // Scroll Pane
      timingScroll = new JScrollPane(timingPanel);
      timingScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      timingScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      timingScroll.getVerticalScrollBar().setUnitIncrement(20);
      timingScroll.getHorizontalScrollBar().setUnitIncrement(20);
      timingScroll.setBorder(null);

      // Attach Header and Corners
      timingScroll.setRowHeaderView(timingPanel.getRowHeader());
      timingScroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, new JPanel() {
        {
          setBackground(Theme.PALETTE_BACKGROUND);
        }
      });
      timingScroll.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, new JPanel() {
        {
          setBackground(Theme.PALETTE_BACKGROUND);
        }
      });

      timingFrame.add(timingScroll, BorderLayout.CENTER);
      timingFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

      // --- Simulation Controller ---
      simController = new SimulationController(circuitPanel.getCircuit(), () -> {
        circuitPanel.repaint();
        if (timingFrame != null && timingFrame.isVisible()) {
          timingPanel.tick();
        }
      });
      simController.start();

      circuitPanel.setOnCircuitChanged(newCircuit -> simController.setCircuit(newCircuit));

      // Hook up interaction listener
      interaction.setOnOpenTiming(selection -> addSelectionToTiming(selection));

      // --- Build Menu Bar ---
      menuBar = new JMenuBar();

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

      // Add Settings Item
      JMenuItem settingsItem = new JMenuItem("Settings...");
      settingsItem.addActionListener(e -> showSettingsDialog());
      fileMenu.addSeparator();
      fileMenu.add(settingsItem);

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

      JMenu viewMenu = new JMenu("View");
      JMenuItem timingItem = new JMenuItem("Show Timing Diagram");
      timingItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

      timingItem.addActionListener(e -> {
        boolean visible = !timingFrame.isVisible();
        timingFrame.setVisible(visible);
        if (visible) {
          SwingUtilities.invokeLater(() -> timingPanel.scrollToPresent());
        }
      });

      viewMenu.add(timingItem);
      viewMenu.addSeparator();

      JMenu themeMenu = new JMenu("Theme");
      ButtonGroup themeGroup = new ButtonGroup();

      String[] builtIns = {
          "Default Light", "Default Dark", "One Dark", "Catppuccin Mocha",
          "High Contrast", "Dracula", "Monokai", "Nord",
          "Solarized Light", "GitHub Light", "Blueprint",
          "Cyberpunk", "Gruvbox Dark"
      };

      List<String> allThemes = new ArrayList<>(List.of(builtIns));
      File userThemeDir = new File(System.getProperty("user.home") + "/.logik/themes");
      if (userThemeDir.exists() && userThemeDir.isDirectory()) {
        for (File f : userThemeDir.listFiles()) {
          if (f.getName().endsWith(".properties")) {
            String name = f.getName().replace(".properties", "");
            if (!allThemes.contains(name))
              allThemes.add(name);
          }
        }
      }

      String currentTheme = SettingsManager.getThemeName();
      for (String t : allThemes) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(t);
        if (t.equals(currentTheme))
          item.setSelected(true);
        item.addActionListener(e -> loadAndApplyTheme(t));
        themeGroup.add(item);
        themeMenu.add(item);
      }

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
      snapGridItem.setSelected(SettingsManager.isSnapToGrid());
      circuitPanel.getInteraction().setSnapToGrid(snapGridItem.isSelected());
      snapGridItem.addActionListener(e -> {
        boolean val = snapGridItem.isSelected();
        circuitPanel.getInteraction().setSnapToGrid(val);
        SettingsManager.setSnapToGrid(val);
      });

      viewMenu.add(themeMenu);
      viewMenu.add(zoomInItem);
      viewMenu.add(zoomOutItem);
      viewMenu.addSeparator();
      viewMenu.add(zoomResetItem);
      viewMenu.addSeparator();
      viewMenu.add(snapGridItem);

      JMenu toolsMenu = new JMenu("Tools");
      JMenuItem autoLayoutItem = new JMenuItem("Auto-Organise Circuit");
      autoLayoutItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      autoLayoutItem.addActionListener(e -> {
        AutoLayout.organise(circuitPanel.getCircuit());
        circuitPanel.repaint();
        circuitPanel.getInteraction().saveHistory();
      });
      JMenuItem addProbeItem = new JMenuItem("Add Selected to Timing Diagram");
      addProbeItem.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      addProbeItem.addActionListener(e -> {
        List<Component> selection = circuitPanel.getInteraction().getSelectedComponents();
        addSelectionToTiming(selection);
      });
      toolsMenu.add(autoLayoutItem);
      toolsMenu.add(addProbeItem);

      JMenu simMenu = new JMenu("Simulation");
      JMenuItem startItem = new JMenuItem("Start");
      startItem.addActionListener(e -> simController.start());
      JMenuItem stopItem = new JMenuItem("Stop");
      stopItem.addActionListener(e -> simController.stop());
      JMenuItem stepItem = new JMenuItem("Step (Manual Tick)");
      stepItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
      stepItem.addActionListener(e -> simController.step());
      JMenu clockSpeedMenu = new JMenu("Clock Speed");
      ButtonGroup clockGroup = new ButtonGroup();
      addSpeedItem(clockSpeedMenu, clockGroup, "0.5 Hz (Slow)", 2000, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "1 Hz", 1000, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "2 Hz (Default)", 500, true);
      addSpeedItem(clockSpeedMenu, clockGroup, "5 Hz", 200, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "10 Hz", 100, false);
      addSpeedItem(clockSpeedMenu, clockGroup, "50 Hz", 20, false);
      JMenu logicSpeedMenu = new JMenu("Logic Speed (Propagation)");
      ButtonGroup logicGroup = new ButtonGroup();
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Instant (1000 updates/frame)", 1000, true);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Fast (50 updates/frame)", 50, false);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Visible (5 updates/frame)", 5, false);
      addLogicSpeedItem(logicSpeedMenu, logicGroup, "Slow Motion (1 update/frame)", 1, false);
      simMenu.add(startItem);
      simMenu.add(stopItem);
      simMenu.add(stepItem);
      simMenu.addSeparator();
      simMenu.add(clockSpeedMenu);
      simMenu.add(logicSpeedMenu);

      menuBar.add(fileMenu);
      menuBar.add(editMenu);
      menuBar.add(viewMenu);
      menuBar.add(toolsMenu);
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

      loadAndApplyTheme(SettingsManager.getThemeName());

      boolean isMax = SettingsManager.isMaximized();

      if (isMax) {
        frame.setSize(1280, 800);
        frame.setLocationRelativeTo(null);
      } else {
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

      frame.setVisible(true);

      if (isMax) {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      }

      circuitPanel.requestFocusInWindow();
    });
  }

  private static void showSettingsDialog() {
    // Use "Preferences" if Title doesn't fit
    JDialog dialog = new JDialog(frame, "Preferences", true);
    dialog.setLayout(new BorderLayout());
    // Increase width to prevent cutoff
    dialog.setSize(400, 250);
    dialog.setLocationRelativeTo(frame);

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    content.setBackground(Theme.PALETTE_BACKGROUND);

    // --- FIX: Add Internal Title ---
    JLabel titleLabel = new JLabel("Simulation Settings");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
    titleLabel.setForeground(Theme.PALETTE_HEADINGS);
    titleLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    content.add(titleLabel);
    content.add(Box.createVerticalStrut(20));

    // Custom Checkbox
    JCheckBox enableDelay = new JCheckBox("Enable Propagation Delay (Hazards)");
    enableDelay.setSelected(SettingsManager.isPropagationDelayEnabled());
    enableDelay.setOpaque(false);
    enableDelay.setForeground(Theme.TEXT_COLOR);
    enableDelay.setFocusPainted(false);
    enableDelay.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

    if (Theme.isDarkMode) {
      enableDelay.setIcon(new FlatCheckIcon());
    }

    content.add(enableDelay);
    content.add(Box.createVerticalStrut(15));

    // Spinner Panel
    JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    delayPanel.setOpaque(false);
    delayPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

    JLabel delayLabel = new JLabel("Gate Delay (Ticks): ");
    delayLabel.setForeground(Theme.TEXT_COLOR);

    JSpinner delaySpinner = new JSpinner(new SpinnerNumberModel(SettingsManager.getGateDelay(), 1, 50, 1));

    // --- FIX: Style the Spinner Buttons and Field ---
    styleSpinner(delaySpinner);

    delayPanel.add(delayLabel);
    delayPanel.add(Box.createHorizontalStrut(10));
    delayPanel.add(delaySpinner);

    delaySpinner.setEnabled(enableDelay.isSelected());
    enableDelay.addActionListener(e -> delaySpinner.setEnabled(enableDelay.isSelected()));

    content.add(delayPanel);
    content.add(Box.createVerticalGlue());

    // Buttons
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.setBackground(Theme.PALETTE_BACKGROUND);

    JButton restoreBtn = createStyledButton("Restore Defaults");
    restoreBtn.addActionListener(e -> {
      enableDelay.setSelected(true);
      delaySpinner.setValue(1);
      delaySpinner.setEnabled(true);
    });

    JButton okBtn = createStyledButton("OK");
    okBtn.addActionListener(e -> {
      SettingsManager.setPropagationDelayEnabled(enableDelay.isSelected());
      SettingsManager.setGateDelay((Integer) delaySpinner.getValue());
      dialog.dispose();
    });

    buttons.add(restoreBtn);
    buttons.add(okBtn);

    dialog.add(content, BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);
    dialog.setVisible(true);
  }

  private static void styleSpinner(JSpinner spinner) {
    if (Theme.isDarkMode) {
      spinner.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));

      JComponent editor = spinner.getEditor();
      if (editor instanceof JSpinner.DefaultEditor) {
        JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
        tf.setBackground(Theme.BUTTON_BACKGROUND);
        tf.setForeground(Theme.TEXT_COLOR);
        tf.setCaretColor(Theme.TEXT_COLOR);
      }

      // Hack to style the arrows: Iterate components
      for (java.awt.Component c : spinner.getComponents()) {
        if (c instanceof JButton) {
          JButton btn = (JButton) c;
          btn.setBackground(Theme.BUTTON_BACKGROUND);
          btn.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));
          // We can't easily replace the icon without deep look-and-feel changes,
          // but removing the 3D border makes it look 90% better.
        }
      }
    }
  }

  private static JButton createStyledButton(String text) {
    JButton btn = new JButton(text);
    btn.setFocusPainted(false);
    if (Theme.isDarkMode) {
      btn.setBackground(Theme.BUTTON_BACKGROUND);
      btn.setForeground(Theme.TEXT_COLOR);
      btn.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(Theme.BUTTON_BORDER),
          BorderFactory.createEmptyBorder(5, 15, 5, 15)));
      btn.setOpaque(true);
      btn.setContentAreaFilled(true);

      btn.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          btn.setBackground(Theme.BUTTON_HOVER);
        }

        public void mouseExited(MouseEvent e) {
          btn.setBackground(Theme.BUTTON_BACKGROUND);
        }
      });
    }
    return btn;
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
          timingPanel.addMonitor(new SignalMonitor(
              c.getName(),
              w,
              Theme.WIRE_ON,
              timingPanel.getBufferSize()));
          added = true;
        }
      }
    }
    if (added) {
      timingFrame.setVisible(true);
      SwingUtilities.invokeLater(() -> timingPanel.scrollToPresent());
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
    if (timingPanel != null)
      timingPanel.repaint();
  }

  private static void updateUIColors() {
    circuitPanel.updateTheme();
    palette.updateTheme();

    if (Theme.isDarkMode) {
      UIManager.put("CheckBoxMenuItem.checkIcon", new FlatCheckIcon());
      UIManager.put("RadioButtonMenuItem.checkIcon", new FlatRadioIcon());
    } else {
      // Reset to default (null usually forces L&F to use its own)
      UIManager.put("CheckBoxMenuItem.checkIcon", null);
      UIManager.put("RadioButtonMenuItem.checkIcon", null);
    }

    if (scrollPalette != null) {
      scrollPalette.setBackground(Theme.PALETTE_BACKGROUND);
      scrollPalette.getViewport().setBackground(Theme.PALETTE_BACKGROUND);
      scrollPalette.getVerticalScrollBar().setUI(new ThemedScrollBarUI());
      scrollPalette.getHorizontalScrollBar().setUI(new ThemedScrollBarUI());
    }

    if (splitPane != null) {
      splitPane.setBackground(Theme.PALETTE_BACKGROUND);
      splitPane.repaint();
    }

    if (menuBar != null) {
      menuBar.setBackground(Theme.isDarkMode ? Theme.PALETTE_BACKGROUND : null);

      if (Theme.isDarkMode) {
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BUTTON_BORDER));
      } else {
        menuBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
      }

      for (int i = 0; i < menuBar.getMenuCount(); i++) {
        JMenu m = menuBar.getMenu(i);
        if (m != null)
          styleMenu(m);
      }

      // Force repaint of the menu structure to pick up new icons
      SwingUtilities.updateComponentTreeUI(menuBar);

      if (zoomStatusLabel != null)
        zoomStatusLabel.setForeground(Theme.PALETTE_HEADINGS);
    }

    if (timingFrame != null) {
      SwingUtilities.updateComponentTreeUI(timingFrame);
    }

    if (timingPanel != null) {
      timingPanel.updateTheme();
    }

    if (timingScroll != null) {
      timingScroll.getViewport().setBackground(Theme.BACKGROUND);
      timingScroll.getVerticalScrollBar().setUI(new ThemedScrollBarUI());
      timingScroll.getHorizontalScrollBar().setUI(new ThemedScrollBarUI());

      JComponent cornerUL = (JComponent) timingScroll.getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER);
      if (cornerUL != null)
        cornerUL.setBackground(Theme.PALETTE_BACKGROUND);

      JComponent cornerLL = (JComponent) timingScroll.getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER);
      if (cornerLL != null)
        cornerLL.setBackground(Theme.PALETTE_BACKGROUND);

      timingScroll.repaint();
    }

    if (timingTools != null) {
      timingTools.setBackground(Theme.PALETTE_BACKGROUND);

      for (java.awt.Component c : timingTools.getComponents()) {
        if (c instanceof JButton) {
          JButton btn = (JButton) c;
          btn.setOpaque(true);
          btn.setBorderPainted(false);
          btn.setFocusPainted(false);
          btn.setBackground(Theme.BUTTON_BACKGROUND);
          btn.setForeground(Theme.TEXT_COLOR);

          if (btn.getMouseListeners().length < 2) {
            btn.addMouseListener(new MouseAdapter() {
              public void mouseEntered(MouseEvent e) {
                btn.setBackground(Theme.BUTTON_HOVER);
              }

              public void mouseExited(MouseEvent e) {
                btn.setBackground(Theme.BUTTON_BACKGROUND);
              }
            });
          }
        }
      }
    }
  }

  private static void styleMenu(JComponent item) {
    if (Theme.isDarkMode) {
      item.setBackground(Theme.PALETTE_BACKGROUND);
      item.setForeground(Theme.TEXT_COLOR);
      item.setOpaque(true);

      if (item instanceof JPopupMenu) {
        item.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));
        item.setBackground(Theme.PALETTE_BACKGROUND);
      }

      if (item instanceof JMenuItem) {
        item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      }

      if (item instanceof JMenu) {
        JPopupMenu popup = ((JMenu) item).getPopupMenu();
        popup.setBackground(Theme.PALETTE_BACKGROUND);
        popup.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));
      }

      if (item instanceof JPopupMenu.Separator) {
        item.setBackground(Theme.PALETTE_BACKGROUND);
        item.setForeground(Theme.GRID_MAJOR);
      }

    } else {
      item.setBackground(null);
      item.setForeground(Color.BLACK);
      item.setOpaque(false);

      if (item instanceof JMenu) {
        JPopupMenu popup = ((JMenu) item).getPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
      }
    }

    if (item instanceof JMenu) {
      for (java.awt.Component c : ((JMenu) item).getMenuComponents()) {
        if (c instanceof JComponent)
          styleMenu((JComponent) c);
      }
    }
  }

  private static void saveSettingsAndExit() {
    int state = frame.getExtendedState();
    boolean isMaximized = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;

    if (!isMaximized) {
      SettingsManager.setWindowBounds(
          frame.getX(), frame.getY(),
          frame.getWidth(), frame.getHeight(),
          false);
    } else {
      SettingsManager.setWindowBounds(-1, -1, 1280, 800, true);
    }
    System.exit(0);
  }

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

        if (timingPanel != null)
          timingPanel.clear();

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

  // --- Inner Classes for Flat Icons ---

  private static class FlatCheckIcon implements Icon {
    @Override
    public int getIconWidth() {
      return 16;
    }

    @Override
    public int getIconHeight() {
      return 16;
    }

    @Override
    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      AbstractButton b = (AbstractButton) c;

      // Draw Box
      g2.setColor(Theme.TEXT_COLOR);
      g2.setStroke(new BasicStroke(1.5f));
      g2.drawRoundRect(x + 3, y + 3, 10, 10, 2, 2);

      if (b.isSelected()) {
        g2.setColor(Theme.WIRE_ON); // Use the green accent for check
        // Draw tick
        g2.drawLine(x + 5, y + 8, x + 7, y + 10);
        g2.drawLine(x + 7, y + 10, x + 11, y + 5);
      }
      g2.dispose();
    }
  }

  private static class FlatRadioIcon implements Icon {
    @Override
    public int getIconWidth() {
      return 16;
    }

    @Override
    public int getIconHeight() {
      return 16;
    }

    @Override
    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      AbstractButton b = (AbstractButton) c;

      // Draw Circle Outline
      g2.setColor(Theme.TEXT_COLOR);
      g2.setStroke(new BasicStroke(1.5f));
      g2.drawOval(x + 3, y + 3, 10, 10);

      if (b.isSelected()) {
        g2.setColor(Theme.WIRE_ON);
        g2.fillOval(x + 6, y + 6, 5, 5);
      }
      g2.dispose();
    }
  }
}
