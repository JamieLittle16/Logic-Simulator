package uk.ac.cam.jml229.logic.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import uk.ac.cam.jml229.logic.model.Wire;

public abstract class Component {
  private String name;
  private int x, y;

  // Dynamic Inputs and Outputs ---
  private final List<Wire> outputWires = new ArrayList<>();
  private final List<Boolean> inputs = new ArrayList<>();

  // Default input count (can be changed by subclasses)
  private int inputCount = 0;

  public Component(String name) {
    this.name = name;
  }

  // ==========================================
  // INPUT MANAGEMENT
  // ==========================================

  public void setInput(int index, boolean state) {
    // Auto-grow inputs list to handle the index
    while (inputs.size() <= index) {
      inputs.add(false);
    }

    // Only update if value actually changed (Optimisation)
    if (inputs.get(index) != state) {
      inputs.set(index, state);
      update(); // Trigger logic recalculation
    }
  }

  public boolean getInput(int index) {
    if (index >= 0 && index < inputs.size()) {
      return inputs.get(index);
    }
    return false; // Default to low if unconnected
  }

  /**
   * Subclasses (like AndGate) should set this in their constructor.
   */
  protected void setInputCount(int count) {
    this.inputCount = count;
    // Pre-fill list
    while (inputs.size() < count)
      inputs.add(false);
  }

  public int getInputCount() {
    return inputCount;
  }

  // ==========================================
  // OUTPUT MANAGEMENT
  // ==========================================

  // --- Backward Compatibility (Proxy methods) ---
  public Wire getOutputWire() {
    return getOutputWire(0);
  }

  public void setOutputWire(Wire w) {
    setOutputWire(0, w);
  }

  // --- Multi-Output Support ---
  public Wire getOutputWire(int index) {
    if (index >= 0 && index < outputWires.size()) {
      return outputWires.get(index);
    }
    return null;
  }

  public void setOutputWire(int index, Wire w) {
    while (outputWires.size() <= index) {
      outputWires.add(null);
    }
    outputWires.set(index, w);
  }

  public List<Wire> getAllOutputs() {
    return outputWires.stream().filter(Objects::nonNull).toList();
  }

  public int getOutputCount() {
    // Default to 1 output pin if a wire exists, or 0 if empty.
    // CustomComponents will override this to return specific numbers.
    if (outputWires.isEmpty())
      return 1;
    return Math.max(1, outputWires.size());
  }

  // ==========================================
  // CORE LOGIC
  // ==========================================

  public String getName() {
    return name;
  }

  public void setPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public abstract void update();

  /**
   * Creates a fresh copy of this component using Reflection.
   */
  public Component makeCopy() {
    try {
      Component copy = this.getClass().getConstructor(String.class).newInstance(this.name);
      // Copy basic properties if needed (like input count)
      copy.setInputCount(this.getInputCount());
      return copy;
    } catch (Exception e) {
      throw new RuntimeException("Failed to copy component: " + name, e);
    }
  }
}
