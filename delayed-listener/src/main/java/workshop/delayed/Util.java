package workshop.delayed;

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import workshop.model.Station;
import workshop.model.Stop;
import workshop.model.Train;

import java.util.logging.Level;
import java.util.logging.Logger;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.STATION_BOARDS_CACHE_NAME;

public class Util {

  private static final Logger log = Logger.getLogger(Util.class.getName());

  static void remoteCacheManager(Future<RemoteCacheManager> f) {
    try {
      RemoteCacheManager client = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host(DATAGRID_HOST)
          .port(DATAGRID_PORT)
          .marshaller(ProtoStreamMarshaller.class)
          .build());

      SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);

      ctx.registerProtoFiles(FileDescriptorSource.fromResources("station-board.proto"));
      ctx.registerMarshaller(new Stop.Marshaller());
      ctx.registerMarshaller(new Station.Marshaller());
      ctx.registerMarshaller(new Train.Marshaller());
      f.complete(client);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error creating client", e);
      throw new RuntimeException(e);
    }
  }

  static Handler<Future<RemoteCache<String, Stop>>> remoteCache(RemoteCacheManager remote) {
    return f -> f.complete(remote.getCache(STATION_BOARDS_CACHE_NAME));
  }

  static Single<HttpResponse<String>> httpGet(String host, String uri, Vertx vertx) {
    log.info("Call HTTP GET " + host + uri);
    WebClient client = WebClient.create(vertx);
    return client
      .get(8080, host, uri)
      .as(BodyCodec.string())
      .rxSend();
  }

}
