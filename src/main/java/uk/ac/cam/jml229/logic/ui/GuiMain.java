package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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

public class GuiMain {

  private static boolean isFullScreen = false;
  private static Point prevLocation = null;
  private static Dimension prevSize = null;

  // Keep references to rebuild UI on load
  private static CircuitPanel circuitPanel;
  private static ComponentPalette palette;
  private static JFrame frame;

  public static void main(String[] args) {
    System.setProperty("sun.java2d.opengl", "true");
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    SwingUtilities.invokeLater(() -> {
      frame = new JFrame("Logic Simulator");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      // --- Setup Menu Bar ---
      JMenuBar menuBar = new JMenuBar();
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
      menuBar.add(fileMenu);
      frame.setJMenuBar(menuBar);

      // --- Setup Components ---
      circuitPanel = new CircuitPanel();
      CircuitInteraction interaction = circuitPanel.getInteraction();
      CircuitRenderer renderer = circuitPanel.getRenderer();

      palette = new ComponentPalette(interaction, renderer);
      interaction.setPalette(palette);

      // --- Layout ---
      JScrollPane scrollPalette = new JScrollPane(palette);
      scrollPalette.setBorder(null);
      scrollPalette.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPalette, circuitPanel);
      splitPane.setDividerLocation(130);
      splitPane.setContinuousLayout(true);
      splitPane.setResizeWeight(0.0);

      frame.add(splitPane);

      frame.setSize(1280, 800);
      frame.setLocationRelativeTo(null);

      // --- F11 Full Screen Toggle ---
      circuitPanel.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_F11)
            toggleFullScreen(frame);
        }
      });

      frame.setVisible(true);
      circuitPanel.requestFocusInWindow();
    });
  }

  private static void performSave() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Logik Files (.lgk)", "lgk"));
    if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      if (!file.getName().endsWith(".lgk")) {
        file = new File(file.getAbsolutePath() + ".lgk");
      }

      try {
        // Collect custom tools currently in palette
        // Note: We need a way to get them. For now, we will just rely on
        // finding them in the circuit, but ideally Palette should expose them.
        // In this implementation, we save what's on the board.
        // (Advanced: Update Palette to expose list of CustomComponents)
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

        // 1. Hot-Swap the Circuit
        // (We reuse the panel/interaction but replace the internal circuit model)
        // This requires CircuitInteraction/Panel to support setCircuit, or we rebuild.
        // Easier approach: Rebuild the frame content or pass the new circuit to the
        // panel.
        // Let's assume we need to update the panel.

        // WARNING: The current CircuitPanel creates its own circuit in constructor.
        // We need to inject the new one.

        circuitPanel.setCircuit(result.circuit());

        // Add Loaded Custom Tools to Palette
        for (CustomComponent cc : result.customTools()) {
          palette.addCustomTool(cc);
        }

        circuitPanel.repaint();
        JOptionPane.showMessageDialog(frame, "Loaded successfully!");

      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(frame, "Error loading: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static void toggleFullScreen(JFrame frame) {
    // (Same as before)
    frame.dispose();
    isFullScreen = !isFullScreen;
    if (isFullScreen) {
      prevLocation = frame.getLocation();
      prevSize = frame.getSize();
      frame.setUndecorated(true);
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      if (gd.isFullScreenSupported()) {
        try {
          gd.setFullScreenWindow(frame);
        } catch (Exception ex) {
          gd.setFullScreenWindow(null);
        }
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
