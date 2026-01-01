package uk.ac.cam.jml229.logic.ui.interaction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.misc.TextLabel;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.panels.CircuitPanel;
import uk.ac.cam.jml229.logic.ui.panels.ComponentPalette;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WireSegment;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;
import uk.ac.cam.jml229.logic.ui.interaction.state.*;
import uk.ac.cam.jml229.logic.io.HistoryManager;
import uk.ac.cam.jml229.logic.io.StorageManager;
import uk.ac.cam.jml229.logic.app.Theme;

public class CircuitInteraction extends MouseAdapter implements KeyListener {

  private Circuit circuit;
  private final CircuitPanel panel;
  private final CircuitRenderer renderer;
  private final CircuitHitTester hitTester;
  private final HistoryManager history = new HistoryManager();
  private ComponentPalette palette;

  private InteractionState currentState;

  // --- Callback for Timing ---
  private Consumer<List<Component>> onOpenTiming;

  // --- SHARED VIEW STATE ---
  private final List<Component> selectedComponents = new ArrayList<>();
  private final List<WaypointRef> selectedWaypoints = new ArrayList<>();
  private WireSegment selectedWireSegment = null;

  // Transient state for rendering
  public Pin hoveredPin = null;
  public WireSegment hoveredWire = null;
  public WaypointRef hoveredWaypoint = null;
  public Pin connectionStartPin = null;
  public Point currentMousePoint = null;
  public Rectangle selectionRect = null;
  public Component componentToPlace = null;

  // Flags
  private boolean snapToGrid = false;
  private boolean preventNextClick = false;
  private static String clipboardString = null;

  public CircuitInteraction(Circuit circuit, CircuitPanel panel, CircuitRenderer renderer) {
    this.circuit = circuit;
    this.panel = panel;
    this.renderer = renderer;
    this.hitTester = new CircuitHitTester(circuit, renderer);
    this.history.pushState(circuit);

    setState(new IdleState(this));
  }

  // --- Setter for callback ---
  public void setOnOpenTiming(Consumer<List<Component>> callback) {
    this.onOpenTiming = callback;
  }

  public void setState(InteractionState newState) {
    if (currentState != null)
      currentState.onExit();
    currentState = newState;
    if (currentState != null)
      currentState.onEnter();
    panel.repaint();
  }

  // --- Helper: Centralised Hover Logic ---
  public void updateHoverState(MouseEvent e) {
    currentMousePoint = getWorldPoint(e);
    Point p = currentMousePoint;

    // Reset all
    hoveredPin = null;
    hoveredWire = null;
    hoveredWaypoint = null;

    // Check Pins
    hoveredPin = hitTester.findPinAt(p);
    if (hoveredPin != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return;
    }

    // Check Waypoints
    hoveredWaypoint = hitTester.findWaypointAt(p);
    if (hoveredWaypoint != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return;
    }

    // Check Wires
    hoveredWire = hitTester.findWireAt(p);
    if (hoveredWire != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      return;
    }

    // Check Components (for cursor only, Renderer handles body highlight via
    // selection usually)
    Component c = hitTester.findComponentAt(p);
    if (c != null) {
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    } else {
      panel.setCursor(Cursor.getDefaultCursor());
    }
  }

  public void setPreventNextClick(boolean prevent) {
    this.preventNextClick = prevent;
  }

  public boolean shouldPreventNextClick() {
    if (preventNextClick) {
      preventNextClick = false; // Consume it
      return true;
    }
    return false;
  }

  // --- Events ---
  @Override
  public void mousePressed(MouseEvent e) {
    // Context Menu Logic
    if (SwingUtilities.isRightMouseButton(e)) {
      Component c = hitTester.findComponentAt(getWorldPoint(e));
      if (c != null) {
        // Select it if not already selected
        if (!selectedComponents.contains(c)) {
          if (!e.isShiftDown())
            clearSelection();
          addToSelection(c);
        }
        showContextMenu(e);
        return;
      } else if (!selectedComponents.isEmpty()) {
        showContextMenu(e);
        return;
      }
    }
    currentState.mousePressed(e);
  }

