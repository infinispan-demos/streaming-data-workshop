package delayed.train.positions;

import io.vertx.core.Future;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;

public class HttpApp extends AbstractVerticle {
  private static final String TRAINS_POSITIONS_ADDRESS = "trains.positions";

  private int pos = 0;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(HttpApp.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions options = new BridgeOptions()
      .addOutboundPermitted(new PermittedOptions().setAddress(TRAINS_POSITIONS_ADDRESS));
    sockJSHandler.bridge(options);

    router.route("/eventbus/*").handler(sockJSHandler);

    vertx.setPeriodic(3000, tid -> {
      String[] positions = positions();
      String position = positions[(pos++) % positions.length];
      System.out.println("position = " + position);
      vertx.eventBus().send("trains.positions", position);
    });

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .rxListen(9000)
      .subscribe(httpServer -> startFuture.complete(), startFuture::fail);
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
