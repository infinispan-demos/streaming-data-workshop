package datamodel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static utils.DomainUtils.bs;
import static utils.DomainUtils.str;

public class TrainPosition implements Serializable {

  final byte[] name;
  final byte[] cat;
  final byte[] lastStopName;
  final TimedPosition current;
  final List<TimedPosition> futurePositions;

  private TrainPosition(byte[] name, byte[] cat, byte[] lastStopName, TimedPosition current, List<TimedPosition> futurePositions) {
    this.name = name;
    this.cat = cat;
    this.lastStopName = lastStopName;
    this.current = current;
    this.futurePositions = futurePositions;
  }

  public String getName() {
    return str(name);
  }

  public String getCat() {
    return str(cat);
  }

  public String getLastStopName() {
    return str(lastStopName);
  }

  @Override
  public String toString() {
    return "TrainPosition{" +
      "name=" + getName() +
      ", cat=" + getCat() +
      ", lastStopName=" + getLastStopName() +
      ", current=" + current +
      ", futurePositions=" + futurePositions +
      '}';
  }

  public static TrainPosition make(String name, String cat, String lastStopName, TimedPosition current, List<TimedPosition> futurePositions) {
    return new TrainPosition(bs(name), bs(cat), bs(lastStopName), current, futurePositions);
  }

}
