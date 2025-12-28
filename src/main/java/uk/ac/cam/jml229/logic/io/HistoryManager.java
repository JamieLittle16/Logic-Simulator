package uk.ac.cam.jml229.logic.io;

import java.util.Stack;
import uk.ac.cam.jml229.logic.core.Circuit;

public class HistoryManager {

  private final Stack<String> undoStack = new Stack<>();
  private final Stack<String> redoStack = new Stack<>();
  private static final int MAX_HISTORY = 50;

  public void pushState(Circuit circuit) {
    // Save current state to string
    String state = StorageManager.saveToString(circuit, null);

    // Avoid duplicate states
    if (!undoStack.isEmpty() && undoStack.peek().equals(state)) {
      return;
    }

    undoStack.push(state);
    if (undoStack.size() > MAX_HISTORY) {
      undoStack.remove(0); // Drop oldest
    }
    redoStack.clear();
  }

  public Circuit undo(Circuit currentCircuit) {
    if (undoStack.isEmpty())
      return null;

    // Save current state to Redo stack
    String currentState = StorageManager.saveToString(currentCircuit, null);
    redoStack.push(currentState);

    String previousState = undoStack.pop();
    try {
      return StorageManager.loadFromString(previousState).circuit();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public Circuit redo(Circuit currentCircuit) {
    if (redoStack.isEmpty())
      return null;

    // Save current state to Undo stack
    String currentState = StorageManager.saveToString(currentCircuit, null);
    undoStack.push(currentState);

    String nextState = redoStack.pop();
    try {
      return StorageManager.loadFromString(nextState).circuit();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // --- For loading new files ---
  public void clear() {
    undoStack.clear();
    redoStack.clear();
  }
}
