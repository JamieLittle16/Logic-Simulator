package uk.ac.cam.jml229.logic.components;

public class HexDisplay extends Component {

  // Segment patterns for 0-9, A-F
  // (a, b, c, d, e, f, g)
  private static final int[][] PATTERNS = {
      { 1, 1, 1, 1, 1, 1, 0 }, // 0
      { 0, 1, 1, 0, 0, 0, 0 }, // 1
      { 1, 1, 0, 1, 1, 0, 1 }, // 2
      { 1, 1, 1, 1, 0, 0, 1 }, // 3
      { 0, 1, 1, 0, 0, 1, 1 }, // 4
      { 1, 0, 1, 1, 0, 1, 1 }, // 5
      { 1, 0, 1, 1, 1, 1, 1 }, // 6
      { 1, 1, 1, 0, 0, 0, 0 }, // 7
      { 1, 1, 1, 1, 1, 1, 1 }, // 8
      { 1, 1, 1, 1, 0, 1, 1 }, // 9
      { 1, 1, 1, 0, 1, 1, 1 }, // A
      { 0, 0, 1, 1, 1, 1, 1 }, // b
      { 1, 0, 0, 1, 1, 1, 0 }, // C
      { 0, 1, 1, 1, 1, 0, 1 }, // d
      { 1, 0, 0, 1, 1, 1, 1 }, // E
      { 1, 0, 0, 0, 1, 1, 1 } // F
  };

  public HexDisplay(String name) {
    super(name);
    // Inputs: 8, 4, 2, 1
    setInputCount(4);
  }

  @Override
  public void update() {
    // Visual only
  }

  @Override
  public int getOutputCount() {
    return 0;
  }

  public boolean isSegmentOn(int segmentIndex) {
    if (segmentIndex < 0 || segmentIndex > 6)
      return false;

    // Calculate 4-bit value
    int val = 0;
    if (getInput(0))
      val += 8; // MSB
    if (getInput(1))
      val += 4;
    if (getInput(2))
      val += 2;
    if (getInput(3))
      val += 1; // LSB

    return PATTERNS[val][segmentIndex] == 1;
  }
}
