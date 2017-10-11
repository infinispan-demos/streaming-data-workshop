package datamodel;

import java.io.Serializable;

public class GeoLocBearing implements Serializable {

  public final double lat;
  public final double lng;
  public final Double bearing; // nullable

  public GeoLocBearing(double lat, double lng, Double bearing) {
    this.lat = lat;
    this.lng = lng;
    this.bearing = bearing;
  }

  @Override
  public String toString() {
    return "GeoLocBearing{" +
      "lat=" + lat +
      ", lng=" + lng +
      ", bearing=" + bearing +
      '}';
  }

}
