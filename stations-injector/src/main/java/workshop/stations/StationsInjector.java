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
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaWriteStream;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.impl.AsyncResultCompletable;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.kafka.clients.producer.ProducerRecord;

public class StationsInjector extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(StationsInjector.class.getName());

  @Override
  public void start(Future<Void> future) {
    Router router = Router.router(vertx);
    router.get(STATIONS_INJECTOR_URI).handler(this::inject);

    // TODO live coding
    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .rxListen(8080)
      .subscribe(
        server -> {
          log.info("Station injector HTTP server started");
          future.complete();
        },
        future::fail
      );
  }

  @Override
  public void stop() {
    // TODO live coding
  }

  // TODO: Duplicate
  private void inject(RoutingContext ctx) {
    // TODO live coding
    ctx.response().end("TODO");
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

  private Single<JsonObject> kafkaCfg() {
    ConfigStoreOptions store = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(new JsonObject()
        .put("path", "app-config.yaml")
      );
    return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(store)).rxGetConfig();
  }

  private Completable dispatch(Map.Entry<String, String> entry) {
    // TODO live coding

    return new AsyncResultCompletable(h -> {
      log.info("Entry read " + entry.getKey());
      // TODO live coding
    });
  }

}
