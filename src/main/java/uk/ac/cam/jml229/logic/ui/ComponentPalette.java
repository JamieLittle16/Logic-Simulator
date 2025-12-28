package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.List;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class ComponentPalette extends JPanel implements Scrollable {

  private final CircuitInteraction interaction;
  private final CircuitRenderer renderer;
  private boolean hasCustomHeading = false;
  private JPanel currentSection;

  public ComponentPalette(CircuitInteraction interaction, CircuitRenderer renderer) {
    this.interaction = interaction;
    this.renderer = renderer;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(Theme.PALETTE_BACKGROUND);

    // --- Generate from Registry ---
    Map<String, List<ComponentRegistry>> categories = ComponentRegistry.getByCategory();

    for (var entry : categories.entrySet()) {
      addLabel(entry.getKey());
      for (ComponentRegistry item : entry.getValue()) {
        addTool(item.createInstance());
      }
    }
  }

  public void updateTheme() {
    setBackground(Theme.PALETTE_BACKGROUND);
    SwingUtilities.updateComponentTreeUI(this); // Updates children too
    for (java.awt.Component comp : getComponents()) {
      updateComponentTheme(comp);
    }
  }

  private void updateComponentTheme(java.awt.Component c) {
    if (c instanceof JPanel p) {
      if (p.getComponentCount() > 0 && p.getComponent(0) instanceof JLabel l) {
        l.setForeground(Theme.PALETTE_HEADINGS);
      } else if (p.getMouseListeners().length > 0) {
        p.setBackground(Theme.BUTTON_BACKGROUND);
      }
      for (java.awt.Component child : p.getComponents())
        updateComponentTheme(child);
    }
  }

  private void addLabel(String text) {
    if (getComponentCount() > 0)
      add(Box.createRigidArea(new Dimension(0, 15)));
    JLabel label = new JLabel(text);
    label.setFont(new Font("SansSerif", Font.BOLD, 12));
    label.setForeground(Theme.PALETTE_HEADINGS);
    label.setAlignmentX(LEFT_ALIGNMENT);

    JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    labelPanel.setOpaque(false);
    labelPanel.add(label);
    labelPanel.setAlignmentX(CENTER_ALIGNMENT);
    labelPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

    add(labelPanel);
    add(Box.createRigidArea(new Dimension(0, 5)));

    currentSection = new SectionPanel();
    add(currentSection);
  }

  public void addCustomTool(Component prototype) {
    if (!hasCustomHeading) {
      addLabel("Custom IC");
      hasCustomHeading = true;
    }
    addTool(prototype);
    revalidate();
    repaint();
  }

  private void addTool(Component prototype) {
    JPanel button = new JPanel() {
      @Override
      public void setBackground(Color bg) {
        super.setBackground(bg);
        repaint();
      }

      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        if (getBackground().equals(Theme.BUTTON_BACKGROUND))
          g2.setColor(Theme.BUTTON_BORDER);
        else
          g2.setColor(new Color(100, 150, 255));
        g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        // Draw Icon
        AffineTransform oldTx = g2.getTransform();
        double scale = Math.min((getHeight() - 20.0) / 60.0, (getWidth() - 20.0) / 60.0);
        g2.translate(getWidth() / 2, getHeight() / 2);
        g2.scale(scale, scale);
        g2.translate(-25 - prototype.getX(), -20 - prototype.getY()); // Center approx
        renderer.drawComponentBody(g2, prototype, false, false);
        g2.setTransform(oldTx);
      }
    };

    button.setPreferredSize(new Dimension(100, 60));
    button.setMaximumSize(new Dimension(100, 60));
    button.setBackground(Theme.BUTTON_BACKGROUND);
    button.setOpaque(false);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // --- UPDATED: Create fresh copy via Registry or Prototype ---
        Component newComp;
        if (prototype instanceof CustomComponent) {
          newComp = prototype.makeCopy();
        } else {
          var entry = ComponentRegistry.fromComponent(prototype);
          newComp = entry.map(ComponentRegistry::createInstance).orElse(null);
        }

        if (newComp != null)
          interaction.startPlacing(newComp);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(new Color(235, 245, 255));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Theme.BUTTON_BACKGROUND);
      }
    });

    if (currentSection != null)
      currentSection.add(button);
    else
      add(button);
  }

  // Scrollable impl...
  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
    return 20;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
    return 100;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  private class SectionPanel extends JPanel {
    public SectionPanel() {
      super(new FlowLayout(FlowLayout.CENTER, 5, 5));
      setOpaque(false);
      setAlignmentX(CENTER_ALIGNMENT);
    }

    @Override
    public Dimension getPreferredSize() {
      int w = (getParent() != null) ? getParent().getWidth() : 130;
      if (w <= 0)
        w = 130;
      int cols = (w - 10) / 105;
      if (cols < 1)
        cols = 1;
      int rows = (getComponentCount() + cols - 1) / cols;
      return new Dimension(w, rows * 65 + 10);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
  }
}
