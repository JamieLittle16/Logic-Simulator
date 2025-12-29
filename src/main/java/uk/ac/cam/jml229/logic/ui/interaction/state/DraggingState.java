package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.ui.interaction.CircuitInteraction;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;

public class DraggingState implements InteractionState {

  private final CircuitInteraction ctx;
  private final Point startPt;

  // Store initial positions for relative movement
  private final Map<Component, Point> initialCompPositions = new HashMap<>();
  private final Map<Point, Point> initialWaypointPositions = new HashMap<>();

  private boolean hasDragged = false;

  public DraggingState(CircuitInteraction ctx, Point startPt) {
    this.ctx = ctx;
    this.startPt = startPt;

    // Snapshot Component positions
    for (Component c : ctx.getSelectedComponents()) {
      initialCompPositions.put(c, new Point(c.getX(), c.getY()));
    }

    // Snapshot Waypoint positions
    for (WaypointRef wp : ctx.getSelectedWaypoints()) {
      // Key is the actual point object in the model, Value is a copy of its
      // coordinates
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

    if (ctx.isSnapToGrid()) {
      dx = Math.round(dx / 20.0f) * 20;
      dy = Math.round(dy / 20.0f) * 20;
    }

    // Move Components relative to start
    for (Map.Entry<Component, Point> entry : initialCompPositions.entrySet()) {
      Point initial = entry.getValue();
      entry.getKey().setPosition(initial.x + dx, initial.y + dy);
    }

    // Move Waypoints relative to start
    for (Map.Entry<Point, Point> entry : initialWaypointPositions.entrySet()) {
      Point initial = entry.getValue();
      Point target = entry.getKey(); // The actual Point object in the circuit

      target.setLocation(initial.x + dx, initial.y + dy);
    }

    ctx.getPanel().repaint();
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
