package stream;

import datamodel.Stop;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
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

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;
import static stream.Util.mkRemoteCacheManager;

public class DelayedTrainsListenerVerticle extends AbstractVerticle {

  private RemoteCacheManager client;
  private RemoteCache<String, Stop> stopsCache;
  private ContinuousQuery<String, Stop> delayedTrainsCQ;

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
        addDelayedTrainsListener(stopsCache);
      });
  }

  @Override
  public void stop() throws Exception {
    if (delayedTrainsCQ != null)
      delayedTrainsCQ.removeAllListeners();

    if (stopsCache != null)
      stopsCache.clear();

    if (client != null)
      client.stop();
  }

  private void addDelayedTrainsListener(RemoteCache<String, Stop> cache) {
    QueryFactory qf = Search.getQueryFactory(cache);

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
          });
        }

        @Override
        public void resultUpdated(String id, Stop stop) {
        }

        @Override
        public void resultLeaving(String id) {
        }
      };

    delayedTrainsCQ = Search.getContinuousQuery(cache);
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
  }

  private static String readInputStream(InputStream is) {
    try {
      return Util.read(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
