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
import uk.ac.cam.jml229.logic.model.Wire;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.WireSegment;
import uk.ac.cam.jml229.logic.ui.CircuitRenderer.WaypointRef;

public class CircuitInteraction extends MouseAdapter implements KeyListener {

  private final Circuit circuit;
  private final CircuitPanel panel;
  private final CircuitRenderer renderer;
  private ComponentPalette palette;

  // --- Interaction State ---
  private final List<Component> selectedComponents = new ArrayList<>();
  private WireSegment selectedWireSegment = null;
  private Pin hoveredPin = null;
  private WireSegment hoveredWire = null;
  private Pin connectionStartPin = null;
  private Point currentMousePoint = null;
  private Rectangle selectionRect;
  private Point selectionStartPt;
  private Point lastMousePt;
  private boolean isDraggingItems = false;
  private boolean isMouseInsidePanel = false;
  private Component componentToPlace = null;

  // --- Waypoint State ---
  private WaypointRef selectedWaypoint = null;
  private WaypointRef hoveredWaypoint = null;

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

  public WaypointRef getSelectedWaypoint() {
    return selectedWaypoint;
  }

  public WaypointRef getHoveredWaypoint() {
    return hoveredWaypoint;
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
    // 1. Delete Waypoint (Priority)
    if (selectedWaypoint != null) {
      selectedWaypoint.connection().waypoints.remove(selectedWaypoint.point());
      selectedWaypoint = null;
    }
    // 2. Delete Wire
    else if (selectedWireSegment != null) {
      circuit.removeConnection(
          selectedWireSegment.connection.component,
          selectedWireSegment.connection.inputIndex);
      selectedWireSegment = null;
    }
    // 3. Delete Components
    else if (!selectedComponents.isEmpty()) {
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
    hoveredPin = getPinAt(e.getPoint());
    hoveredWaypoint = getWaypointAt(e.getPoint());
    hoveredWire = (hoveredPin == null && hoveredWaypoint == null) ? getWireAt(e.getPoint()) : null;

    if (hoveredPin != null || hoveredWire != null || hoveredWaypoint != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } else {
      Component c = getLogicComponentAt(e.getPoint());
      panel.setCursor(c != null ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
          : Cursor.getDefaultCursor());
    }

    panel.repaint();
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

    // Left Click - Component Placement
    if (componentToPlace != null) {
      circuit.addComponent(componentToPlace);
      if (e.isControlDown()) {
        componentToPlace = componentToPlace.makeCopy();
      } else {
        componentToPlace = null;
      }
      panel.repaint();
      return;
    }

    Pin clickedPin = getPinAt(e.getPoint());

    // Wiring Phase 2: Finishing a wire connection
    if (connectionStartPin != null) {
      if (clickedPin != null) {
        if (connectionStartPin.isInput() != clickedPin.isInput()) {
          Pin sourcePin = connectionStartPin.isInput() ? clickedPin : connectionStartPin;
          Pin destPin = connectionStartPin.isInput() ? connectionStartPin : clickedPin;
          circuit.addConnection(sourcePin.component(), sourcePin.index(), destPin.component(), destPin.index());
        }
        connectionStartPin = null;
      } else {
        connectionStartPin = null;
      }
      panel.repaint();
      return;
    }

    // Wiring Phase 1: Starting a wire connection
    if (clickedPin != null) {
      connectionStartPin = clickedPin;
      selectedComponents.clear();
      selectedWireSegment = null;
      selectedWaypoint = null;
      panel.repaint();
      return;
    }

    // --- HIT TESTING ---

    // 1. Waypoint Click
    WaypointRef clickedWP = getWaypointAt(e.getPoint());
    if (clickedWP != null) {
      selectedWaypoint = clickedWP;
      selectedWireSegment = new WireSegment(getWireForConnection(clickedWP.connection()), clickedWP.connection());
      selectedComponents.clear();
      panel.repaint();
      return;
    }

    // 2. Wire Click (Select or Split)
    WireSegment clickedWire = getWireAt(e.getPoint());
    if (clickedWire != null) {
      // If clicking the ALREADY selected wire -> Add new waypoint!
      if (selectedWireSegment != null &&
          clickedWire.wire == selectedWireSegment.wire &&
          clickedWire.connection == selectedWireSegment.connection) {

        Point newPt = e.getPoint();
        insertWaypoint(clickedWire, newPt);
        // Auto-select the new point
        selectedWaypoint = new WaypointRef(clickedWire.connection, newPt);
      } else {
        // Just selecting a new wire
        selectedWireSegment = clickedWire;
        selectedWaypoint = null;
        selectedComponents.clear();
      }
      panel.repaint();
      return;
    } else {
      selectedWireSegment = null;
      selectedWaypoint = null;
    }

    // 3. Component Click
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

    // Move Waypoint
    if (selectedWaypoint != null) {
      Point pt = selectedWaypoint.point();
      pt.setLocation(e.getPoint());

      // Snapping
      List<Point> points = selectedWaypoint.connection().waypoints;
      int index = points.indexOf(pt);

      Point prev = null;
      Point next = null;

      Wire w = getWireForConnection(selectedWaypoint.connection());

      // Find Prev
      if (index > 0) {
        prev = points.get(index - 1);
      } else if (w != null) {
        Component src = w.getSource();
        int srcIdx = 0;
        for (int i = 0; i < src.getOutputCount(); i++)
          if (src.getOutputWire(i) == w)
            srcIdx = i;
        prev = renderer.getPinLocation(src, false, srcIdx);
      }

      // Find Next
      if (index < points.size() - 1) {
        next = points.get(index + 1);
      } else {
        next = renderer.getPinLocation(selectedWaypoint.connection().component, true,
            selectedWaypoint.connection().inputIndex);
      }

      int snapDist = 15;
      if (prev != null) {
        if (Math.abs(pt.x - prev.x) < snapDist)
          pt.x = prev.x;
        if (Math.abs(pt.y - prev.y) < snapDist)
          pt.y = prev.y;
      }
      if (next != null) {
        if (Math.abs(pt.x - next.x) < snapDist)
          pt.x = next.x;
        if (Math.abs(pt.y - next.y) < snapDist)
          pt.y = next.y;
      }

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
  // HELPER METHODS
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
    if (palette != null)
      palette.addCustomTool(newTool);
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

  // --- REVISED HELPER: Get Waypoint At ---
  private WaypointRef getWaypointAt(Point p) {
    int hitSize = 8;
    // We only check waypoints if they are visible (i.e. if wire is selected OR just
    // generally check all)
    // Standard UI behavior: check all.
    for (Wire w : circuit.getWires()) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        for (Point pt : pc.waypoints) {
          if (p.distance(pt) <= hitSize) {
            return new WaypointRef(pc, pt);
          }
        }
      }
    }
    return null;
  }

  // Helper to find parent wire for a connection (needed for drag source logic)
  private Wire getWireForConnection(Wire.PortConnection pc) {
    for (Wire w : circuit.getWires()) {
      if (w.getDestinations().contains(pc))
        return w;
    }
    return null;
  }

  private void insertWaypoint(WireSegment ws, Point clickPt) {
    Wire.PortConnection pc = ws.connection;
    Wire w = ws.wire;
    List<Point> waypoints = pc.waypoints;

    Component src = w.getSource();
    int srcIdx = 0;
    for (int i = 0; i < src.getOutputCount(); i++)
      if (src.getOutputWire(i) == w)
        srcIdx = i;
    Point start = renderer.getPinLocation(src, false, srcIdx);
    Point end = renderer.getPinLocation(pc.component, true, pc.inputIndex);

    List<Point> fullPath = new ArrayList<>();
    fullPath.add(start);
    fullPath.addAll(waypoints);
    fullPath.add(end);

    int insertIndex = waypoints.size();
    for (int i = 0; i < fullPath.size() - 1; i++) {
      Point p1 = fullPath.get(i);
      Point p2 = fullPath.get(i + 1);

      GeneralPath segmentPath = new GeneralPath();
      segmentPath.moveTo(p1.x, p1.y);
      double dist = Math.abs(p2.x - p1.x) * 0.5;
      segmentPath.curveTo(p1.x + dist, p1.y, p2.x - dist, p2.y, p2.x, p2.y);

      Shape stroked = new BasicStroke(7).createStrokedShape(segmentPath);
      if (stroked.contains(clickPt)) {
        insertIndex = i;
        break;
      }
    }
    if (insertIndex >= 0 && insertIndex <= waypoints.size())
      waypoints.add(insertIndex, clickPt);
    else
      waypoints.add(clickPt);
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
        Shape path = renderer.createWireShape(p1, p2, pc.waypoints);
        Shape strokedShape = new BasicStroke(hitThreshold).createStrokedShape(path);
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
      int inputCount = renderer.getInputCount(c);
      int outputCount = c.getOutputCount();
      int maxPins = Math.max(inputCount, outputCount);
      int h = Math.max(40, maxPins * 20);
      int w = 50;
      if (p.x >= c.getX() && p.x <= c.getX() + w && p.y >= c.getY() && p.y <= c.getY() + h)
        return c;
    }
    return null;
  }
}
