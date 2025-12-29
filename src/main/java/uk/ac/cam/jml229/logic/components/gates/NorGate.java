package uk.ac.cam.jml229.logic.components.gates;

public class NorGate extends LogicGate {
  public NorGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    // NOR is just NOT (OR)
    state = !reduceInputs(false, (acc, val) -> acc || val);
  }
}
