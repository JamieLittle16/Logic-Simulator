package uk.ac.cam.jml229.logic.ui.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.ui.interaction.*;
import uk.ac.cam.jml229.logic.ui.render.*;
import uk.ac.cam.jml229.logic.app.Theme;

public class ComponentPalette extends JPanel implements Scrollable {

  private final CircuitInteraction interaction;
  private final CircuitRenderer renderer;
  private boolean hasCustomHeading = false;
  private JPanel currentSection;
  private final List<Component> customPrototypes = new ArrayList<>();

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
    SwingUtilities.updateComponentTreeUI(this);
    for (java.awt.Component comp : getComponents()) {
      updateComponentTheme(comp);
    }
  }

  private void updateComponentTheme(java.awt.Component c) {
    if (c instanceof JPanel p) {
      if (p.getComponentCount() > 0 && p.getComponent(0) instanceof JLabel l) {
        l.setForeground(Theme.PALETTE_HEADINGS);
      } else if (p.getMouseListeners().length > 0 && !(p instanceof SectionPanel)) {
        p.setBackground(Theme.BUTTON_BACKGROUND);
      }
      for (java.awt.Component child : p.getComponents())
        updateComponentTheme(child);
    }
  }

  private void addLabel(String text) {
    if (getComponentCount() > 0)
      add(Box.createRigidArea(new Dimension(0, 15)));

    // --- Collapsible Header ---
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
    headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // Match alignment with the button grid (CENTER) so they stack vertically
    headerPanel.setAlignmentX(CENTER_ALIGNMENT);

    // Center the text inside the header
    JLabel label = new JLabel("\u25BC " + text, SwingConstants.CENTER);
    label.setFont(new Font("SansSerif", Font.BOLD, 12));
    label.setForeground(Theme.PALETTE_HEADINGS);
    // Removed left border so it centers perfectly
    // label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

    headerPanel.add(label, BorderLayout.CENTER);
    add(headerPanel);
    add(Box.createRigidArea(new Dimension(0, 5)));

    // --- Section Content ---
    SectionPanel section = new SectionPanel();
    currentSection = section;
    add(section);

    // --- Toggle Logic ---
    headerPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        boolean isVisible = section.isVisible();
        section.setVisible(!isVisible);
        label.setText((!isVisible ? "\u25BC " : "\u25B6 ") + text);
        revalidate();
        repaint();
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        label.setForeground(Theme.TEXT_COLOR);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        label.setForeground(Theme.PALETTE_HEADINGS);
      }
    });
  }

  public void addCustomTool(Component prototype) {
    customPrototypes.add(prototype);

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

        // --- DYNAMIC SCALING ---
        AffineTransform oldTx = g2.getTransform();

        Rectangle bounds = renderer.getComponentBounds(prototype);
        int compW = bounds.width;
        int compH = bounds.height;

        double availableW = getWidth() - 25.0;
        double availableH = getHeight() - 25.0;

        double scaleX = availableW / compW;
        double scaleY = availableH / compH;
        double scale = Math.min(scaleX, scaleY);
        scale = Math.min(scale, 1.5);

        g2.translate(getWidth() / 2.0, getHeight() / 2.0);
        g2.scale(scale, scale);

        double centerX = bounds.x + bounds.width / 2.0;
        double centerY = bounds.y + bounds.height / 2.0;
        g2.translate(-centerX, -centerY);

        renderer.drawComponentStubs(g2, prototype);
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
        // Right-Click to Delete Custom ICs
        if (SwingUtilities.isRightMouseButton(e)) {
          if (prototype instanceof CustomComponent) {
            JPopupMenu popup = new JPopupMenu();
            // Style the popup to match the theme
            popup.setBackground(Theme.PALETTE_BACKGROUND);
            popup.setBorder(BorderFactory.createLineBorder(Theme.BUTTON_BORDER));

            JMenuItem deleteItem = new JMenuItem("Delete " + prototype.getName());
            deleteItem.setBackground(Theme.PALETTE_BACKGROUND);
            deleteItem.setForeground(Theme.TEXT_COLOR);

            deleteItem.addActionListener(event -> {
              int confirm = JOptionPane.showConfirmDialog(ComponentPalette.this,
                  "Are you sure you want to delete '" + prototype.getName() + "'?",
                  "Delete Custom Component", JOptionPane.YES_NO_OPTION);

              if (confirm == JOptionPane.YES_OPTION) {
                customPrototypes.remove(prototype);

                Container parent = button.getParent();
                if (parent != null) {
                  parent.remove(button);
                  parent.revalidate();
                  parent.repaint();
                }
              }
            });

            popup.add(deleteItem);
            popup.show(button, e.getX(), e.getY());
          }
          return; // Stop processing (don't place component on right click)
        }

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
        button.setBackground(Theme.BUTTON_HOVER);
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

  public List<Component> getCustomPrototypes() {
    return customPrototypes;
  }

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
      if (!isVisible())
        return new Dimension(0, 0);

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
      if (!isVisible())
        return new Dimension(Integer.MAX_VALUE, 0);
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
  }
}
