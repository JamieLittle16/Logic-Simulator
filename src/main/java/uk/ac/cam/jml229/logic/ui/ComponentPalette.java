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
    setBackground(new Color(245, 245, 245));

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
    label.setFont(new Font("Segoe UI", Font.BOLD, 12));
    label.setForeground(Color.GRAY);
    label.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
    label.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    add(label);
  }

  private void addTool(Component prototype) {
    // Custom Rounded Button Panel
    JPanel button = new JPanel() {
      private boolean hovered = false;

      @Override
      public void setBackground(Color bg) {
        super.setBackground(bg);
        repaint();
      }

      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Background (Rounded)
        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        // 2. Draw Border
        if (getBackground().equals(Color.WHITE) || getBackground().getGreen() > 240) {
          g2.setColor(new Color(200, 200, 200));
        } else {
          g2.setColor(new Color(100, 150, 255)); // Hover border
        }
        g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        // 3. Draw Component Icon
        int offsetX = (getWidth() - 50) / 2;
        int offsetY = (getHeight() - 40) / 2;

        int oldX = prototype.getX();
        int oldY = prototype.getY();
        prototype.setPosition(offsetX, offsetY);

        renderer.drawComponentBody(g2, prototype, false, false);

        prototype.setPosition(oldX, oldY);
      }
    };

    button.setPreferredSize(new Dimension(100, 60));
    button.setMaximumSize(new Dimension(100, 60));
    button.setBackground(Color.WHITE);
    button.setOpaque(false); // Important for round corners
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        Component newComp = createNewInstance(prototype);
        interaction.startPlacing(newComp);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(235, 245, 255));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Color.WHITE);
      }
    });

    button.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    add(button);
    add(Box.createRigidArea(new Dimension(0, 8)));
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
