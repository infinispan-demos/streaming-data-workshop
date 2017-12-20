package workshop.stations;

import static java.util.logging.Level.SEVERE;
import static workshop.shared.Constants.STATIONS_INJECTOR_URI;
import static workshop.shared.Constants.STATION_BOARDS_TOPIC;

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

import org.apache.kafka.clients.producer.ProducerRecord;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaWriteStream;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.impl.AsyncResultCompletable;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class StationsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(StationsInjector.class.getName());

  private KafkaWriteStream<String, String> stream;


  @Override
  public void start(Future<Void> future) {
    Router router = Router.router(vertx);
    router.get(STATIONS_INJECTOR_URI).handler(this::inject);

    retrieveConfiguration()
      .doOnSuccess(json ->
        stream = KafkaWriteStream
          .create(vertx.getDelegate(), json.getJsonObject("kafka").getMap(), String.class, String.class))
      .flatMapCompletable(v ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .doOnSuccess(server -> log.info("Station injector HTTP server started"))
          .doOnError(t -> log.log(Level.SEVERE, "Station injector HTTP server failed to start", t))
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

  private void inject(RoutingContext ctx) {
    Flowable<String> fileFlowable = rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz");
    fileFlowable
      .map(StationsInjector::toEntry)
      .flatMapCompletable(this::dispatch)
      .subscribeOn(Schedulers.io())
      .doOnError(t -> log.log(SEVERE, "Error while loading", t))
      .subscribe();
    ctx.response().end("Injector started");
  }

  // TODO: Duplicate
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

  private static Map.Entry<String, String> toEntry(String line) {
    JsonObject json = new JsonObject(line);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");

    JsonObject jsonStop = json.getJsonObject("stop");
    JsonObject jsonStation = jsonStop.getJsonObject("station");
    long stationId = Long.parseLong(jsonStation.getString("id"));

    String departure = jsonStop.getString("departure");
    String stopId = String.format("%s/%s/%s/%s", stationId, trainName, trainTo, departure);

    return new AbstractMap.SimpleImmutableEntry<>(stopId, line);
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

  private Completable dispatch(Map.Entry<String, String> entry) {
    ProducerRecord<String, String> record
      = new ProducerRecord<>(STATION_BOARDS_TOPIC, entry.getKey(), entry.getValue());
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
