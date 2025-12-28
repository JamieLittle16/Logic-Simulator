package uk.ac.cam.jml229.logic.components.gates;

import uk.ac.cam.jml229.logic.components.Component;

public abstract class UnaryGate extends Component {

  protected boolean state = false;

  public UnaryGate(String name) {
    super(name);
    setInputCount(1);
  }

  protected boolean getInputA() {
    return getInput(0);
  }

  @Override
  public void update() {
    updateLogic();
    if (getOutputWire() != null) {
      getOutputWire().setSignal(state);
    }
  }

  protected abstract void updateLogic();
}
