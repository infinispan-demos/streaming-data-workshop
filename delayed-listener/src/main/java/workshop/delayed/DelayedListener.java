package workshop.delayed;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
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

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.DELAYED_TRAINS_CACHE_NAME;
import static workshop.shared.Constants.STATION_BOARDS_CACHE_NAME;
import static workshop.shared.Constants.WORKSHOP_MAIN_HOST;
import static workshop.shared.Constants.WORKSHOP_MAIN_URI;

public class DelayedListener extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(DelayedListener.class.getName());

  private RemoteCacheManager client;
  private boolean listenerStarted = false;

  @Override
  public void start(Future<Void> future) throws Exception {
    log.info("Starting delay listener verticle");

    Router router = Router.router(vertx.getDelegate());
    router.route("/eventbus/*").handler(this.sockJSHandler());
    //router.get(LISTEN_URI).handler(this::listen);

    vertx
      .<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createClient()))
      .doOnSuccess(remoteClient -> client = remoteClient)
      .subscribe(res -> {
        log.info("Starting delay listener HTTP server");
        vertx.getDelegate()
          .createHttpServer()
          .requestHandler(router::accept)
          .listen(8080, ar -> {
            if (ar.succeeded()) {
              log.info("Listener HTTP server started");
              future.complete();
            } else {
              future.fail(ar.cause());
            }
          });
      }, future::fail);
  }

//  private void listen(RoutingContext ctx) {
//    httpGet(WORKSHOP_MAIN_HOST, WORKSHOP_MAIN_URI)
//      .flatMap(rsp -> vertx
//          .<RemoteCache<String, Stop>>rxExecuteBlocking(fut ->
//              fut.complete(client.getCache(STATION_BOARDS_CACHE_NAME))))
//      .subscribe(stations -> {
//        addContinuousQuery(stations);
//        ctx.response().end("Delayed listener started");
//      }, t -> {
//        log.log(Level.SEVERE, "Error starting listener", t);
//        ctx.response().end("Failed to start listener");
//      });
//  }

  private void listen() {
    httpGet(WORKSHOP_MAIN_HOST, WORKSHOP_MAIN_URI)
      .flatMap(rsp -> vertx
        .<RemoteCache<String, Stop>>rxExecuteBlocking(fut ->
          fut.complete(client.getCache(STATION_BOARDS_CACHE_NAME))))
      .subscribe(
        this::addContinuousQuery,
        t -> log.log(Level.SEVERE, "Error starting listener", t)
      );
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
          vertx.runOnContext(x -> {
            vertx.eventBus().publish("delayed-trains", toJson(stop));
            RemoteCache<String, String> delayed = client.getCache(DELAYED_TRAINS_CACHE_NAME);
            delayed.putAsync(stop.train.getName(), stop.train.getName());
          });
        }
      };

    ContinuousQuery<String, Stop> continuousQuery = Search.getContinuousQuery(stations);
    continuousQuery.removeAllListeners();
    continuousQuery.addContinuousQueryListener(query, listener);
  }

  @Override
  public void stop() throws Exception {
    if (Objects.nonNull(client))
      client.stop();
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
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx.getDelegate());
    PermittedOptions outPermit = new PermittedOptions().setAddress("delayed-trains");
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER) {
        log.info("SockJs: client connected");
        if (!listenerStarted) {
          listen();
          listenerStarted = true;
        }
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
