package uk.ac.cam.jml229.logic.ui;

import java.awt.*;
import java.awt.geom.*;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;

public class ComponentPainter {

  private static final int PIN_SIZE = 8;

  public void drawComponent(Graphics2D g2, Component c, boolean sel, boolean drawLabel) {
    AffineTransform oldTx = g2.getTransform();
    int x = c.getX();
    int y = c.getY();

    Dimension dim = getComponentSize(c);
    int cx = x + dim.width / 2;
    int cy = y + dim.height / 2;

    g2.rotate(Math.toRadians(c.getRotation() * 90), cx, cy);

    if (c instanceof Switch)
      drawSwitch(g2, (Switch) c, x, y, sel);
    else if (c instanceof Clock)
      drawClock(g2, (Clock) c, x, y, sel);
    else if (c instanceof OutputProbe)
      drawLight(g2, (OutputProbe) c, x, y, sel);
    else if (c instanceof AndGate)
      drawAndGate(g2, c, x, y, sel);
    else if (c instanceof OrGate)
      drawOrGate(g2, c, x, y, sel);
    else if (c instanceof XorGate)
      drawXorGate(g2, c, x, y, sel);
    else if (c instanceof NotGate)
      drawNotGate(g2, c, x, y, sel);
    else if (c instanceof NandGate)
      drawNandGate(g2, c, x, y, sel);
    else if (c instanceof NorGate)
      drawNorGate(g2, c, x, y, sel);
    else if (c instanceof BufferGate)
      drawBufferGate(g2, c, x, y, sel);
    else
      drawGenericBox(g2, c, x, y, sel);

    if (drawLabel && !(c instanceof CustomComponent)) {
      g2.setColor(Theme.TEXT_COLOR);
      g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
      FontMetrics fm = g2.getFontMetrics();
      String name = c.getName();
      int tw = fm.stringWidth(name);
      int textX = x + (50 - tw) / 2;
      int textY = y - 5;

      AffineTransform rotatedTx = g2.getTransform();
      g2.rotate(Math.toRadians(-c.getRotation() * 90), cx, cy);
      g2.drawString(name, textX, textY);
      g2.setTransform(rotatedTx);
    }

    g2.setTransform(oldTx);
  }

  public void drawStubs(Graphics2D g2, Component c) {
    if (c instanceof OutputProbe)
      return;
    AffineTransform oldTx = g2.getTransform();
    Dimension dim = getComponentSize(c);
    int cx = c.getX() + dim.width / 2;
    int cy = c.getY() + dim.height / 2;
    g2.rotate(Math.toRadians(c.getRotation() * 90), cx, cy);

    g2.setColor(Theme.STUB_COLOR); // THEME
    g2.setStroke(new BasicStroke(3));
    int x = c.getX();
    int y = c.getY();

    int outCount = c.getOutputCount();
    boolean hasBubbleOutput = (c instanceof NandGate || c instanceof NorGate);
    for (int i = 0; i < outCount; i++) {
      int yOffset = (outCount == 1) ? 20 : 10 + (i * 20);
      int startX = hasBubbleOutput ? 55 : 50;
      if (c instanceof Switch && outCount == 1)
        startX = 40;
      g2.drawLine(x + startX, y + yOffset, x + 60, y + yOffset);
    }

    int inputCount = getInputCount(c);
    for (int i = 0; i < inputCount; i++) {
      int yOffset = (inputCount == 1) ? 20 : 10 + (i * 20);
      int endX = x;
      if (c instanceof OrGate || c instanceof NorGate || c instanceof XorGate)
        endX = x + 8;
      g2.drawLine(x - 10, y + yOffset, endX, y + yOffset);
    }
    g2.setTransform(oldTx);
  }

