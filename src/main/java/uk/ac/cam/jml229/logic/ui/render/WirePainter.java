package uk.ac.cam.jml229.logic.ui.render;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.jml229.logic.app.Theme;

public class WirePainter {

  public static final int HANDLE_SIZE = 6;
  public static final int HANDLE_HIT_SIZE = 10;

  public Shape createWireShape(Point start, Point end, List<Point> waypoints) {
    GeneralPath path = new GeneralPath();
    path.moveTo(start.x, start.y);

    List<Point> allPoints = new ArrayList<>();
    allPoints.add(start);
    allPoints.addAll(waypoints);
    allPoints.add(end);

    for (int i = 0; i < allPoints.size() - 1; i++) {
      Point p1 = allPoints.get(i);
      Point p2 = allPoints.get(i + 1);

      double dist = Math.abs(p2.x - p1.x) * 0.5;
      path.curveTo(p1.x + dist, p1.y, p2.x - dist, p2.y, p2.x, p2.y);
    }
    return path;
  }

  public void drawWire(Graphics2D g2, Shape path, boolean isSignalOn, boolean isSelected, boolean isHovered) {
    if (isSelected || isHovered) {
      g2.setColor(isSelected ? Theme.SELECTION_BORDER : Theme.HOVER_COLOR);
      g2.setStroke(new BasicStroke(6));
      g2.draw(path);
      g2.setStroke(new BasicStroke(3));
    }
    g2.setColor(isSignalOn ? Theme.WIRE_ON : Theme.WIRE_OFF);
    g2.draw(path);
  }

  public void drawHandle(Graphics2D g2, Point pt, boolean isSelected, boolean isHovered) {
    if (isSelected || isHovered) {
      g2.setColor(isSelected ? Theme.SELECTION_BORDER : Theme.HOVER_COLOR);
      int s = HANDLE_HIT_SIZE;
      g2.fillRect(pt.x - s / 2, pt.y - s / 2, s, s);
      g2.setColor(Color.WHITE);
      g2.drawRect(pt.x - s / 2, pt.y - s / 2, s, s);
    } else {
      g2.setColor(Color.WHITE);
      int s = HANDLE_SIZE;
      g2.fillRect(pt.x - s / 2, pt.y - s / 2, s, s);
      g2.setColor(Theme.SELECTION_BORDER);
      g2.drawRect(pt.x - s / 2, pt.y - s / 2, s, s);
    }
  }
}
