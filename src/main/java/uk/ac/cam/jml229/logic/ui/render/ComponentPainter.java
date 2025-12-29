package uk.ac.cam.jml229.logic.ui.render;

import java.awt.*;
import java.awt.geom.*;

import uk.ac.cam.jml229.logic.app.Theme;
import uk.ac.cam.jml229.logic.components.*;
import uk.ac.cam.jml229.logic.components.Component;
import uk.ac.cam.jml229.logic.components.gates.*;
import uk.ac.cam.jml229.logic.components.io.*;
import uk.ac.cam.jml229.logic.components.seq.*;

public class ComponentPainter {

  private static final int PIN_SIZE = 8;

  // ==========================================
  // Public Entry Point
  // ==========================================

  public void drawComponent(Graphics2D g2, Component c, boolean sel, boolean drawLabel) {
    AffineTransform oldTx = g2.getTransform();
    int x = c.getX();
    int y = c.getY();

    Dimension dim = getComponentSize(c);
    int cx = x + dim.width / 2;
    int cy = y + dim.height / 2;

    // Apply Component Rotation
    g2.rotate(Math.toRadians(c.getRotation() * 90), cx, cy);

    // Identify Type from Registry
    String typeId = ComponentRegistry.fromComponent(c)
        .map(ComponentRegistry::getId)
        .orElse("CUSTOM");

    // Dispatch to specific drawer
    dispatchDraw(g2, c, typeId, sel);

    // Draw Label (if applicable)
    if (drawLabel && shouldDrawLabel(typeId)) {
      drawLabel(g2, c, x, y, dim);
    }

    g2.setTransform(oldTx);
  }

  private void dispatchDraw(Graphics2D g2, Component c, String id, boolean sel) {
    int x = c.getX();
    int y = c.getY();
    switch (id) {
      // IO
      case "SWITCH" -> drawSwitch(g2, (Switch) c, x, y, sel);
      case "LIGHT" -> drawLight(g2, (OutputProbe) c, x, y, sel);
      case "SEVEN_SEG" -> drawSevenSegment(g2, (SevenSegmentDisplay) c, x, y, sel);
      case "HEX" -> drawHexDisplay(g2, (HexDisplay) c, x, y, sel);

      // Sequential
      case "CLOCK" -> drawClock(g2, (Clock) c, x, y, sel);
      case "D_FF" -> drawDFlipFlop(g2, (DFlipFlop) c, x, y, sel);
      case "JK_FF" -> drawJKFlipFlop(g2, (JKFlipFlop) c, x, y, sel);
      case "T_FF" -> drawTFlipFlop(g2, (TFlipFlop) c, x, y, sel);

      // Gates
      case "AND" -> drawAndGate(g2, c, x, y, sel);
      case "OR" -> drawOrGate(g2, c, x, y, sel);
      case "NOT" -> drawNotGate(g2, c, x, y, sel);
      case "NAND" -> drawNandGate(g2, c, x, y, sel);
      case "NOR" -> drawNorGate(g2, c, x, y, sel);
      case "XOR" -> drawXorGate(g2, c, x, y, sel);
      case "BUFFER" -> drawBufferGate(g2, c, x, y, sel);

      default -> drawGenericBox(g2, c, x, y, sel);
    }
  }

  private boolean shouldDrawLabel(String id) {
    return switch (id) {
      case "CUSTOM", "D_FF", "JK_FF", "T_FF", "SEVEN_SEG", "HEX" -> false;
      default -> true;
    };
  }

