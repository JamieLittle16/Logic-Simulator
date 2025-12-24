package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component; // Explicit import to avoid collision with java.awt.Component

/**
 * CircuitPanel: The main "Canvas" for the Logic Simulator.
 * * Responsibilities:
 * 1. Store the list of active Components and Wires.
 * 2. Handle user interaction (Mouse Clicks on Switches).
 * 3. Render the circuit using high-quality 2D graphics (Anti-aliasing,
 * Gradients, Bezier Curves).
 */
public class CircuitPanel extends JPanel {

  // --- State ---
  private final List<Component> components = new ArrayList<>();
  private final List<Wire> wires = new ArrayList<>();

  // --- Visual Styling Constants ---
  private static final int GRID_SIZE = 20;
  private static final Color GRID_COLOR = new Color(230, 230, 230); // Very light gray

  // Wire Colors
  private static final Color WIRE_OFF = new Color(80, 80, 80); // Dark Gray
  private static final Color WIRE_ON = new Color(255, 60, 60); // Bright Red

  // Gate Styling
  private static final Color PIN_COLOR = Color.DARK_GRAY;
  private static final Color GATE_GRADIENT_TOP = new Color(60, 100, 180); // Blue-ish
  private static final Color GATE_GRADIENT_BOTTOM = new Color(100, 140, 220); // Lighter Blue

  public CircuitPanel() {
    // 1. Setup the Panel
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);

