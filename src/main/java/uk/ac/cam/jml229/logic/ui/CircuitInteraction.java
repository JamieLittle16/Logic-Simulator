package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.WireSegment;

public class CircuitInteraction extends MouseAdapter implements KeyListener {

  private final Circuit circuit;
  private final CircuitPanel panel;
  private final CircuitRenderer renderer;

  // --- Interaction State ---
  private final List<Component> selectedComponents = new ArrayList<>();
  private WireSegment selectedWireSegment = null;

  // Dragging
  private Point lastMousePt;
  private boolean isDraggingItems = false;
  private boolean isMouseInsidePanel = false;

  // Selection Box
  private Rectangle selectionRect;
  private Point selectionStartPt;

  // Wiring
  private Pin dragStartPin = null;
  private Point dragCurrentPoint = null;

  // Ghost component following the mouse
  private Component componentToPlace = null;

  public CircuitInteraction(Circuit circuit, CircuitPanel panel, CircuitRenderer renderer) {
    this.circuit = circuit;
    this.panel = panel;
    this.renderer = renderer;
  }

  // --- Accessors ---
  public List<Component> getSelectedComponents() {
    return selectedComponents;
  }

  public WireSegment getSelectedWire() {
    return selectedWireSegment;
  }

  public Pin getDragStartPin() {
    return dragStartPin;
  }

  public Point getDragCurrentPoint() {
    return dragCurrentPoint;
  }

  public Rectangle getSelectionRect() {
    return selectionRect;
  }

  // NEW: Only return the ghost component if the mouse is actually inside the
  // panel
  public Component getComponentToPlace() {
    return isMouseInsidePanel ? componentToPlace : null;
  }

  public void startPlacing(Component c) {
    this.componentToPlace = c;
    panel.repaint();
  }

  // --- Logic: Deletion ---
  public void deleteSelection() {
    if (selectedWireSegment != null) {
      circuit.removeConnection(
          selectedWireSegment.connection.component,
          selectedWireSegment.connection.inputIndex);
      selectedWireSegment = null;
    }

    if (!selectedComponents.isEmpty()) {
      for (Component c : new ArrayList<>(selectedComponents)) {
        circuit.removeComponent(c);
      }
      selectedComponents.clear();
    }
    panel.repaint();
  }

  // --- Logic: Mouse Handling ---

  @Override
  public void mouseEntered(MouseEvent e) {
    isMouseInsidePanel = true;
    panel.repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    isMouseInsidePanel = false;
    panel.repaint();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    panel.requestFocusInWindow();
    lastMousePt = e.getPoint();

    if (SwingUtilities.isRightMouseButton(e)) {
      componentToPlace = null;
      panel.repaint();
      return;
    }

    if (componentToPlace != null) {
      circuit.addComponent(componentToPlace);
      componentToPlace = null;
      panel.repaint();
      return;
    }

    Pin clickedPin = getPinAt(e.getPoint());
    if (clickedPin != null && !clickedPin.isInput()) {
      dragStartPin = clickedPin;
      dragCurrentPoint = e.getPoint();
      selectedWireSegment = null;
      panel.repaint();
      return;
    }

    WireSegment clickedWire = getWireAt(e.getPoint());
    if (clickedWire != null) {
      selectedWireSegment = clickedWire;
      selectedComponents.clear();
      panel.repaint();
      return;
    } else {
      selectedWireSegment = null;
    }

    Component clickedComp = getLogicComponentAt(e.getPoint());
    if (clickedComp != null) {
      handleComponentSelection(e, clickedComp);
    } else {
      startSelectionBox(e);
    }
    panel.repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (componentToPlace != null) {
      int gridX = Math.round(e.getX() / 20.0f) * 20;
      int gridY = Math.round(e.getY() / 20.0f) * 20;
      componentToPlace.setPosition(gridX, gridY);
      panel.repaint();
      return;
    }

    Component c = getLogicComponentAt(e.getPoint());
    panel.setCursor(c != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        : Cursor.getDefaultCursor());
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (dragStartPin != null) {
      dragCurrentPoint = e.getPoint();
      panel.repaint();
      return;
    }

    if (isDraggingItems) {
      int dx = e.getX() - lastMousePt.x;
      int dy = e.getY() - lastMousePt.y;
      for (Component c : selectedComponents) {
        c.setPosition(c.getX() + dx, c.getY() + dy);
      }
      lastMousePt = e.getPoint();
      panel.repaint();
      return;
    }

    if (selectionRect != null) {
      updateSelectionBox(e);
      panel.repaint();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (dragStartPin != null) {
      Pin endPin = getPinAt(e.getPoint());
      if (endPin != null && endPin.isInput()) {
        circuit.addConnection(dragStartPin.component(), endPin.component(), endPin.index());
      }
      dragStartPin = null;
      dragCurrentPoint = null;
      panel.repaint();
      return;
    }

    if (selectionRect != null) {
      finalizeSelectionBox();
    }
    isDraggingItems = false;
    panel.repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (dragStartPin == null && !isDraggingItems && componentToPlace == null) {
      Component c = getLogicComponentAt(e.getPoint());
      if (c instanceof Switch) {
        ((Switch) c).toggle(!((Switch) c).getState());
        panel.repaint();
      }
    }
  }

  // --- Logic: Keyboard Handling ---
  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      deleteSelection();
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  // --- Helpers ---

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
    for (Component c : circuit.getComponents()) {
      if (selectionRect.contains(c.getX() + 20, c.getY() + 20)) {
        if (!selectedComponents.contains(c))
          selectedComponents.add(c);
      }
    }
    selectionRect = null;
  }

  // --- Hit Testing ---

  private Pin getPinAt(Point p) {
    int threshold = CircuitRenderer.PIN_SIZE + 4;
    for (Component c : circuit.getComponents()) {
      Point outLoc = renderer.getPinLocation(c, false, 0);
      if (p.distance(outLoc) <= threshold)
        return new Pin(c, 0, false, outLoc);

      int inputCount = renderer.getInputCount(c);
      for (int i = 0; i < inputCount; i++) {
        Point inLoc = renderer.getPinLocation(c, true, i);
        if (p.distance(inLoc) <= threshold)
          return new Pin(c, i, true, inLoc);
      }
    }
    return null;
  }

  private WireSegment getWireAt(Point p) {
    int hitThreshold = 5;
    for (Wire w : circuit.getWires()) {
      Component src = w.getSource();
      if (src == null)
        continue;
      Point p1 = renderer.getPinLocation(src, false, 0);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Point p2 = renderer.getPinLocation(pc.component, true, pc.inputIndex);
        CubicCurve2D.Double curve = renderer.createWireCurve(p1.x, p1.y, p2.x, p2.y);

        Shape strokedShape = new BasicStroke(hitThreshold).createStrokedShape(curve);
        if (strokedShape.contains(p))
          return new WireSegment(w, pc);
      }
    }
    return null;
  }

  private Component getLogicComponentAt(Point p) {
    List<Component> comps = circuit.getComponents();
    for (int i = comps.size() - 1; i >= 0; i--) {
      Component c = comps.get(i);
      if (p.x >= c.getX() && p.x <= c.getX() + 40 &&
          p.y >= c.getY() && p.y <= c.getY() + 40)
        return c;
    }
    return null;
  }
}
