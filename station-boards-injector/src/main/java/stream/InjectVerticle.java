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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author Thomas Segismont
 * @author galderz
 */
public class InjectVerticle extends AbstractVerticle {
  private static final Logger log = Logger.getLogger(InjectVerticle.class.getName());

  static Calendar calendar = null;
  static String lastDate = "?";

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
    long timerId = vertx.setPeriodic(5000L, l -> {
      log.info(stopsLoaded + " stops loaded");
    });
    Util.rxReadGunzippedTextResource("station-boards-dump-3_weeks.tsv.gz")
      .skip(1) // header
      .map(this::toEntry)
      .doOnNext(entry -> stopsCache.put(entry.getKey(), entry.getValue()))
      .doOnNext(entry -> stopsLoaded.incrementAndGet())
      .doAfterTerminate(() -> vertx.cancelTimer(timerId))
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
    String[] parts = line.split("\t");

    String id = parts[0];
    //Date entryTs = parseTimestamp(parts[1]);
    //Date entryTs = null;
    long stopId = Long.parseLong(parts[2]);
    String stopName = parts[3];
    Date departureTs = parseTimestamp(parts[4]);
    String trainName = parts[5];
    String trainCat = parts[6];
    String trainOperator = parts[7];
    String trainTo = parts[8];
    int delayMin = parts[9].isEmpty() ? 0 : Integer.parseInt(parts[9]);
    String capacity1st = parts[10];
    String capacity2nd = parts[11];

    Train train = Train.make(trainName, trainTo, trainCat, trainOperator);
    Station station = Station.make(stopId, stopName);
    Stop stop = Stop.make(train, departureTs, null, null, delayMin, station, null, capacity1st, capacity2nd);

    return new SimpleImmutableEntry<>(id, stop);
  }

  private static Date parseTimestamp(String date) {
    if (!date.startsWith(lastDate)) {
      //System.out.println("New date!");
      Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"), Locale.ENGLISH);
      int year = Integer.parseInt(date.substring(11, 15));
      int month = toMonth(date.substring(4, 7));
      int day = Integer.parseInt(date.substring(8, 10));
//         System.out.println("Year: " + year);
//         System.out.println("Month: " + month);
//         System.out.println("Day: " + day);
      c.set(year, month, day, 0, 0, 0);
      calendar = c;
      lastDate = date.substring(0, 15);
    }

    String hour = date.substring(16, 18);
    String minute = date.substring(19, 21);
//      System.out.println("Hour: " + hour);
//      System.out.println("Minute: " + minute);
    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
    calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
//      System.out.println(calendar.getTime());
    return calendar.getTime();
  }

  private static int toMonth(String m) {
    switch (m) {
      case "Jan":
        return Calendar.JANUARY;
      case "Feb":
        return Calendar.FEBRUARY;
      case "Mar":
        return Calendar.MARCH;
      case "Apr":
        return Calendar.APRIL;
      case "May":
        return Calendar.MAY;
      case "Jun":
        return Calendar.JUNE;
      case "Jul":
        return Calendar.JULY;
      case "Aug":
        return Calendar.AUGUST;
      case "Sep":
        return Calendar.SEPTEMBER;
      case "Oct":
        return Calendar.OCTOBER;
      case "Nov":
        return Calendar.NOVEMBER;
      case "Dec":
        return Calendar.DECEMBER;
      default:
        throw new IllegalArgumentException("Unknown month: `" + m + "`");
    }
  }

}
