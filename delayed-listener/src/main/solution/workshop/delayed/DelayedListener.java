package workshop.delayed;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import rx.Single;
import workshop.model.Station;
import workshop.model.Stop;
import workshop.model.Train;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static workshop.shared.Constants.*;

public class DelayedListener extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(DelayedListener.class.getName());

  private RemoteCacheManager client;

  @Override
  public void start(Future<Void> future) throws Exception {
    log.info("Starting delay listener verticle");

    Router router = Router.router(vertx);
    router.route("/eventbus/*").handler(this.sockJSHandler());

    vertx
      .<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createClient()))
      .doOnSuccess(remoteClient -> client = remoteClient)
      .flatMap(v -> {
        log.info("Starting delay listener HTTP server");
        return vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080);

      })
      .doOnSuccess(v -> log.info("Listener HTTP server started"))
      .subscribe(res -> future.complete(), future::fail);
  }

  private void listen() {
    httpGet(WORKSHOP_MAIN_HOST, WORKSHOP_MAIN_URI)
      .flatMap(rsp -> {
        return vertx.<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> {
          fut.complete(client.getCache(STATION_BOARDS_CACHE_NAME));
        });
      }).subscribe(this::addContinuousQuery, t -> log.log(Level.SEVERE, "Error starting listener", t));
  }

  private Single<HttpResponse<String>> httpGet(String host, String uri) {
    log.info("Call HTTP GET " + host + uri);
    WebClient client = WebClient.create(vertx);
    return client
      .get(8080, host, uri)
      .as(BodyCodec.string())
      .rxSend();
  }

  private void addContinuousQuery(RemoteCache<String, Stop> stations) {
    QueryFactory qf = Search.getQueryFactory(stations);

    Query query = qf.from(Stop.class)
      .having("delayMin").gt(0L)
      .build();

    ContinuousQueryListener<String, Stop> listener =
      new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String id, Stop stop) {
          vertx.eventBus().publish("delayed-trains", toJson(stop));
          RemoteCache<String, String> delayed = client.getCache(DELAYED_TRAINS_CACHE_NAME);
          delayed.putAsync(stop.train.getName(), stop.train.getName());
        }
      };

    ContinuousQuery<String, Stop> continuousQuery = Search.getContinuousQuery(stations);
    continuousQuery.removeAllListeners();
    continuousQuery.addContinuousQueryListener(query, listener);
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    vertx.<Void>rxExecuteBlocking(fut -> {
      if (Objects.nonNull(client)) {
        client.stop();
      }
      fut.complete();
    }).subscribe(RxHelper.toSubscriber(stopFuture));
  }

  private static String toJson(Stop stop) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", stop.train.getCategory());
    map.put("departure", String.format("%tR", stop.departureTs));
    map.put("station", stop.station.getName());
    map.put("destination", stop.train.getTo());
    map.put("delay", stop.delayMin);
    map.put("trainName", stop.train.getName());
    return new JsonObject(map).encode();
  }

  private Handler<RoutingContext> sockJSHandler() {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    PermittedOptions outPermit = new PermittedOptions().setAddress("delayed-trains");
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER) {
        log.info("SockJs: client connected");
        listen();
      }

      be.complete(true);
    });
    return sockJSHandler;
  }

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

}
