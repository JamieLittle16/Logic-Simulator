package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.*;

public class CircuitPanel extends JPanel {
  private final List<Component> components = new ArrayList<>();
  // New list for wires
  private final List<Wire> wires = new ArrayList<>();

  public CircuitPanel() {
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);
  }

  public void addComponent(Component c) {
    components.add(c);
    // Automatically find wires connected to this component's output
    if (c.getOutputWire() != null) {
      wires.add(c.getOutputWire());
    }
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 1. DRAW WIRES (Draw these first so they appear behind gates)
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2)); // Thicker lines

    for (Wire w : wires) {
      Component source = w.getSource();
      // Calculate center of source component
      int x1 = source.getX() + 30; // 30 is half of width (60)
      int y1 = source.getY() + 20; // 20 is half of height (40)

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        int x2 = dest.getX() + 30;
        int y2 = dest.getY() + 20;

        // Draw the line
        g2.setColor(w.getSignal() ? Color.RED : Color.BLACK); // Red if ON, Black if OFF
        g2.drawLine(x1, y1, x2, y2);
      }
    }

    // 2. DRAW COMPONENTS
    for (Component c : components) {
      drawComponent(g2, c);
    }
  }

  private void drawComponent(Graphics2D g2, Component c) {
    int x = c.getX();
    int y = c.getY();

    // Change color based on type
    if (c instanceof Switch) {
      // Switches are Circles
      g2.setColor(((Switch) c).getOutputWire().getSignal() ? Color.GREEN : Color.RED);
      g2.fillOval(x, y, 40, 40);
      g2.setColor(Color.BLACK);
      g2.drawString(c.getName(), x, y - 5);

    } else if (c instanceof OutputProbe) {
      // Lights are Circles (Yellow=ON, Gray=OFF)
      g2.setColor(c.getState() ? Color.YELLOW : Color.GRAY);
      g2.fillOval(x, y, 40, 40);
      g2.setColor(Color.BLACK);
      g2.drawString("Light", x + 5, y + 25);

    } else {
      // Standard Gates are Rectangles
      g2.setColor(Color.BLUE);
      g2.fillRect(x, y, 60, 40);
      g2.setColor(Color.WHITE);
      g2.drawString(c.getClass().getSimpleName().replace("Gate", ""), x + 10, y + 25);
    }
  }
}
