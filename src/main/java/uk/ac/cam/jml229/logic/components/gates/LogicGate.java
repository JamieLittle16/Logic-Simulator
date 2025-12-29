package uk.ac.cam.jml229.logic.components.gates;

import uk.ac.cam.jml229.logic.components.Component;

/**
 * Base class for multi-input logic gates.
 * Replaces BinaryGate to support N-inputs.
 */
public abstract class LogicGate extends Component {

  protected boolean state = false;

  public LogicGate(String name) {
    super(name);
    setInputCount(2); // Default to 2 inputs
  }

  /**
   * Functional Interface for gate logic.
   * (boolean accumulator, boolean input) -> boolean result
   */
  @FunctionalInterface
  protected interface LogicOp {
    boolean apply(boolean acc, boolean val);
  }

  /**
   * Applies a function to all inputs to calculate the result.
   * 
   * @param identity The starting value (e.g., true for AND, false for OR)
   * @param op       The logic to apply (e.g., (a, b) -> a && b)
   */
  protected boolean reduceInputs(boolean identity, LogicOp op) {
    boolean result = identity;
    int n = getInputCount();
    for (int i = 0; i < n; i++) {
      result = op.apply(result, getInput(i));
    }
    return result;
  }

  public void resizeInputs(int count) {
    if (count < 2)
      count = 2;
    if (count > 32)
      count = 32; // Safety limit

    if (getInputCount() != count) {
      setInputCount(count);
      update(); // Recalculate with new input count
    }
  }

  @Override
  public void update() {
    updateLogic();
    if (getOutputWire() != null) {
      getOutputWire().setSignal(state);
    }
  }

  protected abstract void updateLogic();
}
