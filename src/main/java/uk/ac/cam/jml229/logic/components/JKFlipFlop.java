package uk.ac.cam.jml229.logic.components;

public class JKFlipFlop extends Component {
  private boolean state = false;
  private boolean lastClock = false;

  public JKFlipFlop(String name) {
    super(name);
    // 0: J, 1: Clk, 2: K
    setInputCount(3);
    setOutputWire(0, null); // Q
    setOutputWire(1, null); // !Q
  }

  @Override
  public void update() {
    boolean j = getInput(0);
    boolean clk = getInput(1);
    boolean k = getInput(2);

    // Rising Edge
    if (clk && !lastClock) {
      if (j && k)
        state = !state; // Toggle
      else if (j)
        state = true; // Set
      else if (k)
        state = false; // Reset
      // else hold
    }
    lastClock = clk;

    if (getOutputWire(0) != null)
      getOutputWire(0).setSignal(state);
    if (getOutputWire(1) != null)
      getOutputWire(1).setSignal(!state);
  }
}
