package org.expee.lidar.parsers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Parses a stream of Lidar Data into human objects. Flushes periodically to
 * a given output stream or standard out if not specified
 */
public abstract class Parser {
  static class Distance {
    final int distance;
    final boolean warning;
    
    public Distance(int distance, boolean warning) {
      this.distance = distance;
      this.warning = warning;
    }
  }
  
  static final int DEGREES = 360;
  static final int MAX_DIST = 9000;
  
  // Number of millimeters we estimate to be half of average body width.
  static final int BODY_WIDTH = 150;
    
  protected OutputStream os = System.out;
  
  void setOutput(OutputStream os) {
    this.os = os;
  }
  
  public abstract void readData(int angle, int distance, boolean warning) throws IOException;
}
