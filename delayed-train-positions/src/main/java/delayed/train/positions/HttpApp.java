package delayed.train.positions;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApp extends AbstractVerticle {

  private int i = 0;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner r = new Runner("delayed-train-positions/src/main/java/");
    r.runExample(HttpApp.class);
  }

  @Override
  public void start(Future<Void> future) {
    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/position").handler(this::position);
    router.get("/").handler(StaticHandler.create());

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(
        // Retrieve the port from the configuration, default to 8080.
        config().getInteger("http.port", 9000), "localhost", ar -> {
          if (ar.succeeded()) {
            System.out.println("Server starter on port " + ar.result().actualPort());
          }
          future.handle(ar.mapEmpty());
        });
  }

  private void position(RoutingContext rc) {
    int pos = this.i++ % 3;
    System.out.println("Retrieve /position [" + pos + "]");
    rc.response()
      .putHeader("Access-Control-Allow-Origin", "*")
      .end(
        positions()[pos]
      );
  }

  private String[] positions() {
    return new String[]{
      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.474748\t9.481462\t0.0\n" +
        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.175546\t10.146762\t190.0\n" +
        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.854834\t6.176659\t110.0\n" +
        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.03891\t7.761331\t210.0\n" +
        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.907376\t9.810728\t130.0\n" +
        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.886584\t6.767367\t200.0"
      ,
      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.475152\t9.487655\t0.0\n" +
        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.172813\t10.142475\t190.0\n" +
        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.865099\t6.163211\t110.0\n" +
        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.037202\t7.759758\t210.0\n" +
        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.90823\t9.806602\t140.0\n" +
        "84/269912/18/19/95\tIRE\tIRE 4211\tLindau Hbf\t47.737449\t9.620462\t210.0\n" +
        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.879141\t6.759222\t200.0"
      ,
      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.475925\t9.493354\t10.0\n" +
        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.167339\t10.133917\t190.0\n" +
        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.87096\t6.155525\t110.0\n" +
        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.035269\t7.75796\t210.0\n" +
        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.90965\t9.798466\t140.0\n" +
        "84/269912/18/19/95\tIRE\tIRE 4211\tLindau Hbf\t47.729403\t9.609414\t200.0\n" +
        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.865099\t6.741451\t200.0"
    };
  }

}
