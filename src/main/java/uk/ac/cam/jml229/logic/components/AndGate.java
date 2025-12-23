package uk.ac.cam.jml229.logic.components;

public class AndGate extends BinaryGate {

  public AndGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = inputA && inputB;
  }
}
