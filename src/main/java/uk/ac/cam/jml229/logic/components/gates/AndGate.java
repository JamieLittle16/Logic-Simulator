package uk.ac.cam.jml229.logic.components.gates;

public class AndGate extends LogicGate {

  public AndGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    // Start with TRUE. If any input is FALSE, the result becomes FALSE.
    state = reduceInputs(true, (acc, val) -> acc && val);
  }
}
