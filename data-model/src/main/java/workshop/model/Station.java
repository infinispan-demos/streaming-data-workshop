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

import static workshop.model.ModelUtils.*;

/**
 * @author galderz
 */
public class Station implements Serializable {

  public final long id;
  private final byte[] name;
//   private GeoLoc loc;

  private Station(long id, byte[] name) {
    this.id = id;
    this.name = name;
  }

  public static Station make(long id, String name) {
    return new Station(id, bs(name));
  }

  public String getName() {
    return str(name);
  }

  @Override
  public String toString() {
    return "Station{" +
      "id=" + id +
      ", name='" + getName() + '\'' +
      '}';
  }

  public static final class Marshaller implements MessageMarshaller<Station> {

    @Override
    public Station readFrom(ProtoStreamReader reader) throws IOException {
      long id = reader.readLong("id");
      byte[] name = reader.readBytes("name");
      return new Station(id, name);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Station obj) throws IOException {
      writer.writeLong("id", obj.id);
      writer.writeBytes("name", obj.name);
    }

    @Override
    public Class<? extends Station> getJavaClass() {
      return Station.class;
    }

    @Override
    public String getTypeName() {
      return "workshop.model.Station";
    }

  }


}
