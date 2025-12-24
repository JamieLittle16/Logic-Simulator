package uk.ac.cam.jml229.logic.components;

public abstract class Component {
  private final String name;
  protected Wire outputWire;
  protected boolean state = false;
  protected int x, y;

  public Component(String name) {
    this.name = name;
  }

  // Sets the logic that updates the gates state
  public abstract void updateLogic();

  // Sets the logic that updates the input(s)
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

  public void setPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean getState() {
    return state;
  }

  // Updates its own state, and triggers update in the output wire
  public void update() {
    updateLogic();
    if (outputWire != null) {
      outputWire.setSignal(state);
    }
  }
}
