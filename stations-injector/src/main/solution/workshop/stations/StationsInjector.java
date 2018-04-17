package workshop.stations;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import workshop.model.Station;
import workshop.model.Stop;
import workshop.model.Train;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.util.logging.Level.*;
import static workshop.shared.Constants.*;

public class StationsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(StationsInjector.class.getName());

  private RemoteCacheManager remote;
  private RemoteCache<String, Stop> stationBoardsCache;

  private long progressTimer;
  private Disposable injector;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    Router router = Router.router(vertx);
    router.get(STATIONS_INJECTOR_URI).handler(this::inject);

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

  private void inject(RoutingContext ctx) {
    if (injector != null) {
      injector.dispose();
      vertx.cancelTimer(progressTimer);
    }

    vertx
      .rxExecuteBlocking(stationBoardsCache())
      .flatMapCompletable(x -> clearStationBoardsCache())
      .subscribeOn(RxHelper.scheduler(vertx.getOrCreateContext()))
      .subscribe(() -> {
        progressTimer = vertx.setPeriodic(5000L, l ->
          vertx.executeBlocking(fut -> {
            log.info(String.format("Progress: stored=%d%n", stationBoardsCache.size()));
            fut.complete();
          }, false, ar -> {
          }));

        Flowable<String> fileFlowable = rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz");

        Flowable<Map.Entry<String, Stop>> pairFlowable = fileFlowable
          .map(StationsInjector::toEntry)
          .zipWith(Flowable.interval(5, TimeUnit.MILLISECONDS).onBackpressureDrop(), (item, interval) -> item);

        Completable completable = pairFlowable
          .map(
            e -> {
              CompletableFuture<Stop> putCompletableFuture =
                stationBoardsCache.putAsync(e.getKey(), e.getValue());
              return CompletableInterop.fromFuture(putCompletableFuture);
            })
          .to(flowable -> Completable.merge(flowable, 100));

        injector = completable.subscribe(
          () -> log.info("Reached end")
          , t -> log.log(SEVERE, "Error while loading", t)
        );

        ctx.response().end("Injector started");
      });
  }

  // TODO: Duplicate
  private void remoteCacheManager(Future<Void> f) {
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
        FileDescriptorSource.fromResources("station-board.proto")
      );

      ctx.registerMarshaller(new Stop.Marshaller());
      ctx.registerMarshaller(new Station.Marshaller());
      ctx.registerMarshaller(new Train.Marshaller());

      f.complete();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      f.fail(e);
    }
  }

  private Handler<Future<Void>> stationBoardsCache() {
    return f -> {
      this.stationBoardsCache = remote.getCache(STATION_BOARDS_CACHE_NAME);
      f.complete();
    };
  }

  private Completable clearStationBoardsCache() {
    return CompletableInterop.fromFuture(stationBoardsCache.clearAsync());
  }

  private static Flowable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = StationsInjector.class.getClassLoader().getResource(resource);
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

  private static Map.Entry<String, Stop> toEntry(String line) {
    JsonObject json = new JsonObject(line);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");
    String trainCat = json.getString("category");
    String trainOperator = json.getString("operator");

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

    Stop stop = Stop.make(train, delayMin, station, departureTs);

    return new AbstractMap.SimpleImmutableEntry<>(stopId, stop);
  }

  @SuppressWarnings("unchecked")
  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

}
