package uk.ac.cam.jml229.logic.components.seq;

import uk.ac.cam.jml229.logic.components.Component;

public class DFlipFlop extends Component {

  private boolean state = false; // The stored bit (Q)
  private boolean lastClock = false; // To detect rising edge

  public DFlipFlop(String name) {
    super(name);
    // Input 0: Data (D)
    // Input 1: Clock (>)
    setInputCount(2);

    // Output 0: Q
    // Output 1: !Q (Not Q)
    setOutputWire(0, null);
    setOutputWire(1, null);
  }

  @Override
  public void update() {
    boolean d = getInput(0);
    boolean clk = getInput(1);

    // RISING EDGE DETECTOR (Low -> High)
    if (clk && !lastClock) {
      state = d; // Latch the data
    }
    lastClock = clk;

    // Update outputs
    if (getOutputWire(0) != null)
      getOutputWire(0).setSignal(state);
    if (getOutputWire(1) != null)
      getOutputWire(1).setSignal(!state);
  }
}
