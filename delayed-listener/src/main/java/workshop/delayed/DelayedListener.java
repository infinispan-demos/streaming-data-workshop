package workshop.delayed;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import workshop.model.Stop;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static workshop.shared.Constants.DELAYED_TRAINS_CACHE_NAME;
import static workshop.shared.Constants.WORKSHOP_MAIN_HOST;
import static workshop.shared.Constants.WORKSHOP_MAIN_URI;

public class DelayedListener extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(DelayedListener.class.getName());

  private RemoteCacheManager client;

  @Override
  public void start(io.vertx.core.Future<Void> future) throws Exception {
    log.info("Starting delay listener verticle");

    Router router = Router.router(vertx);
    router.route("/eventbus/*").handler(this.sockJSHandler());

    vertx
      .rxExecuteBlocking(Util::remoteCacheManager)
      .flatMap(remote ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .map(s -> remote)
      )
      .subscribe(
        remote -> {
          client = remote;
          log.info("Listener HTTP server started");
          future.complete();
        },
        future::fail
      );
  }

  private void listen() {
    Util.httpGet(WORKSHOP_MAIN_HOST, WORKSHOP_MAIN_URI, vertx)
      .flatMap(rsp -> vertx.rxExecuteBlocking(Util.remoteCache(client)))
      .subscribe(
        this::addContinuousQuery
        , t -> log.log(Level.SEVERE, "Error starting listener", t)
      );
  }

  private void addContinuousQuery(RemoteCache<String, Stop> stations) {
    // TODO live coding
  }

  @Override
  public void stop() {
    if (Objects.nonNull(client)) client.stop();
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

}
