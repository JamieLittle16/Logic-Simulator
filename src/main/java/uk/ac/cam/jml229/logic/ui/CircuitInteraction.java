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

  // --- Waypoint Editing State ---
  private Point draggedWaypoint = null;
  private List<Point> draggedWaypointList = null;

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
    draggedWaypoint = null;

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
      // Stamp Mode: Ctrl + Click keeps placing
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
        // We need exactly one Input and one Output.
        if (connectionStartPin.isInput() != clickedPin.isInput()) {
          Pin sourcePin = connectionStartPin.isInput() ? clickedPin : connectionStartPin;
          Pin destPin = connectionStartPin.isInput() ? connectionStartPin : clickedPin;
          circuit.addConnection(sourcePin.component(), sourcePin.index(), destPin.component(), destPin.index());
        }
        connectionStartPin = null;
      } else {
        // Clicked empty space -> Cancel wiring
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
      panel.repaint();
      return;
    }

    // --- WAYPOINT / WIRE LOGIC ---

    // Check if clicking an EXISTING waypoint on the SELECTED wire
    if (selectedWireSegment != null) {
      Wire.PortConnection pc = selectedWireSegment.connection;
      for (Point pt : pc.waypoints) {
        if (e.getPoint().distance(pt) < 6) { // 6px hit box
          draggedWaypoint = pt;
          draggedWaypointList = pc.waypoints;
          panel.repaint();
          return;
        }
      }
    }

    // Check if clicking on a Wire Segment (to select it OR split it)
    WireSegment clickedWire = getWireAt(e.getPoint());
    if (clickedWire != null) {
      // If clicking the ALREADY selected wire -> Add new waypoint!
      if (selectedWireSegment != null &&
          clickedWire.wire == selectedWireSegment.wire &&
          clickedWire.connection == selectedWireSegment.connection) {

        Point newPt = e.getPoint();
        insertWaypoint(clickedWire, newPt);
        draggedWaypoint = newPt; // Immediately start dragging it
        draggedWaypointList = clickedWire.connection.waypoints;
      } else {
        // Just selecting a new wire
        selectedWireSegment = clickedWire;
        selectedComponents.clear();
      }
      panel.repaint();
      return;
    } else {
      selectedWireSegment = null;
    }

    // --- COMPONENT SELECTION LOGIC ---
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
    if (draggedWaypoint != null && selectedWireSegment != null) {
      draggedWaypoint.setLocation(e.getPoint());

      List<Point> points = draggedWaypointList;
      int index = points.indexOf(draggedWaypoint);

      Point prev = null;
      Point next = null;

      // Find Prev Point (Waypoint or Source Pin)
      if (index > 0) {
        prev = points.get(index - 1);
      } else {
        // Index 0 -> Prev is Source Pin
        Component src = selectedWireSegment.wire.getSource();
        // Need to find which output index this wire is attached to
        int srcIdx = 0;
        for (int i = 0; i < src.getOutputCount(); i++)
          if (src.getOutputWire(i) == selectedWireSegment.wire)
            srcIdx = i;
        prev = renderer.getPinLocation(src, false, srcIdx);
      }

      // Find Next Point (Waypoint or Dest Pin)
      if (index < points.size() - 1) {
        next = points.get(index + 1);
      } else {
        // Last Index -> Next is Dest Pin
        Wire.PortConnection pc = selectedWireSegment.connection;
        next = renderer.getPinLocation(pc.component, true, pc.inputIndex);
      }

      int snapDist = 15;

      // Apply Snap (Straightening)
      if (prev != null) {
        if (Math.abs(draggedWaypoint.x - prev.x) < snapDist)
          draggedWaypoint.x = prev.x;
        if (Math.abs(draggedWaypoint.y - prev.y) < snapDist)
          draggedWaypoint.y = prev.y;
      }
      if (next != null) {
        if (Math.abs(draggedWaypoint.x - next.x) < snapDist)
          draggedWaypoint.x = next.x;
        if (Math.abs(draggedWaypoint.y - next.y) < snapDist)
          draggedWaypoint.y = next.y;
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
    draggedWaypoint = null;
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

  private void insertWaypoint(WireSegment ws, Point clickPt) {
    Wire.PortConnection pc = ws.connection;
    Wire w = ws.wire;
    List<Point> waypoints = pc.waypoints;

    // Get Start Point (Source Pin)
    Component src = w.getSource();
    int srcIdx = 0;
    for (int i = 0; i < src.getOutputCount(); i++)
      if (src.getOutputWire(i) == w)
        srcIdx = i;
    Point start = renderer.getPinLocation(src, false, srcIdx);

    // Get End Point (Destination Pin)
    Point end = renderer.getPinLocation(pc.component, true, pc.inputIndex);

    // Build Full Path List (Start -> W1 -> W2 -> ... -> End)
    List<Point> fullPath = new ArrayList<>();
    fullPath.add(start);
    fullPath.addAll(waypoints);
    fullPath.add(end);

    // Find which segment was clicked
    // We iterate through segments 0..(N-1). If hit segment i, we insert at index i.
    int insertIndex = waypoints.size(); // Default to append if logic fails

    for (int i = 0; i < fullPath.size() - 1; i++) {
      Point p1 = fullPath.get(i);
      Point p2 = fullPath.get(i + 1);

      // Recreate the exact curve used by renderer for this segment
      GeneralPath segmentPath = new GeneralPath();
      segmentPath.moveTo(p1.x, p1.y);
      double dist = Math.abs(p2.x - p1.x) * 0.5;
      segmentPath.curveTo(p1.x + dist, p1.y, p2.x - dist, p2.y, p2.x, p2.y);

      // Hit test with slightly wider stroke for leniency
      Shape stroked = new BasicStroke(7).createStrokedShape(segmentPath);
      if (stroked.contains(clickPt)) {
        insertIndex = i;
        break;
      }
    }

    // Insert
    if (insertIndex >= 0 && insertIndex <= waypoints.size()) {
      waypoints.add(insertIndex, clickPt);
    } else {
      waypoints.add(clickPt);
    }
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

      // Calculate dynamic height from renderer to match drawn box size
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
