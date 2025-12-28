package uk.ac.cam.jml229.logic.components.io;

import uk.ac.cam.jml229.logic.components.Component;

public class Switch extends Component {

  private boolean state = false; // Internal state (ON/OFF)

  public Switch(String name) {
    super(name);
    setInputCount(0); // Switches have 0 inputs
  }

  /**
   * Called by Circuit when a wire is attached.
   * Pushes the current state to the output.
   */
  @Override
  public void update() {
    if (getOutputWire() != null) {
      getOutputWire().setSignal(state);
    }
  }

  /**
   * Toggles the switch state and triggers an update.
   */
  public void toggle(boolean newState) {
    if (this.state != newState) {
      this.state = newState;
      update(); // Push the new state immediately
    }
  }

  public boolean getState() {
    return state;
  }
}
