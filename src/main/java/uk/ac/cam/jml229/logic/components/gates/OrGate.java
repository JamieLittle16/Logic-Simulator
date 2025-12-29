package uk.ac.cam.jml229.logic.components.gates;

public class OrGate extends LogicGate {
  public OrGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    // Start with FALSE. If any input is TRUE, the result becomes TRUE.
    state = reduceInputs(false, (acc, val) -> acc || val);
  }
}
