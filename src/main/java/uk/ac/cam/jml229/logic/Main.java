package uk.ac.cam.jml229.logic;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.model.Wire;

public class Main {
  public static void main(String[] args) {
    // 1. Create Components
    Switch swA = new Switch("Switch A");
    Switch swB = new Switch("Switch B");

    XorGate xor = new XorGate("XOR");
    AndGate and = new AndGate("AND");

    OutputProbe sumLight = new OutputProbe("Sum (Light)");
    OutputProbe carryLight = new OutputProbe("Carry (Light)");

    // 2. Wire them up (The "Netlist")
    // Connect Switch A -> XOR(0) and AND(0)
    Wire wireA = new Wire(swA);
    wireA.addDestination(xor, 0);
    wireA.addDestination(and, 0);

    // Connect Switch B -> XOR(1) and AND(1)
    Wire wireB = new Wire(swB);
    wireB.addDestination(xor, 1);
    wireB.addDestination(and, 1);

    // Connect XOR -> Sum Light
    Wire wireSum = new Wire(xor);
    wireSum.addDestination(sumLight, 0);

    // Connect AND -> Carry Light
    Wire wireCarry = new Wire(and);
    wireCarry.addDestination(carryLight, 0);

    // 3. Run Simulation
    System.out.println("--- Test 1: 0 + 0 ---");
    swA.toggle(false);
    swB.toggle(false);

    System.out.println("\n--- Test 2: 1 + 0 ---");
    swA.toggle(true);

    System.out.println("\n--- Test 3: 1 + 1 ---");
    swB.toggle(true);
    // Expected: Sum OFF, Carry ON
  }
}
