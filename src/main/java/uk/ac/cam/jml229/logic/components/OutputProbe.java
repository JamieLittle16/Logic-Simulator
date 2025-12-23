package uk.ac.cam.jml229.logic.components;

public class OutputProbe extends UnaryGate {
  public OutputProbe(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = inputA;
    // Output to Console - for now
    System.out.println("Output [" + getName() + "]: " + (state ? "ON" : "OFF"));
  }
}
