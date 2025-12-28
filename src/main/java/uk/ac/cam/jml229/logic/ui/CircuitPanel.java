package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer; // For the listener
import uk.ac.cam.jml229.logic.model.Circuit;

public class CircuitPanel extends JPanel {

  private Circuit circuit;
  private final CircuitRenderer renderer;
  private final CircuitInteraction interaction;

  // Viewport State
  private double panX = 0;
  private double panY = 0;
  private double scale = 1.0;

  // Callback for GUI updates (e.g. "100%" label)
  private Consumer<Double> onZoomChanged;

  public CircuitPanel() {
    this.circuit = new Circuit();
    this.renderer = new CircuitRenderer();
    this.interaction = new CircuitInteraction(circuit, this, renderer);

    addMouseListener(interaction);
    addMouseMotionListener(interaction);
    addKeyListener(interaction);

    // Mouse Wheel Zoom (Zooms to Cursor)
    addMouseWheelListener(e -> {
      double zoomFactor = 1.1;
      double newScale = (e.getWheelRotation() < 0) ? scale * zoomFactor : scale / zoomFactor;
      newScale = Math.max(0.1, Math.min(newScale, 5.0));
      setZoom(newScale, e.getPoint());
    });

    setFocusable(true);
    setBackground(Color.WHITE);
  }

  public void updateTheme() {
    setBackground(Theme.PANEL_BACKGROUND);
    repaint();
  }

  public void setOnZoomChanged(Consumer<Double> listener) {
    this.onZoomChanged = listener;
  }

  // --- Zoom Actions (For Menus/Keyboard) ---
  public void zoomIn() {
    // Zoom to Center of Screen
    Point center = new Point(getWidth() / 2, getHeight() / 2);
    double newScale = Math.min(scale * 1.25, 5.0); // 25% increments for buttons
    setZoom(newScale, center);
  }

  public void zoomOut() {
    Point center = new Point(getWidth() / 2, getHeight() / 2);
    double newScale = Math.max(scale / 1.25, 0.1);
    setZoom(newScale, center);
  }

  public void resetZoom() {
    Point center = new Point(getWidth() / 2, getHeight() / 2);
    setZoom(1.0, center);
    // Optional: Also reset Pan to 0,0?
    // this.panX = 0; this.panY = 0; repaint();
  }

  public void setZoom(double newScale, Point anchor) {
    double worldX = (anchor.x - panX) / scale;
    double worldY = (anchor.y - panY) / scale;

    this.scale = newScale;

    this.panX = anchor.x - (worldX * scale);
    this.panY = anchor.y - (worldY * scale);

    repaint();
    if (onZoomChanged != null)
      onZoomChanged.accept(scale);
  }

  // --- Edit Actions (Proxies) ---
  public void rotateSelection() {
    interaction.rotateSelection();
  }

  public void deleteSelection() {
    interaction.deleteSelection();
  }

  public void undo() {
    interaction.undo();
  }

  public void redo() {
    interaction.redo();
  }

  public void copy() {
    interaction.copy();
  }

  public void cut() {
    interaction.cut();
  }

  public void paste() {
    interaction.paste();
  }

  // --- Getters/Setters ---
  public CircuitInteraction getInteraction() {
    return interaction;
  }

  public CircuitRenderer getRenderer() {
    return renderer;
  }

  public Circuit getCircuit() {
    return circuit;
  }

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
    g2.translate(panX, panY);
    g2.scale(scale, scale);

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
