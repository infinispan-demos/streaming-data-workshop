package workshop;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX;
import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;
import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;
import static workshop.shared.Constants.DELAYED_TRAINS_CACHE_NAME;
import static workshop.shared.Constants.STATION_BOARDS_CACHE_NAME;
import static workshop.shared.Constants.STATION_BOARD_PROTO;
import static workshop.shared.Constants.TRAIN_POSITION_PROTO;

class Admin {

  private static final Logger log = Logger.getLogger(Admin.class.getName());

  static Void createRemoteCaches() {
    RemoteCacheManager client = createManagementClient();
    try {
      RemoteCache<String, String> protoCache = client.getCache(PROTOBUF_METADATA_CACHE_NAME);
      addProtoDescriptorToServer(STATION_BOARD_PROTO, protoCache);
      addProtoDescriptorToServer(TRAIN_POSITION_PROTO, protoCache);

      client.administration().createCache(STATION_BOARDS_CACHE_NAME, "distributed");
      //client.getCache(STATION_BOARDS_CACHE_NAME).clear();

      client.administration().createCache(DELAYED_TRAINS_CACHE_NAME, "replicated");
      //client.getCache(DELAYED_TRAINS_CACHE_NAME).clear();
      return null;
    } finally {
      client.stop();
    }
  }

  private static RemoteCacheManager createManagementClient() {
    return new RemoteCacheManager(new ConfigurationBuilder().addServer()
      .host(DATAGRID_HOST)
      .port(DATAGRID_PORT)
      .marshaller(ProtoStreamMarshaller.class)
      .build());
  }

  private static void addProtoDescriptorToServer(String protoFile, RemoteCache<String, String> protoCache) {
    InputStream is = Admin.class.getResourceAsStream(protoFile);
    protoCache.put(protoFile, readInputStream(is));

    String errors = protoCache.get(ERRORS_KEY_SUFFIX);
    if (errors != null)
      throw new AssertionError("Error in proto file");
    else
      log.info("Added " + protoFile + "file to server");
  }

  private static String readInputStream(InputStream is) {
    try {
      return Util.read(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
