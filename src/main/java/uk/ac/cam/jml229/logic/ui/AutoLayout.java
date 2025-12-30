package uk.ac.cam.jml229.logic.ui;

import java.awt.*;
import java.util.*;
import java.util.List;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.render.ComponentPainter;

public class AutoLayout {

  private static final int GRID_SIZE = 20; // 20px grid
  private static final int ATTEMPTS = 100; // Physics iterations

  public static void organize(Circuit circuit) {
    // Untangle Components (Force-Directed Layout)
    untangleComponents(circuit);

    // Route Wires (Orthogonal A* Search)
    routeWires(circuit);
  }

  // --- PHASE 1: Component Placement ---
  private static void untangleComponents(Circuit circuit) {
    List<Component> nodes = circuit.getComponents();
    if (nodes.isEmpty())
      return;

    for (int i = 0; i < ATTEMPTS; i++) {
      Map<Component, Point> forces = new HashMap<>();

      for (Component c : nodes) {
        double fx = 0, fy = 0;

        // Repulsion (Push everything apart)
        for (Component other : nodes) {
          if (c == other)
            continue;
          double dx = c.getX() - other.getX();
          double dy = c.getY() - other.getY();
          double distSq = dx * dx + dy * dy;
          if (distSq < 100)
            distSq = 100; // Clamp
          double force = 500000 / distSq; // Inverse square law
          double angle = Math.atan2(dy, dx);
          fx += Math.cos(angle) * force;
          fy += Math.sin(angle) * force;
        }

        // Attraction (Pull towards center to keep them roughly on screen)
        double dx = 0 - c.getX();
        double dy = 0 - c.getY();
        fx += dx * 0.05;
        fy += dy * 0.05;

        forces.put(c, new Point((int) fx, (int) fy));
      }

      // Apply forces
      for (Component c : nodes) {
        Point f = forces.get(c);
        int nx = c.getX() + Math.max(-20, Math.min(20, f.x));
        int ny = c.getY() + Math.max(-20, Math.min(20, f.y));

        // Snap to Grid
        nx = (nx / GRID_SIZE) * GRID_SIZE;
        ny = (ny / GRID_SIZE) * GRID_SIZE;

        c.setPosition(nx, ny);
      }
    }
  }

  // --- PHASE 2: Wire Routing (A*) ---
  private static void routeWires(Circuit circuit) {
    ComponentPainter painter = new ComponentPainter();
    List<Rectangle> obstacles = new ArrayList<>();

    // Build Obstacle Map (Components)
    for (Component c : circuit.getComponents()) {
      Rectangle r = painter.getComponentBounds(c);
      // Inflate slightly so wires don't touch the edges
      r.grow(10, 10);
      obstacles.add(r);
    }

    // Route each wire connection
    for (Wire w : circuit.getWires()) {
      Component src = w.getSource();
      if (src == null)
        continue;

      int sourceIndex = -1;
      for (int i = 0; i < src.getOutputCount(); i++) {
        if (src.getOutputWire(i) == w) {
          sourceIndex = i;
          break;
        }
      }
      if (sourceIndex == -1)
        continue;

      for (Wire.PortConnection pc : w.getDestinations()) {
        Component dst = pc.component;

        Point start = painter.getPinLocation(src, false, sourceIndex);
        Point end = painter.getPinLocation(dst, true, pc.inputIndex);

        List<Point> newPath = findPath(start, end, obstacles);

        pc.waypoints.clear();
        if (newPath.size() > 2) {
          // Exclude start and end points
          for (int i = 1; i < newPath.size() - 1; i++) {
            pc.waypoints.add(newPath.get(i));
          }
        }
      }
    }
  }

  // --- A* Pathfinding Logic ---
  private static List<Point> findPath(Point start, Point end, List<Rectangle> obstacles) {
    PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
    Map<Point, Node> allNodes = new HashMap<>();

    Node startNode = new Node(start, null, 0, manhattan(start, end));
    openSet.add(startNode);
    allNodes.put(start, startNode);

    int iterations = 0;
    while (!openSet.isEmpty() && iterations < 2000) { // Safety break
      Node current = openSet.poll();
      iterations++;

      if (current.pos.distance(end) < GRID_SIZE) {
        return reconstructPath(current, end);
      }

      int[][] dirs = { { 0, GRID_SIZE }, { 0, -GRID_SIZE }, { GRID_SIZE, 0 }, { -GRID_SIZE, 0 } };
      for (int[] d : dirs) {
        Point neighborPos = new Point(current.pos.x + d[0], current.pos.y + d[1]);

        if (isColliding(neighborPos, obstacles) && !isEndpoint(neighborPos, end)) {
          continue;
        }

        int moveCost = 10;
        if (current.parent != null) {
          Point prevDir = new Point(current.pos.x - current.parent.pos.x, current.pos.y - current.parent.pos.y);
          if (prevDir.x != d[0] || prevDir.y != d[1]) {
            moveCost += 5; // Penalty for turning
          }
        }

        int newG = current.g + moveCost;
        Node neighbor = allNodes.getOrDefault(neighborPos, new Node(neighborPos, null, Integer.MAX_VALUE, 0));

        if (newG < neighbor.g) {
          neighbor.parent = current;
          neighbor.g = newG;
          neighbor.f = newG + manhattan(neighborPos, end);
          if (!openSet.contains(neighbor)) {
            openSet.add(neighbor);
            allNodes.put(neighborPos, neighbor);
          }
        }
      }
    }
    // Fallback: Straight line if no path found
    List<Point> straight = new ArrayList<>();
    straight.add(start);
    straight.add(end);
    return straight;
  }

  private static int manhattan(Point a, Point b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
  }

  private static boolean isColliding(Point p, List<Rectangle> obstacles) {
    for (Rectangle r : obstacles) {
      if (r.contains(p))
        return true;
    }
    return false;
  }

  private static boolean isEndpoint(Point p, Point end) {
    return p.distance(end) < GRID_SIZE * 2;
  }

  private static List<Point> reconstructPath(Node node, Point endReal) {
    List<Point> path = new ArrayList<>();
    path.add(endReal);
    while (node != null) {
      path.add(0, node.pos);
      node = node.parent;
    }
    return path;
  }

  private static class Node {
    Point pos;
    Node parent;
    int g, f;

    Node(Point pos, Node parent, int g, int f) {
      this.pos = pos;
      this.parent = parent;
      this.g = g;
      this.f = f;
    }
  }
}
