package uk.ac.cam.jml229.logic.components.io;

import uk.ac.cam.jml229.logic.components.gates.UnaryGate;

public class OutputProbe extends UnaryGate {

  private Runnable onStateChanged;

  public OutputProbe(String name) {
    super(name);
  }

  public void setOnStateChanged(Runnable listener) {
    this.onStateChanged = listener;
  }

  @Override
  public void updateLogic() {
    state = getInputA();
    // Debug output is fine, but not necessary for logic
    // System.out.println("Output [" + getName() + "]: " + (state ? "ON" : "OFF"));
  }

  @Override
  public void update() {
    // Run standard gate logic (calculate state, push to any connected wires)
    super.update();

    // Notify listener (This allows CustomComponent to know when the result is
    // ready)
    if (onStateChanged != null) {
      onStateChanged.run();
    }
  }

  public boolean getState() {
    return getInput(0);
  }
}
