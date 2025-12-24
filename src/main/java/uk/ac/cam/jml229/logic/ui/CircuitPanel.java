package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class CircuitPanel extends JPanel {

  // --- Model Data ---
  private final List<Component> components = new ArrayList<>();
  private final List<Wire> wires = new ArrayList<>();

  // --- Interaction State ---
  private final List<Component> selectedComponents = new ArrayList<>();

  // Wire Selection (A specific connection from Source -> Dest)
  private static class WireSegment {
    Wire wire;
    Wire.PortConnection connection;

    public WireSegment(Wire w, Wire.PortConnection pc) {
      wire = w;
      connection = pc;
    }
  }

  private WireSegment selectedWireSegment = null;

  // Dragging Logic
  private Point lastMousePt;
  private Rectangle selectionRect;
  private Point selectionStartPt;
  private boolean isDraggingItems = false;

  // Wire Creation State
  private Pin dragStartPin = null;
  private Point dragCurrentPoint = null;

  // --- Visual Constants ---
  private static final int GRID_SIZE = 20;
  private static final int PIN_SIZE = 8;
  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private static final Color SELECTION_BORDER = new Color(0, 200, 255);
  private static final Color SELECTION_FILL = new Color(0, 200, 255, 50);
  private static final Color PIN_COLOR = Color.DARK_GRAY;
  private static final Color WIRE_OFF = new Color(80, 80, 80);
  private static final Color WIRE_ON = new Color(255, 60, 60);

  public CircuitPanel() {
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);
    setFocusable(true); // Allow panel to receive Key Events

    InputHandler inputHandler = new InputHandler();
    addMouseListener(inputHandler);
    addMouseMotionListener(inputHandler);

    // Keyboard Listener for Deletion
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
          deleteSelection();
        }
      }
    });
  }

  public void addComponent(Component c) {
    components.add(c);
    if (c.getOutputWire() != null) {
      wires.add(c.getOutputWire());
    }
    repaint();
  }

  private void deleteSelection() {
    // 1. Delete Selected Wire Connection
    if (selectedWireSegment != null) {
      // FIX: Tell the component "Input is OFF" before removing the wire
      selectedWireSegment.connection.component.setInput(
          selectedWireSegment.connection.inputIndex,
          false);

      // Now remove the connection from the wire's list
      selectedWireSegment.wire.removeDestination(
          selectedWireSegment.connection.component,
          selectedWireSegment.connection.inputIndex);
      selectedWireSegment = null;
    }

    // 2. Delete Selected Components
    if (!selectedComponents.isEmpty()) {
      // FIX: Handle wires starting FROM deleted components
      // We must find any components listening to these wires and turn their inputs
      // OFF
      for (Wire w : wires) {
        if (selectedComponents.contains(w.getSource())) {
          for (Wire.PortConnection pc : w.getDestinations()) {
            // Reset destination input to false
            pc.component.setInput(pc.inputIndex, false);
          }
        }
      }
      // Now safe to remove the wires
      wires.removeIf(w -> selectedComponents.contains(w.getSource()));

      // FIX: Handle wires going TO deleted components
      // Just clean up the wire's destination list so it doesn't point to a ghost
      for (Wire w : wires) {
        w.getDestinations().removeIf(pc -> selectedComponents.contains(pc.component));
      }

      // Remove the components themselves
      components.removeAll(selectedComponents);
      selectedComponents.clear();
    }

    repaint();
  }

  private void createConnection(Component source, Component dest, int inputIndex) {
    if (source == dest)
      return; // No loops to self

    // Prevent multiple wires to the same input
    for (Wire w : wires) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        if (pc.component == dest && pc.inputIndex == inputIndex) {
          return; // Input occupied
        }
      }
    }

    Wire w = source.getOutputWire();
    if (w == null) {
      w = new Wire(source);
      wires.add(w);
    }
    w.addDestination(dest, inputIndex);
  }

  // =========================================================
  // INPUT HANDLING
  // =========================================================

  private class InputHandler extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      requestFocusInWindow();
      lastMousePt = e.getPoint();

      // 1. Check Pins (Start Wire)
      Pin clickedPin = getPinAt(e.getPoint());
      if (clickedPin != null && !clickedPin.isInput) {
        dragStartPin = clickedPin;
        dragCurrentPoint = e.getPoint();
        selectedWireSegment = null;
        repaint();
        return;
      }

      // 2. Check Wires (Select)
      WireSegment clickedWire = getWireAt(e.getPoint());
      if (clickedWire != null) {
        selectedWireSegment = clickedWire;
        selectedComponents.clear();
        repaint();
        return;
      } else {
        selectedWireSegment = null;
      }

      // 3. Check Components (Select/Move)
      Component clickedComp = getLogicComponentAt(e.getPoint());
      if (clickedComp != null) {
        handleComponentSelection(e, clickedComp);
      } else {
        startSelectionBox(e);
      }
      repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (dragStartPin != null) {
        dragCurrentPoint = e.getPoint();
        repaint();
        return;
      }

      if (isDraggingItems) {
        int dx = e.getX() - lastMousePt.x;
        int dy = e.getY() - lastMousePt.y;
        for (Component c : selectedComponents) {
          c.setPosition(c.getX() + dx, c.getY() + dy);
        }
        lastMousePt = e.getPoint();
        repaint();
        return;
      }

      if (selectionRect != null) {
        updateSelectionBox(e);
        repaint();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (dragStartPin != null) {
        Pin endPin = getPinAt(e.getPoint());
        if (endPin != null && endPin.isInput) {
          createConnection(dragStartPin.component, endPin.component, endPin.index);
        }
        dragStartPin = null;
        dragCurrentPoint = null;
        repaint();
        return;
      }

      if (selectionRect != null) {
        finalizeSelectionBox();
      }
      isDraggingItems = false;
      repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (dragStartPin == null && !isDraggingItems) {
        Component c = getLogicComponentAt(e.getPoint());
        if (c instanceof Switch) {
          ((Switch) c).toggle(!((Switch) c).getState());
          repaint();
        }
      }
    }
  }

  // --- Wire/Curve Helpers ---

  private WireSegment getWireAt(Point p) {
    int hitThreshold = 5;
    for (Wire w : wires) {
      Component src = w.getSource();
      Point p1 = getPinLocation(src, false, 0);
      for (Wire.PortConnection pc : w.getDestinations()) {
        Point p2 = getPinLocation(pc.component, true, pc.inputIndex);
        CubicCurve2D.Double curve = createWireCurve(p1.x, p1.y, p2.x, p2.y);
        Shape strokedShape = new BasicStroke(hitThreshold).createStrokedShape(curve);
        if (strokedShape.contains(p))
          return new WireSegment(w, pc);
      }
    }
    return null;
  }

  private CubicCurve2D.Double createWireCurve(int x1, int y1, int x2, int y2) {
    CubicCurve2D.Double curve = new CubicCurve2D.Double();
    double ctrlDist = Math.abs(x2 - x1) * 0.5;
    if (ctrlDist < 20)
      ctrlDist = 20;
    curve.setCurve(x1, y1, x1 + ctrlDist, y1, x2 - ctrlDist, y2, x2, y2);
    return curve;
  }

  // --- Pin Helpers ---
  private record Pin(Component component, int index, boolean isInput, Point location) {
  }

  private Pin getPinAt(Point p) {
    int threshold = PIN_SIZE + 4;
    for (Component c : components) {
      Point outLoc = getPinLocation(c, false, 0);
      if (p.distance(outLoc) <= threshold)
        return new Pin(c, 0, false, outLoc);

      int inputCount = getInputCount(c);
      for (int i = 0; i < inputCount; i++) {
        Point inLoc = getPinLocation(c, true, i);
        if (p.distance(inLoc) <= threshold)
          return new Pin(c, i, true, inLoc);
      }
    }
    return null;
  }

  private Point getPinLocation(Component c, boolean isInput, int index) {
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

  private int getInputCount(Component c) {
    if (c instanceof Switch)
      return 0;
    if (c instanceof UnaryGate || c instanceof OutputProbe)
      return 1;
    return 2;
  }

  // --- Component Selection Helpers ---
  private Component getLogicComponentAt(Point p) {
    for (int i = components.size() - 1; i >= 0; i--) {
      Component c = components.get(i);
      if (p.x >= c.getX() && p.x <= c.getX() + 40 &&
          p.y >= c.getY() && p.y <= c.getY() + 40)
        return c;
    }
    return null;
  }

  private void handleComponentSelection(MouseEvent e, Component clickedComp) {
    if (e.isShiftDown()) {
      if (selectedComponents.contains(clickedComp))
        selectedComponents.remove(clickedComp);
      else
        selectedComponents.add(clickedComp);
    } else {
      if (!selectedComponents.contains(clickedComp)) {
        selectedComponents.clear();
        selectedComponents.add(clickedComp);
      }
    }
    isDraggingItems = true;
  }

  private void startSelectionBox(MouseEvent e) {
    if (!e.isShiftDown())
      selectedComponents.clear();
    selectionStartPt = e.getPoint();
    selectionRect = new Rectangle(e.getX(), e.getY(), 0, 0);
  }

  private void updateSelectionBox(MouseEvent e) {
    int x = Math.min(selectionStartPt.x, e.getX());
    int y = Math.min(selectionStartPt.y, e.getY());
    int w = Math.abs(e.getX() - selectionStartPt.x);
    int h = Math.abs(e.getY() - selectionStartPt.y);
    selectionRect.setBounds(x, y, w, h);
  }

  private void finalizeSelectionBox() {
    for (Component c : components) {
      if (selectionRect.contains(c.getX() + 20, c.getY() + 20)) {
        if (!selectedComponents.contains(c))
          selectedComponents.add(c);
      }
    }
    selectionRect = null;
  }

  // =========================================================
  // RENDERING
  // =========================================================

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    drawGrid(g2);
    drawWires(g2);
    drawComponents(g2);

    // Draw "Drag Line" for new wire
    if (dragStartPin != null && dragCurrentPoint != null) {
      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[] { 5 }, 0));
      g2.drawLine(dragStartPin.location.x, dragStartPin.location.y, dragCurrentPoint.x, dragCurrentPoint.y);
    }

    if (selectionRect != null) {
      g2.setColor(SELECTION_FILL);
      g2.fill(selectionRect);
      g2.setColor(SELECTION_BORDER);
      g2.draw(selectionRect);
    }
  }

  private void drawWires(Graphics2D g2) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    for (Wire w : wires) {
      Component source = w.getSource();
      Point p1 = getPinLocation(source, false, 0);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        Point p2 = getPinLocation(dest, true, pc.inputIndex);

        boolean isSelected = (selectedWireSegment != null &&
            selectedWireSegment.wire == w &&
            selectedWireSegment.connection == pc);

        if (isSelected) {
          g2.setColor(SELECTION_BORDER);
          g2.setStroke(new BasicStroke(6));
          g2.draw(createWireCurve(p1.x, p1.y, p2.x, p2.y));
          g2.setStroke(new BasicStroke(3));
        }

        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);
        g2.draw(createWireCurve(p1.x, p1.y, p2.x, p2.y));
      }
    }
  }

  private void drawComponents(Graphics2D g2) {
    for (Component c : components) {
      boolean isSelected = selectedComponents.contains(c);
      drawComponentBody(g2, c, isSelected);
      drawPins(g2, c);
    }
  }

  private void drawPins(Graphics2D g2, Component c) {
    // Draw Output Pin
    if (!(c instanceof OutputProbe)) {
      Point out = getPinLocation(c, false, 0);
      drawPinCircle(g2, out);
    }
    // Draw Input Pins
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

  private void drawComponentBody(Graphics2D g2, Component c, boolean sel) {
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
    else
      drawGenericBox(g2, c, x, y, sel);

    g2.setColor(Color.BLACK);
    g2.setFont(new Font("Arial", Font.BOLD, 10));
    g2.drawString(c.getName(), x, y - 5);
  }

  // --- DRAWING IMPLEMENTATIONS ---

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
    boolean on = s.getOutputWire().getSignal();
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

  private void drawNotGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 35, y + 20);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
    g2.setColor(Color.WHITE);
    g2.fillOval(x + 32, y + 15, 10, 10);
    g2.setColor(Color.BLACK);
    g2.drawOval(x + 32, y + 15, 10, 10);
  }

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

  private void drawGrid(Graphics2D g2) {
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    for (int x = 0; x < getWidth(); x += GRID_SIZE)
      g2.drawLine(x, 0, x, getHeight());
    for (int y = 0; y < getHeight(); y += GRID_SIZE)
      g2.drawLine(0, y, getWidth(), y);
  }
}
