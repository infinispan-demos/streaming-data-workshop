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

import static utils.DomainUtils.*;

/**
 * @author galderz
 */
public class Stop implements Serializable {

  public final Train train;
  public final Date departureTs;
  private final byte[] platform; // nullable in this use case
  public final Date arrivalTs; // nullable in this use case

  public final int delayMin;

  // New entries
  public final Station station;
  public final Date ts; // nullable in this use case
  private final byte[] capacity1st;
  private final byte[] capacity2nd;

  public Stop(
    Train train, Date departureTs, byte[] platform,
    Date arrivalTs, int delayMin, Station station, Date ts,
    byte[] capacity1st, byte[] capacity2nd) {
    this.train = train;
    this.departureTs = departureTs;
    this.platform = platform;
    this.arrivalTs = arrivalTs;
    this.delayMin = delayMin;
    this.station = station;
    this.ts = ts;
    this.capacity1st = capacity1st;
    this.capacity2nd = capacity2nd;
  }

  public static Stop make(
    Train train, Date departureTs, String platform,
    Date arrivalTs, int delayMin, Station station, Date ts,
    String capacity1st, String capacity2nd) {
    return new Stop(
      train, departureTs, bs(platform), arrivalTs, delayMin, station,
      ts, bs(capacity1st), bs(capacity2nd));
  }

  public String getPlatform() {
    return str(platform);
  }

  public String getCapacity1st() {
    return str(capacity1st);
  }

  public String getCapacity2nd() {
    return str(capacity2nd);
  }

  @Override
  public String toString() {
    return "StationBoardEntryAnalytics{" +
      "train=" + train +
      ", departureTs=" + departureTs +
      ", platform='" + getPlatform() + '\'' +
      ", delayMin=" + delayMin +
      ", arrivalTs=" + arrivalTs +
      ", stop=" + station +
      ", ts=" + ts +
      ", capacity1st='" + getCapacity1st() + '\'' +
      ", capacity2nd='" + getCapacity2nd() + '\'' +
      '}';
  }

  public static final class Marshaller implements MessageMarshaller<Stop> {

    @Override
    public Stop readFrom(ProtoStreamReader reader) throws IOException {
      Train train = reader.readObject("train", Train.class);
      Date departureTs = reader.readDate("departureTs");
      byte[] platform = reader.readBytes("platform");
      Date arrivalTs = reader.readDate("arrivalTs");
      int delayMin = reader.readInt("delayMin");
      Station station = reader.readObject("station", Station.class);
      Date ts = reader.readDate("ts");
      byte[] capacity1st = reader.readBytes("capacity1st");
      byte[] capacity2nd = reader.readBytes("capacity2nd");
      return new Stop(
        train, departureTs, platform, arrivalTs, delayMin,
        station, ts, capacity1st, capacity2nd);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Stop obj) throws IOException {
      writer.writeObject("train", obj.train, Train.class);
      writer.writeDate("departureTs", obj.departureTs);
      writer.writeBytes("platform", obj.platform);
      writer.writeDate("arrivalTs", obj.arrivalTs);
      writer.writeInt("delayMin", obj.delayMin);
      writer.writeObject("station", obj.station, Station.class);
      writer.writeDate("ts", obj.ts);
      writer.writeBytes("capacity1st", obj.capacity1st);
      writer.writeBytes("capacity2nd", obj.capacity2nd);
    }

    @Override
    public Class<? extends Stop> getJavaClass() {
      return Stop.class;
    }

    @Override
    public String getTypeName() {
      return "analytics.Stop";
    }

  }

}
