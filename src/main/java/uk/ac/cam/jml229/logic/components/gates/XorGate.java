package uk.ac.cam.jml229.logic.components.gates;

public class XorGate extends BinaryGate {
  public XorGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = getInputA() ^ getInputB();
  }
}
