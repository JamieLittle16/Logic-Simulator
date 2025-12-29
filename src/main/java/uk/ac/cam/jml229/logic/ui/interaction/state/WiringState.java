package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.Point;
import java.awt.event.MouseEvent;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.core.Wire;
import uk.ac.cam.jml229.logic.ui.interaction.CircuitInteraction;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WireSegment;

public class WiringState implements InteractionState {

  private final CircuitInteraction ctx;

  public WiringState(CircuitInteraction ctx, Pin startPin, Point initialMousePt) {
    this.ctx = ctx;
    ctx.connectionStartPin = startPin;
    ctx.currentMousePoint = initialMousePt;
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    ctx.updateHoverState(e);
    ctx.currentMousePoint = ctx.getWorldPoint(e);
    ctx.getPanel().repaint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    mouseMoved(e);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // --- Drag-to-Connect Logic ---
    // If the user releases the mouse over a valid target (that isn't the start
    // pin),
    // we complete the connection immediately.

    Point worldPt = ctx.getWorldPoint(e);

    // Try Connecting to Pin
    Pin endPin = ctx.getHitTester().findPinAt(worldPt);
    if (endPin != null && endPin != ctx.connectionStartPin) {
      if (connectToPin(endPin))
        finish();
      return;
    }

    // Try Connecting to Wire (T-Junction)
    WireSegment seg = ctx.getHitTester().findWireAt(worldPt);
    if (seg != null && ctx.connectionStartPin.isInput()) {
      connectTunction(seg, worldPt);
      finish();
      return;
    }

  }

  @Override
  public void mousePressed(MouseEvent e) {
    // --- Click-Click Logic (Second Click) ---
    Point worldPt = ctx.getWorldPoint(e);

    // Clicked a Pin
    Pin endPin = ctx.getHitTester().findPinAt(worldPt);
    if (endPin != null) {
      // If clicking a different pin, try connect
      if (endPin != ctx.connectionStartPin) {
        if (connectToPin(endPin))
          finish();
      }
      // If clicking start pin again, ignore (or could cancel)
      return;
    }

    // Clicked a Wire
    WireSegment seg = ctx.getHitTester().findWireAt(worldPt);
    if (seg != null && ctx.connectionStartPin.isInput()) {
      connectTunction(seg, worldPt);
      finish();
      return;
    }

    // Clicked Empty Space -> Cancel Wiring
    finish();
  }

  // --- Helper Methods to reduce duplication ---

  private boolean connectToPin(Pin endPin) {
    Pin start = ctx.connectionStartPin;
    // Only connect Input <-> Output
    if (start.isInput() != endPin.isInput()) {
      ctx.saveHistory();
      Pin source = start.isInput() ? endPin : start;
      Pin dest = start.isInput() ? start : endPin;
      ctx.getCircuit().addConnection(source.component(), source.index(), dest.component(), dest.index());
      return true;
    }
    return false;
  }

  private void connectTunction(WireSegment seg, Point pt) {
    Wire w = seg.wire();
    Component source = w.getSource();
    if (source == null)
      return;

    // Find which output index of source owns this wire
    int srcIdx = -1;
    for (int i = 0; i < source.getOutputCount(); i++) {
      if (source.getOutputWire(i) == w) {
        srcIdx = i;
        break;
      }
    }
    if (srcIdx == -1)
      return;

    ctx.saveHistory();

    // Insert a waypoint at the T-junction point
    int idx = ctx.getHitTester().getWaypointInsertionIndex(seg, pt);
    seg.connection().waypoints.add(idx, new Point(pt));

    // Create the new connection
    boolean ok = ctx.getCircuit().addConnection(source, srcIdx, ctx.connectionStartPin.component(),
        ctx.connectionStartPin.index());

    // If successful, copy the path from source up to the T-junction
    if (ok) {
      for (Wire.PortConnection pc : w.getDestinations()) {
        if (pc.component == ctx.connectionStartPin.component() && pc.inputIndex == ctx.connectionStartPin.index()) {
          for (int k = 0; k <= idx; k++)
            pc.waypoints.add(new Point(seg.connection().waypoints.get(k)));
          break;
        }
      }
    }
  }

  private void finish() {
    ctx.connectionStartPin = null;
    ctx.setPreventNextClick(true); // Prevent accidental interactions (like toggling switches) immediately after
    ctx.setState(new IdleState(ctx));
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void keyPressed(java.awt.event.KeyEvent e) {
    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)
      finish();
  }
}
