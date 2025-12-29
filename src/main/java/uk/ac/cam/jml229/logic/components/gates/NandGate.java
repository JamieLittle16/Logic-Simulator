package uk.ac.cam.jml229.logic.components.gates;

public class NandGate extends LogicGate {
  public NandGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    // NAND is just NOT (AND)
    state = !reduceInputs(true, (acc, val) -> acc && val);
  }
}
