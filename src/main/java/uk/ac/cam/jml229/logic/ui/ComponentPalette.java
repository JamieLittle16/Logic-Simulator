package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class ComponentPalette extends JPanel {

  private final CircuitInteraction interaction;
  private final CircuitRenderer renderer;

  public ComponentPalette(CircuitInteraction interaction, CircuitRenderer renderer) {
    this.interaction = interaction;
    this.renderer = renderer;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(new Color(240, 240, 240));

    addLabel("IO / Probes");
    addTool(new Switch("Switch"));
    addTool(new OutputProbe("Light"));

    addLabel("Basic Gates");
    addTool(new AndGate("AND"));
    addTool(new OrGate("OR"));
    addTool(new NotGate("NOT"));

    addLabel("Advanced");
    addTool(new NandGate("NAND"));
    addTool(new NorGate("NOR"));
    addTool(new XorGate("XOR"));
    addTool(new BufferGate("BUFF"));
  }

  private void addLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.BOLD, 12));
    label.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
    label.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    add(label);
  }

  private void addTool(Component prototype) {
    JPanel button = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Fix: Paints background!
        Graphics2D g2 = (Graphics2D) g;

        // Center the component
        int offsetX = (getWidth() - 50) / 2;
        int offsetY = (getHeight() - 40) / 2;

        int oldX = prototype.getX();
        int oldY = prototype.getY();
        prototype.setPosition(offsetX, offsetY);

        // Draw icon WITHOUT label
        renderer.drawComponentBody(g2, prototype, false, false);

        prototype.setPosition(oldX, oldY);
      }
    };

    button.setPreferredSize(new Dimension(100, 60));
    button.setMaximumSize(new Dimension(100, 60));
    button.setBackground(Color.WHITE);
    button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        Component newComp = createNewInstance(prototype);
        interaction.startPlacing(newComp);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(230, 240, 255));
        button.repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Color.WHITE);
        button.repaint();
      }
    });

    button.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    add(button);
    add(Box.createRigidArea(new Dimension(0, 5)));
  }

  private Component createNewInstance(Component prototype) {
    if (prototype instanceof Switch)
      return new Switch("SW");
    if (prototype instanceof OutputProbe)
      return new OutputProbe("Out");
    if (prototype instanceof AndGate)
      return new AndGate("AND");
    if (prototype instanceof OrGate)
      return new OrGate("OR");
    if (prototype instanceof NotGate)
      return new NotGate("NOT");
    if (prototype instanceof XorGate)
      return new XorGate("XOR");
    if (prototype instanceof NandGate)
      return new NandGate("NAND");
    if (prototype instanceof NorGate)
      return new NorGate("NOR");
    if (prototype instanceof BufferGate)
      return new BufferGate("BUF");
    return null;
  }
}
