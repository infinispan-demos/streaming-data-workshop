package datamodel;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
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

  public static final class Marshaller implements MessageMarshaller<TimedPosition> {

    @Override
    public TimedPosition readFrom(ProtoStreamReader reader) throws IOException {
      Long timestamp = reader.readLong("timestamp");
      GeoLocBearing position = reader.readObject("position", GeoLocBearing.class);
      return new TimedPosition(timestamp, position);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TimedPosition obj) throws IOException {
      writer.writeLong("timestamp", obj.timestamp);
      writer.writeObject("position", obj.position, GeoLocBearing.class);
    }

    @Override
    public Class<? extends TimedPosition> getJavaClass() {
      return TimedPosition.class;
    }

    @Override
    public String getTypeName() {
      return "datamodel.TimedPosition";
    }

  }

}