  public void drawStubs(Graphics2D g2, Component c) {
    if (c instanceof OutputProbe)
      return;
    AffineTransform oldTx = g2.getTransform();
    Dimension dim = getComponentSize(c);
    int cx = c.getX() + dim.width / 2;
    int cy = c.getY() + dim.height / 2;
    g2.rotate(Math.toRadians(c.getRotation() * 90), cx, cy);

    g2.setColor(Theme.STUB_COLOR);
    g2.setStroke(new BasicStroke(3));
    int x = c.getX();
    int y = c.getY();
    int w = dim.width;
    int h = dim.height;

    int outCount = c.getOutputCount();
    boolean hasBubbleOutput = (c instanceof NandGate || c instanceof NorGate);
    for (int i = 0; i < outCount; i++) {
      // Use center (h/2) if single output, otherwise stack them
      int yOffset = (outCount == 1) ? h / 2 : 10 + (i * 20);

      int startX = hasBubbleOutput ? w + 5 : w;
      if (c instanceof Switch && outCount == 1)
        startX = 40;
      if (c instanceof DFlipFlop || c instanceof JKFlipFlop || c instanceof TFlipFlop)
        startX = 40;

      g2.drawLine(x + startX - 10, y + yOffset, x + startX + 10, y + yOffset);
    }

    int inputCount = getInputCount(c);
    for (int i = 0; i < inputCount; i++) {
      int yOffset = (inputCount == 1) ? 20 : 10 + (i * 20);
      int endX = x;
      if (c instanceof OrGate || c instanceof NorGate || c instanceof XorGate)
        endX = x + 8;
      if (inputCount > 2)
        endX = x;

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
    g2.setColor(Theme.PIN_COLOR);
    g2.fillOval(p.x - PIN_SIZE / 2, p.y - PIN_SIZE / 2, PIN_SIZE, PIN_SIZE);
  }

  // ==========================================
  // Layout Helpers
  // ==========================================

  public Point getPinLocation(Component c, boolean isInput, int index) {
    Dimension dim = getComponentSize(c);
    int w = dim.width;
    int h = dim.height;
    int dx, dy;

    if (!isInput) {
      int outCount = c.getOutputCount();
      dx = w + 10;
      if (c instanceof DFlipFlop || c instanceof JKFlipFlop || c instanceof TFlipFlop)
        dx = 50;

      dy = (outCount <= 1) ? h / 2 : 10 + (index * 20);
    } else {
      int inCount = getInputCount(c);
      dx = -10;
      dy = (inCount == 1) ? 20 : 10 + (index * 20);
    }

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
    if (c instanceof Switch || c instanceof Clock)
      return 0;
    if (c instanceof UnaryGate || c instanceof OutputProbe)
      return 1;
    return c.getInputCount();
  }

  public Dimension getComponentSize(Component c) {
    int w = 50;
    int h = 40;

    if (c instanceof DFlipFlop || c instanceof TFlipFlop) {
      w = 40;
      h = 40;
    } else if (c instanceof JKFlipFlop) {
      w = 40;
      h = 60;
    } else if (c instanceof SevenSegmentDisplay) {
      w = 60;
      h = 160;
    } else if (c instanceof HexDisplay) {
      w = 60;
      h = 80;
    } else {
      int inputCount = getInputCount(c);
      int outputCount = c.getOutputCount();
      int maxPins = Math.max(inputCount, outputCount);
      h = Math.max(40, maxPins * 20);
    }
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

  // ==========================================
  // Drawing Implementations
  // ==========================================

  private void drawLabel(Graphics2D g2, Component c, int x, int y, Dimension dim) {
    g2.setColor(Theme.TEXT_COLOR);
    g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
    FontMetrics fm = g2.getFontMetrics();
    String name = c.getName();
    int tw = fm.stringWidth(name);

    int tx = x + (dim.width - tw) / 2;
    int ty = y - 5;

    if (c.getRotation() == 2) { // Counter-rotate if upside down
      AffineTransform saved = g2.getTransform();
      g2.rotate(Math.PI, x + dim.width / 2.0, ty - fm.getAscent() / 2.0);
      g2.drawString(name, tx, ty);
      g2.setTransform(saved);
    } else {
      g2.drawString(name, tx, ty);
    }
  }

  private void drawGenericBox(Graphics2D g2, Component c, int x, int y, boolean sel) {
    Dimension d = getComponentSize(c);
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, d.width, d.height);
    }
    g2.setColor(Theme.GENERIC_BOX_FILL);
    g2.fillRect(x, y, d.width, d.height);
    g2.setColor(Theme.GENERIC_HEADER_FILL);
    g2.fillRect(x, y, d.width, 16);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, d.width, d.height);

    g2.setColor(Color.WHITE);
    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
    FontMetrics fm = g2.getFontMetrics();
    String name = c.getName();
    if (fm.stringWidth(name) > 46) {
      name = name.substring(0, Math.min(name.length(), 5)) + "..";
    }
    int textWidth = fm.stringWidth(name);
    int tx = x + (d.width - textWidth) / 2;
    int ty = y + 12;

