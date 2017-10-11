package datamodel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static utils.DomainUtils.bs;
import static utils.DomainUtils.str;

public class TrainPosition implements Serializable {

  final byte[] trainId;
  final byte[] name;
  final byte[] cat;
  final byte[] lastStopName;
  public final TimedPosition current;
  public final List<TimedPosition> futurePositions;
  public final int delay;

  private TrainPosition(byte[] trainId, byte[] name, int delay, byte[] cat, byte[] lastStopName, TimedPosition current, List<TimedPosition> futurePositions) {
    this.trainId = trainId;
    this.name = name;
    this.cat = cat;
    this.lastStopName = lastStopName;
    this.current = current;
    this.futurePositions = futurePositions;
    this.delay = delay;
  }

  public String getTrainId() {
    return str(trainId);
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
      "trainId=" + getTrainId() +
      ", name=" + getName() +
      ", delay=" + delay +
      ", cat=" + getCat() +
      ", lastStopName=" + getLastStopName() +
      ", current=" + current +
      ", futurePositions=" + futurePositions +
      '}';
  }

  public static TrainPosition make(String trainId, String name, int delay, String cat, String lastStopName, TimedPosition current, List<TimedPosition> futurePositions) {
    return new TrainPosition(
      bs(trainId), bs(name), delay, bs(cat), bs(lastStopName), current, futurePositions);
  }

}
