package uk.ac.cam.jml229.logic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.ui.Theme;

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

    // IO Section
    addLabel("IO / Probes");
    addTool(new Switch("Switch"));
    addTool(new OutputProbe("Light"));
    addTool(new SevenSegmentDisplay("7Seg"));
    addTool(new HexDisplay("Hex"));

    // Basic Gates
    addLabel("Basic Gates");
    addTool(new AndGate("AND"));
    addTool(new OrGate("OR"));
    addTool(new NotGate("NOT"));

    // Advanced Gates
    addLabel("Advanced");
    addTool(new NandGate("NAND"));
    addTool(new NorGate("NOR"));
    addTool(new XorGate("XOR"));
    addTool(new BufferGate("BUFF"));

    // Sequential Logic
    addLabel("Sequential");
    addTool(new Clock("CLK"));
    addTool(new DFlipFlop("D-FF"));
    addTool(new JKFlipFlop("JK-FF"));
    addTool(new TFlipFlop("T-FF"));
  }

  public void updateTheme() {
    setBackground(Theme.PALETTE_BACKGROUND);
    for (java.awt.Component comp : getComponents()) {
      updateComponentTheme(comp);
    }
    revalidate();
    repaint();
  }

  private void updateComponentTheme(java.awt.Component c) {
    if (c instanceof JPanel) {
      JPanel p = (JPanel) c;
      if (p.getComponentCount() > 0 && p.getComponent(0) instanceof JLabel) {
        ((JLabel) p.getComponent(0)).setForeground(Theme.PALETTE_HEADINGS);
      } else if (p.getMouseListeners().length > 0) {
        p.setBackground(Theme.BUTTON_BACKGROUND);
      } else {
        for (java.awt.Component child : p.getComponents()) {
          updateComponentTheme(child);
        }
      }
    }
  }

  private void addLabel(String text) {
    if (getComponentCount() > 0) {
      add(Box.createRigidArea(new Dimension(0, 15)));
    }

    JLabel label = new JLabel(text);
    label.setFont(new Font("SansSerif", Font.BOLD, 12));
    label.setForeground(Theme.PALETTE_HEADINGS);
    label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

    JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    labelPanel.setOpaque(false);
    labelPanel.add(label);
    labelPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        if (getBackground().equals(Theme.BUTTON_BACKGROUND)) {
          g2.setColor(Theme.BUTTON_BORDER);
        } else {
          g2.setColor(new Color(100, 150, 255));
        }
        g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);

        int inputs = prototype.getInputCount();
        int outputs = prototype.getOutputCount();
        int maxPins = Math.max(inputs, outputs);
        int realHeight = Math.max(40, maxPins * 20);
        int realWidth = 50;

        double availableH = getHeight() - 20;
        double availableW = getWidth() - 20;

        double scale = 1.0;
        if (realHeight > availableH || realWidth > availableW) {
          scale = Math.min(availableH / realHeight, availableW / realWidth);
        }

        AffineTransform oldTx = g2.getTransform();
        g2.translate(getWidth() / 2, getHeight() / 2);
        g2.scale(scale, scale);
        g2.translate(-realWidth / 2 - prototype.getX(), -realHeight / 2 - prototype.getY());
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
        Component newComp = createNewInstance(prototype);
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

    // Sequential
    if (prototype instanceof Clock)
      return new Clock("CLK");
    if (prototype instanceof DFlipFlop)
      return new DFlipFlop("D-FF");
    if (prototype instanceof JKFlipFlop)
      return new JKFlipFlop("JK-FF");
    if (prototype instanceof TFlipFlop)
      return new TFlipFlop("T-FF");

    // Displays
    if (prototype instanceof SevenSegmentDisplay)
      return new SevenSegmentDisplay("7Seg");
    if (prototype instanceof HexDisplay)
      return new HexDisplay("Hex");

    if (prototype instanceof CustomComponent)
      return prototype.makeCopy();
    return null;
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 20;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
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
      setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    }

    @Override
    public Dimension getPreferredSize() {
      int width = (getParent() != null) ? getParent().getWidth() : getWidth();
      if (width <= 0)
        width = 130;
      Insets insets = getInsets();
      int availableWidth = width - insets.left - insets.right;
      int n = getComponentCount();
      if (n == 0)
        return new Dimension(width, 0);
      int itemW = 100;
      int itemH = 60;
      int hGap = 5;
      int vGap = 5;
      int cols = (availableWidth + hGap) / (itemW + hGap);
      if (cols < 1)
        cols = 1;
      int rows = (n + cols - 1) / cols;
      int height = rows * (itemH + vGap) + vGap + insets.top + insets.bottom;
      return new Dimension(width, height);
    }

    @Override
    public Dimension getMaximumSize() {
      return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
  }
}
