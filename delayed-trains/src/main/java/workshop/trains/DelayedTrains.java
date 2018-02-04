package workshop.trains;

import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import workshop.model.TrainPosition;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static workshop.shared.Constants.DELAYED_TRAINS_CACHE_NAME;
import static workshop.shared.Constants.DELAYED_TRAINS_POSITIONS_ADDRESS;
import static workshop.shared.Constants.DELAYED_TRAINS_POSITIONS_URI;
import static workshop.shared.Constants.LISTEN_URI;
import static workshop.shared.Constants.TRAIN_POSITIONS_CACHE_NAME;

public class DelayedTrains extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(DelayedTrains.class.getName());

  private RemoteCacheManager client;

  private ConcurrentMap<String, String> trainIds = new ConcurrentHashMap<>();

  @Override
  public void start(io.vertx.core.Future<Void> future) throws Exception {
    Router router = Router.router(vertx);

    router.get(DELAYED_TRAINS_POSITIONS_URI).blockingHandler(this::positionsHandler);

    // TODO live coding

    router.get(LISTEN_URI).handler(this::listen);

    vertx
      .rxExecuteBlocking(Util::remoteCacheManager)
      .flatMap(remote ->
        vertx.createHttpServer()
          .requestHandler(router::accept)
          .rxListen(8080)
          .map(s -> remote)
      )
      .subscribe(
        remote -> {
          client = remote;
          log.info("Delayed trains HTTP server started");
          future.complete();
        },
        future::fail
      );
  }

  private void listen(RoutingContext ctx) {
    vertx
      .rxExecuteBlocking(this::addDelayedTrainsListener)
      .doOnSuccess(v -> vertx.setPeriodic(3000, l -> publishPositions()))
      .subscribe(res ->
          ctx.response().end("Listener started")
        , t -> {
          log.log(Level.SEVERE, "Failed to start listener", t);
          ctx.response().end("Failed to start listener");
        });
  }

  private void publishPositions() {
    // TODO live coding
  }

  private void addDelayedTrainsListener(Future<Void> f) {
    RemoteCache<Object, Object> delayed = client.getCache(DELAYED_TRAINS_CACHE_NAME);
    delayed.clear();
    delayed.addClientListener(new DelayedTrainListener());
    log.info("Added delayed train listener");
    f.complete();
  }

  @Override
  public void stop() {
    if (Objects.nonNull(client)) client.stop();
  }

  private void positionsHandler(RoutingContext ctx) {
    log.info(() -> "HTTP GET " + DELAYED_TRAINS_POSITIONS_URI);
    ctx.response()
      .putHeader("Access-Control-Allow-Origin", "*")
      .end(showPositions());
  }

  private void positions(Future<String> f) {
    f.complete(showPositions());
  }

  private String showPositions() {
    return
      "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n" +
        showTrains(trainIds);
  }

  private String showTrains(ConcurrentMap<String, String> trainIds) {
    RemoteCache<String, TrainPosition> positions = client.getCache(TRAIN_POSITIONS_CACHE_NAME);
    return trainIds.entrySet().stream()
      .map(e -> getTrainId(e, positions))
      .filter(Objects::nonNull)
      .map(positions::get)
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
    QueryFactory queryFactory = Search.getQueryFactory(positionsCache);

    // TODO 3 - Create Infinispan Ickle to get train ids for all train positions with a given train name
    Query query = queryFactory.create("select tp.trainId from workshop.model.TrainPosition tp where name = :trainName");
    query.setParameter("trainName", trainName);

    // TODO 4 - List the results of the query
    List<Object[]> trains = query.list();

    Iterator<Object[]> it = trains.iterator();
    if (it.hasNext()) {
      // Not accurate but simplest of methods
      String trainId = (String) it.next()[0];
      trainIds.put(trainName, trainId);
      return trainId;
    }

    return null;
  }

  @ClientListener
  public final class DelayedTrainListener {

    @ClientCacheEntryCreated
    @SuppressWarnings("unused")
    public void created(ClientCacheEntryCreatedEvent<String> e) {
      log.info("Created event: " + e);
      String trainName = e.getKey();
      trainIds.put(trainName, "");
    }

  }

}
