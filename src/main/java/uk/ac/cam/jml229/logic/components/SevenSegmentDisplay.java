package uk.ac.cam.jml229.logic.components;

public class SevenSegmentDisplay extends Component {

  public SevenSegmentDisplay(String name) {
    super(name);
    // Inputs: a, b, c, d, e, f, g, dp (dot)
    setInputCount(8);
  }

  @Override
  public int getOutputCount() {
    return 0;
  }

  @Override
  public void update() {
    // This component is purely visual; it doesn't output logic.
    // Ideally, we trigger a repaint here, but the Circuit loop handles that.
  }

  // Helper to check which segments are ON
  public boolean isSegmentOn(int index) {
    return getInput(index);
  }
}
