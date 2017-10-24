package datamodel;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
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

  public static final class Marshaller implements MessageMarshaller<GeoLocBearing> {

    @Override
    public GeoLocBearing readFrom(ProtoStreamReader reader) throws IOException {
      Double lat = reader.readDouble("lat");
      Double lng = reader.readDouble("lng");
      Double bearing = reader.readDouble("bearing");
      return new GeoLocBearing(lat, lng, bearing);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, GeoLocBearing obj) throws IOException {
      writer.writeDouble("lat", obj.lat);
      writer.writeDouble("lng", obj.lng);
      writer.writeDouble("bearing", obj.bearing);
    }

    @Override
    public Class<? extends GeoLocBearing> getJavaClass() {
      return GeoLocBearing.class;
    }

    @Override
    public String getTypeName() {
      return "datamodel.GeoLocBearing";
    }

  }

}
