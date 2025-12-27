package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;
import uk.ac.cam.jml229.logic.model.Wire; // Imported properly now!
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.WireSegment;

public class CircuitInteraction extends MouseAdapter implements KeyListener {

  private final Circuit circuit;
  private final CircuitPanel panel;
  private final CircuitRenderer renderer;
  private ComponentPalette palette;

  // --- Interaction State ---
  private final List<Component> selectedComponents = new ArrayList<>();
  private WireSegment selectedWireSegment = null;

  // Hover State
  private Pin hoveredPin = null;
  private WireSegment hoveredWire = null;

  // Wiring State
  private Pin connectionStartPin = null;
  private Point currentMousePoint = null;

  // Selection Box
  private Rectangle selectionRect;
  private Point selectionStartPt;
  private Point lastMousePt;
  private boolean isDraggingItems = false;
  private boolean isMouseInsidePanel = false;

  // Ghost component
  private Component componentToPlace = null;

  public CircuitInteraction(Circuit circuit, CircuitPanel panel, CircuitRenderer renderer) {
    this.circuit = circuit;
    this.panel = panel;
    this.renderer = renderer;
  }

  public void setPalette(ComponentPalette palette) {
    this.palette = palette;
  }

  // --- Accessors ---
  public List<Component> getSelectedComponents() {
    return selectedComponents;
  }

  public WireSegment getSelectedWire() {
    return selectedWireSegment;
  }

  public Pin getHoveredPin() {
    return hoveredPin;
  }

  public WireSegment getHoveredWire() {
    return hoveredWire;
  }

  public Pin getConnectionStartPin() {
    return connectionStartPin;
  }

  public Point getCurrentMousePoint() {
    return currentMousePoint;
  }

  public Rectangle getSelectionRect() {
    return selectionRect;
  }

  public Component getComponentToPlace() {
    return isMouseInsidePanel ? componentToPlace : null;
  }

  // --- Actions ---

  public void startPlacing(Component c) {
    this.componentToPlace = c;
    this.connectionStartPin = null;
    panel.repaint();
  }

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

  // --- Mouse Handling ---

