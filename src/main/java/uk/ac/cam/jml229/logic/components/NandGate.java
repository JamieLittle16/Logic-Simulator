package uk.ac.cam.jml229.logic.components;

public class NandGate extends BinaryGate {
  public NandGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = !(inputA && inputB);
  }
}
