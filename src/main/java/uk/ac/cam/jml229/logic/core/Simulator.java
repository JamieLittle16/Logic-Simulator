package uk.ac.cam.jml229.logic.core;

import java.util.PriorityQueue;
import java.util.Queue;

public class Simulator {

  // A wrapper for events to happen at a specific time
  private static class SimEvent implements Comparable<SimEvent> {
    long tickTime;
    Runnable action;

    SimEvent(long tickTime, Runnable action) {
      this.tickTime = tickTime;
      this.action = action;
    }

    @Override
    public int compareTo(SimEvent other) {
      return Long.compare(this.tickTime, other.tickTime);
    }
  }

  // Priority Queue sorts by time (lowest/earliest first)
  private static final Queue<SimEvent> eventQueue = new PriorityQueue<>();

  // Global simulation clock
  private static long currentTick = 0;

  /**
   * Schedules an update to happen IMMEDIATELY (next micro-step).
   * Used for standard 0-delay logic.
   */
  public static void enqueue(Runnable event) {
    schedule(event, 0);
  }

  /**
   * Schedules an update to happen in the future.
   * 
   * @param delayTicks How many ticks to wait (0 = instant)
   */
  public static void schedule(Runnable event, int delayTicks) {
    eventQueue.add(new SimEvent(currentTick + delayTicks, event));
  }

  /**
   * Processes the simulation.
   * * @param maxTicks How many "time units" to advance.
   * Previously this was "steps", now it represents clock ticks.
   */
  public static void run(int maxTicks) {
    for (int i = 0; i < maxTicks; i++) {
      // Process all events scheduled for NOW (currentTick)
      while (!eventQueue.isEmpty() && eventQueue.peek().tickTime <= currentTick) {
        eventQueue.poll().action.run();
      }
      // Advance time
      currentTick++;
    }
  }

  public static void clear() {
    eventQueue.clear();
    currentTick = 0;
  }

  public static boolean isStable() {
    return eventQueue.isEmpty();
  }

  public static long getTick() {
    return currentTick;
  }
}
