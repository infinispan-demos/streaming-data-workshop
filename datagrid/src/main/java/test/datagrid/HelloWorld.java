package test.datagrid;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketAddress;
import java.util.Set;

public class HelloWorld extends AbstractVerticle {

  private static final String template = "Hola, %s!";

  @Override
  public void start(Future<Void> future) {
    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/api/greeting").handler(this::greeting);

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(
        // Retrieve the port from the configuration, default to 8080.
        config().getInteger("http.port", 8080), ar -> {
          if (ar.succeeded()) {
            System.out.println("Server starter on port " + ar.result().actualPort());
          }
          future.handle(ar.mapEmpty());
        });
  }

  private void greeting(RoutingContext rc) {
    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }

    JsonObject response;
    RemoteCacheManager client = null;
    try {
      client = new RemoteCacheManager();
      RemoteCache<String, String> cache = client.getCache("default");
      String content = String.format(template, name);

      String prev = cache.put(name, content);
      String value = cache.get(name);

      Set<SocketAddress> topology =
        cache.getCacheTopologyInfo().getSegmentsPerServer().keySet();

      response = new JsonObject()
        .put("put", prev == null ? "null" : prev)
        .put("get", value == null ? "null" : value)
        .put("topology", topology.toString());
    } catch (Exception e) {
      response = new JsonObject().put("error", printStackTrace(e));
    } finally {
      if (client != null) client.stop();
    }

    rc.response()
      .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
      .end(response.encodePrettily());
  }

  private static String printStackTrace(Exception e) {
    StringWriter errors = new StringWriter();
    e.printStackTrace(new PrintWriter(errors));
    return errors.toString();
  }

}
