package workshop.model;

import static workshop.model.ModelUtils.bs;
import static workshop.model.ModelUtils.str;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import org.infinispan.protostream.MessageMarshaller;

import io.vertx.core.json.JsonObject;

public class TrainPosition implements Serializable {

  final byte[] trainId;
  final byte[] name;
  public final int delay;
  final byte[] cat;
  final byte[] lastStopName;
  public final TimedPosition current;
  // public final List<TimedPosition> futurePositions;

  private TrainPosition(byte[] trainId, byte[] name, int delay, byte[] cat, byte[] lastStopName, TimedPosition current) {
    this.trainId = trainId;
    this.name = name;
    this.delay = delay;
    this.cat = cat;
    this.lastStopName = lastStopName;
    this.current = current;
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
      '}';
  }

  public static TrainPosition make(String message) {
    JsonObject json = new JsonObject(message);

    String trainId = json.getString("trainid");
    long ts = json.getLong("timeStamp");
    String name = json.getString("name").trim();
    String cat = json.getString("category").trim();
    String lastStopName = json.getString("lstopname").trim();
    int delay = Integer.valueOf(orNull(json.getString("delay"), "0"));

    double y = Double.parseDouble(json.getString("y")) / 1000000;
    double x = Double.parseDouble(json.getString("x")) / 1000000;
    String dirOrEmpty = json.getString("direction");
    Double direction = dirOrEmpty.isEmpty() ? null : Double.parseDouble(dirOrEmpty) * 10;
    TimedPosition current = new TimedPosition(ts, new GeoLocBearing(y, x, direction));

    // TODO: Parse future positions to get continuous move (poly field)

    return TrainPosition.make(trainId, name, delay, cat, lastStopName, current);
  }

  private static TrainPosition make(String trainId, String name, int delay, String cat, String lastStopName, TimedPosition current) {
    return new TrainPosition(
      bs(trainId), bs(name), delay, bs(cat), bs(lastStopName), current);
  }

  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

  public static final class Marshaller implements MessageMarshaller<TrainPosition> {

    @Override
    public TrainPosition readFrom(ProtoStreamReader reader) throws IOException {
      String trainId = reader.readString("trainId");
      String name = reader.readString("name");
      Integer delay = reader.readInt("delay");
      byte[] cat = reader.readBytes("cat");
      byte[] lastStopName = reader.readBytes("lastStopName");
      TimedPosition current = reader.readObject("current", TimedPosition.class);
      return new TrainPosition(bs(trainId), bs(name), delay, cat, lastStopName, current);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TrainPosition obj) throws IOException {
      writer.writeString("trainId", obj.getTrainId());
      writer.writeString("name", obj.getName());
      writer.writeInt("delay", obj.delay);
      writer.writeBytes("cat", obj.cat);
      writer.writeBytes("lastStopName", obj.lastStopName);
      writer.writeObject("current", obj.current, TimedPosition.class);
    }

    @Override
    public Class<? extends TrainPosition> getJavaClass() {
      return TrainPosition.class;
    }

    @Override
    public String getTypeName() {
      return "workshop.model.TrainPosition";
    }

  }
}