  private void showContextMenu(MouseEvent e) {
    JPopupMenu menu = new JPopupMenu();

    // Theme Support
    menu.setBackground(Theme.isDarkMode ? Theme.PALETTE_BACKGROUND : Color.WHITE);
    menu.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));

    JMenuItem icItem = new JMenuItem("Create Custom IC");
    icItem.addActionListener(ev -> createCustomComponentFromSelection());
    menu.add(icItem);

    JMenuItem timingItem = new JMenuItem("Add to Timing Diagram");
    timingItem.addActionListener(ev -> {
      if (onOpenTiming != null) {
        onOpenTiming.accept(new ArrayList<>(selectedComponents));
      }
    });
    menu.add(timingItem);

    JMenuItem delayItem = new JMenuItem("Set Propagation Delay...");
    delayItem.addActionListener(ev -> {
      String input = JOptionPane.showInputDialog(panel, "Enter delay in ticks (Leave empty for default):", "Set Delay",
          JOptionPane.QUESTION_MESSAGE);
      if (input != null) {
        Integer val = null;
        if (!input.trim().isEmpty()) {
          try {
            val = Integer.parseInt(input.trim());
            if (val < 0)
              val = 0;
          } catch (NumberFormatException ex) {
            return;
          }
        }
        for (Component c : selectedComponents) {
          c.setCustomDelay(val);
        }
      }
    });
    menu.add(delayItem);

    menu.addSeparator();
    JMenuItem renameItem = new JMenuItem("Rename");
    renameItem.addActionListener(ev -> {
      if (selectedComponents.size() == 1) {
        Component c = selectedComponents.get(0);
        String newName = JOptionPane.showInputDialog(panel, "Rename:", c.getName());
        if (newName != null && !newName.trim().isEmpty()) {
          // Allow Labels to have long text (30 chars), keep others short (8 chars)
          int maxLen = (c instanceof TextLabel) ? 30 : 8;

          if (newName.length() > maxLen) {
            newName = newName.substring(0, maxLen);
          }

          saveHistory();
          c.setName(newName);
          panel.repaint();
        }
      }
    });
    // Only enable rename if exactly one component is selected
    renameItem.setEnabled(selectedComponents.size() == 1);
    menu.add(renameItem);

    JMenuItem copyItem = new JMenuItem("Copy");
    copyItem.addActionListener(ev -> copy());
    menu.add(copyItem);

    JMenuItem delItem = new JMenuItem("Delete");
    delItem.addActionListener(ev -> deleteSelection());
    menu.add(delItem);

    // Style Items
    if (Theme.isDarkMode) {
      menu.setBackground(Theme.PALETTE_BACKGROUND);
      menu.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));

      for (java.awt.Component c : menu.getComponents()) {
        if (c instanceof JMenuItem) {
          c.setBackground(Theme.PALETTE_BACKGROUND);
          c.setForeground(Theme.TEXT_COLOR);
          ((JComponent) c).setOpaque(true);
          // Remove the white "gutter" on the left
          ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
        if (c instanceof JPopupMenu.Separator) {
          c.setBackground(Theme.PALETTE_BACKGROUND);
          c.setForeground(Theme.GRID_MAJOR);
        }
      }
    } else {
      // Standard Light Theme Styling
      menu.setBackground(Color.WHITE);
      menu.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
      for (java.awt.Component c : menu.getComponents()) {
        c.setBackground(Color.WHITE);
        c.setForeground(Color.BLACK);
      }
    }

    menu.show(e.getComponent(), e.getX(), e.getY());
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    currentState.mouseReleased(e);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    currentState.mouseDragged(e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    currentState.mouseMoved(e);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    currentState.mouseClicked(e);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    currentState.keyPressed(e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  // --- Actions ---
  public void startPlacing(Component c) {
    setState(new PlacingState(this, c));
  }

  public void rotateSelection() {
    if (currentState instanceof PlacingState) {
      ((PlacingState) currentState).rotate();
    } else if (!selectedComponents.isEmpty()) {
      saveHistory();
      for (Component c : selectedComponents)
        c.rotate();
      panel.repaint();
    }
  }

  public void deleteSelection() {
    if (selectedComponents.isEmpty() && selectedWireSegment == null && selectedWaypoints.isEmpty())
      return;
    saveHistory();

    for (WaypointRef wp : selectedWaypoints)
      wp.connection().waypoints.remove(wp.point());
    selectedWaypoints.clear();

    if (selectedWireSegment != null) {
      circuit.removeConnection(selectedWireSegment.connection().component, selectedWireSegment.connection().inputIndex);
      selectedWireSegment = null;
    }

    for (Component c : new ArrayList<>(selectedComponents))
      circuit.removeComponent(c);
    selectedComponents.clear();
    panel.repaint();
  }

  public void copy() {
    if (selectedComponents.isEmpty())
      return;
    Circuit temp = new Circuit();
    for (Component c : selectedComponents)
      temp.addComponent(c);
    clipboardString = StorageManager.saveToString(temp, null);
  }

  public void cut() {
    copy();
    deleteSelection();
  }

  public void paste() {
    if (clipboardString == null)
      return;
    try {
      saveHistory();
      StorageManager.LoadResult result = StorageManager.loadFromString(clipboardString);
      Circuit pasted = result.circuit();
      if (pasted.getComponents().isEmpty())
        return;

      if (palette != null)
        for (CustomComponent cc : result.customTools())
          palette.addCustomTool(cc);

      clearSelection();
      for (Component c : pasted.getComponents()) {
        c.setPosition(c.getX() + 20, c.getY() + 20);
        circuit.addComponent(c);
        selectedComponents.add(c);
      }
      panel.repaint();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void createCustomComponentFromSelection() {
    if (selectedComponents.isEmpty())
      return;

    String name = JOptionPane.showInputDialog(panel, "Enter Name (max 5 chars):", "New Component",
        JOptionPane.PLAIN_MESSAGE);
    if (name == null || name.trim().isEmpty())
      return;
    if (name.length() > 5)
      name = name.substring(0, 5);

    // Create a mini-circuit from selection
    Circuit innerCircuit = new Circuit();
    Map<Component, Component> oldToNew = new HashMap<>();

    // Clone Components
    for (Component original : selectedComponents) {
      Component clone = original.makeCopy();
      // Preserve relative positions
      clone.setPosition(original.getX(), original.getY());
      innerCircuit.addComponent(clone);
      oldToNew.put(original, clone);
    }

    // Clone Internal Wires
    for (Wire w : circuit.getWires()) {
      Component source = w.getSource();
      // Only care if source is in selection
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
        // Only care if destination is in selection
        if (selectedComponents.contains(pc.component)) {
          Component newSource = oldToNew.get(source);
          Component newDest = oldToNew.get(pc.component);
          innerCircuit.addConnection(newSource, sourceIndex, newDest, pc.inputIndex);
        }
      }
    }

    // Create the CustomComponent wrapper
    CustomComponent newTool = new CustomComponent(name, innerCircuit);

    // Add to Palette
    if (palette != null) {
      palette.addCustomTool(newTool);
      JOptionPane.showMessageDialog(panel, "Custom Component '" + name + "' added to palette!");
    }
  }

  public void undo() {
    Circuit prev = history.undo(circuit);
    if (prev != null)
      panel.setCircuit(prev);
  }

  public void redo() {
    Circuit next = history.redo(circuit);
    if (next != null)
      panel.setCircuit(next);
  }

  public void saveHistory() {
    history.pushState(circuit);
  }

  public void resetHistory() {
    history.clear();
    history.pushState(circuit);
  }

  // --- Accessors ---
  public Circuit getCircuit() {
    return circuit;
  }

  public CircuitPanel getPanel() {
    return panel;
  }

  public CircuitRenderer getRenderer() {
    return renderer;
  }

  public CircuitHitTester getHitTester() {
    return hitTester;
  }

  public ComponentPalette getPalette() {
    return palette;
  }

  public List<Component> getSelectedComponents() {
    return selectedComponents;
  }

  public List<Component> getSelection() {
    return selectedComponents;
  }

  public List<WaypointRef> getSelectedWaypoints() {
    return selectedWaypoints;
  }

  public WaypointRef getSelectedWaypoint() {
    return selectedWaypoints.isEmpty() ? null : selectedWaypoints.get(0);
  }

  public WireSegment getSelectedWire() {
    return selectedWireSegment;
  }

  public void setSelectedWire(WireSegment ws) {
    this.selectedWireSegment = ws;
  }

  public void addToSelection(Component c) {
    if (!selectedComponents.contains(c))
      selectedComponents.add(c);
  }

  public void removeFromSelection(Component c) {
    selectedComponents.remove(c);
  }

  public void clearSelection() {
    selectedComponents.clear();
    selectedWaypoints.clear();
    selectedWireSegment = null;
  }

  public void setSnapToGrid(boolean b) {
    this.snapToGrid = b;
  }

  public boolean isSnapToGrid() {
    return snapToGrid;
  }

  public void setPalette(ComponentPalette p) {
    this.palette = p;
  }

  public void setCircuit(Circuit c) {
    this.circuit = c;
    this.hitTester.setCircuit(c);
    clearSelection();
    setState(new IdleState(this));
    panel.repaint();
  }

  public Point getWorldPoint(MouseEvent e) {
    double s = panel.getScale();
    int wx = (int) Math.round((e.getX() - panel.getPanX()) / s);
    int wy = (int) Math.round((e.getY() - panel.getPanY()) / s);
    return new Point(wx, wy);
  }

  public Pin getHoveredPin() {
    return hoveredPin;
  }

  public WireSegment getHoveredWire() {
    return hoveredWire;
  }

  public WaypointRef getHoveredWaypoint() {
    return hoveredWaypoint;
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
    return componentToPlace;
  }
}
