package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.IdentityHashMap;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.interaction.CircuitInteraction;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;

public class DraggingState implements InteractionState {

  private final CircuitInteraction ctx;
  private final Point startPt;

  private final Map<Component, Point> initialCompPositions = new HashMap<>();

  private final Map<Point, Point> initialWaypointPositions = new IdentityHashMap<>();

  private boolean hasDragged = false;

  public DraggingState(CircuitInteraction ctx, Point startPt) {
    this.ctx = ctx;
    this.startPt = startPt;

    for (Component c : ctx.getSelectedComponents()) {
      initialCompPositions.put(c, new Point(c.getX(), c.getY()));
    }

    for (WaypointRef wp : ctx.getSelectedWaypoints()) {
      initialWaypointPositions.put(wp.point(), new Point(wp.point().x, wp.point().y));
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (!hasDragged) {
      ctx.saveHistory();
      hasDragged = true;
    }

    Point current = ctx.getWorldPoint(e);
    int dx = current.x - startPt.x;
    int dy = current.y - startPt.y;

    // Move Components
    int gridDx = dx;
    int gridDy = dy;
    if (ctx.isSnapToGrid()) {
      gridDx = Math.round(dx / 20.0f) * 20;
      gridDy = Math.round(dy / 20.0f) * 20;
    }
    for (Map.Entry<Component, Point> entry : initialCompPositions.entrySet()) {
      Point initial = entry.getValue();
      entry.getKey().setPosition(initial.x + gridDx, initial.y + gridDy);
    }

    // Move Waypoints
    for (WaypointRef wp : ctx.getSelectedWaypoints()) {
      Point initial = initialWaypointPositions.get(wp.point());
      if (initial == null)
        continue;

      Point pt = wp.point();
      int targetX = initial.x + dx;
      int targetY = initial.y + dy;

      if (ctx.isSnapToGrid()) {
        pt.x = Math.round(targetX / 20.0f) * 20;
        pt.y = Math.round(targetY / 20.0f) * 20;
      } else {
        // Set target first
        pt.setLocation(targetX, targetY);

        // --- Magnetic Snap Logic ---
        List<Point> points = wp.connection().waypoints;
        int index = points.indexOf(pt);
        Point prev = null;
        Point next = null;
        Wire w = getWireForConnection(wp.connection());

        if (index > 0) {
          prev = points.get(index - 1);
        } else if (w != null) {
          Component src = w.getSource();
          if (src != null) {
            int srcIdx = 0;
            for (int i = 0; i < src.getOutputCount(); i++)
              if (src.getOutputWire(i) == w)
                srcIdx = i;
            prev = ctx.getRenderer().getPinLocation(src, false, srcIdx);
          }
        }

        if (index < points.size() - 1) {
          next = points.get(index + 1);
        } else {
          next = ctx.getRenderer().getPinLocation(wp.connection().component, true,
              wp.connection().inputIndex);
        }

        int snapDist = 15;
        if (prev != null) {
          if (Math.abs(pt.x - prev.x) < snapDist)
            pt.x = prev.x;
          if (Math.abs(pt.y - prev.y) < snapDist)
            pt.y = prev.y;
        }
        if (next != null) {
          if (Math.abs(pt.x - next.x) < snapDist)
            pt.x = next.x;
          if (Math.abs(pt.y - next.y) < snapDist)
            pt.y = next.y;
        }
      }
    }

    ctx.getPanel().repaint();
  }

  private Wire getWireForConnection(Wire.PortConnection pc) {
    for (Wire w : ctx.getCircuit().getWires())
      if (w.getDestinations().contains(pc))
        return w;
    return null;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    ctx.setState(new IdleState(ctx));
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void keyPressed(java.awt.event.KeyEvent e) {
  }
}
