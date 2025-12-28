package uk.ac.cam.jml229.logic.components;

import java.util.ArrayList;
import java.util.List;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.components.io.*;

public class CustomComponent extends Component {

  private final Circuit innerCircuit;
  private final List<Switch> internalInputs = new ArrayList<>();
  private final List<OutputProbe> internalOutputs = new ArrayList<>();

  public CustomComponent(String name, Circuit templateCircuit) {
    super(name);

    // Deep Copy the template so this chip works independently
    this.innerCircuit = templateCircuit.cloneCircuit();

    // Find the IO components inside the copy
    for (Component c : innerCircuit.getComponents()) {
      if (c instanceof Switch) {
        internalInputs.add((Switch) c);
      } else if (c instanceof OutputProbe) {
        internalOutputs.add((OutputProbe) c);
      }
    }

    internalInputs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
    internalOutputs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));

    // Configure External Pins
    setInputCount(internalInputs.size());
  }

  public Circuit getInnerCircuit() {
    return innerCircuit;
  }

  /**
   * Override makeCopy to use the correct constructor (Name + Circuit).
   * The base class uses Reflection to find a (String) constructor, which we don't
   * have.
   */
  @Override
  public Component makeCopy() {
    // We pass our 'innerCircuit' as the template.
    // The constructor will call .cloneCircuit() on it, ensuring the new copy
    // has its own independent logic.
    return new CustomComponent(getName(), this.innerCircuit);
  }

  @Override
  public int getOutputCount() {
    return Math.max(1, internalOutputs.size());
  }

  @Override
  public void update() {
    // Bridge In: External Input -> Internal Switch
    for (int i = 0; i < internalInputs.size(); i++) {
      if (i < getInputCount()) {
        boolean val = getInput(i);
        internalInputs.get(i).toggle(val);
      }
    }

    // Bridge Out: Internal Probe -> External Output Wire
    for (int i = 0; i < internalOutputs.size(); i++) {
      boolean result = internalOutputs.get(i).getState();

      Wire w = getOutputWire(i);
      if (w != null) {
        w.setSignal(result);
      }
    }
  }
}
