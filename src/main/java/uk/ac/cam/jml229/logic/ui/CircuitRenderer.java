package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;

public class CircuitRenderer {

  // --- Styling Constants ---
  private static final int GRID_SIZE = 20;
  public static final int PIN_SIZE = 8;

  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private static final Color SELECTION_BORDER = new Color(0, 200, 255);
  private static final Color SELECTION_FILL = new Color(0, 200, 255, 50);
  private static final Color PIN_COLOR = Color.DARK_GRAY;
  private static final Color WIRE_OFF = new Color(80, 80, 80);
  private static final Color WIRE_ON = new Color(255, 60, 60);

  // --- Shared Types ---
  public record Pin(Component component, int index, boolean isInput, Point location) {
  }

  public static class WireSegment {
    public final Wire wire;
    public final Wire.PortConnection connection;

    public WireSegment(Wire w, Wire.PortConnection pc) {
      this.wire = w;
      this.connection = pc;
    }
  }

  public void render(Graphics2D g2,
      List<Component> components,
      List<Wire> wires,
      List<Component> selectedComponents,
      WireSegment selectedWire,
      Pin dragStartPin,
      Point dragCurrentPoint,
      Rectangle selectionRect,
      Component ghostComponent) {

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    drawGrid(g2, g2.getClipBounds());
    drawWires(g2, wires, selectedWire);
    drawComponents(g2, components, selectedComponents);

    drawDragLine(g2, dragStartPin, dragCurrentPoint);
    drawSelectionBox(g2, selectionRect);

    if (ghostComponent != null) {
      Composite originalComposite = g2.getComposite();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
      // Don't draw label for ghost to keep it clean
      drawComponentBody(g2, ghostComponent, false, false);
      drawPins(g2, ghostComponent);
      g2.setComposite(originalComposite);
    }
  }

  // --- Layers ---

  private void drawGrid(Graphics2D g2, Rectangle bounds) {
    if (bounds == null)
      return;
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    for (int x = 0; x < bounds.width + bounds.x; x += GRID_SIZE)
      g2.drawLine(x, 0, x, bounds.height + bounds.y);
    for (int y = 0; y < bounds.height + bounds.y; y += GRID_SIZE)
      g2.drawLine(0, y, bounds.width + bounds.x, y);
  }

