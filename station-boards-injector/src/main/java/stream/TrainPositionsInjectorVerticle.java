package stream;

import datamodel.GeoLocBearing;
import datamodel.TimedPosition;
import datamodel.TrainPosition;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import rx.functions.Actions;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static stream.Util.mkRemoteCacheManager;
import static stream.Util.orNull;

/**
 * TODO: Should live in separate module
 */
public class TrainPositionsInjectorVerticle extends AbstractVerticle {
  private static final Logger log = Logger.getLogger(TrainPositionsInjectorVerticle.class.getName());

  RemoteCacheManager client;

  // A train route has potentially multiple trains doing same route at one point in time.
  // Each train has a train id but that cannot be correlated with the station board data.
  // So, maintain a map of train ids associated with a particular train route.
  // When a train route is delayed, find the first train id with delays and track that train's positions.
  RemoteCache<String, Map<String, TrainPosition>> positionsCache;
  private long loadStart;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(TrainPositionsInjectorVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createRemoteCacheManager()))
      .doOnSuccess(remoteCacheManager -> {
        client = remoteCacheManager;
        client.administration().createCache("train-positions", "distributed");
      }).<Void>map(x -> null)
      .flatMap(v -> vertx.<RemoteCache<String, Map<String, TrainPosition>>>rxExecuteBlocking(fut -> fut.complete(client.getCache("train-positions"))))
      .doOnSuccess(remoteCache -> positionsCache = remoteCache).<Void>map(x -> null)
      .subscribe(result -> {
        startFuture.complete(result);
        startLoading();
      }, startFuture::fail);
  }

  public RemoteCacheManager createRemoteCacheManager() {
    Properties p = new Properties();
    p.put("infinispan.client.hotrod.server_list", "${server.host:datagrid-hotrod}:${server.port:11222}");
    ConfigurationBuilder cfg = new ConfigurationBuilder();
    cfg.withProperties(p);
    RemoteCacheManager client = new RemoteCacheManager(cfg.build());
    return client;
  }

  private void startLoading() {
    AtomicLong stopsLoaded = new AtomicLong();
    loadStart = System.nanoTime();
    vertx.setPeriodic(5000L, l -> {
      vertx.executeBlocking(fut -> {
        log.info(String.format(
          "Progress: loaded=%d stored=%d%n", stopsLoaded.get(), positionsCache.size()
        ));
        fut.complete();
      }, false, ar -> {});
    });
    Util.rxReadGunzippedTextResource("cff_train_position-2016-02-29__.jsonl.gz")
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
      .doOnNext(entry -> {
          String routeName = entry.getKey();
          Map<String, TrainPosition> trainPositions = positionsCache.get(routeName);
          if (trainPositions == null)
            trainPositions = new HashMap<>();

          trainPositions.put(entry.getValue().getTrainId(), entry.getValue());
          positionsCache.put(routeName, trainPositions);
        }
      )
      .doOnNext(entry -> stopsLoaded.incrementAndGet())
      .subscribe(Actions.empty(), t -> log.log(SEVERE, "Error while loading station boards", t));
  }

  private Entry<String, TrainPosition> toEntry(String line) {
    JsonObject json = new JsonObject(line);

    String trainId = json.getString("trainid");
    long ts = json.getLong("timeStamp");
    String name = json.getString("name").trim();
    String cat = json.getString("category").trim();
    String lastStopName = json.getString("lstopname").trim();
    int delay = Integer.valueOf(orNull(json.getString("delay"), "0"));

    double y = Double.parseDouble(json.getString("y")) / 1000000;
    double x = Double.parseDouble(json.getString("x")) / 1000000;
    String dirOrEmpty = json.getString("direction");
    Double direction = dirOrEmpty.isEmpty() ? null : Double.parseDouble(dirOrEmpty) * 10;
    TimedPosition current = new TimedPosition(ts, new GeoLocBearing(x, y, direction));

    // TODO: Parse future positions to get continuous move (poly field)

    TrainPosition trainPosition = TrainPosition.make(
      trainId, name, delay, cat, lastStopName, current, Collections.emptyList());
    return new AbstractMap.SimpleImmutableEntry<>(name, trainPosition);
  }

}
