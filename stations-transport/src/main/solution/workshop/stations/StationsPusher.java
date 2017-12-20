package workshop.stations;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.STATIONS_TRANSPORT_URI;
import static workshop.shared.Constants.STATION_BOARDS_CACHE_NAME;
import static workshop.shared.Constants.STATION_BOARDS_TOPIC;

import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import io.reactivex.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import workshop.model.Station;
import workshop.model.Stop;
import workshop.model.Train;

/**
 * Listens Kafka and inserts data in Infinispan
 */
public class StationsPusher extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(StationsPusher.class.getName());

  private KafkaReadStream<String, String> consumer;
  private RemoteCacheManager client;
  private RemoteCache<String, Stop> cache;

  @Override
  public void start(Future<Void> future) {

    Router router = Router.router(vertx);
    router.get(STATIONS_TRANSPORT_URI).handler(this::consumeAndPushToCache);

    retrieveConfiguration()
      .doOnSuccess(json ->
        consumer = KafkaReadStream.create(vertx.getDelegate(), json.getJsonObject("kafka").getMap(), String.class, String.class))
      .flatMapCompletable(v ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .doOnSuccess(server -> log.info("Stations transport HTTP server started"))
          .doOnError(t -> log.log(Level.SEVERE, "Stations transport HTTP server failed to start", t))
          .toCompletable() // Ignore result
      )
      .subscribe(CompletableHelper.toObserver(future));

  }

  private void consumeAndPushToCache(RoutingContext ctx) {
    vertx.<Infinispan>rxExecuteBlocking(fut -> fut.complete(createClient()))
      .doOnSuccess(infinispan -> {
        log.info("Connected to Infinispan");
        client = infinispan.remoteClient;
        cache = infinispan.cache;
        consumer.handler(record -> {
          log.info("Object read from kafka id=" + record.key());
          Stop stop = Stop.make(record.value());
          cache.putAsync(record.key(), stop);
        });
        consumer.subscribe(Collections.singleton(STATION_BOARDS_TOPIC), ar -> {
          if (ar.succeeded()) {
            log.info("Subscription correct to " + STATION_BOARDS_TOPIC);
          } else {
            log.log(Level.SEVERE, "Could not connect to the topic", ar.cause());
          }
        });
      }).subscribe();

    ctx.response().end("Transporter started");
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

  @Override
  public void stop() {
    if (Objects.nonNull(consumer)) {
      consumer.close();
    }
    if (Objects.nonNull(client)) {
      client.stopAsync();
    }
  }

  public static class Infinispan {
    final RemoteCacheManager remoteClient;
    final RemoteCache<String, Stop> cache;

    public Infinispan(RemoteCacheManager remoteClient, RemoteCache<String, Stop> cache) {
      this.remoteClient = remoteClient;
      this.cache = cache;
    }
  }

  private static Infinispan createClient() {
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
      return new Infinispan(client, client.getCache(STATION_BOARDS_CACHE_NAME));

    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      throw new RuntimeException(e);
    }
  }
}
