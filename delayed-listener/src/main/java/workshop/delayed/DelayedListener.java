package workshop.delayed;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
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
import workshop.model.Station;
import workshop.model.Stop;
import workshop.model.Train;

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
    QueryFactory queryFactory = Search.getQueryFactory(stations);

    // TODO 1 - Create query for Stop where delayMin is bigger than 0
    Query query = null;

    ContinuousQueryListener<String, Stop> listener =
      new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String id, Stop stop) {
          JsonObject stopAsJson = toJson(stop);
          // TODO 2 - Publish stopAsJson to "delayed-trains" event-bus address
          // ...

          RemoteCache<String, String> delayed = client.getCache(DELAYED_TRAINS_CACHE_NAME);
          delayed.putAsync(stop.train.getName(), stop.train.getName());
        }
      };

    ContinuousQuery<String, Stop> continuousQuery = Search.getContinuousQuery(stations);
    continuousQuery.removeAllListeners();

    // TODO 3 - Join query with listener
    // ...
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    vertx.<Void>rxExecuteBlocking(fut -> {
      if (Objects.nonNull(client)) {
        client.stop();
      }
      fut.complete();
    }).subscribe(SingleHelper.toObserver(stopFuture));
  }

  private static JsonObject toJson(Stop stop) {
    return new JsonObject()
      .put("type", stop.train.getCategory())
      .put("departure", String.format("%tR", stop.departureTs))
      .put("station", stop.station.getName())
      .put("destination", stop.train.getTo())
      .put("delay", stop.delayMin)
      .put("trainName", stop.train.getName());
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
