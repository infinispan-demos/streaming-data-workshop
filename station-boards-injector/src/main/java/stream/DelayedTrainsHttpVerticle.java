package stream;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.core.Vertx;

/**
 * TODO: Should be in a different module
 */
public class DelayedTrainsHttpVerticle extends AbstractVerticle {

  // Does not need to be volatile, it's only read/updated by the verticle EL thread
  private boolean injectorStarted = false;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(DelayedTrainsHttpVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> future) {
    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/").handler(StaticHandler.create());

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

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    PermittedOptions outPermit = new PermittedOptions().setAddress("delayed-trains");
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER) {
        System.out.println("SockJs: Connected, start data injector");
        if (!injectorStarted) {
          deployStationBoardsInjectorVerticle();
          injectorStarted = true;
        }
      }
      be.complete(true);
    });
    router.route("/eventbus/*").handler(sockJSHandler);

    deployDelayedTrainsListenerVerticle();
  }

  private void deployStationBoardsInjectorVerticle() {
    // TODO: Does it need setWorker(true) ?
    DeploymentOptions options = new DeploymentOptions();
    vertx.deployVerticle(StationBoardsInjectorVerticle.class.getName(), options);
  }

  private void deployDelayedTrainsListenerVerticle() {
    DeploymentOptions options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle(DelayedTrainsListenerVerticle.class.getName(), options);
  }

}
