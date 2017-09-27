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

package stream;

import datamodel.Station;
import datamodel.Stop;
import datamodel.Train;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import rx.Observable;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * @author Thomas Segismont
 */
public class Util {

  /**
   * Open a gunzipped classpath resource for reading.
   *
   * The returned {@link Observable} emits lines of text and operates on the {@link Schedulers#io() IO-scheduler}.
   */
  public static Observable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = Util.class.getClassLoader().getResource(resource);
    Objects.requireNonNull(url);
    Observable<String> uncompressed = StringObservable.using(() -> {
      InputStream inputStream = url.openStream();
      InputStream gzipStream = new GZIPInputStream(inputStream);
      Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
      return new BufferedReader(decoder);
    }, StringObservable::from)
      .compose(StringObservable::byLine);
    return uncompressed
      .subscribeOn(Schedulers.io());
  }

  public static RemoteCacheManager mkRemoteCacheManager() {
    RemoteCacheManager client = new RemoteCacheManager();
    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);
    addProtoDescriptorToClient(ctx);
    addProtoMarshallersToClient(ctx);
    return client;
  }

  private static void addProtoDescriptorToClient(SerializationContext ctx) {
    try {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources("datamodel.proto"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void addProtoMarshallersToClient(SerializationContext ctx) {
    ctx.registerMarshaller(new Stop.Marshaller());
    ctx.registerMarshaller(new Station.Marshaller());
    ctx.registerMarshaller(new Train.Marshaller());
  }

  @SuppressWarnings("unchecked")
  static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

  static <T> T s(NoisySupplier<T> s) {
    try {
      return s.get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public interface NoisySupplier<T> {
    T get() throws Exception;
  }

  private Util() {
    // Utility class
  }
}
