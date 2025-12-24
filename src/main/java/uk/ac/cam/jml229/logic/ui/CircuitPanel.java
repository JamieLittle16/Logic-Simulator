package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class CircuitPanel extends JPanel {

  // --- Model Data ---
  private final List<Component> components = new ArrayList<>();
  private final List<Wire> wires = new ArrayList<>();

  // --- Selection & Interaction State ---
  private final List<Component> selectedComponents = new ArrayList<>();

  // Dragging Logic Variables
  private Point lastMousePt; // Tracks mouse position during drags
  private Point selectionStartPt; // Where the selection box started
  private Rectangle selectionRect; // The visible selection box (null if not active)
  private boolean isDraggingItems = false;

  // --- Visual Constants ---
  private static final int GRID_SIZE = 20;
  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private static final Color SELECTION_BORDER = new Color(0, 200, 255); // Cyan highlight
  private static final Color SELECTION_FILL = new Color(0, 200, 255, 50); // Transparent Blue

  private static final Color WIRE_OFF = new Color(80, 80, 80);
  private static final Color WIRE_ON = new Color(255, 60, 60);
  private static final Color PIN_COLOR = Color.DARK_GRAY;
  private static final Color GATE_GRADIENT_TOP = new Color(60, 100, 180);
  private static final Color GATE_GRADIENT_BOTTOM = new Color(100, 140, 220);

  public CircuitPanel() {
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);

    // Initialize Input Handling (Mouse Listener)
    InputHandler inputHandler = new InputHandler();
    addMouseListener(inputHandler);
    addMouseMotionListener(inputHandler);
  }

  public void addComponent(Component c) {
    components.add(c);
    if (c.getOutputWire() != null) {
      wires.add(c.getOutputWire());
    }
    repaint();
  }

  // =========================================================
  // INPUT HANDLER (Interaction Logic)
  // =========================================================

  private class InputHandler extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent e) {
      lastMousePt = e.getPoint();
      // FIX 1: Use the new unique name here
      Component clickedComp = getLogicComponentAt(e.getPoint());

      if (clickedComp != null) {
        // --- CASE 1: Clicked on a Component ---

        if (e.isShiftDown()) {
          // Shift+Click: Add/Remove from selection
          if (selectedComponents.contains(clickedComp)) {
            selectedComponents.remove(clickedComp);
          } else {
            selectedComponents.add(clickedComp);
          }
        } else {
          // Normal Click: Select only this (unless it's already part of the group)
          if (!selectedComponents.contains(clickedComp)) {
            selectedComponents.clear();
            selectedComponents.add(clickedComp);
          }
        }
        isDraggingItems = true;

      } else {
        // --- CASE 2: Clicked on Empty Space ---
        if (!e.isShiftDown()) {
          selectedComponents.clear(); // Deselect all
        }
        // Start drawing Selection Box
        isDraggingItems = false;
        selectionStartPt = e.getPoint();
        selectionRect = new Rectangle(e.getX(), e.getY(), 0, 0);
      }
      repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (isDraggingItems) {
        // Move all selected components
        int dx = e.getX() - lastMousePt.x;
        int dy = e.getY() - lastMousePt.y;

        for (Component c : selectedComponents) {
          c.setPosition(c.getX() + dx, c.getY() + dy);
        }
        lastMousePt = e.getPoint();

      } else if (selectionRect != null) {
        // Resize Selection Box
        int x = Math.min(selectionStartPt.x, e.getX());
        int y = Math.min(selectionStartPt.y, e.getY());
        int width = Math.abs(e.getX() - selectionStartPt.x);
        int height = Math.abs(e.getY() - selectionStartPt.y);

        selectionRect.setBounds(x, y, width, height);
      }
      repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (selectionRect != null) {
        // Finalize Selection Box: Select everything inside
        for (Component c : components) {
          // Check if center of component is inside box
          if (selectionRect.contains(c.getX() + 20, c.getY() + 20)) {
            if (!selectedComponents.contains(c)) {
              selectedComponents.add(c);
            }
          }
        }
        selectionRect = null; // Hide box
      }
      isDraggingItems = false;
      repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // Handle Switch Toggling
      // FIX 2: Use the new unique name here
      Component c = getLogicComponentAt(e.getPoint());
      if (c instanceof Switch) {
        if (!e.isShiftDown() && selectedComponents.size() <= 1) {
          ((Switch) c).toggle(!((Switch) c).getState());
          repaint();
        }
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      // Change cursor to Hand if hovering over a component
      // FIX 3: Use the new unique name here
      Component c = getLogicComponentAt(e.getPoint());
      setCursor(c != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          : Cursor.getDefaultCursor());
    }
  }

  // --- Helper to find component at x,y ---
  // RENAMED METHOD to avoid collision with Swing's getComponentAt
  private Component getLogicComponentAt(Point p) {
    // Iterate backwards so we click top-most items first
    for (int i = components.size() - 1; i >= 0; i--) {
      Component c = components.get(i);
      if (p.x >= c.getX() && p.x <= c.getX() + 40 &&
          p.y >= c.getY() && p.y <= c.getY() + 40) {
        return c;
      }
    }
    return null;
  }

  // =========================================================
  // RENDERING
  // =========================================================

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    // High Quality Rendering Settings
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    drawGrid(g2);
    drawWires(g2);
    drawComponents(g2);

    // Draw Selection Box (if active)
    if (selectionRect != null) {
      g2.setColor(SELECTION_FILL);
      g2.fill(selectionRect);
      g2.setColor(SELECTION_BORDER);
      // Dashed line pattern
      g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0));
      g2.draw(selectionRect);
    }
  }

  private void drawGrid(Graphics2D g2) {
    g2.setColor(GRID_COLOR);
    g2.setStroke(new BasicStroke(1));
    for (int x = 0; x < getWidth(); x += GRID_SIZE)
      g2.drawLine(x, 0, x, getHeight());
    for (int y = 0; y < getHeight(); y += GRID_SIZE)
      g2.drawLine(0, y, getWidth(), y);
  }

  private void drawWires(Graphics2D g2) {
    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (Wire w : wires) {
      Component source = w.getSource();
      int x1 = source.getX() + 50;
      int y1 = source.getY() + 20;

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dest = pc.component;
        int x2 = dest.getX();
        int y2 = dest.getY() + 10 + (pc.inputIndex * 20);

        g2.setColor(w.getSignal() ? WIRE_ON : WIRE_OFF);

        // Cubic Bezier Curve for smooth wires
        CubicCurve2D.Double curve = new CubicCurve2D.Double();
        double ctrlDist = Math.abs(x2 - x1) * 0.5;
        curve.setCurve(x1, y1, x1 + ctrlDist, y1, x2 - ctrlDist, y2, x2, y2);
        g2.draw(curve);
      }
    }
  }

  private void drawComponents(Graphics2D g2) {
    for (Component c : components) {
      int x = c.getX();
      int y = c.getY();

      // Draw Pins
      g2.setColor(PIN_COLOR);
      g2.setStroke(new BasicStroke(2));
      g2.drawLine(x + 40, y + 20, x + 50, y + 20);

      // Draw Shape based on type
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
        drawGenericBox(g2, c, x, y);

      // Draw Selection Border if selected
      if (selectedComponents.contains(c)) {
        g2.setColor(SELECTION_BORDER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x - 2, y - 2, 44, 44, 5, 5);
      }

      // Draw Label
      g2.setColor(Color.BLACK);
      g2.setFont(new Font("Arial", Font.BOLD, 10));
      g2.drawString(c.getName(), x, y - 5);
    }
  }

  // --- Component Drawers ---

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y) {
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRoundRect(x, y, 40, 40, 5, 5);
    g2.setColor(Color.GRAY);
    g2.drawRoundRect(x, y, 40, 40, 5, 5);
    boolean on = s.getOutputWire().getSignal();
    Color toggleColor = on ? new Color(50, 200, 50) : new Color(200, 50, 50);
    GradientPaint gp = new GradientPaint(x, y, toggleColor.brighter(), x, y + 40, toggleColor.darker());
    g2.setPaint(gp);
    int toggleY = on ? y + 5 : y + 20;
    g2.fillRoundRect(x + 10, toggleY, 20, 15, 2, 2);
    g2.setColor(Color.BLACK);
    g2.drawRoundRect(x + 10, toggleY, 20, 15, 2, 2);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y) {
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

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y) {
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(x, y, 50, 40);
    g2.setColor(Color.BLACK);
    g2.drawRect(x, y, 50, 40);
  }

  private void drawAndGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.lineTo(x + 20, y);
    path.curveTo(x + 50, y, x + 50, y + 40, x + 20, y + 40);
    path.lineTo(x, y + 40);
    path.closePath();
    fillAndOutlineGate(g2, path);
    drawInputPins(g2, x, y, 2);
  }

  private void drawOrGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.quadTo(x + 15, y + 20, x, y + 40);
    path.quadTo(x + 35, y + 40, x + 50, y + 20);
    path.quadTo(x + 35, y, x, y);
    path.closePath();
    fillAndOutlineGate(g2, path);
    drawInputPins(g2, x + 5, y, 2);
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y) {
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    Path2D backLine = new Path2D.Double();
    backLine.moveTo(x - 5, y);
    backLine.quadTo(x + 10, y + 20, x - 5, y + 40);
    g2.draw(backLine);
    drawOrGate(g2, c, x + 5, y);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y) {
    Path2D path = new Path2D.Double();
    path.moveTo(x, y);
    path.lineTo(x + 35, y + 20);
    path.lineTo(x, y + 40);
    path.closePath();
    fillAndOutlineGate(g2, path);
    g2.setColor(Color.WHITE);
    g2.fillOval(x + 32, y + 15, 10, 10);
    g2.setColor(Color.BLACK);
    g2.drawOval(x + 32, y + 15, 10, 10);
    drawInputPins(g2, x, y, 1);
  }

  private void fillAndOutlineGate(Graphics2D g2, Path2D path) {
    GradientPaint gp = new GradientPaint(0, 0, GATE_GRADIENT_TOP, 0, 40, GATE_GRADIENT_BOTTOM);
    g2.setPaint(gp);
    g2.fill(path);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.draw(path);
  }

  private void drawInputPins(Graphics2D g2, int x, int y, int count) {
    g2.setColor(PIN_COLOR);
    g2.setStroke(new BasicStroke(2));
    if (count == 1)
      g2.drawLine(x - 10, y + 20, x, y + 20);
    else if (count == 2) {
      g2.drawLine(x - 10, y + 10, x, y + 10);
      g2.drawLine(x - 10, y + 30, x, y + 30);
    }
  }
}
