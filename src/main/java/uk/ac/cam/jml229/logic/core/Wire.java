package uk.ac.cam.jml229.logic.core;

import java.util.ArrayList;
import java.util.List;
import java.awt.Point;
import uk.ac.cam.jml229.logic.components.Component;

public class Wire {
  private boolean signal;
  private Component source;
  private List<PortConnection> destinations = new ArrayList<>();

  public Wire(Component source) {
    this.source = source;
  }

  public static class PortConnection {
    public Component component;
    public int inputIndex;
    public final List<Point> waypoints = new ArrayList<>();

    PortConnection(Component c, int i) {
      component = c;
      inputIndex = i;
    }
  }

  public boolean getSignal() {
    return signal;
  }

  public void setSignal(boolean newSignal) {
    // Ensures it only updates if a change has occurred
    if (signal == newSignal) {
      return;
    }
    signal = newSignal;

    for (PortConnection pc : destinations) {
      // Capture the state and the target in a lambda
      Simulator.enqueue(() -> {
        pc.component.setInput(pc.inputIndex, signal);
      });
    }
  }

  public void setSource(Component c) {
    source = c;
  }

  public Component getSource() {
    return source;
  }

  public void addDestination(Component c, int inputIndex) {
    destinations.add(new PortConnection(c, inputIndex));
  }

  public void removeDestination(Component c, int inputIndex) {
    destinations.removeIf(connection -> connection.component == c && connection.inputIndex == inputIndex);
  }

  public List<PortConnection> getDestinations() {
    return destinations;
  }
}
