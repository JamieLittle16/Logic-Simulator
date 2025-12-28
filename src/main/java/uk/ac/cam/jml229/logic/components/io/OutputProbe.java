package uk.ac.cam.jml229.logic.components.io;

import uk.ac.cam.jml229.logic.components.gates.UnaryGate;

public class OutputProbe extends UnaryGate {
  public OutputProbe(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = getInputA();
    // Output to Console - for now
    System.out.println("Output [" + getName() + "]: " + (state ? "ON" : "OFF"));
  }

  public boolean getState() {
    return getInput(0);
  }
}
