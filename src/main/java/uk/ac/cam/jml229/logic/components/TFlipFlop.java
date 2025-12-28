package uk.ac.cam.jml229.logic.components;

public class TFlipFlop extends Component {
  private boolean state = false;
  private boolean lastClock = false;

  public TFlipFlop(String name) {
    super(name);
    // 0: T, 1: Clk
    setInputCount(2);
    setOutputWire(0, null);
    setOutputWire(1, null);
  }

  @Override
  public void update() {
    boolean t = getInput(0);
    boolean clk = getInput(1);

    if (clk && !lastClock) {
      if (t)
        state = !state; // Toggle
    }
    lastClock = clk;

    if (getOutputWire(0) != null)
      getOutputWire(0).setSignal(state);
    if (getOutputWire(1) != null)
      getOutputWire(1).setSignal(!state);
  }
}
