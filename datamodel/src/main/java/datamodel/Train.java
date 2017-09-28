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

import static utils.DomainUtils.*;

/**
 * @author galderz
 */
public class Train implements Serializable {

  //   private String id;
  private final byte[] name;
  private final byte[] to;
  private final byte[] cat;
  private final byte[] operator; // nullable

  private Train(byte[] name, byte[] to, byte[] cat, byte[] operator) {
    this.name = name;
    this.to = to;
    this.cat = cat;
    this.operator = operator;
  }

  public static Train make(String name, String to, String cat, String operator) {
    return new Train(bs(name), bs(to), bs(cat), bs(operator));
  }

  public String getName() {
    return str(name);
  }

  public String getTo() {
    return str(to);
  }

  public String getCategory() {
    return str(cat);
  }

  public String getOperator() {
    return str(operator);
  }

  @Override
  public String toString() {
    return "Train{" +
      "name='" + getName() + '\'' +
      ", to='" + getTo() + '\'' +
      ", category='" + getCategory() + '\'' +
      ", operator='" + getOperator() + '\'' +
      '}';
  }

  public static final class Marshaller implements MessageMarshaller<Train> {

    @Override
    public Train readFrom(ProtoStreamReader reader) throws IOException {
      byte[] name = reader.readBytes("name");
      byte[] to = reader.readBytes("to");
      byte[] cat = reader.readBytes("cat");
      byte[] operator = reader.readBytes("operator");
      return new Train(name, to, cat, operator);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Train obj) throws IOException {
      writer.writeBytes("name", obj.name);
      writer.writeBytes("to", obj.to);
      writer.writeBytes("cat", obj.cat);
      writer.writeBytes("operator", obj.operator);
    }

    @Override
    public Class<? extends Train> getJavaClass() {
      return Train.class;
    }

    @Override
    public String getTypeName() {
      return "datamodel.Train";
    }

  }

}
