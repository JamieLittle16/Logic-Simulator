package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class CircuitPanel extends JPanel {
  private final List<Component> components = new ArrayList<>();
  private final List<Wire> wires = new ArrayList<>();

  // --- Styling Constants ---
  private static final int GRID_SIZE = 20;
  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private static final Color WIRE_OFF = new Color(80, 80, 80);
  private static final Color WIRE_ON = new Color(255, 60, 60);
  private static final Color PIN_COLOR = Color.DARK_GRAY;

  // Gate Gradients
  private static final Color GATE_BODY_COLOR_1 = new Color(60, 100, 180);
  private static final Color GATE_BODY_COLOR_2 = new Color(100, 140, 220);

  public CircuitPanel() {
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);
  }

  public void addComponent(Component c) {
    components.add(c);
    if (c.getOutputWire() != null) {
      wires.add(c.getOutputWire());
    }
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // Enable Antialiasing for smooth lines and curves
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    drawGrid(g2);
    drawWires(g2);
    drawComponents(g2);
  }

  // --- 1. Background Grid ---
  private void drawGrid(Graphics2D g2) {
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    for (int x = 0; x < getWidth(); x += GRID_SIZE) {
      g2.drawLine(x, 0, x, getHeight());
    }
    for (int y = 0; y < getHeight(); y += GRID_SIZE) {
      g2.drawLine(0, y, getWidth(), y);
    }
  }

  // --- 2. Curved Wires ---
  private void drawWires(Graphics2D g2) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (Wire w : wires) {
      Component source = w.getSource();
      // Start from the right side of the source component
      int x1 = source.getX() + 50;
      int y1 = source.getY() + 20;

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        // End at the left side of the destination, adjusted for input index
        // (Input 0 is higher, Input 1 is lower)
        int x2 = dest.getX();
        int y2 = dest.getY() + 10 + (pc.inputIndex * 20);

        // Set Color
        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);

        // Draw Cubic Bezier Curve
        CubicCurve2D.Double curve = new CubicCurve2D.Double();
        // Control points extend horizontally to create smooth S-shape
        double ctrlDist = Math.abs(x2 - x1) * 0.5;
        curve.setCurve(x1, y1, x1 + ctrlDist, y1, x2 - ctrlDist, y2, x2, y2);

        g2.draw(curve);
      }
    }
  }

  // --- 3. Component Rendering ---
  private void drawComponents(Graphics2D g2) {
    for (Component c : components) {
      int x = c.getX();
      int y = c.getY();

      // Draw input/output pins (little stubs)
      g2.setColor(PIN_COLOR);
      g2.setStroke(new BasicStroke(2));
      // Output pin stub
      g2.drawLine(x + 40, y + 20, x + 50, y + 20);

      // Render specific shape based on class
      if (c instanceof Switch)
        drawSwitch(g2, (Switch) c, x, y);
      else if (c instanceof OutputProbe)
        drawLight(g2, (OutputProbe) c, x, y);
      else if (c instanceof AndGate)
        drawAndGate(g2, c, x, y);
      else if (c instanceof OrGate)
        drawOrGate(g2, c, x, y);
      else if (c instanceof XorGate)
        drawXorGate(g2, c, x, y);
      else if (c instanceof NotGate)
        drawNotGate(g2, c, x, y);
      else
        drawGenericBox(g2, c, x, y); // Fallback

      // Draw Label
      g2.setColor(Color.BLACK);
      g2.setFont(new Font("Arial", Font.BOLD, 10));
      g2.drawString(c.getName(), x, y - 5);
    }
  }

  // --- Shape Helpers ---

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y) {
    // Base plate
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRoundRect(x, y, 40, 40, 5, 5);
    g2.setColor(Color.GRAY);
    g2.drawRoundRect(x, y, 40, 40, 5, 5);

    // Toggle Switch
    boolean on = s.getOutputWire().getSignal();
    Color toggleColor = on ? new Color(50, 200, 50) : new Color(200, 50, 50);

    // Gradient for 3D effect
    GradientPaint gp = new GradientPaint(x, y, toggleColor.brighter(), x, y + 40, toggleColor.darker());
    g2.setPaint(gp);

    // Draw the switch lever (Up for ON, Down for OFF)
    int toggleY = on ? y + 5 : y + 20;
    g2.fillRoundRect(x + 10, toggleY, 20, 15, 2, 2);

    g2.setColor(Color.BLACK);
    g2.drawRoundRect(x + 10, toggleY, 20, 15, 2, 2);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y) {
    boolean on = p.getState();

    // Glow effect
    if (on) {
      float[] dist = { 0.0f, 0.8f };
      Color[] colors = { new Color(255, 255, 200), new Color(255, 200, 0, 0) };
      RadialGradientPaint glow = new RadialGradientPaint(
          new Point2D.Float(x + 20, y + 20), 30, dist, colors);
      g2.setPaint(glow);
      g2.fillOval(x - 10, y - 10, 60, 60);
    }

    // Bulb Body
    Color core = on ? Color.YELLOW : new Color(60, 60, 60);
    Color rim = on ? Color.ORANGE : Color.BLACK;

    GradientPaint gp = new GradientPaint(x, y, core.brighter(), x + 20, y + 20, core.darker());
    g2.setPaint(gp);
    g2.fillOval(x, y, 40, 40);

    g2.setColor(rim);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);

    // Glass shine
    g2.setColor(new Color(255, 255, 255, 100));
    g2.fillOval(x + 10, y + 5, 15, 10);
  }

  private void drawAndGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y); // Top-left
    path.lineTo(x + 20, y); // Top-middle
    path.curveTo(x + 50, y, x + 50, y + 40, x + 20, y + 40); // The "D" Curve
    path.lineTo(x, y + 40); // Bottom-left
    path.closePath(); // Back to Top-left

    fillAndOutlineGate(g2, path);
    drawInputPins(g2, x, y, 2);
  }

  private void drawOrGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.quadTo(x + 15, y + 20, x, y + 40); // Curved Back (Concave)
    path.quadTo(x + 35, y + 40, x + 50, y + 20); // Bottom Curve to Tip
    path.quadTo(x + 35, y, x, y); // Top Curve from Tip
    path.closePath();

    fillAndOutlineGate(g2, path);
    drawInputPins(g2, x + 5, y, 2); // Pins start slightly deeper due to curve
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y) {
    // 1. Draw the extra curved line at the back
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    Path2D backLine = new Path2D.Double();
    backLine.moveTo(x - 5, y);
    backLine.quadTo(x + 10, y + 20, x - 5, y + 40);
    g2.draw(backLine);

    // 2. Draw the Standard OR gate slightly offset
    drawOrGate(g2, c, x + 5, y);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.lineTo(x + 35, y + 20); // Tip
    path.lineTo(x, y + 40);
    path.closePath();

    fillAndOutlineGate(g2, path);

    // The Bubble
    g2.setColor(Color.WHITE);
    g2.fillOval(x + 32, y + 15, 10, 10);
    g2.setColor(Color.BLACK);
    g2.drawOval(x + 32, y + 15, 10, 10);

    drawInputPins(g2, x, y, 1);
  }

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y) {
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(x, y, 50, 40);
    g2.setColor(Color.BLACK);
    g2.drawRect(x, y, 50, 40);
  }

  // --- Helpers ---

  private void fillAndOutlineGate(Graphics2D g2, Path2D path) {
    // Gradient Fill
    GradientPaint gp = new GradientPaint(0, 0, GATE_BODY_COLOR_1, 0, 40, GATE_BODY_COLOR_2);
    g2.setPaint(gp);
    g2.fill(path);

    // Outline
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.draw(path);
  }

  private void drawInputPins(Graphics2D g2, int x, int y, int count) {
    g2.setColor(PIN_COLOR);
    g2.setStroke(new BasicStroke(2));
    if (count == 1) {
      g2.drawLine(x - 10, y + 20, x, y + 20); // One pin middle
    } else if (count == 2) {
      g2.drawLine(x - 10, y + 10, x, y + 10); // Pin 0
      g2.drawLine(x - 10, y + 30, x, y + 30); // Pin 1
    }
  }
}
