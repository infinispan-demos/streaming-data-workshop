package workshop.positions;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rxjava.core.AbstractVerticle;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.util.logging.Level.SEVERE;
import static rx.Completable.fromFuture;
import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.POSITIONS_INJECTOR_URI;
import static workshop.shared.Constants.TRAIN_POSITIONS_CACHE_NAME;

public class PositionsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(PositionsInjector.class.getName());

  private RemoteCacheManager client;

  @Override
  public void start(Future<Void> future) throws Exception {
    Router router = Router.router(vertx.getDelegate());
    router.get(POSITIONS_INJECTOR_URI).handler(this::inject);

    vertx
      .<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createClient()))
      .doOnSuccess(remoteClient -> client = remoteClient)
      .subscribe(res ->
          // TODO: Best practice for chaining rx-style vert.x web server startup and duplicate
          vertx.getDelegate()
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(8080, ar -> {
              if (ar.succeeded()) {
                log.info("Positions injector HTTP server started");
                future.complete();
              } else {
                log.severe("Positions injector HTTP server failed to start");
                future.fail(ar.cause());
              }
            }),
        future::fail);
  }

  // TODO: Duplicate
  private void inject(RoutingContext ctx) {
    vertx
      .<RemoteCache<String, TrainPosition>>rxExecuteBlocking(fut -> fut.complete(client.getCache(TRAIN_POSITIONS_CACHE_NAME)))
      // Remove data on start, to start clean
      .map(positions -> fromFuture(positions.clearAsync()).to(x -> positions))
      .subscribe(positions -> {
        vertx.setPeriodic(5000L, l ->
          vertx.executeBlocking(fut -> {
            log.info(String.format("Progress: stored=%d%n", positions.size()));
            fut.complete();
          }, false, ar -> {}));

        rxReadGunzippedTextResource("cff_train_position-2016-02-29__.jsonl.gz")
          .map(PositionsInjector::toEntry)
          .repeatWhen(notification -> notification.map(terminal -> {
            log.info("Reached end of file, clear and restart");
            positions.clear(); // If it reaches the end of the file, start again
            return Notification.createOnNext(null);
          }))
          // TODO: Should be a flatmapObservable call putAsync wrapped with Completable?
          .doOnNext(entry -> positions.put(entry.getKey(), entry.getValue()))
          .subscribe(Actions.empty(),
            t -> log.log(SEVERE, "Error while loading", t));

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
  private static RemoteCacheManager createClient() {
    try {
      RemoteCacheManager client = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .marshaller(ProtoStreamMarshaller.class)
          .build());

      SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);

      ctx.registerProtoFiles(FileDescriptorSource.fromResources("train-position.proto"));
      ctx.registerMarshaller(new TrainPosition.Marshaller());
      ctx.registerMarshaller(new TimedPosition.Marshaller());
      ctx.registerMarshaller(new GeoLocBearing.Marshaller());
      return client;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      throw new RuntimeException(e);
    }
  }

  // TODO: Duplicate
  private static Observable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = PositionsInjector.class.getClassLoader().getResource(resource);
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

  // TODO: Duplicate
  @SuppressWarnings("unchecked")
  static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

}
