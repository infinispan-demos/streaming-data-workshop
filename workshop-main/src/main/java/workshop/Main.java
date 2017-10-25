package workshop;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.net.SocketAddress;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static workshop.Admin.createRemoteCaches;
import static workshop.shared.Constants.STATIONS_INJECTOR_HOST;

public class Main extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(Future<Void> future) throws Exception {
    log.info("Starting main verticle");

    Router router = Router.router(vertx.getDelegate());
    router.get("/test").blockingHandler(this::test);
    router.get("/inject").handler(this::inject);
    router.route("/eventbus/*").handler(this.sockJSHandler());

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

//    vertx
//      .<Void>rxExecuteBlocking(fut -> fut.complete(createRemoteCaches()))
//      .subscribe(x ->
//        vertx.getDelegate()
//          .createHttpServer()
//          .requestHandler(router::accept)
//          .listen(8080, ar -> {
//            if (ar.succeeded()) log.info("Main HTTP server started");
//            future.handle(ar.mapEmpty());
//          })
//      , future::fail);
  }

  private void inject(RoutingContext ctx) {
    log.info("HTTP GET /inject");
    vertx
      .<Void>rxExecuteBlocking(fut -> fut.complete(createRemoteCaches()))
      .subscribe(x -> {
        log.info("Call GET stations-injector/inject-stations");
        WebClient client = WebClient.create(vertx.getDelegate());
        client
          .get(8080, STATIONS_INJECTOR_HOST, "/inject")
          .send(ar -> {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              log.info("Station boards inject replied: " + response.body().toString());
              ctx.response().end("Station boards injector started");
            } else {
              log.severe("Something went wrong: " + ar.cause().getMessage());
              ctx.response().end("Station boards injector failed to start");
            }
          });
      }, t -> {
        log.log(Level.SEVERE, "Error starting data injection", t);
        ctx.response().end("Failed to start data injection");
      });

//      .doOnSuccess(x -> {
//        log.info("Call GET stations-injector/inject");
//        WebClient client = WebClient.create(vertx.getDelegate());
//        client
//          .get(8080, STATIONS_INJECTOR_HOST, "/inject")
//          .send(ar -> {
//            if (ar.succeeded()) {
//              HttpResponse<Buffer> response = ar.result();
//              log.info("Station boards inject replied: " + response.toString());
//              ctx.response().end("Station boards injector started");
//            } else {
//              log.severe("Something went wrong: " + ar.cause().getMessage());
//              ctx.response().end("Station boards injector failed to start");
//            }
//          });
//      }).doOnError(t -> {
//        log.log(Level.SEVERE, "Error starting data injection", t);
//        ctx.response().end("Failed to start data injection");
//      });


//    WebClient client = WebClient.create(vertx.getDelegate());
//    client
//      .get(80, STATIONS_INJECTOR_HOST, "/inject")
//      .send(ar -> {
//        if (ar.succeeded()) {
//          HttpResponse<Buffer> response = ar.result();
//          log.info("Station boards inject replied: " + response.toString());
//          ctx.response().end("Station boards injector started");
//        } else {
//          log.severe("Something went wrong " + ar.cause().getMessage());
//          ctx.response().end("Failed to start data injection");
//        }
//      });
  }

  private void test(RoutingContext ctx) {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host("datagrid-hotrod")
        .port(11222).build());

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

  private Handler<RoutingContext> sockJSHandler() {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx.getDelegate());
    PermittedOptions outPermit = new PermittedOptions().setAddress("delayed-trains");
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER)
        log.info("SockJs: client connected");

      be.complete(true);
    });
    return sockJSHandler;
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(Main.class.getName());
  }

}
