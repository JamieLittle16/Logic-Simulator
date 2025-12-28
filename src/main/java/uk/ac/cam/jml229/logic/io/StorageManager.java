package uk.ac.cam.jml229.logic.io;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.Point;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Wire;

public class StorageManager {

  private static final int CURRENT_VERSION = 3;

  public static void save(File file, Circuit circuit, List<Component> paletteTools) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
      writer.print(saveToString(circuit, paletteTools));
    }
  }

  public static LoadResult load(File file) throws IOException {
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null)
        content.append(line).append("\n");
    }
    return loadFromString(content.toString());
  }

  public static String saveToString(Circuit circuit, List<Component> paletteTools) {
    StringWriter sw = new StringWriter();
    PrintWriter writer = new PrintWriter(sw);

    writer.println("LOGIK_VERSION " + CURRENT_VERSION);
    writer.println("# Logik Snapshot");
    writer.println();

    Set<String> savedDefs = new HashSet<>();

    // Save Custom Definitions
    List<Component> allComps = new ArrayList<>();
    if (paletteTools != null)
      allComps.addAll(paletteTools);
    allComps.addAll(circuit.getComponents());

    for (Component c : allComps) {
      if (c instanceof CustomComponent) {
        saveCustomDefinition(writer, (CustomComponent) c, savedDefs);
      }
    }

    writer.println("SECTION MAIN");
    saveCircuit(writer, circuit);
    return sw.toString();
  }

  public static LoadResult loadFromString(String data) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(data));
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
        case "LOGIK_VERSION" -> {
          version = Integer.parseInt(parts[1]);
          if (version > CURRENT_VERSION)
            throw new IOException("Version " + version + " not supported");
        }
        case "DEF" -> {
          currentDefName = parseString(line.substring(4));
          currentCircuit = new Circuit();
          idMap.clear();
        }
        case "ENDDEF" -> {
          if (currentDefName != null && currentCircuit != null) {
            prototypes.put(currentDefName, new CustomComponent(currentDefName, currentCircuit));
          }
          currentCircuit = null;
          idMap.clear();
        }
        case "SECTION" -> {
          if (parts[1].equals("MAIN")) {
            currentCircuit = mainCircuit;
            idMap.clear();
          }
        }
        case "COMP" -> {
          if (currentCircuit != null)
            parseComponent(parts, currentCircuit, idMap, prototypes, version);
        }
        case "WIRE" -> {
          if (currentCircuit != null)
            parseWire(line, currentCircuit, idMap);
        }
      }
    }
    return new LoadResult(mainCircuit, new ArrayList<>(prototypes.values()));
  }

  // --- Helpers ---

  private static void saveCustomDefinition(PrintWriter writer, CustomComponent cc, Set<String> savedDefs) {
    if (savedDefs.contains(cc.getName()))
      return;
    writer.println("DEF \"" + cc.getName() + "\"");
    saveCircuit(writer, cc.getInnerCircuit());
    writer.println("ENDDEF\n");
    savedDefs.add(cc.getName());
  }

  private static void saveCircuit(PrintWriter writer, Circuit circuit) {
    Map<Component, Integer> idMap = new HashMap<>();
    int idCounter = 0;

    for (Component c : circuit.getComponents()) {
      int id = idCounter++;
      idMap.put(c, id);

      String type = "UNKNOWN";
      String extra = "";

      if (c instanceof CustomComponent) {
        type = "CUSTOM";
        extra = " \"" + c.getName() + "\"";
      } else {
        // --- UPDATED: Use Registry ---
        var entry = ComponentRegistry.fromComponent(c);
        if (entry.isPresent()) {
          type = entry.get().getId();
        }
      }

      writer.printf("COMP %s %d %d %d %d%s%n", type, id, c.getX(), c.getY(), c.getRotation(), extra);
    }

    for (Wire w : circuit.getWires()) {
      Component src = w.getSource();
      if (src == null || !idMap.containsKey(src))
        continue;
      int srcId = idMap.get(src);
      int srcIdx = getOutputIndex(src, w);

      for (Wire.PortConnection pc : w.getDestinations()) {
        if (!idMap.containsKey(pc.component))
          continue;
        writer.printf("WIRE %d:%d %d:%d", srcId, srcIdx, idMap.get(pc.component), pc.inputIndex);
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

  private static void parseComponent(String[] parts, Circuit circuit, Map<Integer, Component> idMap,
      Map<String, CustomComponent> prototypes, int fileVersion) {
    try {
      String type = parts[1];
      int id = Integer.parseInt(parts[2]);
      int x = Integer.parseInt(parts[3]);
      int y = Integer.parseInt(parts[4]);
      int rotation = (parts.length > 5) ? Integer.parseInt(parts[5]) : 0;
      int nameIdx = (fileVersion >= 2) ? 6 : 5;

      Component c = null;

      if (type.equals("CUSTOM")) {
        if (parts.length > nameIdx) {
          String name = parseString(parts[nameIdx]);
          if (prototypes.containsKey(name))
            c = prototypes.get(name).makeCopy();
        }
      } else {
        // --- UPDATED: Use Registry ---
        var entry = ComponentRegistry.fromId(type);
        if (entry.isPresent()) {
          c = entry.get().createInstance();
        }
      }

      if (c != null) {
        c.setPosition(x, y);
        c.setRotation(rotation);
        circuit.addComponent(c);
        idMap.put(id, c);
      }
    } catch (Exception e) {
      System.err.println("Error parsing component: " + Arrays.toString(parts));
    }
  }

  private static void parseWire(String line, Circuit circuit, Map<Integer, Component> idMap) {
    try {
      Pattern p = Pattern.compile("WIRE (\\d+):(\\d+) (\\d+):(\\d+)(?: \\[(.*)\\])?");
      Matcher m = p.matcher(line);
      if (m.find()) {
        Component src = idMap.get(Integer.parseInt(m.group(1)));
        int srcIdx = Integer.parseInt(m.group(2));
        Component dst = idMap.get(Integer.parseInt(m.group(3)));
        int dstIdx = Integer.parseInt(m.group(4));

        if (src != null && dst != null) {
          circuit.addConnection(src, srcIdx, dst, dstIdx);
          if (m.group(5) != null) {
            Wire w = src.getOutputWire(srcIdx);
            // Find connection and add waypoints
            for (var pc : w.getDestinations()) {
              if (pc.component == dst && pc.inputIndex == dstIdx) {
                for (String s : m.group(5).split(" ")) {
                  String[] xy = s.split(",");
                  pc.waypoints.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      /* ignore */ }
  }

  private static String parseString(String s) {
    if (s.startsWith("\"") && s.endsWith("\""))
      return s.substring(1, s.length() - 1);
    return s;
  }

  private static int getOutputIndex(Component c, Wire w) {
    for (int i = 0; i < c.getOutputCount(); i++)
      if (c.getOutputWire(i) == w)
        return i;
    return -1;
  }

  public record LoadResult(Circuit circuit, List<CustomComponent> customTools) {
  }
}