    if (c.getRotation() == 2) {
      AffineTransform saved = g2.getTransform();
      double centerX = tx + textWidth / 2.0;
      double centerY = ty - fm.getAscent() / 2.0;
      g2.rotate(Math.PI, centerX, centerY);
      g2.drawString(name, tx, ty);
      g2.setTransform(saved);
    } else {
      g2.drawString(name, tx, ty);
    }
  }

  private void drawSwitch(Graphics2D g2, Switch s, int x, int y, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRoundRect(x, y + 5, 40, 30, 15, 15);
    }
    g2.setColor(Theme.SWITCH_FILL);
    g2.fillRoundRect(x, y + 5, 40, 30, 30, 30);
    boolean on = s.getState();
    int circleX = on ? x + 22 : x + 2;
    g2.setColor(on ? new Color(100, 255, 100) : new Color(200, 200, 200));
    g2.fillOval(circleX, y + 7, 26, 26);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(1));
    g2.drawOval(circleX, y + 7, 26, 26);
  }

  private void drawClock(Graphics2D g2, Clock c, int x, int y, boolean sel) {
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

    boolean on = c.getState();
    g2.setColor(on ? new Color(100, 255, 100) : new Color(100, 100, 100));
    g2.setStroke(new BasicStroke(2));
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
    g2.setPaint(new GradientPaint(x, y, core.brighter(), x + 30, y + 30, core.darker()));
    g2.fillOval(x, y, 40, 40);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(2));
    g2.drawOval(x, y, 40, 40);
    g2.setColor(new Color(255, 255, 255, 100));
    g2.fillOval(x + 10, y + 8, 12, 8);
  }

  // --- Flip Flops ---
  private void drawFlipFlopBase(Graphics2D g2, Component c, int x, int y, int w, int h, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, w, h);
    }
    g2.setColor(Theme.GENERIC_BOX_FILL);
    g2.fillRect(x, y, w, h);
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, w, h);
    g2.setColor(Theme.TEXT_COLOR);
    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
  }

  private void drawClockTri(Graphics2D g2, int x, int y) {
    g2.drawLine(x, y - 4, x + 6, y);
    g2.drawLine(x + 6, y, x, y + 4);
  }

  private void drawDFlipFlop(Graphics2D g2, DFlipFlop c, int x, int y, boolean sel) {
    drawFlipFlopBase(g2, c, x, y, 40, 40, sel);
    g2.drawString("D", x + 4, y + 13);
    drawClockTri(g2, x, y + 30);
    g2.drawString("Q", x + 28, y + 13);
    g2.drawString("!Q", x + 25, y + 33);
  }

  private void drawTFlipFlop(Graphics2D g2, TFlipFlop c, int x, int y, boolean sel) {
    drawFlipFlopBase(g2, c, x, y, 40, 40, sel);
    g2.drawString("T", x + 4, y + 13);
    drawClockTri(g2, x, y + 30);
    g2.drawString("Q", x + 28, y + 13);
    g2.drawString("!Q", x + 25, y + 33);
  }

  private void drawJKFlipFlop(Graphics2D g2, JKFlipFlop c, int x, int y, boolean sel) {
    drawFlipFlopBase(g2, c, x, y, 40, 60, sel);
    g2.drawString("J", x + 4, y + 13);
    drawClockTri(g2, x, y + 30);
    g2.drawString("K", x + 4, y + 53);
    g2.drawString("Q", x + 28, y + 13);
    g2.drawString("!Q", x + 25, y + 53);
  }

  // --- Displays ---
  private interface SegmentChecker {
    boolean isOn(int idx);
  }

  private void drawDisplayImpl(Graphics2D g2, int x, int y, int w, int h, boolean sel, SegmentChecker check) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.drawRect(x, y, w, h);
    }
    g2.setColor(new Color(20, 20, 20));
    g2.fillRect(x, y, w, h);
    g2.setColor(Color.GRAY);
    g2.setStroke(new BasicStroke(2));
    g2.drawRect(x, y, w, h);
    int dispW = 50;
    int dispH = 80;
    int dispX = x + (w - dispW) / 2;
    int dispY = y + (h - dispH) / 2;
    g2.setColor(new Color(40, 40, 40));
    g2.fillRect(dispX, dispY, dispW, dispH);
    int[][] segs = { { 10, 10, 30, 5 }, { 40, 15, 5, 25 }, { 40, 45, 5, 25 }, { 10, 70, 30, 5 }, { 5, 45, 5, 25 },
        { 5, 15, 5, 25 }, { 10, 40, 30, 5 } };
    for (int i = 0; i < 7; i++) {
      boolean on = check.isOn(i);
      g2.setColor(on ? new Color(255, 50, 50) : new Color(60, 0, 0));
      g2.fillRect(dispX + segs[i][0], dispY + segs[i][1], segs[i][2], segs[i][3]);
    }
    boolean dp = check.isOn(7);
    g2.setColor(dp ? new Color(255, 50, 50) : new Color(60, 0, 0));
    g2.fillOval(dispX + 40, dispY + 70, 5, 5);
  }

  private void drawSevenSegment(Graphics2D g2, SevenSegmentDisplay c, int x, int y, boolean sel) {
    drawDisplayImpl(g2, x, y, 60, 160, sel, c::isSegmentOn);
  }

  private void drawHexDisplay(Graphics2D g2, HexDisplay c, int x, int y, boolean sel) {
    drawDisplayImpl(g2, x, y, 60, 80, sel, c::isSegmentOn);
  }

  // ==========================================
  // Multi-Input Logic Gates
  // ==========================================

  private void fillGate(Graphics2D g2, Path2D p, boolean sel) {
    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(p);
    }
    GradientPaint gp = new GradientPaint(0, 0, Theme.COMP_FILL_GRADIENT_1, 0, 40, Theme.COMP_FILL_GRADIENT_2);
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

  // --- RAIL HELPER ---
  private void drawRail(Graphics2D g2, int x, int y, int height) {
    // Vertical Rail Line
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.drawLine(x, y + 10, x, y + height - 10);

    // Horizontal "Feeder" to gate body center
    int centerY = y + height / 2;
    g2.drawLine(x, centerY, x + 5, centerY);
  }

  private void drawAndGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    int inputCount = c.getInputCount();
    int height = getComponentSize(c).height;
    int yOffset = (inputCount > 2) ? (height - 40) / 2 : 0;

    if (inputCount > 2)
      drawRail(g2, x, y, height);

    // Draw centered body
    Path2D p = new Path2D.Double();
    int by = y + yOffset;
    p.moveTo(x, by);
    p.lineTo(x + 25, by);
    p.curveTo(x + 57, by, x + 57, by + 40, x + 25, by + 40);
    p.lineTo(x, by + 40);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawOrGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    int inputCount = c.getInputCount();
    int height = getComponentSize(c).height;
    int yOffset = (inputCount > 2) ? (height - 40) / 2 : 0;

    if (inputCount > 2) {
      drawRail(g2, x, y, height);
      // Small connector for OR gate indentation
      int centerY = y + height / 2;
      g2.setColor(Theme.COMP_BORDER);
      g2.drawLine(x + 5, centerY, x + 10, centerY);
    }

    Path2D p = new Path2D.Double();
    int by = y + yOffset;
    p.moveTo(x, by);
    p.quadTo(x + 15, by + 20, x, by + 40);
    p.quadTo(x + 35, by + 40, x + 50, by + 20);
    p.quadTo(x + 35, by, x, by);
    p.closePath();
    fillGate(g2, p, sel);
  }

  private void drawXorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    int inputCount = c.getInputCount();
    int height = getComponentSize(c).height;
    int yOffset = (inputCount > 2) ? (height - 40) / 2 : 0;

    int xShift = 0;
    if (inputCount > 2) {
      drawRail(g2, x, y, height);
      // XOR needs to shift right so the rail doesn't overlap the detached curve
      xShift = 5;
      // Connect Rail to Detached Curve
      int centerY = y + height / 2;
      g2.setColor(Theme.COMP_BORDER);
      g2.drawLine(x, centerY, x + xShift, centerY);
    }

    int bx = x + xShift;
    int by = y + yOffset;

    Path2D b = new Path2D.Double();
    b.moveTo(bx - 4, by);
    b.quadTo(bx + 11, by + 20, bx - 4, by + 40);

    if (sel) {
      g2.setColor(Theme.SELECTION_BORDER);
      g2.setStroke(new BasicStroke(5));
      g2.draw(b);
    }
    g2.setColor(Theme.COMP_BORDER);
    g2.setStroke(new BasicStroke(2));
    g2.draw(b);

    // Draw the OR part shifted
    // We reuse the OR path logic but manually here to apply shift
    Path2D p = new Path2D.Double();
    int orX = bx + 5;
    p.moveTo(orX, by);
    p.quadTo(orX + 15, by + 20, orX, by + 40);
    p.quadTo(orX + 35, by + 40, orX + 50, by + 20);
    p.quadTo(orX + 35, by, orX, by);
    p.closePath();
    fillGate(g2, p, sel);
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
    int inputCount = c.getInputCount();
    int height = getComponentSize(c).height;
    int yOffset = (inputCount > 2) ? (height - 40) / 2 : 0;

    drawAndGate(g2, c, x, y, sel); // This draws rail + body

    // Bubble (needs to be centered relative to the body, not top-left)
    int by = y + yOffset;
    drawBubble(g2, x + 45, by + 15, sel);
  }

  private void drawNorGate(Graphics2D g2, Component c, int x, int y, boolean sel) {
    int inputCount = c.getInputCount();
    int height = getComponentSize(c).height;
    int yOffset = (inputCount > 2) ? (height - 40) / 2 : 0;

    drawOrGate(g2, c, x, y, sel); // This draws rail + body

    // Bubble
    int by = y + yOffset;
    drawBubble(g2, x + 45, by + 15, sel);
  }
}
