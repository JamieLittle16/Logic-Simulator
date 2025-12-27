package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.model.Circuit;

public class CircuitPanel extends JPanel {

  private final Circuit circuit;
  private final CircuitRenderer renderer;
  private final CircuitInteraction interaction;

  public CircuitPanel() {
    setPreferredSize(new Dimension(800, 600));
    setBackground(Color.WHITE);
    setFocusable(true);

    // 1. Initialize Model
    this.circuit = new Circuit();

    // 2. Initialize View
    this.renderer = new CircuitRenderer();

    // 3. Initialize Controller
    this.interaction = new CircuitInteraction(circuit, this, renderer);

    // 4. Wire them up
    addMouseListener(interaction);
    addMouseMotionListener(interaction);
    addKeyListener(interaction);
  }

  // Accessors for GuiMain
  public CircuitInteraction getInteraction() {
    return interaction;
  }

  public CircuitRenderer getRenderer() {
    return renderer;
  }

  public void addComponent(Component c) {
    circuit.addComponent(c);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Pass the updated list of arguments to the renderer
    renderer.render(
        (Graphics2D) g,
        circuit.getComponents(),
        circuit.getWires(),
        interaction.getSelectedComponents(),
        interaction.getSelectedWire(),

        // Hover State
        interaction.getSelectedWaypoint(),
        interaction.getHoveredPin(),
        interaction.getHoveredWire(),
        interaction.getHoveredWaypoint(),

        // Wiring State
        interaction.getConnectionStartPin(),
        interaction.getCurrentMousePoint(),

        interaction.getSelectionRect(),
        interaction.getComponentToPlace());
  }
}
