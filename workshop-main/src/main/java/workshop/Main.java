package workshop;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import rx.Single;

import java.net.SocketAddress;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static workshop.Admin.createRemoteCaches;
import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.DELAYED_TRAINS_HOST;
import static workshop.shared.Constants.LISTEN_URI;
import static workshop.shared.Constants.POSITIONS_INJECTOR_HOST;
import static workshop.shared.Constants.POSITIONS_INJECTOR_URI;
import static workshop.shared.Constants.STATIONS_INJECTOR_HOST;
import static workshop.shared.Constants.STATIONS_INJECTOR_URI;

public class Main extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(Future<Void> future) throws Exception {
    log.info("Starting Main verticle");

    Router router = Router.router(vertx.getDelegate());
    router.get("/test").blockingHandler(this::test);
    router.get("/inject").handler(this::inject);

    // TODO: Best practice for chaining rx-style vert.x web server startup and duplicate
    vertx.getDelegate()
      .createHttpServer()
        .requestHandler(router::accept)
        .listen(8080, ar -> {
          if (ar.succeeded()) {
            log.info("Main HTTP server started");
            future.complete();
          } else {
            future.fail(ar.cause());
          }
        });
  }

  private void inject(RoutingContext ctx) {
    log.info("HTTP GET /inject");
    vertx
      .<Void>rxExecuteBlocking(fut -> fut.complete(createRemoteCaches()))
      .flatMap(x -> httpGet(DELAYED_TRAINS_HOST, LISTEN_URI))
      .flatMap(x -> httpGet(STATIONS_INJECTOR_HOST, STATIONS_INJECTOR_URI))
      .flatMap(x -> httpGet(POSITIONS_INJECTOR_HOST, POSITIONS_INJECTOR_URI))
      .subscribe(rsp -> {
        log.info("Inject replied: " + rsp.body());
        ctx.response().end("Inject OK");
      }, t -> {
        log.log(Level.SEVERE, "Error starting data injection", t);
        ctx.response().end("Failed to start data injection");
      });
  }

  private Single<HttpResponse<String>> httpGet(String host, String uri) {
    log.info("Call HTTP GET " + host + uri);
    WebClient client = WebClient.create(vertx);
    return client
      .get(8080, host, uri)
      .as(BodyCodec.string())
      .rxSend();
  }

  private void test(RoutingContext ctx) {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host(DATAGRID_HOST)
        .port(DATAGRID_PORT).build());

    RemoteCache<String, String> cache = client.getCache("default");
    cache.put("hello", "world");
    Object value = cache.get("hello");

    Set<SocketAddress> topology =
      cache.getCacheTopologyInfo().getSegmentsPerServer().keySet();

    JsonObject rsp = new JsonObject()
      .put("get(hello)", value)
      .put("topology", topology.toString());

    ctx.response()
      .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
      .end(rsp.encodePrettily());

    client.stop();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(Main.class.getName());
  }

}
