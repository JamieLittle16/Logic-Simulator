package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import uk.ac.cam.jml229.logic.model.Circuit;

public class CircuitPanel extends JPanel {

  private Circuit circuit;
  private final CircuitRenderer renderer;
  private final CircuitInteraction interaction;

  // Viewport State
  private double panX = 0;
  private double panY = 0;
  private double scale = 1.0;

  public CircuitPanel() {
    this.circuit = new Circuit();
    this.renderer = new CircuitRenderer();
    this.interaction = new CircuitInteraction(circuit, this, renderer);

    addMouseListener(interaction);
    addMouseMotionListener(interaction);
    addKeyListener(interaction);

    addMouseWheelListener(e -> {
      double zoomFactor = 1.1;
      double newScale = (e.getWheelRotation() < 0) ? scale * zoomFactor : scale / zoomFactor;

      // Clamp zoom (e.g., between 10% and 500%)
      newScale = Math.max(0.1, Math.min(newScale, 5.0));

      setZoom(newScale, e.getPoint());
    });

    setFocusable(true);
    setBackground(Color.WHITE);
  }

  public CircuitInteraction getInteraction() {
    return interaction;
  }

  public CircuitRenderer getRenderer() {
    return renderer;
  }

  public Circuit getCircuit() {
    return circuit;
  }

  // --- Viewport Getters ---
  public double getPanX() {
    return panX;
  }

  public double getPanY() {
    return panY;
  }

  public double getScale() {
    return scale;
  }

  public void setPan(double x, double y) {
    this.panX = x;
    this.panY = y;
    repaint();
  }

  public void setZoom(double newScale, Point anchor) {
    double worldX = (anchor.x - panX) / scale;
    double worldY = (anchor.y - panY) / scale;

    this.scale = newScale;

    // NewPan = ScreenPoint - (WorldPoint * NewScale)
    this.panX = anchor.x - (worldX * scale);
    this.panY = anchor.y - (worldY * scale);

    repaint();
  }

  public void setCircuit(Circuit newCircuit) {
    this.circuit = newCircuit;
    this.interaction.setCircuit(newCircuit);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    AffineTransform oldTransform = g2.getTransform();

    // Apply Transform: Translate THEN Scale
    g2.translate(panX, panY);
    g2.scale(scale, scale);

    // Calculate visible world bounds for grid optimization
    // Visible Screen: (0,0) to (W,H)
    // World TopLeft = (0 - PanX) / Scale
    double wx = -panX / scale;
    double wy = -panY / scale;
    double ww = getWidth() / scale;
    double wh = getHeight() / scale;

    Rectangle visibleWorldBounds = new Rectangle((int) wx, (int) wy, (int) ww + 1, (int) wh + 1);

    renderer.render(g2,
        circuit.getComponents(),
        circuit.getWires(),
        interaction.getSelectedComponents(),
        interaction.getSelectedWire(),
        interaction.getSelectedWaypoint(),
        interaction.getHoveredPin(),
        interaction.getHoveredWire(),
        interaction.getHoveredWaypoint(),
        interaction.getConnectionStartPin(),
        interaction.getCurrentMousePoint(),
        interaction.getSelectionRect(),
        interaction.getComponentToPlace(),
        visibleWorldBounds);

    g2.setTransform(oldTransform);
  }
}
