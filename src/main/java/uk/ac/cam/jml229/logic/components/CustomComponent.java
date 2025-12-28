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

    // Sort pins by Y position to match visual layout
    internalInputs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
    internalOutputs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));

    // Configure External Pins
    setInputCount(internalInputs.size());

    // --- FIX: Event-Driven Outputs ---
    // Instead of polling outputs in update(), we listen for when they change.
    // This handles the propagation delay correctly.
    for (int i = 0; i < internalOutputs.size(); i++) {
      final int index = i;
      OutputProbe probe = internalOutputs.get(i);

      probe.setOnStateChanged(() -> {
        boolean val = probe.getState();
        Wire w = getOutputWire(index);
        if (w != null) {
          w.setSignal(val);
        }
      });
    }
  }

  public Circuit getInnerCircuit() {
    return innerCircuit;
  }

  @Override
  public Component makeCopy() {
    return new CustomComponent(getName(), this.innerCircuit);
  }

  @Override
  public int getOutputCount() {
    return Math.max(1, internalOutputs.size());
  }

  @Override
  public void update() {
    // Bridge In: External Input -> Internal Switch
    // When these switches toggle, they queue events in the Simulator.
    // Eventually, those events ripple to the OutputProbes, triggering the listeners
    // above.
    for (int i = 0; i < internalInputs.size(); i++) {
      if (i < getInputCount()) {
        boolean val = getInput(i);
        internalInputs.get(i).toggle(val);
      }
    }

    // Removed: "Bridge Out" loop.
    // We no longer manually push outputs here, because they are not ready yet.
  }
}
