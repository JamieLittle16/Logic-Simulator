package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

  private Circuit circuit;
  private final CircuitPanel panel;
  private final CircuitRenderer renderer;
  private final CircuitHitTester hitTester;
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

  // Selection Box State
  private Rectangle selectionRect;
  private Point selectionStartPt;

  // --- SMOOTH DRAGGING STATE (NEW) ---
  // We store the "Anchor" points when a drag begins.
  // This prevents rounding errors and "jitter" from accumulating.

  // For Panning
  private boolean isPanning = false;
  private Point panStartScreenPt; // Where mouse clicked on screen
  private Point panStartOffset; // Where the pan was (panX, panY) at start

  // For Component Moving
  private boolean isDraggingItems = false;
  private Point dragStartWorldPt; // Where mouse clicked in world
  // Maps component -> Its Top-Left (X,Y) at the start of the drag
  private final Map<Component, Point> initialComponentPositions = new HashMap<>();

  // For Waypoint Moving
  private WaypointRef selectedWaypoint = null;
  private WaypointRef hoveredWaypoint = null;

  // Ghost component (Placement)
  private Component componentToPlace = null;

  public CircuitInteraction(Circuit circuit, CircuitPanel panel, CircuitRenderer renderer) {
    this.circuit = circuit;
    this.panel = panel;
    this.renderer = renderer;
    this.hitTester = new CircuitHitTester(circuit, renderer);
  }

  public void setPalette(ComponentPalette palette) {
    this.palette = palette;
  }

  public void setCircuit(Circuit c) {
    this.circuit = c;
    this.hitTester.setCircuit(c); // Pass it down to the helper

    // Clear selection to avoid trying to move items that no longer exist
    selectedComponents.clear();
    selectedWireSegment = null;
    selectedWaypoint = null;
    hoveredPin = null;
    connectionStartPin = null;
  }

  public Circuit getCircuit() {
    return circuit;
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
    return componentToPlace;
  }

  // --- Actions ---

  public void startPlacing(Component c) {
    this.componentToPlace = c;
    this.connectionStartPin = null;
    panel.repaint();
  }

  public void deleteSelection() {
    if (selectedWaypoint != null) {
      selectedWaypoint.connection().waypoints.remove(selectedWaypoint.point());
      selectedWaypoint = null;
    } else if (selectedWireSegment != null) {
      circuit.removeConnection(
          selectedWireSegment.connection().component,
          selectedWireSegment.connection().inputIndex);
      selectedWireSegment = null;
    } else if (!selectedComponents.isEmpty()) {
      for (Component c : new ArrayList<>(selectedComponents)) {
        circuit.removeComponent(c);
      }
      selectedComponents.clear();
    }
    panel.repaint();
  }

  // --- Coordinate Transformation ---
  private Point getWorldPoint(MouseEvent e) {
    return new Point(e.getX() - panel.getPanX(), e.getY() - panel.getPanY());
  }

  // --- Mouse Handling ---

  @Override
  public void mouseEntered(MouseEvent e) {
    panel.requestFocusInWindow();
    panel.repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    panel.repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    currentMousePoint = getWorldPoint(e);

    // Update Ghost Component
    if (componentToPlace != null) {
      int gridX = Math.round(currentMousePoint.x / 20.0f) * 20;
      int gridY = Math.round(currentMousePoint.y / 20.0f) * 20;
      componentToPlace.setPosition(gridX, gridY);
      panel.repaint();
      return;
    }

    // Update Hover State
    Point worldPt = getWorldPoint(e);

    hoveredPin = hitTester.findPinAt(worldPt);
    hoveredWaypoint = hitTester.findWaypointAt(worldPt);
    hoveredWire = (hoveredPin == null && hoveredWaypoint == null) ? hitTester.findWireAt(worldPt) : null;

    if (hoveredPin != null || hoveredWire != null || hoveredWaypoint != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } else {
      Component c = hitTester.findComponentAt(worldPt);
      panel.setCursor(c != null ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
          : Cursor.getDefaultCursor());
    }

    panel.repaint();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    panel.requestFocusInWindow();
    currentMousePoint = getWorldPoint(e);

    // --- Panning Start ---
    boolean isLaptopPan = SwingUtilities.isLeftMouseButton(e) && e.isAltDown();
    if (SwingUtilities.isMiddleMouseButton(e) || isLaptopPan) {
      isPanning = true;
      panStartScreenPt = e.getPoint(); // Store Screen Coords
      panStartOffset = new Point(panel.getPanX(), panel.getPanY()); // Store Initial Pan
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      return;
    }

    // --- Right Click ---
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

    // --- Left Click ---
    Point worldPt = getWorldPoint(e);

    // Place Component
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

    Pin clickedPin = hitTester.findPinAt(worldPt);

    // Wiring Phase 2
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

    // Wiring Phase 1
    if (clickedPin != null) {
      connectionStartPin = clickedPin;
      selectedComponents.clear();
      selectedWireSegment = null;
      selectedWaypoint = null;
      panel.repaint();
      return;
    }

    // Waypoint Click
    WaypointRef clickedWP = hitTester.findWaypointAt(worldPt);
    if (clickedWP != null) {
      selectedWaypoint = clickedWP;
      selectedWireSegment = new WireSegment(getWireForConnection(clickedWP.connection()), clickedWP.connection());
      selectedComponents.clear();
      panel.repaint();
      return;
    }

    // Wire Click (Select or Split)
    WireSegment clickedWire = hitTester.findWireAt(worldPt);
    if (clickedWire != null) {
      if (selectedWireSegment != null &&
          clickedWire.wire() == selectedWireSegment.wire() &&
          clickedWire.connection() == selectedWireSegment.connection()) {

        int idx = hitTester.getWaypointInsertionIndex(clickedWire, worldPt);
        clickedWire.connection().waypoints.add(idx, worldPt);
        selectedWaypoint = new WaypointRef(clickedWire.connection(), worldPt);
      } else {
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

    // Component Selection
    Component clickedComp = hitTester.findComponentAt(worldPt);
    if (clickedComp != null) {
      handleComponentSelection(e, clickedComp);

      // --- Prepare Smooth Drag ---
      isDraggingItems = true;
      dragStartWorldPt = worldPt;
      initialComponentPositions.clear();
      for (Component c : selectedComponents) {
        initialComponentPositions.put(c, new Point(c.getX(), c.getY()));
      }
    } else {
      startSelectionBox(worldPt);
    }
    panel.repaint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {

    // --- SMOOTH PANNING ---
    if (isPanning) {
      // Calculate delta from the START of the drag
      int dx = e.getX() - panStartScreenPt.x;
      int dy = e.getY() - panStartScreenPt.y;

      // Apply delta to the INITIAL pan offset
      panel.setPan(panStartOffset.x + dx, panStartOffset.y + dy);
      return;
    }

    if (connectionStartPin != null) {
      currentMousePoint = getWorldPoint(e);
      panel.repaint();
      return;
    }

    // Waypoint Dragging (Already uses absolute setLocation, so it's smooth)
    if (selectedWaypoint != null) {
      Point pt = selectedWaypoint.point();
      pt.setLocation(getWorldPoint(e));

      // Snapping logic
      List<Point> points = selectedWaypoint.connection().waypoints;
      int index = points.indexOf(pt);
      Point prev = null;
      Point next = null;
      Wire w = getWireForConnection(selectedWaypoint.connection());

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

    // --- SMOOTH COMPONENT DRAGGING ---
    if (isDraggingItems) {
      Point currentWorld = getWorldPoint(e);

      // Calculate delta from START of drag
      int dx = currentWorld.x - dragStartWorldPt.x;
      int dy = currentWorld.y - dragStartWorldPt.y;

      for (Component c : selectedComponents) {
        Point startPos = initialComponentPositions.get(c);
        if (startPos != null) {
          // Set position relative to original position
          c.setPosition(startPos.x + dx, startPos.y + dy);
        }
      }
      panel.repaint();
      return;
    }

    if (selectionRect != null) {
      updateSelectionBox(getWorldPoint(e));
      panel.repaint();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (isPanning) {
      isPanning = false;
      panel.setCursor(Cursor.getDefaultCursor());
    }
    if (selectionRect != null) {
      finalizeSelectionBox();
    }
    // Clear drag state
    isDraggingItems = false;
    initialComponentPositions.clear();

    panel.repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (connectionStartPin == null && !isDraggingItems && componentToPlace == null) {
      Point worldPt = getWorldPoint(e);
      Component c = hitTester.findComponentAt(worldPt);
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
    // Note: isDraggingItems is set in mousePressed now
  }

  private void startSelectionBox(Point p) {
    selectedComponents.clear();
    selectionStartPt = p;
    selectionRect = new Rectangle(p.x, p.y, 0, 0);
  }

  private void updateSelectionBox(Point p) {
    int x = Math.min(selectionStartPt.x, p.x);
    int y = Math.min(selectionStartPt.y, p.y);
    int w = Math.abs(p.x - selectionStartPt.x);
    int h = Math.abs(p.y - selectionStartPt.y);
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

  private Wire getWireForConnection(Wire.PortConnection pc) {
    for (Wire w : circuit.getWires()) {
      if (w.getDestinations().contains(pc))
        return w;
    }
    return null;
  }
}
