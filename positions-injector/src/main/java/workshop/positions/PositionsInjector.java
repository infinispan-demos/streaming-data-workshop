package workshop.positions;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import workshop.model.GeoLocBearing;
import workshop.model.TimedPosition;
import workshop.model.TrainPosition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.util.logging.Level.*;
import static workshop.shared.Constants.*;

public class PositionsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(PositionsInjector.class.getName());

  private RemoteCacheManager remote;
  private RemoteCache<String, TrainPosition> trainPositionsCache;

  private long progressTimer;
  private Disposable injector;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    Router router = Router.router(vertx);
    router.get(POSITIONS_INJECTOR_URI).handler(this::inject);

    vertx
      .rxExecuteBlocking(this::remoteCacheManager)
      .flatMap(x ->
        vertx
          .createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
      )
      .subscribe(
        server -> {
          log.info("Http server started and connected to datagrid");
          future.complete();
        }
        , future::fail
      );
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) {
    if (Objects.nonNull(remote)) {
      remote.stopAsync()
        .thenRun(future::complete);
    } else {
      future.complete();
    }
  }

  // TODO: Duplicate
  private void inject(RoutingContext ctx) {
    if (injector != null) {
      injector.dispose();
      vertx.cancelTimer(progressTimer);
    }

    vertx
      .rxExecuteBlocking(trainPositionsCache())
      .flatMapCompletable(x -> clearTrainPositionsCache())
      .subscribeOn(RxHelper.scheduler(vertx.getOrCreateContext()))
      .subscribe(() -> {
        progressTimer = vertx.setPeriodic(5000L, l ->
          vertx.executeBlocking(fut -> {
            log.info(String.format("Progress: stored=%d%n", trainPositionsCache.size()));
            fut.complete();
          }, false, ar -> {}));

        injector = rxReadGunzippedTextResource("cff_train_position-2016-02-29__.jsonl.gz")
          .map(PositionsInjector::toEntry)
          .zipWith(Flowable.interval(5, TimeUnit.MILLISECONDS).onBackpressureDrop(), (item, interval) -> item)
          .map(e -> CompletableInterop.fromFuture(trainPositionsCache.putAsync(e.getKey(), e.getValue())))
          .to(flowable -> Completable.merge(flowable, 100))
          .subscribe(() -> log.info("Reached end"), t -> log.log(SEVERE, "Error while loading", t));

        ctx.response().end("Injector started");
      });
  }

  private static Map.Entry<String, TrainPosition> toEntry(String line) {
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
    return new AbstractMap.SimpleImmutableEntry<>(trainId, trainPosition);
  }

  // TODO: Duplicate
  private static Flowable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = PositionsInjector.class.getClassLoader().getResource(resource);
    Objects.requireNonNull(url);

    return Flowable.<String, BufferedReader>generate(() -> {
      InputStream inputStream = url.openStream();
      InputStream gzipStream = new GZIPInputStream(inputStream);
      Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
      return new BufferedReader(decoder);
    }, (bufferedReader, emitter) -> {
      String line = bufferedReader.readLine();
      if (line != null) {
        emitter.onNext(line);
      } else {
        emitter.onComplete();
      }
    }, BufferedReader::close)
      .subscribeOn(Schedulers.io());
  }

  // TODO: Duplicate
  @SuppressWarnings("unchecked")
  static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

  private void remoteCacheManager(io.vertx.reactivex.core.Future<Void> f) {
    try {
      remote = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .marshaller(ProtoStreamMarshaller.class)
          .build());

      SerializationContext ctx =
        ProtoStreamMarshaller.getSerializationContext(remote);

      ctx.registerProtoFiles(
        FileDescriptorSource.fromResources("train-position.proto")
      );

      ctx.registerMarshaller(new TrainPosition.Marshaller());
      ctx.registerMarshaller(new TimedPosition.Marshaller());
      ctx.registerMarshaller(new GeoLocBearing.Marshaller());

      f.complete();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      f.fail(e);
    }
  }

  private Handler<io.vertx.reactivex.core.Future<Void>> trainPositionsCache() {
    return f -> {
      RemoteCache<String, TrainPosition> cache =
        remote.getCache(TRAIN_POSITIONS_CACHE_NAME);
      this.trainPositionsCache = cache;
      f.complete();
    };
  }

  private Completable clearTrainPositionsCache() {
    return CompletableInterop.fromFuture(trainPositionsCache.clearAsync());
  }

}
