package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.io.Switch;
import uk.ac.cam.jml229.logic.ui.interaction.*;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WireSegment;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;

public class IdleState implements InteractionState {

  protected final CircuitInteraction ctx;

  // Panning State
  private boolean isPanning = false;
  private Point panStartScreen;
  private Point2D.Double panStartOffset;

  public IdleState(CircuitInteraction ctx) {
    this.ctx = ctx;
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    ctx.updateHoverState(e);
    ctx.getPanel().repaint();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    ctx.getPanel().requestFocusInWindow();
    Point worldPt = ctx.getWorldPoint(e);

    // Pan
    if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown())) {
      isPanning = true;
      panStartScreen = e.getPoint();
      panStartOffset = new Point2D.Double(ctx.getPanel().getPanX(), ctx.getPanel().getPanY());
      ctx.getPanel().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      return;
    }

    // Right Click
    if (SwingUtilities.isRightMouseButton(e)) {
      if (!ctx.getSelection().isEmpty())
        showContextMenu(e.getX(), e.getY());
      return;
    }

    // Hit Testing
    Pin pin = ctx.getHitTester().findPinAt(worldPt);
    if (pin != null) {
      ctx.setState(new WiringState(ctx, pin, worldPt)); // Pass worldPt for immediate preview
      return;
    }

    WaypointRef wp = ctx.getHitTester().findWaypointAt(worldPt);
    if (wp != null) {
      ctx.clearSelection();
      ctx.getSelectedWaypoints().add(wp);
      ctx.setState(new DraggingState(ctx, worldPt));
      return;
    }

    WireSegment wireSeg = ctx.getHitTester().findWireAt(worldPt);
    if (wireSeg != null) {
      if (ctx.getSelectedWire() != null && ctx.getSelectedWire().wire() == wireSeg.wire()) {
        ctx.saveHistory();
        int idx = ctx.getHitTester().getWaypointInsertionIndex(wireSeg, worldPt);
        wireSeg.connection().waypoints.add(idx, worldPt);
        WaypointRef newWp = new WaypointRef(wireSeg.connection(), worldPt);
        ctx.clearSelection();
        ctx.getSelectedWaypoints().add(newWp);
        ctx.setState(new DraggingState(ctx, worldPt));
      } else {
        ctx.clearSelection();
        ctx.setSelectedWire(wireSeg);
      }
      ctx.getPanel().repaint();
      return;
    }

    Component c = ctx.getHitTester().findComponentAt(worldPt);
    if (c != null) {
      handleSelection(e, c);
      ctx.setState(new DraggingState(ctx, worldPt));
      return;
    }

    // Empty Space
    ctx.clearSelection();
    ctx.setState(new SelectionState(ctx, worldPt));
  }

  private void handleSelection(MouseEvent e, Component c) {
    if (e.isShiftDown()) {
      if (ctx.getSelection().contains(c))
        ctx.removeFromSelection(c);
      else
        ctx.addToSelection(c);
    } else {
      if (!ctx.getSelection().contains(c)) {
        ctx.clearSelection();
        ctx.addToSelection(c);
      }
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (isPanning) {
      int dx = e.getX() - panStartScreen.x;
      int dy = e.getY() - panStartScreen.y;
      ctx.getPanel().setPan(panStartOffset.x + dx, panStartOffset.y + dy);
      ctx.getPanel().repaint();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (isPanning) {
      isPanning = false;
      ctx.getPanel().setCursor(Cursor.getDefaultCursor());
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // Prevent clicking if just dragged or placed
    if (ctx.shouldPreventNextClick())
      return;

    Point worldPt = ctx.getWorldPoint(e);
    Component c = ctx.getHitTester().findComponentAt(worldPt);

    if (c != null) {
      if (c instanceof Switch) {
        ((Switch) c).toggle(!((Switch) c).getState());
        ctx.getPanel().repaint();
      } else if (e.getClickCount() == 2) {
        renameComponent(c);
      }
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
      ctx.deleteSelection();
    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_X)
      ctx.cut();
    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C)
      ctx.copy();
    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V)
      ctx.paste();
    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z)
      ctx.undo();
    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y)
      ctx.redo();
    if (e.getKeyCode() == KeyEvent.VK_R)
      ctx.rotateSelection();
  }

  private void renameComponent(Component c) {
    String newName = JOptionPane.showInputDialog(ctx.getPanel(), "Rename:", c.getName());
    if (newName != null && !newName.trim().isEmpty()) {
      if (newName.length() > 8)
        newName = newName.substring(0, 8);
      c.setName(newName);
      ctx.getPanel().repaint();
    }
  }

  private void showContextMenu(int x, int y) {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem rename = new JMenuItem("Rename");
    rename.addActionListener(e -> {
      if (!ctx.getSelection().isEmpty())
        renameComponent(ctx.getSelection().get(0));
    });
    JMenuItem delete = new JMenuItem("Delete");
    delete.addActionListener(e -> ctx.deleteSelection());

    menu.add(rename);
    menu.add(delete);
    menu.show(ctx.getPanel(), x, y);
  }
}
