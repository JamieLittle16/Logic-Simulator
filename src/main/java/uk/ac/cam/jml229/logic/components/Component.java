package uk.ac.cam.jml229.logic.components;

public abstract class Component {
  private final String name;
  protected Wire outputWire;
  protected boolean state = false;

  public Component(String name) {
    this.name = name;
  }

  public abstract void updateLogic();

  public abstract void setInput(int inputIndex, boolean state);

  public String getName() {
    return name;
  }

  public void setOutputWire(Wire w) {
    outputWire = w;
  }

  public Wire getOutputWire() {
    return outputWire;
  }

  public void update() {
    updateLogic();
    if (outputWire != null) {
      outputWire.setSignal(state);
    }
  }
}
