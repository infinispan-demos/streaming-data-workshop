package delayed.train.positions;

import datamodel.Train;
import datamodel.TrainPosition;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApp extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(HttpApp.class.getName());

//  private int i = 0;

  private RemoteCacheManager client;
  private RemoteCache<String, String> delayedCache;
//  RemoteCache<String, Map<String, TrainPosition>> positionsCache;

  private ConcurrentMap<String, String> trainIds = new ConcurrentHashMap<>();

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner r = new Runner("delayed-train-positions/src/main/java/");
    r.runExample(HttpApp.class);
  }

  @Override
  public void start(Future<Void> future) {
    // Create a router object.
    Router router = Router.router(vertx);

    client = new RemoteCacheManager();
    delayedCache = client.getCache("delayed-trains");
    delayedCache.addClientListener(new DelayedTrainListener());

    // positionsCache = client.getCache("train-positions");

    router.get("/position").handler(this::position);
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
  }

  @Override
  public void stop() throws Exception {
    if (delayedCache != null)
      delayedCache.clear();

    if (client != null)
      client.stop();
  }

  private void position(RoutingContext rc) {
    //int pos = this.i++ % 3;
    System.out.println("Retrieve /position");
    rc.response()
      .putHeader("Access-Control-Allow-Origin", "*")
      .end(
        positions()
      );
  }

  private String positions() {
    return
      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
        showTrainPositions(trainIds);
  }

  private String showTrainPositions(ConcurrentMap<String, String> trainIds) {
    RemoteCache<String, Map<String, TrainPosition>> positionsCache = client.getCache("train-positions");
    System.out.println("Position key set: " + positionsCache.keySet());
    return trainIds.entrySet().stream()
      .filter(e -> findTrainId(e) != null)
      .map(e -> {
        String trainRoute = e.getKey();
        String trainId = e.getValue();
        return positionsCache.get(trainRoute).get(trainId);
      })
      .filter(Objects::nonNull)
      .map(pos -> String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
        pos.getTrainId(), pos.getCat(), pos.getName(), pos.getLastStopName(),
        pos.current.position.lat, pos.current.position.lng, pos.current.position.bearing
      ))
      .collect(Collectors.joining("\n"));
  }

  private String findTrainId(Map.Entry<String, String> entry) {
    if (!entry.getValue().isEmpty())
      return entry.getValue();

    RemoteCache<String, Map<String, TrainPosition>> positionsCache = client.getCache("train-positions");

    String trainName = entry.getKey();
    Map<String, TrainPosition> trainPositions = positionsCache.get(trainName);
    if (trainPositions != null) {
      log.info("Position for train `" + trainName + "` is " + trainPositions);
      return trainPositions.values().stream()
        .filter(train -> train.delay > 0)
        .findFirst()
        .map(delayedTrain -> recordTrainId(trainName, delayedTrain))
        .orElse(!trainPositions.isEmpty() ? recordTrainId(trainName, trainPositions.values().iterator().next()) : null);
    }
    return null;
  }

  private String recordTrainId(String trainName, TrainPosition delayedTrain) {
    String trainId = delayedTrain.getTrainId();
    trainIds.put(trainName, trainId);
    return trainId;
  }


//  private String[] positions() {
//    return new String[]{
//      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
//        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.474748\t9.481462\t0.0\n" +
//        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.175546\t10.146762\t190.0\n" +
//        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.854834\t6.176659\t110.0\n" +
//        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.03891\t7.761331\t210.0\n" +
//        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.907376\t9.810728\t130.0\n" +
//        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.886584\t6.767367\t200.0"
//      ,
//      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
//        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.475152\t9.487655\t0.0\n" +
//        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.172813\t10.142475\t190.0\n" +
//        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.865099\t6.163211\t110.0\n" +
//        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.037202\t7.759758\t210.0\n" +
//        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.90823\t9.806602\t140.0\n" +
//        "84/269912/18/19/95\tIRE\tIRE 4211\tLindau Hbf\t47.737449\t9.620462\t210.0\n" +
//        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.879141\t6.759222\t200.0"
//      ,
//      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
//        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.475925\t9.493354\t10.0\n" +
//        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.167339\t10.133917\t190.0\n" +
//        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.87096\t6.155525\t110.0\n" +
//        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.035269\t7.75796\t210.0\n" +
//        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.90965\t9.798466\t140.0\n" +
//        "84/269912/18/19/95\tIRE\tIRE 4211\tLindau Hbf\t47.729403\t9.609414\t200.0\n" +
//        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.865099\t6.741451\t200.0"
//    };
//  }

  @ClientListener
  public final class DelayedTrainListener {

    @ClientCacheEntryCreated
    @SuppressWarnings("unused")
    public void created(ClientCacheEntryCreatedEvent<String> e) {
      log.info("Created event: " + e);
      String trainName = e.getKey();
      trainIds.put(trainName, "");

//      // TODO: Optimise so that it can cached on startup, but for that to happen, cache must have been created
//      // TODO: So, who and where should be created?
//      RemoteCache<String, Map<String, TrainPosition>> positionsCache = client.getCache("train-positions");
//
//      Map<String, TrainPosition> trainPositions = positionsCache.get(trainName);
//      if (trainPositions != null) {
//        log.info("Position for train `"  + trainName + "` is " + trainPositions);
//        trainPositions.values().stream()
//          .filter(train -> train.delay > 0)
//          .findFirst()
//          .map(delayedTrain ->
//            trainIds.put(trainName, delayedTrain.getTrainId()));
    }

//      delayedTrains.put(trainName, trainName);
//      List<TrainPosition> trainPositions = positionsCache.get(trainName);
//      trainPositions.stream()
//        .filter(train -> train.delay > 0)
//        .findFirst()
//        .map(delayedTrain -> delayedTrains.put(trainName, trainName));

//    @ClientCacheEntryModified
//    @SuppressWarnings("unused")
//    public void modified(ClientCacheEntryModifiedEvent e) {
//      modifiedEvents.add(e);
//    }
//
//    @ClientCacheEntryRemoved
//    @SuppressWarnings("unused")
//    public void removed(ClientCacheEntryRemovedEvent e) {
//      // TODO: Remove those trains no longer delayed from map?
//    }
  }

}
