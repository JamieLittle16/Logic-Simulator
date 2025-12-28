package uk.ac.cam.jml229.logic.ui.interaction.state;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.ui.interaction.CircuitInteraction;

public class PlacingState implements InteractionState {

  private final CircuitInteraction ctx;
  private Component ghost;
  private boolean isVisible = false;

  public PlacingState(CircuitInteraction ctx, Component template) {
    this.ctx = ctx;
    this.ghost = template.makeCopy();
    // Don't show ghost yet (it would be at 0,0).
    // Wait for first mouse move/entry.
    ctx.componentToPlace = null;
  }

  @Override
  public void onExit() {
    ctx.componentToPlace = null;
    ctx.getPanel().repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    updateGhostPosition(e);
    ctx.getPanel().repaint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    updateGhostPosition(e);
    ctx.getPanel().repaint();
  }

  private void updateGhostPosition(MouseEvent e) {
    // Once got a mouse event, we know where to put it
    if (!isVisible) {
      isVisible = true;
      ctx.componentToPlace = ghost;
    }

    Point p = ctx.getWorldPoint(e);
    if (ctx.isSnapToGrid()) {
      p.x = Math.round(p.x / 20.0f) * 20;
      p.y = Math.round(p.y / 20.0f) * 20;
    }
    ghost.setPosition(p.x, p.y);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (!isVisible)
      return; // Don't place if we haven't even entered the screen

    ctx.saveHistory();
    ctx.getCircuit().addComponent(ghost);

    // Prevent next click from triggering underlying components (like switches)
    ctx.setPreventNextClick(true);

    if (e.isControlDown()) {
      ghost = ghost.makeCopy();
      ctx.componentToPlace = ghost;
      updateGhostPosition(e);
    } else {
      ctx.setState(new IdleState(ctx));
    }
    ctx.getPanel().repaint();
  }

  public void rotate() {
    ghost.rotate();
    ctx.getPanel().repaint();
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      ctx.setState(new IdleState(ctx));
    }
    if (e.getKeyCode() == KeyEvent.VK_R) {
      rotate();
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }
}
