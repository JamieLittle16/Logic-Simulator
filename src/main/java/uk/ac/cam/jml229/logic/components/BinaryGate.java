package uk.ac.cam.jml229.logic.components;

import uk.ac.cam.jml229.logic.exceptions.InvalidInputException;

public abstract class BinaryGate extends Component {
  protected boolean inputA;
  protected boolean inputB;

  public BinaryGate(String name) {
    super(name);
  }

  @Override
  public void setInput(int pinIndex, boolean signal) throws InvalidInputException {
    if (pinIndex == 0) {
      inputA = signal;
    } else if (pinIndex == 1) {
      inputB = signal;
    } else {
      throw new InvalidInputException(getName(), pinIndex, 2);
    }
    update();
  }
}
