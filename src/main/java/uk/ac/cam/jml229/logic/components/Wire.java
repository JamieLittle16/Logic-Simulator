package uk.ac.cam.jml229.logic.components;

import java.util.ArrayList;

public class Wire {
  private boolean signal;
  private Component source;
  private ArrayList<PortConnection> destinations = new ArrayList<>();

  public Wire(Component source) {
    this.source = source;
    // Updates the input component forming the connection
    if (source != null) {
      source.setOutputWire(this);
    }
  }

  public static class PortConnection {
    public Component component;
    public int inputIndex;

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
    // Updates all the gates connected
    for (PortConnection pc : destinations) {
      pc.component.setInput(pc.inputIndex, signal);
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

  public ArrayList<PortConnection> getDestinations() {
    return destinations;
  }
}
