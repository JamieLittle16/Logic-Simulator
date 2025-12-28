package uk.ac.cam.jml229.logic.core;

import java.util.ArrayDeque;
import java.util.Queue;

public class Simulator {

  // The "To-Do" list for signals
  private static final Queue<Runnable> eventQueue = new ArrayDeque<>();

  /**
   * Schedules an update to happen later.
   */
  public static void enqueue(Runnable event) {
    eventQueue.add(event);
  }

  /**
   * Processes the queue.
   * 
   * @param maxSteps Limits how many updates we do per frame (Speed Limit).
   *                 This prevents an infinite loop from freezing the UI.
   */
  public static void run(int maxSteps) {
    int steps = 0;
    while (!eventQueue.isEmpty() && steps < maxSteps) {
      eventQueue.poll().run();
      steps++;
    }
  }

  public static void clear() {
    eventQueue.clear();
  }

  public static boolean isStable() {
    return eventQueue.isEmpty();
  }
}
