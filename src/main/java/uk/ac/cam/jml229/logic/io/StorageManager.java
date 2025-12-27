package uk.ac.cam.jml229.logic.io;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.Point;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;
import uk.ac.cam.jml229.logic.model.Wire;

public class StorageManager {

  private static final int CURRENT_VERSION = 1;

  public static void save(File file, Circuit circuit, List<Component> paletteTools) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      writer.println("LOGIK_VERSION " + CURRENT_VERSION);
      writer.println("# Logik Simulator Save File (.lgk)");
      writer.println("# Format: COMP [TYPE] [ID] [X] [Y] [LABEL]");
      writer.println();

      // 1. Save Custom Component Definitions (Blueprints)
      Set<String> savedDefs = new HashSet<>();

      for (Component tool : paletteTools) {
        if (tool instanceof CustomComponent) {
          saveCustomDefinition(writer, (CustomComponent) tool, savedDefs);
        }
      }
      // Check board for custom instances
      for (Component c : circuit.getComponents()) {
        if (c instanceof CustomComponent) {
          saveCustomDefinition(writer, (CustomComponent) c, savedDefs);
        }
      }

      // 2. Save Main Circuit
      writer.println("SECTION MAIN");
      saveCircuit(writer, circuit);
    }
  }

  private static void saveCustomDefinition(PrintWriter writer, CustomComponent cc, Set<String> savedDefs) {
    if (savedDefs.contains(cc.getName()))
      return;

    writer.println("DEF \"" + cc.getName() + "\"");
    saveCircuit(writer, cc.getInnerCircuit());
    writer.println("ENDDEF");
    writer.println();

    savedDefs.add(cc.getName());
  }

  private static void saveCircuit(PrintWriter writer, Circuit circuit) {
    Map<Component, Integer> idMap = new HashMap<>();
    int idCounter = 0;

    // Write Components
    for (Component c : circuit.getComponents()) {
      int id = idCounter++;
      idMap.put(c, id);

      String type = getComponentType(c);
      String extra = "";
      if (c instanceof CustomComponent)
        extra = " \"" + c.getName() + "\"";

      writer.printf("COMP %s %d %d %d%s%n", type, id, c.getX(), c.getY(), extra);
    }

    // Write Wires
    for (Wire w : circuit.getWires()) {
      Component src = w.getSource();
      if (src == null || !idMap.containsKey(src))
        continue;

      int srcId = idMap.get(src);
      int srcIdx = getOutputIndex(src, w);

      for (Wire.PortConnection pc : w.getDestinations()) {
        if (!idMap.containsKey(pc.component))
          continue;
        int dstId = idMap.get(pc.component);

        writer.printf("WIRE %d:%d %d:%d", srcId, srcIdx, dstId, pc.inputIndex);

        if (!pc.waypoints.isEmpty()) {
          writer.print(" [");
          for (int i = 0; i < pc.waypoints.size(); i++) {
            Point p = pc.waypoints.get(i);
            writer.print(p.x + "," + p.y + (i < pc.waypoints.size() - 1 ? " " : ""));
          }
          writer.print("]");
        }
        writer.println();
      }
    }
  }

  public static LoadResult load(File file) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;

    int version = 0;
    Circuit mainCircuit = new Circuit();
    Map<String, CustomComponent> prototypes = new HashMap<>();

    Circuit currentCircuit = null;
    String currentDefName = null;
    Map<Integer, Component> idMap = new HashMap<>();

    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
        continue;

      String[] parts = line.split("\\s+");
      String cmd = parts[0];

      switch (cmd) {
        case "LOGIK_VERSION":
          version = Integer.parseInt(parts[1]);
          if (version > CURRENT_VERSION)
            throw new IOException("File version " + version + " is newer than supported (" + CURRENT_VERSION + ")");
          break;

        case "DEF":
          currentDefName = parseString(line.substring(4));
          currentCircuit = new Circuit();
          idMap.clear();
          break;

        case "ENDDEF":
          if (currentDefName != null && currentCircuit != null) {
            CustomComponent proto = new CustomComponent(currentDefName, currentCircuit);
            prototypes.put(currentDefName, proto);
          }
          currentCircuit = null;
          idMap.clear();
          break;

        case "SECTION":
          if (parts[1].equals("MAIN")) {
            currentCircuit = mainCircuit;
            idMap.clear();
          }
          break;

        case "COMP":
          if (currentCircuit == null)
            continue;
          parseComponent(parts, currentCircuit, idMap, prototypes);
          break;

        case "WIRE":
          if (currentCircuit == null)
            continue;
          parseWire(line, currentCircuit, idMap);
          break;
      }
    }

    return new LoadResult(mainCircuit, new ArrayList<>(prototypes.values()));
  }

  // --- Parsing Helpers ---

  private static void parseComponent(String[] parts, Circuit circuit, Map<Integer, Component> idMap,
      Map<String, CustomComponent> prototypes) throws IOException {
    String type = parts[1];
    int id = Integer.parseInt(parts[2]);
    int x = Integer.parseInt(parts[3]);
    int y = Integer.parseInt(parts[4]);

    Component c = null;
    switch (type) {
      case "AND":
        c = new AndGate("AND");
        break;
      case "OR":
        c = new OrGate("OR");
        break;
      case "NOT":
        c = new NotGate("NOT");
        break;
      case "NAND":
        c = new NandGate("NAND");
        break;
      case "NOR":
        c = new NorGate("NOR");
        break;
      case "XOR":
        c = new XorGate("XOR");
        break;
      case "SWITCH":
        c = new Switch("SW");
        break;
      case "LIGHT":
        c = new OutputProbe("OUT");
        break;
      case "BUFFER":
        c = new BufferGate("BUF");
        break;
      case "CUSTOM":
        String name = parseString(parts[5]);
        if (prototypes.containsKey(name)) {
          c = prototypes.get(name).makeCopy();
        } else {
          System.err.println("Warning: Custom definition '" + name + "' not found.");
        }
        break;
    }

    if (c != null) {
      c.setPosition(x, y);
      circuit.addComponent(c);
      idMap.put(id, c);
    }
  }

  private static void parseWire(String line, Circuit circuit, Map<Integer, Component> idMap) {
    try {
      Pattern p = Pattern.compile("WIRE (\\d+):(\\d+) (\\d+):(\\d+)(?: \\[(.*)\\])?");
      Matcher m = p.matcher(line);
      if (m.find()) {
        int srcId = Integer.parseInt(m.group(1));
        int srcIdx = Integer.parseInt(m.group(2));
        int dstId = Integer.parseInt(m.group(3));
        int dstIdx = Integer.parseInt(m.group(4));
        String waypointStr = m.group(5);

        Component src = idMap.get(srcId);
        Component dst = idMap.get(dstId);

        if (src != null && dst != null) {
          circuit.addConnection(src, srcIdx, dst, dstIdx);

          if (waypointStr != null && !waypointStr.isEmpty()) {
            Wire w = src.getOutputWire(srcIdx);
            if (w != null) {
              for (Wire.PortConnection pc : w.getDestinations()) {
                if (pc.component == dst && pc.inputIndex == dstIdx) {
                  for (String pt : waypointStr.split(" ")) {
                    String[] coords = pt.split(",");
                    pc.waypoints.add(new Point(
                        Integer.parseInt(coords[0]),
                        Integer.parseInt(coords[1])));
                  }
                  break;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error parsing wire: " + line);
    }
  }

  private static String parseString(String s) {
    if (s.startsWith("\"") && s.endsWith("\""))
      return s.substring(1, s.length() - 1);
    return s;
  }

  private static String getComponentType(Component c) {
    if (c instanceof AndGate)
      return "AND";
    if (c instanceof OrGate)
      return "OR";
    if (c instanceof NotGate)
      return "NOT";
    if (c instanceof NandGate)
      return "NAND";
    if (c instanceof NorGate)
      return "NOR";
    if (c instanceof XorGate)
      return "XOR";
    if (c instanceof Switch)
      return "SWITCH";
    if (c instanceof OutputProbe)
      return "LIGHT";
    if (c instanceof BufferGate)
      return "BUFFER";
    if (c instanceof CustomComponent)
      return "CUSTOM";
    return "UNKNOWN";
  }

  private static int getOutputIndex(Component c, Wire w) {
    for (int i = 0; i < c.getOutputCount(); i++) {
      if (c.getOutputWire(i) == w)
        return i;
    }
    return -1;
  }

  public record LoadResult(Circuit circuit, List<CustomComponent> customTools) {
  }
}
