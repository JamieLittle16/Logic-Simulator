package uk.ac.cam.jml229.logic.components.gates;

import uk.ac.cam.jml229.logic.components.Component;

public abstract class BinaryGate extends Component {

  // Keep 'state' so the gate remembers its output value
  protected boolean state = false;

  public BinaryGate(String name) {
    super(name);
    setInputCount(2); // Reserve 2 inputs in the base list
  }

  // --- The Bridge: Map Index to Names ---
  protected boolean getInputA() {
    return getInput(0);
  }

  protected boolean getInputB() {
    return getInput(1);
  }

  // --- The Loop: Component calls update() -> We call updateLogic() ---
  @Override
  public void update() {
    // Let the specific gate (AND, OR) calculate the new 'state'
    updateLogic();

    // Push that state to the wire (if connected)
    if (getOutputWire() != null) {
      getOutputWire().setSignal(state);
    }
  }

  // Force subclasses to define the logic
  protected abstract void updateLogic();
}
