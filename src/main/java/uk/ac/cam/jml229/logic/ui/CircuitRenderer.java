package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;
import uk.ac.cam.jml229.logic.model.Wire;

public class CircuitRenderer {

  // --- Styling Constants ---
  private static final int GRID_SIZE = 20;
  public static final int PIN_SIZE = 8;

  private static final Color GRID_COLOR = new Color(235, 235, 235);
  private static final Color SELECTION_BORDER = new Color(0, 180, 255);
  private static final Color SELECTION_FILL = new Color(0, 180, 255, 40);
  private static final Color HOVER_COLOR = new Color(255, 180, 0);
  private static final Color PIN_COLOR = new Color(50, 50, 50);
  private static final Color STUB_COLOR = new Color(0, 0, 0);
  private static final Color WIRE_OFF = new Color(100, 100, 100);
  private static final Color WIRE_ON = new Color(230, 50, 50);

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
      Pin hoveredPin,
      WireSegment hoveredWire,
      Pin connectionStartPin,
      Point currentMousePoint,
      Rectangle selectionRect,
      Component ghostComponent) {

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    drawGrid(g2, g2.getClipBounds());
    drawWires(g2, wires, selectedWire, hoveredWire);
    drawComponents(g2, components, selectedComponents, hoveredPin, connectionStartPin);

    if (connectionStartPin != null && currentMousePoint != null) {
      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[] { 5 }, 0));
      g2.drawLine(connectionStartPin.location.x, connectionStartPin.location.y, currentMousePoint.x,
          currentMousePoint.y);
      g2.setColor(HOVER_COLOR);
      g2.fillOval(currentMousePoint.x - 4, currentMousePoint.y - 4, 8, 8);
    }

    drawSelectionBox(g2, selectionRect);

    if (ghostComponent != null) {
      Composite originalComposite = g2.getComposite();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
      drawComponentBody(g2, ghostComponent, false, false);
      drawPins(g2, ghostComponent, null, null);
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

  private void drawWires(Graphics2D g2, List<Wire> wires, WireSegment selectedWire, WireSegment hoveredWire) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (Wire w : wires) {
      Component source = w.getSource();
      if (source == null)
        continue;

      // Find which output index this wire is connected to
      int sourceIndex = 0;
      for (int i = 0; i < source.getOutputCount(); i++) {
        if (source.getOutputWire(i) == w) {
          sourceIndex = i;
          break;
        }
      }

      // Use the correct index (sourceIndex)
      Point p1 = getPinLocation(source, false, sourceIndex);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        Point p2 = getPinLocation(dest, true, pc.inputIndex);

        boolean isSelected = (selectedWire != null && selectedWire.wire == w && selectedWire.connection == pc);
        boolean isHovered = (hoveredWire != null && hoveredWire.wire == w && hoveredWire.connection == pc);

        CubicCurve2D.Double curve = createWireCurve(p1.x, p1.y, p2.x, p2.y);

        if (isSelected || isHovered) {
          g2.setColor(isSelected ? SELECTION_BORDER : HOVER_COLOR);
          g2.setStroke(new BasicStroke(6));
          g2.draw(curve);
          g2.setStroke(new BasicStroke(3));
        }

        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);
        g2.draw(curve);
      }
    }
  }

  private void drawComponents(Graphics2D g2, List<Component> components, List<Component> selectedComponents,
      Pin hoveredPin, Pin activePin) {
    for (Component c : components) {
      boolean isSelected = selectedComponents.contains(c);
      drawComponentStubs(g2, c);
      drawComponentBody(g2, c, isSelected, true);
      drawPins(g2, c, hoveredPin, activePin);
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

  public void drawComponentBody(Graphics2D g2, Component c, boolean sel, boolean drawLabel) {
    int x = c.getX();
    int y = c.getY();

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
      drawNandGate(g2, c, x, y, sel);
    else if (c instanceof NorGate)
      drawNorGate(g2, c, x, y, sel);
    else if (c instanceof BufferGate)
      drawBufferGate(g2, c, x, y, sel);
    else
      drawGenericBox(g2, c, x, y, sel);

    if (drawLabel) {
      g2.setColor(Color.BLACK);
      g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
      FontMetrics fm = g2.getFontMetrics();
      int tw = fm.stringWidth(c.getName());
      g2.drawString(c.getName(), x + (50 - tw) / 2, y - 5);
    }
  }

  private void drawComponentStubs(Graphics2D g2, Component c) {
    if (c instanceof OutputProbe)
      return;

    g2.setColor(STUB_COLOR);
    g2.setStroke(new BasicStroke(3));
    int x = c.getX();
    int y = c.getY();

    int outCount = c.getOutputCount();
    boolean hasBubbleOutput = (c instanceof NandGate || c instanceof NorGate);

    for (int i = 0; i < outCount; i++) {
      if (outCount == 1) {
        if (!hasBubbleOutput) {
          if (c instanceof Switch)
            g2.drawLine(x + 40, y + 20, x + 60, y + 20);
          else
            g2.drawLine(x + 50, y + 20, x + 60, y + 20);
        } else {
          g2.drawLine(x + 55, y + 20, x + 60, y + 20);
        }
      } else {
        Point p = getPinLocation(c, false, i);
        g2.drawLine(x + 50, p.y, p.x, p.y);
      }
    }

    int inputCount = getInputCount(c);
    for (int i = 0; i < inputCount; i++) {
      Point p = getPinLocation(c, true, i);
      int endX = x;
      if (c instanceof OrGate || c instanceof NorGate || c instanceof XorGate)
        endX = x + 8;
      g2.drawLine(p.x, p.y, endX, p.y);
    }
  }

  public void drawPins(Graphics2D g2, Component c, Pin hoveredPin, Pin activePin) {
    if (!(c instanceof OutputProbe)) {
      int count = c.getOutputCount();
      for (int i = 0; i < count; i++) {
        Point out = getPinLocation(c, false, i);
        Pin myPin = new Pin(c, i, false, out);
        drawPinCircle(g2, myPin, hoveredPin, activePin);
      }
    }
    int count = getInputCount(c);
    for (int i = 0; i < count; i++) {
      Point in = getPinLocation(c, true, i);
      Pin myPin = new Pin(c, i, true, in);
      drawPinCircle(g2, myPin, hoveredPin, activePin);
    }
  }

  private void drawPinCircle(Graphics2D g2, Pin myPin, Pin hoveredPin, Pin activePin) {
    boolean isHovered = (hoveredPin != null && hoveredPin.equals(myPin));
    boolean isActive = (activePin != null && activePin.equals(myPin));

    Point p = myPin.location;

    if (isActive) {
      g2.setColor(SELECTION_BORDER);
      g2.fillOval(p.x - 6, p.y - 6, 12, 12);
    } else if (isHovered) {
      g2.setColor(HOVER_COLOR);
      g2.drawOval(p.x - 6, p.y - 6, 12, 12);
    }

    g2.setColor(PIN_COLOR);
    g2.fillOval(p.x - PIN_SIZE / 2, p.y - PIN_SIZE / 2, PIN_SIZE, PIN_SIZE);
  }

  // --- Shapes ---

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRoundRect(x, y + 5, 40, 30, 15, 15);
    }
    g2.setColor(Color.DARK_GRAY);
    g2.fillRoundRect(x, y + 5, 40, 30, 30, 30);
    boolean on = s.getOutputWire() != null && s.getOutputWire().getSignal();
    int circleX = on ? x + 22 : x + 2;
    Color c = on ? new Color(100, 255, 100) : new Color(200, 200, 200);
    g2.setColor(c);
    g2.fillOval(circleX, y + 7, 26, 26);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(1));
    g2.drawOval(circleX, y + 7, 26, 26);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawOval(x, y, 40, 40);
    }
    boolean on = p.getState();
    Color core = on ? new Color(255, 220, 0) : new Color(50, 50, 50);
    if (on) {
      float[] dist = { 0.0f, 0.7f, 1.0f };
      Color[] colors = { new Color(255, 255, 200, 200), new Color(255, 220, 0, 100), new Color(0, 0, 0, 0) };
      RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(x + 20, y + 20), 35, dist, colors);
      g2.setPaint(glow);
      g2.fillOval(x - 15, y - 15, 70, 70);
    }
    GradientPaint gp = new GradientPaint(x, y, core.brighter(), x + 30, y + 30, core.darker());
    g2.setPaint(gp);
    g2.fillOval(x, y, 40, 40);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);
    g2.setColor(new Color(255, 255, 255, 100));
    g2.fillOval(x + 10, y + 8, 12, 8);
  }

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y, boolean sel) {
    int inputCount = getInputCount(c);
    int outputCount = c.getOutputCount();
    int maxPins = Math.max(inputCount, outputCount);
    // Standard height is 40. For every pin, we need ~20px space.
    int h = Math.max(40, maxPins * 20);
    int w = 50;

    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, w, h);
    }
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(x, y, w, h);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, w, h);

    g2.setColor(Color.WHITE);
    // Use a bold, small font for the chip label
    g2.setFont(new Font("SansSerif", Font.BOLD, 12));

    String name = c.getName();
    // Safety clip if name is too long for 50px width
    FontMetrics fm = g2.getFontMetrics();
    if (fm.stringWidth(name) > 46) {
      // Simple truncation
      while (name.length() > 0 && fm.stringWidth(name + "..") > 46) {
        name = name.substring(0, name.length() - 1);
      }
      name += "..";
    }

    int textWidth = fm.stringWidth(name);
    // Center X: start at x + (width/2) - (text/2)
    // Center Y: header is 16px, so roughly at y + 12
    g2.drawString(name, x + (w - textWidth) / 2, y + 12);
  }

  private void drawAndGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 25, y);
    p.curveTo(x + 57, y, x + 57, y + 40, x + 25, y + 40);
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
    b.moveTo(x - 4, y);
    b.quadTo(x + 11, y + 20, x - 4, y + 40);
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

  private void drawBufferGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 40, y + 20);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawBufferGate(g2, c, x, y, sel);
    drawBubble(g2, x + 40, y + 15, sel);
  }

  private void drawNandGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawAndGate(g2, c, x, y, sel);
    drawBubble(g2, x + 45, y + 15, sel);
  }

  private void drawNorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawOrGate(g2, c, x, y, sel);
    drawBubble(g2, x + 45, y + 15, sel);
  }

  private void fillGate(Graphics2D g2, Path2D p, boolean sel) {
    if (sel) {
      g2.setColor(SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(p);
    }
    GradientPaint gp = new GradientPaint(0, 0, new Color(70, 120, 200), 0, 40, new Color(120, 160, 240));
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
    double ctrlDist = Math.max(20, Math.abs(x2 - x1) * 0.5);
    curve.setCurve(x1, y1, x1 + ctrlDist, y1, x2 - ctrlDist, y2, x2, y2);
    return curve;
  }

  public Point getPinLocation(Component c, boolean isInput, int index) {
    if (!isInput) {
      int outCount = c.getOutputCount();
      if (outCount <= 1)
        return new Point(c.getX() + 60, c.getY() + 20);
      else
        return new Point(c.getX() + 60, c.getY() + 10 + (index * 20));
    } else {
      int count = getInputCount(c);
      if (count == 1)
        return new Point(c.getX() - 10, c.getY() + 20);
      else
        return new Point(c.getX() - 10, c.getY() + 10 + (index * 20));
    }
  }

  public int getInputCount(Component c) {
    if (c instanceof Switch)
      return 0;
    if (c instanceof UnaryGate || c instanceof OutputProbe)
      return 1;
    // Don't assume everything else is 2. Ask the component!
    // This allows CustomComponents to have 3, 4, 8, etc inputs.
    return c.getInputCount();
  }
}
