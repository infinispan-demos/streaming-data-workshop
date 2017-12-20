/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package workshop.model;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.vertx.core.json.JsonObject;

/**
 * @author galderz
 */
public class Stop implements Serializable {

  public final Train train;
  public final int delayMin;
  public final Station station;
  public final Date departureTs;

  public Stop(Train train, int delayMin, Station station, Date departureTs) {
    this.train = train;
    this.delayMin = delayMin;
    this.station = station;
    this.departureTs = departureTs;
  }

  public static Stop make(Train train, int delayMin, Station station, Date departureTs) {
    return new Stop(train, delayMin, station, departureTs);
  }

  @Override
  public String toString() {
    return "Stop{" +
      "train=" + train +
      ", delayMin=" + delayMin +
      ", station=" + station +
      ", departureTs=" + departureTs +
      '}';
  }

  public static Stop make(String message) {
    JsonObject json = new JsonObject(message);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");
    String trainCat = json.getString("category");
    String trainOperator = json.getString("operator");

    Train train = Train.make(trainName, trainTo, trainCat, trainOperator);

    JsonObject jsonStop = json.getJsonObject("stop");
    JsonObject jsonStation = jsonStop.getJsonObject("station");
    long stationId = Long.parseLong(jsonStation.getString("id"));
    String stationName = jsonStation.getString("name");
    Station station = Station.make(stationId, stationName);

    Date departureTs = new Date(jsonStop.getLong("departureTimestamp") * 1000);
    int delayMin = orNull(jsonStop.getValue("delay"), 0);

    String stopId = String.format(
      "%s/%s/%s/%s",
      stationId, trainName, trainTo, jsonStop.getString("departure")
    );

    return Stop.make(train, delayMin, station, departureTs);
  }

  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

  public static final class Marshaller implements MessageMarshaller<Stop> {

    @Override
    public Stop readFrom(ProtoStreamReader reader) throws IOException {
      Train train = reader.readObject("train", Train.class);
      int delayMin = reader.readInt("delayMin");
      Station station = reader.readObject("station", Station.class);
      Date departureTs = reader.readDate("departureTs");
      return new Stop(train, delayMin, station, departureTs);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Stop obj) throws IOException {
      writer.writeObject("train", obj.train, Train.class);
      writer.writeInt("delayMin", obj.delayMin);
      writer.writeObject("station", obj.station, Station.class);
      writer.writeDate("departureTs", obj.departureTs);
    }

    @Override
    public Class<? extends Stop> getJavaClass() {
      return Stop.class;
    }

    @Override
    public String getTypeName() {
      return "workshop.model.Stop";
    }

  }

}
