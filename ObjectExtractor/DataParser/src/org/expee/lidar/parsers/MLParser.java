package org.expee.lidar.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;


public class MLParser extends Parser {
  private static final int CALIBRATION_CYCLES = 60;
  private int cycles;
     
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
  
  private Classifier cls;
  
  private List<Attribute> attributes = new ArrayList<Attribute>();
  
  public MLParser(String classifierPath) throws IOException, ClassNotFoundException {
    cycles = 0;
    
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(classifierPath));
    cls = (Classifier) ois.readObject();
    ois.close();
    
    for (int i = -22; i <= 22; i++) {
      attributes.add(new Attribute("Angle" + i));
    }
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
    double[] confidence = new double[DEGREES / 5];
    for (int i = 0; i < DEGREES; i += 5) {
      if (isData(i)) {
        Instance instance = new DenseInstance(attributes.size());
        int init = data[i];
        for (int j = 0; j < 45; j++) {
          int offset = j - 22;
          int pos = (i + DEGREES + offset) % DEGREES;
          if (isData(pos)) {
            instance.setValue(attributes.get(j), init - data[pos]);
          } else {
            instance.setValue(attributes.get(j), MAX_DIST);
          }
        }
        try {
          double[] dist = cls.distributionForInstance(instance);
          confidence[i] = dist[1];
        } catch (Exception e) {
          confidence[i] = 0;
          e.printStackTrace();
        }
      }
    }
    
    for (int i = 0; i < confidence.length; i++) {
      int before = (i == 0) ? confidence.length - 1 : i - 1;
      int after = (i == confidence.length - 1) ? 0 : i + 1;
      if (confidence[i] > confidence[before] && confidence[i] > confidence[after]) {
        out.format("OBJECT %d %f%n", i * 5, data[i * 5] - BODY_WIDTH);
        out.flush();
      }
    }
    
    out.println("TIMESLICE END");
    out.flush();
  }
  
  private boolean isData(int pos) {
    Background b = background[pos];
    //System.err.println(b.mean - 2 * b.sd + " " + data[pos]);
    return (b.mean - 2 * b.sd - 500) > data[pos];
  }
  
  // Flushes the background data to the outputstream
  private void flushBackground() throws IOException {
    for (Background b : background) {
      b.finish();
    }
    
    int start = 0;
    while (start < DEGREES && isBackground(start)) {
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
