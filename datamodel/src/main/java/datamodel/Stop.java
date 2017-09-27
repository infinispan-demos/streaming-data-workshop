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

package datamodel;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

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
      return "datamodel.Stop";
    }

  }

}
