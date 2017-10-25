package stream;

import workshop.model.Stop;
import workshop.model.TrainPosition;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.util.Util;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;
import static stream.Util.mkRemoteCacheManager;

public class DelayedTrainsListenerVerticle extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(DelayedTrainsListenerVerticle.class.getName());

  private RemoteCacheManager client;
  private RemoteCacheManager delayedCacheClient;

  private RemoteCache<String, Stop> stopsCache;
  private ContinuousQuery<String, Stop> delayedTrainsCQ;
  private RemoteCache<String, String> delayedCache;

  @Override
  public void start() throws Exception {
    vertx.<RemoteCacheManager>executeBlocking(
      fut -> fut.complete(mkRemoteCacheManager()),
      res -> {
        client = res.result();
        addProtoDescriptorToServer(client.getCache(PROTOBUF_METADATA_CACHE_NAME));

        client.administration().createCache("station-board-stops", "distributed");
        stopsCache = client.getCache("station-board-stops");
        stopsCache.clear();

        delayedCacheClient = mkDelayedTrainRemoteCacheManager();
        delayedCacheClient.administration().createCache("delayed-trains", "replicated");
        delayedCache = delayedCacheClient.getCache("delayed-trains");
        delayedCache.clear();

        addDelayedTrainsListener(stopsCache);
      });
  }

  // TODO: Temporary, should be kept in separate module with its own hotrod-client.properties
  public static RemoteCacheManager mkDelayedTrainRemoteCacheManager() {
    ConfigurationBuilder cfg = new ConfigurationBuilder();
    String host = System.getProperty("server.host", "datagrid-hotrod");
    int port = Integer.getInteger("server.port", 11222);
    cfg.addServer().host(host).port(port);
    return new RemoteCacheManager(cfg.build());
  }

  @Override
  public void stop() throws Exception {
    if (delayedTrainsCQ != null)
      delayedTrainsCQ.removeAllListeners();

    if (stopsCache != null)
      stopsCache.clear();

    if (delayedCache != null)
      delayedCache.clear();

    if (client != null)
      client.stop();

    if (delayedCacheClient != null)
      delayedCacheClient.stop();
  }

  private void addDelayedTrainsListener(RemoteCache<String, Stop> stopsCache) {
    QueryFactory qf = Search.getQueryFactory(stopsCache);

    Query query = qf.from(Stop.class)
      .having("delayMin").gt(0L)
      .build();

    ContinuousQueryListener<String, Stop> listener =
      new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String id, Stop stop) {
          vertx.runOnContext(x -> {
            //System.out.println(stop);
            vertx.eventBus().publish("delayed-trains", toJson(stop));
            delayedCache.putAsync(stop.train.getName(), stop.train.getName());
          });
        }

        @Override
        public void resultUpdated(String id, Stop stop) {
        }

        @Override
        public void resultLeaving(String id) {
        }
      };

    delayedTrainsCQ = Search.getContinuousQuery(stopsCache);
    delayedTrainsCQ.addContinuousQueryListener(query, listener);
  }

  private String toJson(Stop stop) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", stop.train.getCategory());
    map.put("departure", String.format("%tR", stop.departureTs));
    map.put("station", stop.station.getName());
    map.put("destination", stop.train.getTo());
    map.put("delay", stop.delayMin);
    map.put("trainName", stop.train.getName());
    return new JsonObject(map).encode();
  }


  private static void addProtoDescriptorToServer(RemoteCache<String, String> metaCache) {
    InputStream is = DelayedTrainsListenerVerticle.class.getResourceAsStream("/datamodel.proto");
    metaCache.put("datamodel.proto", readInputStream(is));

    String errors = metaCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null)
      throw new AssertionError("Error in proto file");
    else
      log.info("Added datamodel.proto file to server");

    is = TrainPosition.class.getResourceAsStream("/train-position.proto");
    metaCache.put("train-position.proto", readInputStream(is));

    errors = metaCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null)
      throw new AssertionError("Error in proto file");
    else
      log.info("Added train-position.proto file to server");
  }

  private static String readInputStream(InputStream is) {
    try {
      return Util.read(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
