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
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static stream.Util.orNull;
import static stream.Util.s;

/**
 * @author Thomas Segismont
 * @author galderz
 */
public class InjectVerticle extends AbstractVerticle {
  private static final Logger log = Logger.getLogger(InjectVerticle.class.getName());

  RemoteCacheManager client;
  RemoteCache<String, Stop> stopsCache;

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
    long start = System.nanoTime();
    long timerId = vertx.setPeriodic(5000L, l -> {
      log.info(String.format(
        "Progress: loaded=%d stored=%d%n", stopsLoaded.get(), stopsCache.size()
      ));
    });
    Util.rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz")
      .map(this::toEntry)
      .doOnError(Throwable::printStackTrace)
      .doOnNext(entry -> stopsCache.put(entry.getKey(), entry.getValue()))
      .doOnNext(entry -> stopsLoaded.incrementAndGet())
      .doAfterTerminate(() -> {
        final long duration = System.nanoTime() - start;
        log.info(String.format(
          "Duration: %d(s) %n", TimeUnit.NANOSECONDS.toSeconds(duration)
        ));
        vertx.cancelTimer(timerId);
      })
      .subscribe();
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
    JSONParser parser = new JSONParser();

    JSONObject json = (JSONObject) s(() -> parser.parse(line));
    String trainName = (String) json.get("name");
    String trainTo = (String) json.get("to");
    String trainCat = (String) json.get("category");
    String trainOperator = (String) json.get("operator");
    String capacity1st = (String) json.get("capacity1st");
    String capacity2nd = (String) json.get("capacity2nd");
    Train train = Train.make(trainName, trainTo, trainCat, trainOperator);

    JSONObject jsonStop = (JSONObject) json.get("stop");
    JSONObject jsonStation = (JSONObject) jsonStop.get("station");
    long stationId = Long.parseLong((String) jsonStation.get("id"));
    String stationName = (String) jsonStation.get("name");
    Station station = Station.make(stationId, stationName);

    Date departureTs = new Date((long) jsonStop.get("departureTimestamp") * 1000);
    Object delayMin = jsonStop.get("delay");

    String stopId = String.format(
      "%s/%s/%s/%s",
      stationId, trainName, trainTo, jsonStop.get("departure")
    );

    Stop stop = Stop.make(train, departureTs, null, null,
      orNull(delayMin, 0L).intValue(), station, null, capacity1st, capacity2nd);

    return new SimpleImmutableEntry<>(stopId, stop);
  }

}
