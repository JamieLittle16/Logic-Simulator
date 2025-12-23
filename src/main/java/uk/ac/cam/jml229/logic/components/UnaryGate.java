package uk.ac.cam.jml229.logic.components;

import uk.ac.cam.jml229.logic.exceptions.InvalidInputException;

public abstract class UnaryGate extends Component {
  protected boolean inputA;

  public UnaryGate(String name) {
    super(name);
  }

  @Override
  public void setInput(int inputIndex, boolean state) throws InvalidInputException {
    if (inputIndex != 0) {
      throw new InvalidInputException(getName(), inputIndex, 1);
    }
    inputA = state;
  }
}
