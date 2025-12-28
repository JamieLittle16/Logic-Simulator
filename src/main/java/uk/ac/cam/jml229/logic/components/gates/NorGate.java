package uk.ac.cam.jml229.logic.components.gates;

public class NorGate extends BinaryGate {
  public NorGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = !(getInputA() || getInputB());
  }
}
