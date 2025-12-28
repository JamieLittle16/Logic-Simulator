package uk.ac.cam.jml229.logic.components.gates;

public class NandGate extends BinaryGate {
  public NandGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = !(getInputA() && getInputB());
  }
}
