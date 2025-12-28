package uk.ac.cam.jml229.logic.components.gates;

public class AndGate extends BinaryGate {

  public AndGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = getInputA() && getInputB();
  }
}
