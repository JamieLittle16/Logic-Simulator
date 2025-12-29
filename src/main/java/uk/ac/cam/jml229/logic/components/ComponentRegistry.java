package uk.ac.cam.jml229.logic.components;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import uk.ac.cam.jml229.logic.components.gates.*;
import uk.ac.cam.jml229.logic.components.io.*;
import uk.ac.cam.jml229.logic.components.seq.*;
import uk.ac.cam.jml229.logic.components.misc.TextLabel; // Import the new component

/**
 * Central "Source of Truth" for all standard component types.
 * Handles mapping between Save IDs, Display Names, and Java Classes.
 */
public enum ComponentRegistry {

  // --- IO / Probes ---
  SWITCH("SWITCH", "Switch", "IO / Probes", () -> new Switch("SW")),
  LIGHT("LIGHT", "Light", "IO / Probes", () -> new OutputProbe("Out")),
  SEVEN_SEG("SEVEN_SEG", "7Seg", "IO / Probes", () -> new SevenSegmentDisplay("7Seg")),
  HEX("HEX", "Hex", "IO / Probes", () -> new HexDisplay("Hex")),

  // --- Basic Gates ---
  AND("AND", "AND", "Basic Gates", () -> new AndGate("AND")),
  OR("OR", "OR", "Basic Gates", () -> new OrGate("OR")),
  NOT("NOT", "NOT", "Basic Gates", () -> new NotGate("NOT")),

  // --- Advanced Gates ---
  NAND("NAND", "NAND", "Advanced", () -> new NandGate("NAND")),
  NOR("NOR", "NOR", "Advanced", () -> new NorGate("NOR")),
  XOR("XOR", "XOR", "Advanced", () -> new XorGate("XOR")),
  BUFFER("BUFFER", "BUFF", "Advanced", () -> new BufferGate("BUF")),

  // --- Sequential ---
  CLOCK("CLOCK", "CLK", "Sequential", () -> new Clock("CLK")),
  D_FF("D_FF", "D-FF", "Sequential", () -> new DFlipFlop("D-FF")),
  JK_FF("JK_FF", "JK-FF", "Sequential", () -> new JKFlipFlop("JK-FF")),
  T_FF("T_FF", "T-FF", "Sequential", () -> new TFlipFlop("T-FF")),

  // --- Misc ---
  LABEL("LABEL", "Label", "Misc", () -> new TextLabel());

  private final String id;
  private final String displayName;
  private final String category;
  private final Supplier<Component> factory;

  ComponentRegistry(String id, String displayName, String category, Supplier<Component> factory) {
    this.id = id;
    this.displayName = displayName;
    this.category = category;
    this.factory = factory;
  }

  // --- Lookups ---

  public static Optional<ComponentRegistry> fromId(String id) {
    return Arrays.stream(values())
        .filter(t -> t.id.equals(id))
        .findFirst();
  }

  public static Optional<ComponentRegistry> fromComponent(Component c) {
    // Matches based on Class type
    return Arrays.stream(values())
        .filter(t -> t.factory.get().getClass().isInstance(c))
        .findFirst();
  }

  public Component createInstance() {
    return factory.get();
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getCategory() {
    return category;
  }

  // --- Category Helper ---
  public static Map<String, List<ComponentRegistry>> getByCategory() {
    // Returns a map ensuring order: IO -> Basic -> Advanced -> Sequential -> Misc
    Map<String, List<ComponentRegistry>> map = new LinkedHashMap<>();
    map.put("IO / Probes", new ArrayList<>());
    map.put("Basic Gates", new ArrayList<>());
    map.put("Advanced", new ArrayList<>());
    map.put("Sequential", new ArrayList<>());
    map.put("Misc", new ArrayList<>());

    for (ComponentRegistry type : values()) {
      map.computeIfAbsent(type.category, k -> new ArrayList<>()).add(type);
    }
    return map;
  }
}
