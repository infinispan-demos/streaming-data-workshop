package workshop.stations;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import rx.Notification;
import rx.Observable;
import rx.functions.Actions;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.util.logging.Level.*;
import static rx.Completable.*;
import static workshop.shared.Constants.*;

public class StationsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(StationsInjector.class.getName());

  private RemoteCacheManager client;

  @Override
  public void start(Future<Void> future) throws Exception {
    Router router = Router.router(vertx);
    router.get(STATIONS_INJECTOR_URI).handler(this::inject);

    vertx
      .<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createClient()))
      .doOnSuccess(remoteClient -> client = remoteClient)
      .flatMap(v -> {
        return vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .doOnSuccess(server -> log.info("Station injector HTTP server started"))
          .doOnError(t -> log.log(Level.SEVERE, "Station injector HTTP server failed to start", t))
          .<Void>map(server -> null); // Ignore result
      }).subscribe(RxHelper.toSubscriber(future));
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    vertx.<Void>rxExecuteBlocking(fut -> {
      if (Objects.nonNull(client))
        client.stop();
      fut.complete();
    }).subscribe(RxHelper.toSubscriber(stopFuture));
  }

  // TODO: Duplicate
  private void inject(RoutingContext ctx) {
    vertx
      .<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> fut.complete(client.getCache(STATION_BOARDS_CACHE_NAME)))
      // Remove data on start, to start clean
      .map(stations -> fromFuture(stations.clearAsync()).to(x -> stations))
      .subscribe(stations -> {
        vertx.setPeriodic(5000L, l ->
          vertx.executeBlocking(fut -> {
            log.info(String.format("Progress: stored=%d%n", stations.size()));
            fut.complete();
          }, false, ar -> {}));

        // TODO 1: Get an observable for "cff-stop-2016-02-29__.jsonl.gz" file calling the method rxReadGunzippedTextResource
        // TODO 2: map each entry into a tuple of String/Stop with StationsInjector::toEntry
        // TODO 3. flatMap each entry to stored it in the stations cache calling putAsync and using Observable.from
        // TODO 4. subscribe Actions.empty() and log an error happened


        ctx.response().end("Injector started");
      });
  }

  // TODO: Duplicate
  private static RemoteCacheManager createClient() {
    try {
      RemoteCacheManager client = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .marshaller(ProtoStreamMarshaller.class)
          .build());

      SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);

      ctx.registerProtoFiles(FileDescriptorSource.fromResources("station-board.proto"));
      ctx.registerMarshaller(new Stop.Marshaller());
      ctx.registerMarshaller(new Station.Marshaller());
      ctx.registerMarshaller(new Train.Marshaller());
      return client;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      throw new RuntimeException(e);
    }
  }

  // TODO: Duplicate
  private static Observable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = StationsInjector.class.getClassLoader().getResource(resource);
    Objects.requireNonNull(url);

    return StringObservable
      .using(() -> {
        InputStream inputStream = url.openStream();
        InputStream gzipStream = new GZIPInputStream(inputStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        return new BufferedReader(decoder);
      }, StringObservable::from)
      .compose(StringObservable::byLine)
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

  // TODO: Duplicate
  @SuppressWarnings("unchecked")
  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

}
