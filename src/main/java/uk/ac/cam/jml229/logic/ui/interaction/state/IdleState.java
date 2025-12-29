package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;

import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.io.Switch;
import uk.ac.cam.jml229.logic.components.gates.LogicGate;
import uk.ac.cam.jml229.logic.components.misc.TextLabel;
import uk.ac.cam.jml229.logic.ui.interaction.*;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.Pin;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WireSegment;
import uk.ac.cam.jml229.logic.ui.render.CircuitRenderer.WaypointRef;
import uk.ac.cam.jml229.logic.app.Theme;

public class IdleState implements InteractionState {

  protected final CircuitInteraction ctx;

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

    if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown())) {
      isPanning = true;
      panStartScreen = e.getPoint();
      panStartOffset = new Point2D.Double(ctx.getPanel().getPanX(), ctx.getPanel().getPanY());
      ctx.getPanel().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      return;
    }

    if (SwingUtilities.isRightMouseButton(e)) {
      // Ensure right-click selects the item if it isn't already selected
      Component c = ctx.getHitTester().findComponentAt(worldPt);
      if (c != null && !ctx.getSelection().contains(c)) {
        if (!e.isShiftDown())
          ctx.clearSelection();
        ctx.addToSelection(c);
      }

      if (!ctx.getSelection().isEmpty())
        showContextMenu(e.getX(), e.getY());
      return;
    }

    Pin pin = ctx.getHitTester().findPinAt(worldPt);
    if (pin != null) {
      ctx.setState(new WiringState(ctx, pin, worldPt));
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

      // Allow Labels to have long text (30 chars), keep others short (8 chars)
      int maxLen = (c instanceof TextLabel) ? 30 : 8;

      if (newName.length() > maxLen) {
        newName = newName.substring(0, maxLen);
      }

      c.setName(newName);
      ctx.getPanel().repaint();
    }
  }

  private void showContextMenu(int x, int y) {
    JPopupMenu menu = new JPopupMenu();

    menu.setBackground(Theme.isDarkMode ? Theme.PALETTE_BACKGROUND : Color.WHITE);
    menu.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));

    // If exactly one component is selected and it is a LogicGate (AND, OR, etc.)
    if (ctx.getSelection().size() == 1 && ctx.getSelection().get(0) instanceof LogicGate) {
      LogicGate gate = (LogicGate) ctx.getSelection().get(0);

      JMenuItem setInputs = new JMenuItem("Set Inputs...");
      setInputs.addActionListener(e -> {
        String input = JOptionPane.showInputDialog(ctx.getPanel(),
            "Number of Inputs (2-32):",
            String.valueOf(gate.getInputCount()));

        if (input != null) {
          try {
            int n = Integer.parseInt(input);
            // Save history BEFORE changing state so Undo works
            ctx.saveHistory();
            gate.resizeInputs(n);
            ctx.getPanel().repaint();
          } catch (NumberFormatException ex) {
            // Ignore invalid numbers
          }
        }
      });
      menu.add(setInputs);
      menu.addSeparator();
    }

    JMenuItem createIC = new JMenuItem("Create Custom IC");
    createIC.addActionListener(e -> ctx.createCustomComponentFromSelection());
    menu.add(createIC);

    menu.addSeparator();

    JMenuItem cut = new JMenuItem("Cut");
    cut.addActionListener(e -> ctx.cut());
    menu.add(cut);

    JMenuItem copy = new JMenuItem("Copy");
    copy.addActionListener(e -> ctx.copy());
    menu.add(copy);

    menu.addSeparator();

    JMenuItem rename = new JMenuItem("Rename");
    rename.addActionListener(e -> {
      if (!ctx.getSelection().isEmpty())
        renameComponent(ctx.getSelection().get(0));
    });
    menu.add(rename);

    JMenuItem delete = new JMenuItem("Delete");
    delete.addActionListener(e -> ctx.deleteSelection());
    menu.add(delete);

    for (java.awt.Component c : menu.getComponents()) {
      if (c instanceof JMenuItem) {
        c.setBackground(menu.getBackground());
        c.setForeground(Theme.TEXT_COLOR);
      }
    }

    menu.show(ctx.getPanel(), x, y);
  }
}
