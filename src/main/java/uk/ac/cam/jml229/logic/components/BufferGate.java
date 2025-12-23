package uk.ac.cam.jml229.logic.components;

public class BufferGate extends UnaryGate {
  public BufferGate(String name) {
    super(name);
  }

  @Override
  public void updateLogic() {
    state = inputA;
  }
}
