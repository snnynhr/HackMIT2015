package org.expee.lidar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.expee.lidar.parsers.MLParser;
import org.expee.lidar.parsers.Parser;

public class DataReader {
  
  public static void readData(Parser p, InputStream is) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String nextline = "";
		while ((nextline = in.readLine()) != null) {
			String[] data = nextline.split(" ");
			if (data.length < 3) {
			  System.err.println("Invalid input: " + nextline);
			}
			int angle = Integer.parseInt(data[0]);
			int distance = Integer.parseInt(data[1]);
			boolean warning = data[2].equals("True");
			p.readData(angle, distance, warning);
		}
	}
  
  public static void readData(Parser p) throws IOException {
    readData(p, System.in);
  }

  public static void main(String[] args) throws Exception {
    // Change this to use explicit input/output streams
    // Currently just assumes input in standard in and output in standard out
    DataReader.readData(new MLParser("data/Classifier1.model"));
  }
}
