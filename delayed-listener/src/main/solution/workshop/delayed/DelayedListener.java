package workshop.delayed;

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
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

  private RemoteCacheManager remote;
  private RemoteCache<String, Stop> stationBoardsCache;
  private ContinuousQuery<String, Stop> continuousQuery;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    log.info("Starting delay listener verticle");

    Router router = Router.router(vertx);
    router.route("/eventbus/*").handler(this.sockJSHandler());

    vertx
      .rxExecuteBlocking(this::remoteCacheManager)
      .flatMap(x ->
        vertx
          .createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
      )
      .subscribe(
        server -> {
          log.info("Http server started and connected to datagrid");
          future.complete();
        }
        , future::fail
      );
  }

  private void listen() {
    vertx
      .rxExecuteBlocking(stationBoardsCache())
      .flatMap(x -> vertx.rxExecuteBlocking(this::removeContinuousQueryListeners))
      .flatMap(x -> httpGet(WORKSHOP_MAIN_HOST, WORKSHOP_MAIN_URI))
      .flatMap(x -> vertx.rxExecuteBlocking(this::addContinuousQuery))
      .subscribe(
        x -> {}
        , t -> log.log(Level.SEVERE, "Error starting listener", t)
      );
  }

  private void removeContinuousQueryListeners(Future<Void> f) {
    continuousQuery.removeAllListeners();
    f.complete();
  }

  private Handler<Future<Void>> stationBoardsCache() {
    return f -> {
      log.info("Get station boards cache and continuous query");
      if (Objects.isNull(stationBoardsCache) && Objects.isNull(continuousQuery)) {
        this.stationBoardsCache = remote.getCache(STATION_BOARDS_CACHE_NAME);
        this.continuousQuery = Search.getContinuousQuery(this.stationBoardsCache);
      }

      f.complete();
    };
  }

  private Single<HttpResponse<String>> httpGet(String host, String uri) {
    log.info("Call HTTP GET " + host + uri);
    WebClient client = WebClient.create(vertx);
    return client
      .get(8080, host, uri)
      .as(BodyCodec.string())
      .rxSend();
  }

  private void addContinuousQuery(Future<Void> f) {
    log.info("Add continuous query");
    QueryFactory queryFactory = Search.getQueryFactory(stationBoardsCache);

    Query query = queryFactory.from(Stop.class)
      .having("delayMin").gt(0L)
      .build();

    ContinuousQueryListener<String, Stop> listener =
      new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String id, Stop stop) {
          log.info(String.format("[%d] Stop id=%s joining result%n", this.hashCode(), id));
          JsonObject stopAsJson = toJson(stop);
          vertx.eventBus().publish("delayed-trains", stopAsJson);
          RemoteCache<String, String> delayed = remote.getCache(DELAYED_TRAINS_CACHE_NAME);
          delayed.putAsync(stop.train.getName(), stop.train.getName());
        }
      };

    continuousQuery.addContinuousQueryListener(query, listener);
    log.info("Continuous query added");
    f.complete();
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) {
    if (Objects.nonNull(remote)) {
      remote.stopAsync()
        .thenRun(future::complete);
    } else {
      future.complete();
    }
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

  private void remoteCacheManager(Future<Void> f) {
    try {
      remote = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .marshaller(ProtoStreamMarshaller.class)
          .build());

      SerializationContext ctx =
        ProtoStreamMarshaller.getSerializationContext(remote);

      ctx.registerProtoFiles(
        FileDescriptorSource.fromResources("station-board.proto")
      );

      ctx.registerMarshaller(new Stop.Marshaller());
      ctx.registerMarshaller(new Station.Marshaller());
      ctx.registerMarshaller(new Train.Marshaller());

      f.complete();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      f.fail(e);
    }
  }

}
