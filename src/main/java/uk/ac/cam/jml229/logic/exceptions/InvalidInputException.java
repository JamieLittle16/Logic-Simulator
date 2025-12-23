package uk.ac.cam.jml229.logic.exceptions;

public class InvalidInputException extends RuntimeException {

  public InvalidInputException(String componentName, int invalidIndex, int maxInputs) {
    super(String.format(
        "Component '%s' has no Input %d. Max input index is %d.",
        componentName, invalidIndex, maxInputs - 1));
  }
}
