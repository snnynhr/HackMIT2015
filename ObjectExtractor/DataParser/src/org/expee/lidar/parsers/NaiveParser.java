package org.expee.lidar.parsers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Assume that the first fixed number of rotations is a calibration period where
 * no humans are in the field of view. Afterwards, it performs naive coalescing to 
 * perform human segmentation. The basic premise is as follows:
 * 
 * - For the first CALIBRATION_CYCLE readings, record total distances and use this data to
 *   represent the background environment
 * - For the remaining incoming data, calculate the existence of objects by filtering away 
 *   background information and calculating each object. An object is detected by seeing if any
 *   information is available and if the data recorded is more than 2 of the standard deviation
 *   of the background calibration towards the lidar. We then distinguish between bodies by
 *   separating upon the first non-data measurement or when the contiguous body becomes
 *   wider than MAX_BODY_WIDTH.
 * - Only one background print occurs, so no background updating is performed.
 */
public class NaiveParser extends Parser {
  private static final int CALIBRATION_CYCLES = 60;
  private int cycles;
  
  // Cutoff for a body (in millimeters) for the width of a body
  private static final int MAX_BODY_WIDTH = 1000;
  private static final int MIN_BODY_WIDTH = 15;
  
  private static class Background {
    private List<Integer> measurements;
    private double mean;
    private double sd;
    
    public Background() {
      measurements = new ArrayList<Integer>();
      mean = 0;
    }
    
    public void addData(int value) {
      measurements.add(value);
      mean += value;
    }
    
    public void finish() {
      mean /= measurements.size();
      
      sd = 0;
      for (int meas : measurements) {
        sd += Math.pow(meas - mean, 2);
      }
      sd /= measurements.size() - 1;
      sd = Math.sqrt(sd);
      
      // Clear measurements to save on memory
      measurements.clear();
    }
  }
 
  // Used for initial background calibration
  private Background[] background = new Background[DEGREES];
  
  private int[] data = new int[DEGREES];
  private int prevAngle = -1;
  
  public NaiveParser() {
    cycles = 0;
  }

  public void readData(int angle, int distance, boolean warning) throws IOException {
    boolean flipped = false;
    while (prevAngle != angle) {
      ++prevAngle;
      if (prevAngle == DEGREES) {
        prevAngle = 0;
        flipped = true;
      }
      if (cycles < CALIBRATION_CYCLES) {
        if (background[prevAngle] == null) {
          background[prevAngle] = new Background();
        }
        if (prevAngle != angle || distance <= 0 || distance >= MAX_DIST) {
          background[prevAngle].addData(MAX_DIST);
        } else {
          background[angle].addData(distance);
        }
      } else {
        data[prevAngle] = MAX_DIST;
      }
    }
    if (distance > 0 && distance < MAX_DIST) {
      data[angle] = distance;
    }
    
    if (flipped) {
      if (cycles < CALIBRATION_CYCLES) {
        ++cycles;
        if (cycles == CALIBRATION_CYCLES) {
          flushBackground();
        }
      } else {
        flushData();
      }
    }
  }
    
  // Flushes the human info data to the outputstream
  private void flushData() throws IOException {
    int start = 0;
    while (start < DEGREES && !isData(start)) {
      ++start;
    }
    start %= DEGREES;

    int pos = start;
    int begin = -1;
    double avgDist = 0;
    do {
      if (begin != -1 && (!isData(pos) || getWidth(begin, pos, avgDist) > MAX_BODY_WIDTH)) {
        // If we started an object and we reached no data or the body is too wide/thin, finish.
        if (getWidth(begin, pos, avgDist) > MIN_BODY_WIDTH) {
          out.format("OBJECT %d %f%n", getMid(begin, pos), 
              avgDist / getDiff(begin, pos) - BODY_WIDTH);
          out.flush();
        }
        begin = -1;
        avgDist = 0;
      } else if (isData(pos)) {
        if (begin == -1) {
          begin = pos;
        }
        avgDist += data[pos];
      }
      pos = (pos + 1) % DEGREES;
    } while (pos != start);
    if (begin != -1 && getWidth(begin, pos, avgDist) >= MIN_BODY_WIDTH) {
      // Flush any remaining if we didn't get to it
      out.format("OBJECT %d %f%n", getMid(begin, pos), avgDist / getDiff(begin, pos) - BODY_WIDTH);
    }    
    out.flush();
  }
  
  // Assume [begin, stop) is a body. Get the midpoint (in degrees) of the body
  private int getMid(int begin, int stop) {
    stop = (stop - 1) % DEGREES;
    if (begin > stop) {
      stop += DEGREES;
    }
    return ((begin + stop) / 2) % DEGREES;
  }
  
  // Assume that [begin, stop) is a body. Get the width (in degrees) of the body
  private int getDiff(int begin, int stop) {
    return (stop + DEGREES - begin) % DEGREES;
  }
  
  private boolean isData(int pos) {
    Background b = background[pos];
    //System.err.println(b.mean - 2 * b.sd + " " + data[pos]);
    return b.mean - 2 * b.sd > data[pos];
  }
  
  private double getWidth(int begin, int cur, double avgDist) {
    return avgDist * 2 * Math.PI * getDiff(begin, cur) / DEGREES;
  }
  
  // Flushes the background data to the outputstream
  private void flushBackground() throws IOException {
    for (Background b : background) {
      b.finish();
    }
    
    int start = 0;
    while (start < DEGREES && !isBackground(start)) {
      ++start;
    }
    start %= DEGREES;
    
    out.format("BACKGROUND START%n");
    int pos = start;
    int vertex = 0;
    do {
      if (!isBackground(pos)) {
        out.format("BACKGROUND POLY%d %d %f%n", ++vertex, pos, background[pos].mean);
      } else {
        vertex = 0;
      }
      pos = (pos + 1) % DEGREES;
    } while (pos != start);
    out.format("BACKGROUND END%n");
    out.flush();
  }
  
  private boolean isBackground(int pos) {
    Background b = background[pos];
    return b.mean + 2 * b.sd >= MAX_DIST;
  }
}