  private void drawWires(Graphics2D g2, List<Wire> wires, WireSegment selectedWire) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (Wire w : wires) {
      Component source = w.getSource();
      if (source == null)
        continue;
      Point p1 = getPinLocation(source, false, 0);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        Point p2 = getPinLocation(dest, true, pc.inputIndex);
        boolean isSelected = (selectedWire != null && selectedWire.wire == w && selectedWire.connection == pc);
        CubicCurve2D.Double curve = createWireCurve(p1.x, p1.y, p2.x, p2.y);

        if (isSelected) {
          g2.setColor(SELECTION_BORDER);
          g2.setStroke(new BasicStroke(6));
          g2.draw(curve);
          g2.setStroke(new BasicStroke(3));
        }
        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);
        g2.draw(curve);
      }
    }
  }

  private void drawComponents(Graphics2D g2, List<Component> components, List<Component> selectedComponents) {
    for (Component c : components) {
      boolean isSelected = selectedComponents.contains(c);
      drawComponentBody(g2, c, isSelected, true); // True = Draw Labels
      drawPins(g2, c);
    }
  }

  private void drawDragLine(Graphics2D g2, Pin startPin, Point currentPoint) {
    if (startPin != null && currentPoint != null) {
      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[] { 5 }, 0));
      g2.drawLine(startPin.location.x, startPin.location.y, currentPoint.x, currentPoint.y);
    }
  }

  private void drawSelectionBox(Graphics2D g2, Rectangle rect) {
    if (rect != null) {
      g2.setColor(SELECTION_FILL);
      g2.fill(rect);
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
      g2.draw(rect);
    }
  }

  // --- Component Drawing ---

  /**
   * Draws the main body of the component.
   * 
   * @param drawLabel If true, draws the text name above the component.
   */
  public void drawComponentBody(Graphics2D g2, Component c, boolean sel, boolean drawLabel) {
    int x = c.getX();
    int y = c.getY();

    // Dispatch to specific drawers
    if (c instanceof Switch)
      drawSwitch(g2, (Switch) c, x, y, sel);
    else if (c instanceof OutputProbe)
      drawLight(g2, (OutputProbe) c, x, y, sel);
    else if (c instanceof AndGate)
      drawAndGate(g2, c, x, y, sel);
    else if (c instanceof OrGate)
      drawOrGate(g2, c, x, y, sel);
    else if (c instanceof XorGate)
      drawXorGate(g2, c, x, y, sel);
    else if (c instanceof NotGate)
      drawNotGate(g2, c, x, y, sel);
    else if (c instanceof NandGate)
      drawNandGate(g2, c, x, y, sel); // NEW
    else if (c instanceof NorGate)
      drawNorGate(g2, c, x, y, sel); // NEW
    else if (c instanceof BufferGate)
      drawBufferGate(g2, c, x, y, sel); // NEW
    else
      drawGenericBox(g2, c, x, y, sel);

    if (drawLabel) {
      g2.setColor(Color.BLACK);
      g2.setFont(new Font("Arial", Font.BOLD, 10));
      g2.drawString(c.getName(), x, y - 5);
    }
  }

  public void drawPins(Graphics2D g2, Component c) {
    if (!(c instanceof OutputProbe)) {
      Point out = getPinLocation(c, false, 0);
      drawPinCircle(g2, out);
    }
    int count = getInputCount(c);
    for (int i = 0; i < count; i++) {
      Point in = getPinLocation(c, true, i);
      drawPinCircle(g2, in);
    }
  }

  private void drawPinCircle(Graphics2D g2, Point p) {
    g2.setColor(PIN_COLOR);
    g2.fillOval(p.x - PIN_SIZE / 2, p.y - PIN_SIZE / 2, PIN_SIZE, PIN_SIZE);
  }

  // --- Shapes ---

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRoundRect(x, y, 40, 40, 5, 5);
    }
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRoundRect(x, y, 40, 40, 5, 5);
    g2.setColor(Color.GRAY);
    g2.setStroke(new BasicStroke(1));
    g2.drawRoundRect(x, y, 40, 40, 5, 5);
    boolean on = s.getOutputWire() != null && s.getOutputWire().getSignal();
    Color c = on ? new Color(50, 200, 50) : new Color(200, 50, 50);
    GradientPaint gp = new GradientPaint(x, y, c.brighter(), x, y + 40, c.darker());
    g2.setPaint(gp);
    g2.fillRoundRect(x + 10, on ? y + 5 : y + 20, 20, 15, 2, 2);
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(x + 10, on ? y + 5 : y + 20, 20, 15, 2, 2);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawOval(x, y, 40, 40);
    }
    boolean on = p.getState();
    if (on) {
      float[] dist = { 0.0f, 0.8f };
      Color[] colors = { new Color(255, 255, 200), new Color(255, 200, 0, 0) };
      RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(x + 20, y + 20), 30, dist, colors);
      g2.setPaint(glow);
      g2.fillOval(x - 10, y - 10, 60, 60);
    }
    Color core = on ? Color.YELLOW : new Color(60, 60, 60);
    Color rim = on ? Color.ORANGE : Color.BLACK;
    GradientPaint gp = new GradientPaint(x, y, core.brighter(), x + 20, y + 20, core.darker());
    g2.setPaint(gp);
    g2.fillOval(x, y, 40, 40);
    g2.setColor(rim);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);
    g2.setColor(new Color(255, 255, 255, 100));
    g2.fillOval(x + 10, y + 5, 15, 10);
  }

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, 50, 40);
    }
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(x, y, 50, 40);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, 50, 40);
  }

  // --- Basic Gates ---

  private void drawAndGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 20, y);
    p.curveTo(x + 50, y, x + 50, y + 40, x + 20, y + 40);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawOrGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.quadTo(x + 15, y + 20, x, y + 40);
    p.quadTo(x + 35, y + 40, x + 50, y + 20);
    p.quadTo(x + 35, y, x, y);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D b = new Path2D.Double();
    b.moveTo(x - 5, y);
    b.quadTo(x + 10, y + 20, x - 5, y + 40);
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(b);
    }
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.draw(b);
    drawOrGate(g2, c, x + 5, y, sel);
  }

  // --- Advanced Gates ---

  private void drawBufferGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 35, y + 20);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawBufferGate(g2, c, x, y, sel); // Draw Triangle
    drawBubble(g2, x + 32, y + 15, sel); // Add Bubble
  }

  private void drawNandGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawAndGate(g2, c, x, y, sel);
    drawBubble(g2, x + 48, y + 15, sel); // Adjusted position for curve tip
  }

  private void drawNorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawOrGate(g2, c, x, y, sel);
    drawBubble(g2, x + 48, y + 15, sel);
  }

  // --- Helpers ---

  private void fillGate(Graphics2D g2, Path2D p, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(p);
    }
    GradientPaint gp = new GradientPaint(0, 0, new Color(60, 100, 180), 0, 40, new Color(100, 140, 220));
    g2.setPaint(gp);
    g2.fill(p);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.draw(p);
  }

  private void drawBubble(Graphics2D g2, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawOval(x, y, 10, 10);
    }
    g2.setColor(Color.WHITE);
    g2.fillOval(x, y, 10, 10);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 10, 10);
  }

  public CubicCurve2D.Double createWireCurve(int x1, int y1, int x2, int y2) {
    CubicCurve2D.Double curve = new CubicCurve2D.Double();
    double ctrlDist = Math.abs(x2 - x1) * 0.5;
    if (ctrlDist < 20)
      ctrlDist = 20;
    curve.setCurve(x1, y1, x1 + ctrlDist, y1, x2 - ctrlDist, y2, x2, y2);
    return curve;
  }

  public Point getPinLocation(Component c, boolean isInput, int index) {
    if (!isInput) {
      return new Point(c.getX() + 50, c.getY() + 20);
    } else {
      int count = getInputCount(c);
      if (count == 1)
        return new Point(c.getX(), c.getY() + 20);
      else
        return new Point(c.getX(), c.getY() + 10 + (index * 20));
    }
  }

  public int getInputCount(Component c) {
    if (c instanceof Switch)
      return 0;
    if (c instanceof UnaryGate || c instanceof OutputProbe)
      return 1;
    return 2;
  }
}
