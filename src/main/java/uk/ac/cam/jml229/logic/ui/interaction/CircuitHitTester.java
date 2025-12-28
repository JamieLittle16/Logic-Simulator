package uk.ac.cam.jml229.logic.ui.interaction;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WireSegment;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer;

public class CircuitHitTester {

  private Circuit circuit;
  private final CircuitRenderer renderer;

  public CircuitHitTester(Circuit circuit, CircuitRenderer renderer) {
    this.circuit = circuit;
    this.renderer = renderer;
  }

  public void setCircuit(Circuit c) {
    this.circuit = c;
  }

  public Pin findPinAt(Point p) {
    int threshold = CircuitRenderer.PIN_SIZE + 4;
    for (Component c : circuit.getComponents()) {
      int outCount = c.getOutputCount();
      for (int i = 0; i < outCount; i++) {
        Point outLoc = renderer.getPinLocation(c, false, i);
        if (p.distance(outLoc) <= threshold)
          return new Pin(c, i, false, outLoc);
      }
      int inputCount = renderer.getInputCount(c);
      for (int i = 0; i < inputCount; i++) {
        Point inLoc = renderer.getPinLocation(c, true, i);
        if (p.distance(inLoc) <= threshold)
          return new Pin(c, i, true, inLoc);
      }
    }
    return null;
  }

  public WaypointRef findWaypointAt(Point p) {
    int hitSize = 8;
    for (Wire w : circuit.getWires()) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        for (Point pt : pc.waypoints) {
          if (p.distance(pt) <= hitSize) {
            return new WaypointRef(pc, pt);
          }
        }
      }
    }
    return null;
  }

  public WireSegment findWireAt(Point p) {
    int hitThreshold = 5;
    for (Wire w : circuit.getWires()) {
      Component src = w.getSource();
      if (src == null)
        continue;

      int outputIndex = 0;
      for (int i = 0; i < src.getOutputCount(); i++) {
        if (src.getOutputWire(i) == w) {
          outputIndex = i;
          break;
        }
      }
      Point p1 = renderer.getPinLocation(src, false, outputIndex);

      for (Wire.PortConnection pc : w.getDestinations()) {
        Point p2 = renderer.getPinLocation(pc.component, true, pc.inputIndex);
        Shape path = renderer.createWireShape(p1, p2, pc.waypoints);
        Shape strokedShape = new BasicStroke(hitThreshold).createStrokedShape(path);

        if (strokedShape.contains(p))
          return new WireSegment(w, pc);
      }
    }
    return null;
  }

  public Component findComponentAt(Point p) {
    List<Component> comps = circuit.getComponents();
    // Iterate in reverse order (Top-most component first)
    for (int i = comps.size() - 1; i >= 0; i--) {
      Component c = comps.get(i);

      // Get the rotated bounds from the renderer/painter
      Rectangle bounds = renderer.getComponentBounds(c);

      if (bounds.contains(p)) {
        return c;
      }
    }
    return null;
  }

  public int getWaypointInsertionIndex(WireSegment ws, Point clickPt) {
    Wire.PortConnection pc = ws.connection();
    Wire w = ws.wire();
    List<Point> waypoints = pc.waypoints;

    Component src = w.getSource();
    int srcIdx = 0;
    for (int i = 0; i < src.getOutputCount(); i++)
      if (src.getOutputWire(i) == w)
        srcIdx = i;
    Point start = renderer.getPinLocation(src, false, srcIdx);
    Point end = renderer.getPinLocation(pc.component, true, pc.inputIndex);

    List<Point> fullPath = new ArrayList<>();
    fullPath.add(start);
    fullPath.addAll(waypoints);
    fullPath.add(end);

    for (int i = 0; i < fullPath.size() - 1; i++) {
      Point p1 = fullPath.get(i);
      Point p2 = fullPath.get(i + 1);

      GeneralPath segmentPath = new GeneralPath();
      segmentPath.moveTo(p1.x, p1.y);
      double dist = Math.abs(p2.x - p1.x) * 0.5;
      segmentPath.curveTo(p1.x + dist, p1.y, p2.x - dist, p2.y, p2.x, p2.y);

      Shape stroked = new BasicStroke(7).createStrokedShape(segmentPath);
      if (stroked.contains(clickPt)) {
        return i;
      }
    }
    return waypoints.size();
  }
}