  @Override
  public void mouseEntered(MouseEvent e) {
    isMouseInsidePanel = true;
    panel.requestFocusInWindow();
    panel.repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    isMouseInsidePanel = false;
    panel.repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    currentMousePoint = e.getPoint();

    // 1. Update Ghost Component
    if (componentToPlace != null) {
      int gridX = Math.round(e.getX() / 20.0f) * 20;
      int gridY = Math.round(e.getY() / 20.0f) * 20;
      componentToPlace.setPosition(gridX, gridY);
      panel.repaint();
      return;
    }

    // 2. Update Hover State
    Pin prevPin = hoveredPin;
    WireSegment prevWire = hoveredWire;

    hoveredPin = getPinAt(e.getPoint());
    hoveredWire = (hoveredPin == null) ? getWireAt(e.getPoint()) : null;

    if (hoveredPin != null || hoveredWire != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } else {
      Component c = getLogicComponentAt(e.getPoint());
      panel.setCursor(c != null ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
          : Cursor.getDefaultCursor());
    }

    if (connectionStartPin != null || !Objects.equals(prevPin, hoveredPin) || !Objects.equals(prevWire, hoveredWire)) {
      panel.repaint();
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    panel.requestFocusInWindow();
    lastMousePt = e.getPoint();
    currentMousePoint = e.getPoint();

    // Right Click
    if (SwingUtilities.isRightMouseButton(e)) {
      if (componentToPlace != null || connectionStartPin != null) {
        componentToPlace = null;
        connectionStartPin = null;
      } else if (!selectedComponents.isEmpty()) {
        showContextMenu(e.getX(), e.getY());
      }
      panel.repaint();
      return;
    }

    // Left Click
    if (componentToPlace != null) {
      circuit.addComponent(componentToPlace);

      // Continous adding with Ctrl
      if (e.isControlDown()) {
        componentToPlace = componentToPlace.makeCopy();
      } else {
        componentToPlace = null;
      }

      panel.repaint();
      return;
    }

    Pin clickedPin = getPinAt(e.getPoint());

    // Wiring Logic
    if (connectionStartPin != null) {
      if (clickedPin != null && clickedPin.isInput()) {
        circuit.addConnection(
            connectionStartPin.component(),
            connectionStartPin.index(),
            clickedPin.component(),
            clickedPin.index());
        connectionStartPin = null;
      } else {
        connectionStartPin = null;
      }
      panel.repaint();
      return;
    }

    if (clickedPin != null) {
      if (!clickedPin.isInput()) {
        connectionStartPin = clickedPin;
        selectedComponents.clear();
        selectedWireSegment = null;
      }
      panel.repaint();
      return;
    }

    // Selection Logic
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
  public void mouseDragged(MouseEvent e) {
    if (connectionStartPin != null) {
      currentMousePoint = e.getPoint();
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
    if (selectionRect != null) {
      finalizeSelectionBox();
    }
    isDraggingItems = false;
    panel.repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (connectionStartPin == null && !isDraggingItems && componentToPlace == null) {
      Component c = getLogicComponentAt(e.getPoint());
      if (c instanceof Switch) {
        ((Switch) c).toggle(!((Switch) c).getState());
        panel.repaint();
      }
    }
  }

  // --- Key Listener ---
  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      deleteSelection();
    }
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      connectionStartPin = null;
      componentToPlace = null;
      panel.repaint();
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  // ==========================================
  // CUSTOM COMPONENT LOGIC
  // ==========================================

  private void showContextMenu(int x, int y) {
    JPopupMenu menu = new JPopupMenu();

    JMenuItem createItem = new JMenuItem("Create Custom Component");
    createItem.addActionListener(e -> createCustomComponentFromSelection());
    menu.add(createItem);

    JMenuItem deleteItem = new JMenuItem("Delete Selection");
    deleteItem.addActionListener(e -> deleteSelection());
    menu.add(deleteItem);

    menu.show(panel, x, y);
  }

  private void createCustomComponentFromSelection() {
    if (selectedComponents.isEmpty())
      return;

    String name = JOptionPane.showInputDialog(panel, "Enter Name (max 5 chars):", "New Component",
        JOptionPane.PLAIN_MESSAGE);
    if (name == null || name.trim().isEmpty())
      return;
    if (name.length() > 5)
      name = name.substring(0, 5);

    Circuit innerCircuit = new Circuit();
    Map<Component, Component> oldToNew = new HashMap<>();

    for (Component original : selectedComponents) {
      Component clone = original.makeCopy();
      clone.setPosition(original.getX(), original.getY());
      innerCircuit.addComponent(clone);
      oldToNew.put(original, clone);
    }

    // Rebuild Internal Wires using clean imports
    for (Wire w : circuit.getWires()) {
      Component source = w.getSource();
      if (source == null || !selectedComponents.contains(source))
        continue;

      int sourceIndex = -1;
      for (int i = 0; i < source.getOutputCount(); i++) {
        if (source.getOutputWire(i) == w) {
          sourceIndex = i;
          break;
        }
      }
      if (sourceIndex == -1)
        continue;

      for (Wire.PortConnection pc : w.getDestinations()) {
        if (selectedComponents.contains(pc.component)) {
          Component newSource = oldToNew.get(source);
          Component newDest = oldToNew.get(pc.component);
          innerCircuit.addConnection(newSource, sourceIndex, newDest, pc.inputIndex);
        }
      }
    }

    CustomComponent newTool = new CustomComponent(name, innerCircuit);

    if (palette != null) {
      palette.addCustomTool(newTool);
    }
  }

  // ==========================================
  // HELPER METHODS
  // ==========================================

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

  private Pin getPinAt(Point p) {
    int threshold = CircuitRenderer.PIN_SIZE + 4;
    for (Component c : circuit.getComponents()) {
      int outCount = c.getOutputCount();
      for (int i = 0; i < outCount; i++) {
        Point outLoc = renderer.getPinLocation(c, false, i);
        if (p.distance(outLoc) <= threshold)
          return new Pin(c, i, false, outLoc);
      }

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

      int outputIndex = 0;
      for (int i = 0; i < src.getOutputCount(); i++) {
        if (src.getOutputWire(i) == w) {
          outputIndex = i;
          break;
        }
      }
      Point p1 = renderer.getPinLocation(src, false, outputIndex);

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
