package datamodel;

import java.io.Serializable;

public class TimedPosition implements Serializable {

  public final long timestamp;
  public final GeoLocBearing position;

  public TimedPosition(long timestamp, GeoLocBearing position) {
    this.timestamp = timestamp;
    this.position = position;
  }

  @Override
  public String toString() {
    return "TimedPosition{" +
      "timestamp=" + timestamp +
      ", position=" + position +
      '}';
  }

}
