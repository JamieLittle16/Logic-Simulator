package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import uk.ac.cam.jml229.logic.components.*;

public class GuiMain {
  public static void main(String[] args) {
    JFrame frame = new JFrame("Logic Simulator");
    CircuitPanel panel = new CircuitPanel();

    // 1. Create Components
    // Switches (Left side)
    Switch swA = new Switch("A");
    swA.setPosition(50, 100); // x=50, y=100

    Switch swB = new Switch("B");
    swB.setPosition(50, 200); // x=50, y=200

    // Gates (Middle)
    XorGate xor = new XorGate("XOR");
    xor.setPosition(250, 100); // Middle top

    AndGate and = new AndGate("AND");
    and.setPosition(250, 200); // Middle bottom

    // Lights (Right side)
    OutputProbe sumLight = new OutputProbe("Sum");
    sumLight.setPosition(450, 100);

    OutputProbe carryLight = new OutputProbe("Carry");
    carryLight.setPosition(450, 200);

    // 2. Wire them up
    // Connect Switch A -> XOR(0) and AND(0)
    Wire wA = new Wire(swA);
    wA.addDestination(xor, 0);
    wA.addDestination(and, 0);

    // Connect Switch B -> XOR(1) and AND(1)
    Wire wB = new Wire(swB);
    wB.addDestination(xor, 1);
    wB.addDestination(and, 1);

    // Connect XOR -> Sum Light
    Wire wSum = new Wire(xor);
    wSum.addDestination(sumLight, 0);

    // Connect AND -> Carry Light
    Wire wCarry = new Wire(and);
    wCarry.addDestination(carryLight, 0);

    // 3. Add EVERYTHING to the panel
    // (Note: The panel automatically finds the wires attached to these components)
    panel.addComponent(swA);
    panel.addComponent(swB);
    panel.addComponent(xor);
    panel.addComponent(and);
    panel.addComponent(sumLight);
    panel.addComponent(carryLight);

    // 4. Show Window
    frame.add(panel);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null); // Center on screen
    frame.setVisible(true);
  }
}
