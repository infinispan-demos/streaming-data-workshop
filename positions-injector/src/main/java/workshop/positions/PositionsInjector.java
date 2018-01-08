package workshop.positions;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaWriteStream;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.impl.AsyncResultCompletable;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.kafka.clients.producer.ProducerRecord;
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
import static workshop.shared.Constants.POSITIONS_INJECTOR_URI;
import static workshop.shared.Constants.TRAIN_POSITIONS_TOPIC;

public class PositionsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(PositionsInjector.class.getName());

  private KafkaWriteStream<String, String> stream;

  @Override
  public void start(Future<Void> future) {

    Router router = Router.router(vertx);
    router.get(POSITIONS_INJECTOR_URI).handler(this::inject);

    retrieveConfiguration()
      .doOnSuccess(json ->
        stream = KafkaWriteStream
          .create(vertx.getDelegate(), json.getJsonObject("kafka").getMap(), String.class, String.class))
      .flatMapCompletable(v ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .doOnSuccess(server -> log.info("Positions injector HTTP server started"))
          .doOnError(t -> log.log(Level.SEVERE, "Positions injector HTTP server failed to start", t))
          .toCompletable() // Ignore result
      )
      .subscribe(CompletableHelper.toObserver(future));
  }

  @Override
  public void stop() {
    if (Objects.nonNull(stream)) {
      stream.close();
    }
  }

  // TODO: Duplicate
  private void inject(RoutingContext ctx) {
    rxReadGunzippedTextResource("cff_train_position-2016-02-29__.jsonl.gz")
      .map(PositionsInjector::toEntry)
      .flatMapCompletable(this::dispatch)
      .subscribeOn(Schedulers.io())
      .doOnError(t -> log.log(SEVERE, "Error while loading", t))
      .subscribe();
    ctx.response().end("Injector started");
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
  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

  private Single<JsonObject> retrieveConfiguration() {
    ConfigStoreOptions store = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(new JsonObject()
        .put("path", "app-config.yaml")
      );
    return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(store)).rxGetConfig();
  }

  private Completable dispatch(Map.Entry<String, TrainPosition> entry) {
    ProducerRecord<String, String> record
      = new ProducerRecord<>(TRAIN_POSITIONS_TOPIC, entry.getKey(), Json.encode(entry.getValue()));
    return new AsyncResultCompletable(
      handler ->
        stream.write(record, x -> {
          if (x.succeeded()) {
            log.info("Entry written in Kafka: " + entry.getKey());
            handler.handle(Future.succeededFuture());
          } else {
            handler.handle(Future.failedFuture(x.cause()));
          }
        }));
  }

}
