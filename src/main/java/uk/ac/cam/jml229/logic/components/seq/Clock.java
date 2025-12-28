package uk.ac.cam.jml229.logic.components.seq;

import uk.ac.cam.jml229.logic.components.Component;

public class Clock extends Component {

  private boolean state = false;

  public Clock(String name) {
    super(name);
    setInputCount(0); // Clocks generate signals, they don't take inputs
  }

  /**
   * Called by the Simulation Timer (in Circuit.tick).
   * Toggles state and pushes to output.
   */
  public void tick() {
    state = !state;
    update();
  }

  @Override
  public void update() {
    if (getOutputWire() != null) {
      getOutputWire().setSignal(state);
    }
  }

  public boolean getState() {
    return state;
  }
}
