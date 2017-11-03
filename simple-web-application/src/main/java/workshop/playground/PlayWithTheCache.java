package workshop.playground;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.core.AbstractVerticle;
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
    vertx.executeBlocking(fut -> {

      Configuration config = new ConfigurationBuilder().addServer()
        .host(DATAGRID_HOST) // contains the value datagrid-hotrod that is a service in Openshift
        .port(DATAGRID_PORT) // contains the hotrod point 11222
        .build();

      client = null; // TODO 1: Init the RemoteCacheManager with the config

      defaultCache = null;// TODO 2: Init the defaultCache calling remoteCacheManager.getCache();

      fut.complete();
    }, res -> {
      if (res.succeeded()) {
        System.out.println("Cache started");
      } else {
        res.cause().printStackTrace();
      }
    });
    
    Router router = Router.router(vertx);

    // Creates / route
    router.get("/").handler(rc -> {
      rc.response().end("Welcome");
    });

    // Creates /api route
    router.get("/api").handler(rc -> {
      rc.response().end(new JsonObject().put("name", "duchess").put("version", 1).encode());
    });

    // Adds body handler to be able to read from the body in the requests. This is needed for POST
    router.route().handler(BodyHandler.create());

    // TODO 3: Add the POST route handled by handleAddDuchess in url /api/duchess

    // TODO 5: Add the GET route handled by handleGetDuchess in url /api/duchess/:id

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080);
  }


  private void handleAddDuchess(RoutingContext rc) {
    HttpServerResponse response = rc.response();
    JsonObject bodyAsJson = rc.getBodyAsJson();
    if (bodyAsJson != null && bodyAsJson.containsKey("id") && bodyAsJson.containsKey("name")) {
      // TODO 4: Add a duchess using putAsync(k,v)

    } else {
      response.end("Body is " + bodyAsJson + ". id and name should be provided");
    }
  }

  private void handleGetDuchess(RoutingContext rc) {
    Integer id = Integer.valueOf(rc.request().getParam("id"));
    // TODO 6: Get the duchess using getAsync(id)

  }

}
