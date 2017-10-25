package delayed.train.positions;

import workshop.model.TrainPosition;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HttpApp extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(HttpApp.class.getName());

//  private int i = 0;

  private RemoteCacheManager client;
  private RemoteCacheManager queryClient;
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

    queryClient = Util.createRemoteCacheManager();
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
    RemoteCache<String, TrainPosition> positionsCache = queryClient.getCache("train-positions");
    // System.out.println("Position key set: " + positionsCache.keySet());
    return trainIds.entrySet().stream()
      .map(e -> getTrainId(e, positionsCache))
      .filter(Objects::nonNull)
      .map(positionsCache::get)
      .map(pos -> String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
        pos.getTrainId(), pos.getCat(), pos.getName(), pos.getLastStopName(),
        pos.current.position.lat, pos.current.position.lng, pos.current.position.bearing
      ))
      .collect(Collectors.joining("\n"));
  }

  private String getTrainId(Map.Entry<String, String> entry, RemoteCache<String, TrainPosition> positionsCache) {
    if (!entry.getValue().isEmpty())
      return entry.getValue();

    String trainName = entry.getKey();
    QueryFactory qf = Search.getQueryFactory(positionsCache);
    Query q = qf.create("select tp.trainId from workshop.model.TrainPosition tp where name = :trainName");
    q.setParameter("trainName", trainName);
    List<Object[]> trains = q.list();

    Iterator<Object[]> it = trains.iterator();
    if (it.hasNext()) {
      // Not accurate but simplest of methods
      String trainId = (String) it.next()[0];
      trainIds.put(trainName, trainId);
      return trainId;
    }

    return null;

//    RemoteCache<String, Map<String, TrainPosition>> positionsCache = client.getCache("train-positions");
//
//    String trainName = entry.getKey();
//    Map<String, TrainPosition> trainPositions = positionsCache.get(trainName);
//    if (trainPositions != null && !trainPositions.isEmpty()) {
//      log.info("Position for train `" + trainName + "` is " + trainPositions);
//      return trainPositions.values().stream()
//        .filter(train -> train.delay > 0)
//        .findFirst()
//        .map(delayedTrain -> recordTrainId(trainName, delayedTrain))
//        .orElse(recordTrainId(trainName, trainPositions.values().iterator().next()));
//    }
//    return null;
  }

  private String recordTrainId(String trainName, TrainPosition trainPosition) {
    String trainId = trainPosition.getTrainId();

    return trainId;
  }


//  private String positions() {
//    return
//      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
//        "84/80849/18/30/95\tS\tS 3\tSt. Margrethen\t47.474748\t9.481462\t0.0\n" +
//        "84/414330/18/21/95\tRE\tRE 2575\tMilano Centrale\t46.175546\t10.146762\t190.0\n" +
//        "84/364141/18/19/95\tTER\tTER96456\tPontarlier (F)\t46.854834\t6.176659\t110.0\n" +
//        "84/451623/18/19/95\tGEX\tGEX 903\tZermatt\t46.03891\t7.761331\t210.0\n" +
//        "84/242400/18/19/95\tRE\tRE 1354\tLandquart\t46.907376\t9.810728\t130.0\n" +
//        "84/25571/18/21/95\tICN\tICN 526\tGenève-Aéroport\t46.886584\t6.767367\t200.0";
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
