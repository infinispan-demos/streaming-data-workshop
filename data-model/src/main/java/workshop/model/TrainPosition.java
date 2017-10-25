package workshop.model;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.io.Serializable;

import static workshop.model.ModelUtils.bs;
import static workshop.model.ModelUtils.str;

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

  public static TrainPosition make(String trainId, String name, int delay, String cat, String lastStopName, TimedPosition current) {
    return new TrainPosition(
      bs(trainId), bs(name), delay, bs(cat), bs(lastStopName), current);
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
