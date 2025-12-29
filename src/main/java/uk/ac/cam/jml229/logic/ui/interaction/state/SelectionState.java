package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.interaction.CircuitInteraction;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;

public class SelectionState implements InteractionState {

  private final CircuitInteraction ctx;
  private final Point startPt;

  public SelectionState(CircuitInteraction ctx, Point startPt) {
    this.ctx = ctx;
    this.startPt = startPt;
    ctx.selectionRect = new Rectangle(startPt.x, startPt.y, 0, 0);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    Point p = ctx.getWorldPoint(e);
    int x = Math.min(startPt.x, p.x);
    int y = Math.min(startPt.y, p.y);
    int w = Math.abs(p.x - startPt.x);
    int h = Math.abs(p.y - startPt.y);

    ctx.selectionRect.setBounds(x, y, w, h);
    ctx.getPanel().repaint();
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // Select Components in box
    for (Component c : ctx.getCircuit().getComponents()) {
      // Simple center-point check
      if (ctx.selectionRect.contains(c.getX() + 20, c.getY() + 20)) {
        ctx.addToSelection(c);
      }
    }

    // Select Waypoints in box
    for (Wire w : ctx.getCircuit().getWires()) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        for (Point wp : pc.waypoints) {
          if (ctx.selectionRect.contains(wp)) {
            ctx.getSelectedWaypoints().add(new WaypointRef(pc, wp));
          }
        }
      }
    }

    // Cleanup
    ctx.selectionRect = null;
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
