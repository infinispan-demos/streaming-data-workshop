package dashboard;

import javafx.beans.property.SimpleStringProperty;

import java.util.Comparator;

public class DelayedTrainView {

  private final SimpleStringProperty type;
  private final SimpleStringProperty departure;
  private final SimpleStringProperty station;
  private final SimpleStringProperty destination;
  private final SimpleStringProperty delay;
  private final SimpleStringProperty trainName;

  public DelayedTrainView(String type,
    String departure,
    String station,
    String destination,
    String delay,
    String trainName) {
    this.type = new SimpleStringProperty(type);
    this.departure = new SimpleStringProperty(departure);
    this.station = new SimpleStringProperty(station);
    this.destination = new SimpleStringProperty(destination);
    this.delay = new SimpleStringProperty(delay);
    this.trainName = new SimpleStringProperty(trainName);
  }

  public String getType() {
    return type.get();
  }

  public SimpleStringProperty typeProperty() {
    return type;
  }

  public void setType(String type) {
    this.type.set(type);
  }

  public String getDeparture() {
    return departure.get();
  }

  public SimpleStringProperty departureProperty() {
    return departure;
  }

  public void setDeparture(String departure) {
    this.departure.set(departure);
  }

  public String getStation() {
    return station.get();
  }

  public SimpleStringProperty stationProperty() {
    return station;
  }

  public void setStation(String station) {
    this.station.set(station);
  }

  public String getDestination() {
    return destination.get();
  }

  public SimpleStringProperty destinationProperty() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination.set(destination);
  }

  public String getDelay() {
    return delay.get();
  }

  public SimpleStringProperty delayProperty() {
    return delay;
  }

  public void setDelay(String delay) {
    this.delay.set(delay);
  }

  public String getTrainName() {
    return trainName.get();
  }

  public SimpleStringProperty trainNameProperty() {
    return trainName;
  }

  public void setTrainName(String trainName) {
    this.trainName.set(trainName);
  }

  public static Comparator<DelayedTrainView> comparator() {
    return new DepatureComparator();
  }

  static final class DepatureComparator implements Comparator<DelayedTrainView> {

    @Override
    public int compare(DelayedTrainView o1, DelayedTrainView o2) {
      return o2.getDeparture().compareTo(o1.getDeparture());
    }

  }

}
