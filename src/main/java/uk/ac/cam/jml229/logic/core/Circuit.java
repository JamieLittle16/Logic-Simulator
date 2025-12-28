package uk.ac.cam.jml229.logic.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.seq.Clock;

public class Circuit {
  // The core data
  private final List<Component> components = new ArrayList<>();
  private final List<Wire> wires = new ArrayList<>();

  /**
   * Advances the simulation by one step.
   * Called by the global Timer.
   */
  public void tick() {
    for (Component c : components) {
      if (c instanceof Clock) {
        ((Clock) c).tick();
      }
    }
  }

  /**
   * Adds a component to the circuit.
   * If the component already has wires attached (e.g. from a copy-paste),
   * it ensures those wires are tracked too.
   */
  public void addComponent(Component c) {
    components.add(c);
    for (Wire w : c.getAllOutputs()) {
      if (!wires.contains(w)) {
        wires.add(w);
      }
    }
  }

  /**
   * Removes a component and safely cleans up all connected wires.
   */
  public void removeComponent(Component c) {
    // 1. Remove wires driven BY this component (All Outputs)
    List<Wire> outputWires = new ArrayList<>();
    for (Wire w : wires) {
      if (w.getSource() == c) {
        outputWires.add(w);
      }
    }

    // --- Turn off the destinations before deleting the wire ---
    for (Wire w : outputWires) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        // Reset the destination input to FALSE so it doesn't stay "Green"
        pc.component.setInput(pc.inputIndex, false);
        // Force the destination to recalculate (e.g. LED turns dark)
        pc.component.update();
      }
    }

    wires.removeAll(outputWires);

    // Remove wires driving INTO this component (Inputs)
    for (Wire w : wires) {
      w.getDestinations().removeIf(pc -> pc.component == c);
    }

    // Remove component
    components.remove(c);
  }

  /**
   * Standard Connection (Default Source Output 0 -> Dest Input Index)
   */
  public boolean addConnection(Component source, Component dest, int inputIndex) {
    return addConnection(source, 0, dest, inputIndex);
  }

  /**
   * Advanced Connection (Source Output Index -> Dest Input Index)
   */
  public boolean addConnection(Component source, int sourceOutputIndex, Component dest, int inputIndex) {
    if (source == dest)
      return false;

    // Check availability
    for (Wire w : wires) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        if (pc.component == dest && pc.inputIndex == inputIndex) {
          return false; // Input occupied
        }
      }
    }

    // Get or Create Wire at specific index
    Wire w = source.getOutputWire(sourceOutputIndex);
    boolean isNewWire = false;

    if (w == null) {
      w = new Wire(source);
      source.setOutputWire(sourceOutputIndex, w);
      wires.add(w);
      isNewWire = true;
    }

    // If it's a new wire, we calculate the source's output immediately
    if (isNewWire) {
      source.update();
    }

    w.addDestination(dest, inputIndex);
    dest.setInput(inputIndex, w.getSignal());

    dest.update();

    return true;
  }

  /**
   * Removes a specific connection (Wire segment).
   */
  public void removeConnection(Component dest, int inputIndex) {
    // Find the wire connected to this specific input
    for (Wire w : wires) {
      boolean wasConnected = false;

      // Check if this wire hits the target
      for (Wire.PortConnection pc : w.getDestinations()) {
        if (pc.component == dest && pc.inputIndex == inputIndex) {
          wasConnected = true;
          break;
        }
      }

      if (wasConnected) {
        // Reset signal to FALSE (The Bug Fix you already had!)
        dest.setInput(inputIndex, false);

        // RECOMMENDATION: Update component so it visually changes color immediately
        dest.update();

        // Remove the physical connection
        w.removeDestination(dest, inputIndex);
        return;
      }
    }
  }

  /**
   * Creates a deep copy of this circuit.
   */
  public Circuit cloneCircuit() {
    Circuit copy = new Circuit();
    java.util.Map<Component, Component> oldToNew = new java.util.HashMap<>();

    for (Component original : this.components) {
      Component clone = original.makeCopy();
      clone.setPosition(original.getX(), original.getY());
      copy.addComponent(clone);
      oldToNew.put(original, clone);
    }

    for (Wire originalWire : this.wires) {
      Component oldSource = originalWire.getSource();
      if (oldSource == null)
        continue;

      int sourceIndex = -1;
      for (int i = 0; i < oldSource.getOutputCount(); i++) {
        if (oldSource.getOutputWire(i) == originalWire) {
          sourceIndex = i;
          break;
        }
      }
      if (sourceIndex == -1)
        continue;

      Component newSource = oldToNew.get(oldSource);

      for (Wire.PortConnection pc : originalWire.getDestinations()) {
        Component oldDest = pc.component;
        Component newDest = oldToNew.get(oldDest);

        copy.addConnection(newSource, sourceIndex, newDest, pc.inputIndex);
      }
    }
    return copy;
  }

  // --- Accessors ---

  public List<Component> getComponents() {
    return Collections.unmodifiableList(components);
  }

  public List<Wire> getWires() {
    return Collections.unmodifiableList(wires);
  }

  public void clear() {
    components.clear();
    wires.clear();
  }
}
