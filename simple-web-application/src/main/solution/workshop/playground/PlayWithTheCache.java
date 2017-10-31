package workshop.playground;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class PlayWithTheCache extends AbstractVerticle {

  protected RemoteCacheManager client;
  protected RemoteCache<Integer, String> defaultCache;

  @Override
  public void start() throws Exception {
    initCache(vertx);
    Router router = Router.router(vertx);

    router.get("/").handler(rc -> {
      rc.response().end("Welcome");
    });

    router.get("/api").handler(rc -> {
      rc.response().end(new JsonObject().put("name", "duchess").put("version", 1).encode());
    });

    router.route().handler(BodyHandler.create());
    router.post("/api/duchess").handler(this::handleAddDuchess);
    router.get("/api/duchess/:id").handler(this::handleGetDuchess);


    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }

  private void handleGetDuchess(RoutingContext rc) {
    defaultCache.getAsync(Integer.parseInt(rc.request().getParam("id")))
      .thenAccept(value -> {
        String response = "Not found";
        if (value != null) {
          response = new JsonObject().put("Duchess", value).encode();
        }
        rc.response().end(response);
      });
  }

  private void handleAddDuchess(RoutingContext rc) {
    HttpServerResponse response = rc.response();
    JsonObject bodyAsJson = rc.getBodyAsJson();
    if (bodyAsJson != null && bodyAsJson.containsKey("id") && bodyAsJson.containsKey("name")) {
      defaultCache.putAsync(bodyAsJson.getInteger("id"), bodyAsJson.getString("name"))
        .thenAccept(s -> {
          response.end("Duchess Added");
        });
    } else {
      response.end("Body is " + bodyAsJson + ". id and name should be provided");
    }
  }


  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    if (client != null) {
      client.stopAsync().whenComplete((e, ex) -> stopFuture.complete());
    }
  }

  protected void initCache(Vertx vertx) {
    vertx.executeBlocking(fut -> {
      client = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .build());

      defaultCache = client.getCache();
      fut.complete();
    }, res -> {
      if (res.succeeded()) {
        System.out.println("Cache started");
      } else {
        res.cause().printStackTrace();
      }
    });
  }
}
