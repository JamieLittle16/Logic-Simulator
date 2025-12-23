package uk.ac.cam.jml229.logic.components;

public class NotGate extends UnaryGate {
  public NotGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = !inputA;
  }
}
