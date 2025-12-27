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

  private final ComponentPainter componentPainter = new ComponentPainter();
  private final WirePainter wirePainter = new WirePainter();

  // Public Constants
  public static final int PIN_SIZE = 8;
  public static final int HANDLE_SIZE = 6;
  public static final int HANDLE_HIT_SIZE = 10;

  // Internal
  private static final int GRID_SIZE = 20;
  private static final Color GRID_COLOR = new Color(235, 235, 235);
  private static final Color SELECTION_FILL = new Color(0, 180, 255, 40);
  private static final Color SELECTION_BORDER = new Color(0, 180, 255);
  private static final Color HOVER_COLOR = new Color(255, 180, 0);
  private static final Color WIRE_OFF = new Color(100, 100, 100);
  private static final Color WIRE_ON = new Color(230, 50, 50);

  public record Pin(Component component, int index, boolean isInput, Point location) {
  }

  public record WaypointRef(Wire.PortConnection connection, Point point) {
  }

  public record WireSegment(Wire wire, Wire.PortConnection connection) {
  }

  public void render(Graphics2D g2,
      List<Component> components,
      List<Wire> wires,
      List<Component> selectedComponents,
      WireSegment selectedWire,
      WaypointRef selectedWaypoint,
      Pin hoveredPin,
      WireSegment hoveredWire,
      WaypointRef hoveredWaypoint,
      Pin connectionStartPin,
      Point currentMousePoint,
      Rectangle selectionRect,
      Component ghostComponent,
      Rectangle viewBounds) {

    setupGraphics(g2);
    drawGrid(g2, viewBounds);
    drawWires(g2, wires, selectedWire, hoveredWire, selectedWaypoint, hoveredWaypoint);
    drawComponents(g2, components, selectedComponents, hoveredPin, activePin(hoveredPin, connectionStartPin));

    if (connectionStartPin != null && currentMousePoint != null) {
      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[] { 5 }, 0));
      g2.drawLine(connectionStartPin.location().x, connectionStartPin.location().y, currentMousePoint.x,
          currentMousePoint.y);
      g2.setColor(HOVER_COLOR);
      g2.fillOval(currentMousePoint.x - 4, currentMousePoint.y - 4, 8, 8);
    }

    drawSelectionBox(g2, selectionRect);

    if (ghostComponent != null) {
      Composite originalComposite = g2.getComposite();
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
      // Delegate drawing
      componentPainter.drawComponent(g2, ghostComponent, false, false);
      componentPainter.drawStubs(g2, ghostComponent);
      g2.setComposite(originalComposite);
    }
  }

  private void setupGraphics(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT); // Default for
                                                                                                 // performance
  }

  private Pin activePin(Pin hovered, Pin start) {
    return (start != null) ? start : hovered;
  }

  private void drawGrid(Graphics2D g2, Rectangle bounds) {
    if (bounds == null)
      return;
    Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    int startX = (int) (Math.floor(bounds.x / (double) GRID_SIZE) * GRID_SIZE);
    int startY = (int) (Math.floor(bounds.y / (double) GRID_SIZE) * GRID_SIZE);
    for (int x = startX; x < bounds.x + bounds.width + GRID_SIZE; x += GRID_SIZE)
      g2.drawLine(x, bounds.y, x, bounds.y + bounds.height);
    for (int y = startY; y < bounds.y + bounds.height + GRID_SIZE; y += GRID_SIZE)
      g2.drawLine(bounds.x, y, bounds.x + bounds.width, y);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
  }

  private void drawWires(Graphics2D g2, List<Wire> wires,
      WireSegment selectedWire, WireSegment hoveredWire,
      WaypointRef selectedWaypoint, WaypointRef hoveredWaypoint) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (Wire w : wires) {
      Component source = w.getSource();
      if (source == null)
        continue;
      int sourceIndex = 0;
      for (int i = 0; i < source.getOutputCount(); i++) {
        if (source.getOutputWire(i) == w) {
          sourceIndex = i;
          break;
        }
      }
      Point p1 = componentPainter.getPinLocation(source, false, sourceIndex);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        Point p2 = componentPainter.getPinLocation(dest, true, pc.inputIndex);

        boolean isWireSelected = (selectedWire != null && selectedWire.wire() == w && selectedWire.connection() == pc);
        boolean isWireHovered = (hoveredWire != null && hoveredWire.wire() == w && hoveredWire.connection() == pc);

        Shape path = wirePainter.createWireShape(p1, p2, pc.waypoints);
        wirePainter.drawWire(g2, path, w.getSignal(), isWireSelected, isWireHovered);

        if (isWireSelected || isWireHovered || !pc.waypoints.isEmpty()) {
          for (Point pt : pc.waypoints) {
            boolean isPtSelected = (selectedWaypoint != null && selectedWaypoint.point() == pt);
            boolean isPtHovered = (hoveredWaypoint != null && hoveredWaypoint.point() == pt);
            if (isPtSelected || isPtHovered || isWireSelected) {
              wirePainter.drawHandle(g2, pt, isPtSelected, isPtHovered);
            }
          }
        }
      }
    }
  }

  private void drawComponents(Graphics2D g2, List<Component> components, List<Component> selectedComponents,
      Pin hoveredPin, Pin activePin) {
    for (Component c : components) {
      boolean isSelected = selectedComponents.contains(c);

      // Delegation!
      componentPainter.drawStubs(g2, c);
      componentPainter.drawComponent(g2, c, isSelected, true);

      // Delegation for pin drawing (loops logic remains here, primitive drawing is
      // delegated)
      if (!(c instanceof OutputProbe)) {
        int outCount = c.getOutputCount();
        for (int i = 0; i < outCount; i++) {
          Point out = componentPainter.getPinLocation(c, false, i);
          Pin p = new Pin(c, i, false, out);
          boolean h = (hoveredPin != null && hoveredPin.equals(p));
          boolean a = (activePin != null && activePin.equals(p));
          componentPainter.drawPinCircle(g2, out, h, a);
        }
      }
      int inCount = componentPainter.getInputCount(c);
      for (int i = 0; i < inCount; i++) {
        Point in = componentPainter.getPinLocation(c, true, i);
        Pin p = new Pin(c, i, true, in);
        boolean h = (hoveredPin != null && hoveredPin.equals(p));
        boolean a = (activePin != null && activePin.equals(p));
        componentPainter.drawPinCircle(g2, in, h, a);
      }
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

  // --- Proxies ---
  public Shape createWireShape(Point start, Point end, List<Point> waypoints) {
    return wirePainter.createWireShape(start, end, waypoints);
  }

  public Point getPinLocation(Component c, boolean isInput, int index) {
    return componentPainter.getPinLocation(c, isInput, index);
  }

  public int getInputCount(Component c) {
    return componentPainter.getInputCount(c);
  }

  public void drawComponentBody(Graphics2D g2, Component c, boolean sel, boolean label) {
    componentPainter.drawComponent(g2, c, sel, label);
  }

  public Rectangle getComponentBounds(Component c) {
    return componentPainter.getComponentBounds(c);
  }
}
