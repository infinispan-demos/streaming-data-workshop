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
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import rx.functions.Actions;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static java.util.logging.Level.*;
import static stream.Util.*;

/**
 * @author Thomas Segismont
 * @author galderz
 */
public class InjectVerticle extends AbstractVerticle {
  private static final Logger log = Logger.getLogger(InjectVerticle.class.getName());

  RemoteCacheManager client;
  RemoteCache<String, Stop> stopsCache;
  private long loadStart;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(InjectVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(new RemoteCacheManager()))
      .doOnSuccess(remoteCacheManager -> client = remoteCacheManager).<Void>map(x -> null)
      .flatMap(v -> vertx.<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> fut.complete(client.getCache("default"))))
      .doOnSuccess(remoteCache -> stopsCache = remoteCache).<Void>map(x -> null)
      .subscribe(result -> {
        startFuture.complete(result);
        startLoading();
      }, startFuture::fail);
  }

  private void startLoading() {
    AtomicLong stopsLoaded = new AtomicLong();
    loadStart = System.nanoTime();
    vertx.setPeriodic(5000L, l -> {
      vertx.executeBlocking(fut -> {
        log.info(String.format(
          "Progress: loaded=%d stored=%d%n", stopsLoaded.get(), stopsCache.size()
        ));
        fut.complete();
      }, false, ar -> {});
    });
    Util.rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz")
      .map(this::toEntry)
      .doAfterTerminate(() -> {
        final long duration = System.nanoTime() - loadStart;
        log.info(String.format(
          "Duration: %d(s) %n", TimeUnit.NANOSECONDS.toSeconds(duration)
        ));
        loadStart = System.nanoTime();
        stopsLoaded.set(0);
      })
      .repeat()
      .doOnNext(entry -> stopsCache.put(entry.getKey(), entry.getValue()))
      .doOnNext(entry -> stopsLoaded.incrementAndGet())
      .subscribe(Actions.empty(), t -> log.log(SEVERE, "Error while loading station boards", t));
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    vertx.<Void>rxExecuteBlocking(fut -> {
      if (stopsCache != null)
        stopsCache.clear();

      if (client != null)
        client.stop();

      fut.complete();
    }).subscribe(stopFuture::complete, stopFuture::fail);
  }

  private Entry<String, Stop> toEntry(String line) {
    JsonObject json = new JsonObject(line);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");
    String trainCat = json.getString("category");
    String trainOperator = json.getString("operator");
    String capacity1st = json.getString("capacity1st");
    String capacity2nd = json.getString("capacity2nd");
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

    Stop stop = Stop.make(train, departureTs, null, null, delayMin, station, null, capacity1st, capacity2nd);

    return new SimpleImmutableEntry<>(stopId, stop);
  }

}
