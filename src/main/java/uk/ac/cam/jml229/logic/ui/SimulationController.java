package uk.ac.cam.jml229.logic.ui;

import javax.swing.Timer;
import uk.ac.cam.jml229.logic.core.Circuit;
import uk.ac.cam.jml229.logic.core.Simulator;

/**
 * Manages the simulation loop, clock timing, and logic propagation.
 * Decouples the "Game Loop" from the main GUI window.
 */
public class SimulationController {

  private final Timer timer;
  private final Runnable repaintCallback;
  private Circuit circuit;

  // Simulation Settings
  private int logicStepsPerFrame = 1000;
  private int clockDelayMs = 500;
  private long lastClockTick = 0;

  public SimulationController(Circuit initialCircuit, Runnable repaintCallback) {
    this.circuit = initialCircuit;
    this.repaintCallback = repaintCallback;

    // 60Hz Loop (approx 16ms)
    this.timer = new Timer(16, e -> tick());
  }

  public void setCircuit(Circuit circuit) {
    this.circuit = circuit;
    Simulator.clear(); // Clear pending events for the old circuit
  }

  public void start() {
    timer.start();
  }

  public void stop() {
    timer.stop();
  }

  public boolean isRunning() {
    return timer.isRunning();
  }

  /**
   * Manual single-step (Process one clock cycle + resulting logic)
   */
  public void step() {
    if (circuit != null) {
      circuit.tick();
      Simulator.run(1000); // Ensure logic ripples through
      repaintCallback.run();
    }
  }

  public void setClockSpeed(int hz) {
    if (hz > 0) {
      this.clockDelayMs = 1000 / hz;
    }
  }

  // Direct MS setter for custom inputs
  public void setClockDelayMs(int ms) {
    this.clockDelayMs = Math.max(1, ms);
  }

  public void setLogicStepsPerFrame(int steps) {
    this.logicStepsPerFrame = steps;
  }

  private void tick() {
    if (circuit == null)
      return;

    // Process Logic Gates (Event Queue)
    Simulator.run(logicStepsPerFrame);

    // Process Clocks
    long now = System.currentTimeMillis();
    if (now - lastClockTick >= clockDelayMs) {
      circuit.tick();
      lastClockTick = now;
    }

    // Update UI
    repaintCallback.run();
  }
}
