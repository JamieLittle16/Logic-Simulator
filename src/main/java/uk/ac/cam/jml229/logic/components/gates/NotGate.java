package uk.ac.cam.jml229.logic.components.gates;

public class NotGate extends UnaryGate {
  public NotGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = !getInputA();
  }
}
