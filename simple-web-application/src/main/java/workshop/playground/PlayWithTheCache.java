package workshop.playground;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

// TODO 1: Extends AbstractVerticle
public class PlayWithTheCache {

  protected RemoteCacheManager client;
  protected RemoteCache<Integer, String> defaultCache;

  // TODO 2: Override 'start' method
  // TODO 3: create 'Router router = Router.router(vertx);' in the start method
  // TODO 4: Create an empty GET endpoint that will respond Welcome from the Router
  // TODO 5: Create a GET endpoint in /api that responds {"name":"duchess","version":1}
  // TODO 6: Create a HttpServer with vertx object with requestHandler router::accept and listening to port 8080

  // TODO 8: Add a body handler to the route router.route().handler(BodyHandler.create());
  // TODO 9: Add the POST router.post("/api/duchess").handler(this::handleAddDuchess);

  // TODO 11: Add the GET router.get("/api/duchess/:id").handler(this::handleGetDuchess);

  private void handleAddDuchess(RoutingContext rc) {
    // TODO 10: Implement handleAddDuchess so that it takes the id and the value in the body using rc.getBodyAsJson() and defaultCache.putAsync(k,v)
  }

  private void handleGetDuchess(RoutingContext rc) {
    // TODO 12: Implement handleGetDuchess so that it takes the id using rc.request().getParam and defaultCache.getAsync(k)
  }

  protected void initCache(Vertx vertx) {
    vertx.executeBlocking(fut -> {

      Configuration config = new ConfigurationBuilder().addServer()
        .host(DATAGRID_HOST) // contains the value datagrid-hotrod that is a service in Openshfitt
        .port(DATAGRID_PORT) // contains the hotrod point 11222
        .build();

      client = null; // TODO 7: Init the RemoteCacheManager with the config

      defaultCache = null;// TODO 8: Init the defaultCache calling remoteCacheManager.getCache();

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
