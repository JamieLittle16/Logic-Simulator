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
import uk.ac.cam.jml229.logic.ui.interaction.state.IdleState;
import uk.ac.cam.jml229.logic.ui.render.*;
import uk.ac.cam.jml229.logic.app.Theme;

public class ComponentPalette extends JPanel implements Scrollable {

  private final CircuitInteraction interaction;
  private final CircuitRenderer renderer;

  // --- STATE TRACKING ---
  private boolean hasCustomHeading = false;
  private JPanel currentSection;

  private final List<Component> customPrototypes = new ArrayList<>();
  private JPanel customHeaderPanel;
  private SectionPanel customSectionPanel;
  private java.awt.Component customSpacer;

  // -- Cancel Listener ---
  // Clicking the sidebar background cancels any active placement
  private final MouseAdapter cancelListener = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
      interaction.setState(new IdleState(interaction));
      interaction.getPanel().repaint();
    }
  };

  public ComponentPalette(CircuitInteraction interaction, CircuitRenderer renderer) {
    this.interaction = interaction;
    this.renderer = renderer;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(Theme.PALETTE_BACKGROUND);

    // Listen for clicks on the main background
    addMouseListener(cancelListener);

    // --- Generate from Registry ---
    Map<String, List<ComponentRegistry>> categories = ComponentRegistry.getByCategory();

    for (var entry : categories.entrySet()) {
      addLabel(entry.getKey());
      for (ComponentRegistry item : entry.getValue()) {
        addTool(item.createInstance());
      }
    }
  }

  // Allow GuiMain to save these
  public List<Component> getCustomPrototypes() {
    return customPrototypes;
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
    // --- Spacer ---
    if (getComponentCount() > 0) {
      java.awt.Component spacer = Box.createRigidArea(new Dimension(0, 15));
      add(spacer);
      if (text.equals("Custom IC")) {
        this.customSpacer = spacer;
      }
    }

    // --- Collapsible Header ---
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
    headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    headerPanel.setAlignmentX(CENTER_ALIGNMENT);

    JLabel label = new JLabel("\u25BC " + text, SwingConstants.CENTER);
    label.setFont(new Font("SansSerif", Font.BOLD, 12));
    label.setForeground(Theme.PALETTE_HEADINGS);

    headerPanel.add(label, BorderLayout.CENTER);
    add(headerPanel);
    add(Box.createRigidArea(new Dimension(0, 5)));

    // --- Section Content ---
    SectionPanel section = new SectionPanel();

    // Listen for clicks on the empty space between buttons
    section.addMouseListener(cancelListener);

    currentSection = section;
    add(section);

    if (text.equals("Custom IC")) {
      this.customHeaderPanel = headerPanel;
      this.customSectionPanel = section;
    }

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

    if (customSectionPanel != null) {
      currentSection = customSectionPanel;
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

    // --- LISTENER: Click + Drag Support ---
    MouseAdapter listener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // Right Click = Delete Custom IC
        if (SwingUtilities.isRightMouseButton(e)) {
          if (prototype instanceof CustomComponent) {
            handleDelete(e, prototype, button);
          }
          return;
        }

        // Left Click = Place Tool
        Component newComp;
        if (prototype instanceof CustomComponent) {
          newComp = prototype.makeCopy();
        } else {
          var entry = ComponentRegistry.fromComponent(prototype);
          newComp = entry.map(ComponentRegistry::createInstance).orElse(null);
        }

        if (newComp != null) {
          interaction.startPlacing(newComp);

          interaction.getPanel().requestFocusInWindow();

          forwardToInteraction(e);
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          forwardToInteraction(e);
        }
      }

      private void forwardToInteraction(MouseEvent e) {
        if (interaction.getPanel().isShowing()) {
          Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), interaction.getPanel());
          interaction.mouseMoved(new MouseEvent(
              interaction.getPanel(),
              MouseEvent.MOUSE_MOVED,
              System.currentTimeMillis(),
              0,
              p.x, p.y,
              0,
              false));
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(Theme.BUTTON_HOVER);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(Theme.BUTTON_BACKGROUND);
      }
    };

    button.addMouseListener(listener);
    button.addMouseMotionListener(listener);

    if (currentSection != null)
      currentSection.add(button);
    else
      add(button);
  }

  private void handleDelete(MouseEvent e, Component prototype, JPanel button) {
    JPopupMenu popup = new JPopupMenu();
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

        if (customPrototypes.isEmpty()) {
          if (customHeaderPanel != null) {
            ComponentPalette.this.remove(customHeaderPanel);
            customHeaderPanel = null;
          }
          if (customSectionPanel != null) {
            ComponentPalette.this.remove(customSectionPanel);
            customSectionPanel = null;
          }
          if (customSpacer != null) {
            ComponentPalette.this.remove(customSpacer);
            customSpacer = null;
          }
          hasCustomHeading = false;
          ComponentPalette.this.revalidate();
          ComponentPalette.this.repaint();
        }
      }
    });

    popup.add(deleteItem);
    popup.show(button, e.getX(), e.getY());
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
