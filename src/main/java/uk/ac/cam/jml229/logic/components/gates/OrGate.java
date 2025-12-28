package uk.ac.cam.jml229.logic.components.gates;

public class OrGate extends BinaryGate {
  public OrGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = getInputA() || getInputB();
  }
}
