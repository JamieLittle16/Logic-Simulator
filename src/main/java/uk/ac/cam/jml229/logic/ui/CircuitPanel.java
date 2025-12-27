package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import uk.ac.cam.jml229.logic.model.Circuit;

public class CircuitPanel extends JPanel {

  private Circuit circuit;
  private final CircuitRenderer renderer;
  private final CircuitInteraction interaction;

  private int panX = 0;
  private int panY = 0;

  public CircuitPanel() {
    this.circuit = new Circuit();
    this.renderer = new CircuitRenderer();
    this.interaction = new CircuitInteraction(circuit, this, renderer);

    addMouseListener(interaction);
    addMouseMotionListener(interaction);
    addKeyListener(interaction);

    setFocusable(true);
    setBackground(Color.WHITE);
  }

  public CircuitInteraction getInteraction() {
    return interaction;
  }

  public CircuitRenderer getRenderer() {
    return renderer;
  }

  public int getPanX() {
    return panX;
  }

  public int getPanY() {
    return panY;
  }

  public void setPan(int x, int y) {
    this.panX = x;
    this.panY = y;
    repaint();
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public void setCircuit(Circuit newCircuit) {
    this.circuit = newCircuit;
    // Pass the new circuit down to the interaction handler
    this.interaction.setCircuit(newCircuit);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    AffineTransform oldTransform = g2.getTransform();
    g2.translate(panX, panY);

    Rectangle visibleWorldBounds = new Rectangle(-panX, -panY, getWidth(), getHeight());

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
