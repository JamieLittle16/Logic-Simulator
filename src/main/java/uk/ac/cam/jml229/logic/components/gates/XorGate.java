package uk.ac.cam.jml229.logic.components.gates;

public class XorGate extends LogicGate {
  public XorGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    // XOR is effectively a partity check, so start with FALSE
    state = reduceInputs(false, (acc, val) -> acc ^ val);
  }
}