  public void drawPinCircle(Graphics2D g2, Point p, boolean isHovered, boolean isActive) {
    if (isActive) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.fillOval(p.x - 6, p.y - 6, 12, 12);
    } else if (isHovered) {
      g2.setColor(Theme.HOVER_COLOR);
      g2.drawOval(p.x - 6, p.y - 6, 12, 12);
    }
    g2.setColor(Theme.PIN_COLOR); // THEME
    g2.fillOval(p.x - PIN_SIZE / 2, p.y - PIN_SIZE / 2, PIN_SIZE, PIN_SIZE);
  }

  // --- Math Methods ---
  public Point getPinLocation(Component c, boolean isInput, int index) {
    int dx, dy;
    if (!isInput) {
      int outCount = c.getOutputCount();
      dx = 60;
      dy = (outCount <= 1) ? 20 : 10 + (index * 20);
    } else {
      int inCount = getInputCount(c);
      dx = -10;
      dy = (inCount == 1) ? 20 : 10 + (index * 20);
    }
    Dimension dim = getComponentSize(c);
    int w = dim.width;
    int h = dim.height;
    int cx = w / 2;
    int cy = h / 2;
    int rx = dx - cx;
    int ry = dy - cy;
    int rotatedX = rx, rotatedY = ry;
    for (int i = 0; i < c.getRotation(); i++) {
      int temp = rotatedX;
      rotatedX = -rotatedY;
      rotatedY = temp;
    }
    return new Point(c.getX() + cx + rotatedX, c.getY() + cy + rotatedY);
  }

  public int getInputCount(Component c) {
    if (c instanceof Switch)
      return 0;
    if (c instanceof UnaryGate || c instanceof OutputProbe)
      return 1;
    return c.getInputCount();
  }

  public Dimension getComponentSize(Component c) {
    int inputCount = getInputCount(c);
    int outputCount = c.getOutputCount();
    int maxPins = Math.max(inputCount, outputCount);
    int h = Math.max(40, maxPins * 20);
    int w = 50;
    return new Dimension(w, h);
  }

  public Rectangle getComponentBounds(Component c) {
    Dimension d = getComponentSize(c);
    int w = d.width;
    int h = d.height;
    if (c.getRotation() == 1 || c.getRotation() == 3) {
      return new Rectangle(c.getX() + (w - h) / 2, c.getY() + (h - w) / 2, h, w);
    }
    return new Rectangle(c.getX(), c.getY(), w, h);
  }

  // --- Primitives ---

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRoundRect(x, y + 5, 40, 30, 15, 15);
    }
    g2.setColor(Theme.SWITCH_FILL); // THEME
    g2.fillRoundRect(x, y + 5, 40, 30, 30, 30);
    boolean on = s.getOutputWire() != null && s.getOutputWire().getSignal();
    int circleX = on ? x + 22 : x + 2;
    Color c = on ? new Color(100, 255, 100) : new Color(200, 200, 200);
    g2.setColor(c);
    g2.fillOval(circleX, y + 7, 26, 26);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(1));
    g2.drawOval(circleX, y + 7, 26, 26);
  }

  private void drawLight(Graphics2D g2, OutputProbe p, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawOval(x, y, 40, 40);
    }
    boolean on = p.getState();
    Color core = on ? new Color(255, 220, 0) : new Color(50, 50, 50);
    if (on) {
      float[] dist = { 0.0f, 0.7f, 1.0f };
      Color[] colors = { new Color(255, 255, 200, 200), new Color(255, 220, 0, 100), new Color(0, 0, 0, 0) };
      RadialGradientPaint glow = new RadialGradientPaint(new Point2D.Float(x + 20, y + 20), 35, dist, colors);
      g2.setPaint(glow);
      g2.fillOval(x - 15, y - 15, 70, 70);
    }
    GradientPaint gp = new GradientPaint(x, y, core.brighter(), x + 30, y + 30, core.darker());
    g2.setPaint(gp);
    g2.fillOval(x, y, 40, 40);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);
    g2.setColor(new Color(255, 255, 255, 100));
    g2.fillOval(x + 10, y + 8, 12, 8);
  }

  private void drawClock(Graphics2D g2, Clock c, int x, int y, boolean sel) {
    // Background Box
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRoundRect(x, y + 5, 40, 30, 5, 5);
    }
    g2.setColor(Theme.SWITCH_FILL);
    g2.fillRoundRect(x, y + 5, 40, 30, 5, 5);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawRoundRect(x, y + 5, 40, 30, 5, 5);

    // Pulse Icon
    boolean on = c.getState();
    g2.setColor(on ? new Color(100, 255, 100) : new Color(100, 100, 100));
    g2.setStroke(new BasicStroke(2));

    // Draw Square Wave symbol
    Path2D wave = new Path2D.Double();
    int sy = y + 25;
    int sx = x + 8;
    wave.moveTo(sx, sy);
    wave.lineTo(sx + 8, sy);
    wave.lineTo(sx + 8, sy - 10);
    wave.lineTo(sx + 16, sy - 10);
    wave.lineTo(sx + 16, sy);
    wave.lineTo(sx + 24, sy);
    g2.draw(wave);
  }

  private void drawAndGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 25, y);
    p.curveTo(x + 57, y, x + 57, y + 40, x + 25, y + 40);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawOrGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.quadTo(x + 15, y + 20, x, y + 40);
    p.quadTo(x + 35, y + 40, x + 50, y + 20);
    p.quadTo(x + 35, y, x, y);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D b = new Path2D.Double();
    b.moveTo(x - 4, y);
    b.quadTo(x + 11, y + 20, x - 4, y + 40);
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(b);
    }
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.draw(b);
    drawOrGate(g2, c, x + 5, y, sel);
  }

  private void drawBufferGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Path2D p = new Path2D.Double();
    p.moveTo(x, y);
    p.lineTo(x + 40, y + 20);
    p.lineTo(x, y + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawNotGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawBufferGate(g2, c, x, y, sel);
    drawBubble(g2, x + 40, y + 15, sel);
  }

  private void drawNandGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawAndGate(g2, c, x, y, sel);
    drawBubble(g2, x + 45, y + 15, sel);
  }

  private void drawNorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    drawOrGate(g2, c, x, y, sel);
    drawBubble(g2, x + 45, y + 15, sel);
  }

  private void fillGate(Graphics2D g2, Path2D p, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(p);
    }
    GradientPaint gp = new GradientPaint(0, 0, Theme.COMP_FILL_GRADIENT_1, 0, 40, Theme.COMP_FILL_GRADIENT_2); // THEME
    g2.setPaint(gp);
    g2.fill(p);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.draw(p);
  }

  private void drawBubble(Graphics2D g2, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawOval(x, y, 10, 10);
    }
    g2.setColor(Color.WHITE);
    g2.fillOval(x, y, 10, 10);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 10, 10);
  }

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Dimension d = getComponentSize(c);
    int w = d.width;
    int h = d.height;
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, w, h);
    }
    g2.setColor(Theme.GENERIC_BOX_FILL);
    g2.fillRect(x, y, w, h);
    g2.setColor(Theme.GENERIC_HEADER_FILL);
    g2.fillRect(x, y, w, 16);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, w, h);

    g2.setColor(Color.WHITE);
    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
    FontMetrics fm = g2.getFontMetrics();
    String name = c.getName();
    if (fm.stringWidth(name) > 46) {
      while (name.length() > 0 && fm.stringWidth(name + "..") > 46)
        name = name.substring(0, name.length() - 1);
      name += "..";
    }
    int textWidth = fm.stringWidth(name);
    g2.drawString(name, x + (w - textWidth) / 2, y + 12);
  }
}
