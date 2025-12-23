package uk.ac.cam.jml229.logic.components;

public class Switch extends Component {

  public Switch(String name) {
    super(name);
  }

  public void toggle(boolean isOn) {
    state = isOn;
    update(); // Propagate signal
  }

  @Override
  public void updateLogic() {
    // No logic needed - state ste manually with toggle()
  }

  @Override
  public void setInput(int inputIndex, boolean state) {
    // Switches don't accept inputs from other wires
    throw new UnsupportedOperationException("Switches do not accept inputs.");
  }
}
