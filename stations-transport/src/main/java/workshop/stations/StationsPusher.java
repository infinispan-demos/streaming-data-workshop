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

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.reactivex.FlowableHelper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import io.reactivex.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
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

  // TODO live coding

  @Override
  public void start(io.vertx.core.Future<Void> future) {

    Router router = Router.router(vertx);
    router.get(STATIONS_TRANSPORT_URI).handler(this::push);

    kafkaCfg()
      .flatMap(json ->
        Single.just(KafkaReadStream
          .create(vertx.getDelegate(), json.getJsonObject("kafka").getMap(), String.class, String.class))
      ).flatMap(stream ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .map(s -> stream)
      ).subscribe(
        stream -> {
          kafka = stream;
          log.info("HTTP server and Kafka reader stream started");
          future.complete();
        },
        future::fail
      );
  }

  private void push(RoutingContext ctx) {
    // TODO live coding
    ctx.response().end("TODO");
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

  @Override
  public void stop() {
    if (Objects.nonNull(kafka)) kafka.close();
  }

  private static void remoteCacheManager(Future<RemoteCacheManager> f) {
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
      f.complete(client);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      throw new RuntimeException(e);
    }
  }

  private static Handler<Future<RemoteCache<String, Stop>>> remoteCache(RemoteCacheManager remote) {
    return f -> f.complete(remote.getCache(STATION_BOARDS_CACHE_NAME));
  }

}
