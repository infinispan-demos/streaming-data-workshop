package stream;

import workshop.model.GeoLocBearing;
import workshop.model.TimedPosition;
import workshop.model.TrainPosition;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import rx.functions.Actions;

import java.util.AbstractMap;
import java.util.Map.Entry;
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

  RemoteCache<String, TrainPosition> positionsCache;
  private long loadStart;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(TrainPositionsInjectorVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(mkRemoteCacheManager()))
      .doOnSuccess(remoteCacheManager -> {
        client = remoteCacheManager;
        client.administration().createCache("train-positions", "distributed");
      }).<Void>map(x -> null)
      .flatMap(v -> vertx.<RemoteCache<String, TrainPosition>>rxExecuteBlocking(fut -> fut.complete(client.getCache("train-positions"))))
      .doOnSuccess(remoteCache -> positionsCache = remoteCache).<Void>map(x -> null)
      .subscribe(result -> {
        startFuture.complete(result);
        startLoading();
      }, startFuture::fail);
  }

//  public RemoteCacheManager createRemoteCacheManager() {
//    Properties p = new Properties();
//    p.put("infinispan.client.hotrod.server_list", "${server.host:datagrid-hotrod}:${server.port:11222}");
//    ConfigurationBuilder cfg = new ConfigurationBuilder();
//    cfg.withProperties(p);
//    RemoteCacheManager client = new RemoteCacheManager(cfg.build());
//    return client;
//  }

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
      .doOnNext(entry -> positionsCache.put(entry.getValue().getTrainId(), entry.getValue()))
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
    TimedPosition current = new TimedPosition(ts, new GeoLocBearing(y, x, direction));

    // TODO: Parse future positions to get continuous move (poly field)

    TrainPosition trainPosition = TrainPosition.make(
      trainId, name, delay, cat, lastStopName, current);
    return new AbstractMap.SimpleImmutableEntry<>(name, trainPosition);
  }

}