    // 2. Add Interaction: Clicking Switches
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        for (Component c : components) {
          if (c instanceof Switch) {
            // Check collision: Is the mouse inside the Switch's 40x40 box?
            if (isMouseOver(e, c)) {
              Switch s = (Switch) c;
              s.toggle(!s.getState()); // Flip state
              repaint(); // Trigger a redraw to show new colors
              return; // Stop checking
            }
          }
        }
      }
    });

    // 3. Add Interaction: Hover Cursor (Hand icon over clickable items)
    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        boolean hovering = false;
        for (Component c : components) {
          if (c instanceof Switch && isMouseOver(e, c)) {
            hovering = true;
            break;
          }
        }
        // Change cursor to HAND if hovering, otherwise DEFAULT arrow
        setCursor(hovering ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            : Cursor.getDefaultCursor());
      }
    });
  }

  /** Helper to check if mouse event is inside a component's bounding box */
  private boolean isMouseOver(MouseEvent e, Component c) {
    return e.getX() >= c.getX() && e.getX() <= c.getX() + 40 &&
        e.getY() >= c.getY() && e.getY() <= c.getY() + 40;
  }

  /**
   * Adds a component to the scene and automatically tracks its output wire.
   */
  public void addComponent(Component c) {
    components.add(c);
    if (c.getOutputWire() != null) {
      wires.add(c.getOutputWire());
    }
    repaint();
  }

  /**
   * The Main Drawing Loop. Called automatically by Java Swing.
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // Turn on "Anti-aliasing" (smooths jagged edges)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    drawGrid(g2); // Layer 1: Background
    drawWires(g2); // Layer 2: Wires (behind gates)
    drawComponents(g2); // Layer 3: Components (on top)
  }

  // =========================================================
  // DRAWING HELPERS
  // =========================================================

  private void drawGrid(Graphics2D g2) {
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    // Vertical lines
    for (int x = 0; x < getWidth(); x += GRID_SIZE) {
      g2.drawLine(x, 0, x, getHeight());
    }
    // Horizontal lines
    for (int y = 0; y < getHeight(); y += GRID_SIZE) {
      g2.drawLine(0, y, getWidth(), y);
    }
  }

  private void drawWires(Graphics2D g2) {
    // Use a thick stroke with rounded ends for wires
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (Wire w : wires) {
      Component source = w.getSource();
      // Start wire at the center-right of the source component
      int x1 = source.getX() + 50;
      int y1 = source.getY() + 20;

      // Draw a connection to EVERY destination
      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        // End wire at the left of the destination
        // Adjust Y based on input index (Pin 0 is higher, Pin 1 is lower)
        int x2 = dest.getX();
        int y2 = dest.getY() + 10 + (pc.inputIndex * 20);

        // Color depends on signal state
        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);

        // Create a "Cubic Bezier Curve" for a smooth S-shape
        CubicCurve2D.Double curve = new CubicCurve2D.Double();

        // Control Points: Pull the wire horizontally before curving
        double ctrlDist = Math.abs(x2 - x1) * 0.5; // Control point distance

        curve.setCurve(
            x1, y1, // Start Point
            x1 + ctrlDist, y1, // Control Point 1 (Pull Right)
            x2 - ctrlDist, y2, // Control Point 2 (Pull Left)
            x2, y2 // End Point
        );

        g2.draw(curve);
      }
    }
  }

  private void drawComponents(Graphics2D g2) {
    for (Component c : components) {
      int x = c.getX();
      int y = c.getY();

      // 1. Draw Output Pin Stub (Small line sticking out the back)
      g2.setColor(PIN_COLOR);
      g2.setStroke(new BasicStroke(2));
      g2.drawLine(x + 40, y + 20, x + 50, y + 20);

      // 2. Draw Specific Shape based on Class Type
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
        drawGenericBox(g2, c, x, y); // Fallback for unknown types

      // 3. Draw Component Name Label
      g2.setColor(Color.BLACK);
      g2.setFont(new Font("Arial", Font.BOLD, 10));
      // Center text roughly above the component
      g2.drawString(c.getName(), x, y - 5);
    }
  }

  // =========================================================
  // SPECIFIC COMPONENT RENDERERS
  // =========================================================

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y) {
    // Draw Base Plate
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRoundRect(x, y, 40, 40, 5, 5);
    g2.setColor(Color.GRAY);
    g2.drawRoundRect(x, y, 40, 40, 5, 5);

    // Determine Color (Green=ON, Red=OFF)
    boolean on = s.getOutputWire().getSignal();
    Color toggleColor = on ? new Color(50, 200, 50) : new Color(200, 50, 50);

    // Add 3D Gradient effect to the button
    GradientPaint gp = new GradientPaint(x, y, toggleColor.brighter(), x, y + 40, toggleColor.darker());
    g2.setPaint(gp);

    // Draw the Toggle Lever (Up position if ON, Down if OFF)
    int toggleY = on ? y + 5 : y + 20;
    g2.fillRoundRect(x + 10, toggleY, 20, 15, 2, 2);

    g2.setColor(Color.BLACK);
    g2.drawRoundRect(x + 10, toggleY, 20, 15, 2, 2);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y) {
    boolean on = p.getState();

    // 1. Draw Glow Effect (if ON)
    if (on) {
      float[] dist = { 0.0f, 0.8f }; // Center to edge
      // Fade from bright yellow to transparent red
      Color[] colors = { new Color(255, 255, 200), new Color(255, 200, 0, 0) };
      RadialGradientPaint glow = new RadialGradientPaint(
          new Point2D.Float(x + 20, y + 20), 30, dist, colors);
      g2.setPaint(glow);
      g2.fillOval(x - 10, y - 10, 60, 60);
    }

    // 2. Draw Bulb Body
    Color core = on ? Color.YELLOW : new Color(60, 60, 60);
    Color rim = on ? Color.ORANGE : Color.BLACK;

    // Gradient for round 3D look
    GradientPaint gp = new GradientPaint(x, y, core.brighter(), x + 20, y + 20, core.darker());
    g2.setPaint(gp);
    g2.fillOval(x, y, 40, 40);

    g2.setColor(rim);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);

    // 3. Draw "Shine" reflection
    g2.setColor(new Color(255, 255, 255, 100)); // Semi-transparent white
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
    // 1. Curved Back (Concave)
    path.quadTo(x + 15, y + 20, x, y + 40);
    // 2. Bottom Curve to Tip
    path.quadTo(x + 35, y + 40, x + 50, y + 20);
    // 3. Top Curve from Tip
    path.quadTo(x + 35, y, x, y);
    path.closePath();

    fillAndOutlineGate(g2, path);
    // Pins start slightly deeper (x+5) due to the curved back
    drawInputPins(g2, x + 5, y, 2);
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y) {
    // 1. Draw the extra "Exclusive" curved line at the back
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    Path2D backLine = new Path2D.Double();
    backLine.moveTo(x - 5, y);
    backLine.quadTo(x + 10, y + 20, x - 5, y + 40);
    g2.draw(backLine);

    // 2. Draw a Standard OR gate slightly offset to the right
    drawOrGate(g2, c, x + 5, y);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y) {
    // Triangle Shape
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.lineTo(x + 35, y + 20); // Tip
    path.lineTo(x, y + 40); // Bottom
    path.closePath();

    fillAndOutlineGate(g2, path);

    // The "Inverter Bubble" at the tip
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

  // --- Low-Level Helpers ---

  private void fillAndOutlineGate(Graphics2D g2, Path2D path) {
    // 1. Fill with Vertical Gradient
    GradientPaint gp = new GradientPaint(0, 0, GATE_GRADIENT_TOP, 0, 40, GATE_GRADIENT_BOTTOM);
    g2.setPaint(gp);
    g2.fill(path);

    // 2. Draw Black Outline
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.draw(path);
  }

  private void drawInputPins(Graphics2D g2, int x, int y, int count) {
    g2.setColor(PIN_COLOR);
    g2.setStroke(new BasicStroke(2));
    if (count == 1) {
      // One pin in the middle (e.g., NOT Gate)
      g2.drawLine(x - 10, y + 20, x, y + 20);
    } else if (count == 2) {
      // Two pins (Input A and B)
      g2.drawLine(x - 10, y + 10, x, y + 10); // Pin 0 (Top)
      g2.drawLine(x - 10, y + 30, x, y + 30); // Pin 1 (Bottom)
    }
  }
}
