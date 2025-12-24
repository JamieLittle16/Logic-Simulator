package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;

public class GuiMain {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Logic Simulator");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      CircuitPanel circuitPanel = new CircuitPanel();
      CircuitInteraction interaction = circuitPanel.getInteraction();
      CircuitRenderer renderer = circuitPanel.getRenderer();

      ComponentPalette palette = new ComponentPalette(interaction, renderer);

      // Add Scroll Pane for Sidebar
      JScrollPane scrollPalette = new JScrollPane(palette);
      scrollPalette.setBorder(null);
      scrollPalette.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPalette, circuitPanel);
      splitPane.setDividerLocation(130);
      splitPane.setContinuousLayout(true);

      frame.add(splitPane);
      frame.setSize(1000, 700);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);

      circuitPanel.requestFocusInWindow();
    });
  }
}
