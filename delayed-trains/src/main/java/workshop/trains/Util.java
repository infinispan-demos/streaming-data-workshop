package workshop.trains;

import io.vertx.reactivex.core.Future;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import workshop.model.GeoLocBearing;
import workshop.model.TimedPosition;
import workshop.model.TrainPosition;

import java.io.IOException;

import static workshop.shared.Constants.DATAGRID_HOST;
import static workshop.shared.Constants.DATAGRID_PORT;

public class Util {

  static void remoteCacheManager(Future<RemoteCacheManager> f) {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host(DATAGRID_HOST)
        .port(DATAGRID_PORT)
        .marshaller(ProtoStreamMarshaller.class)
        .build());

    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);
    try {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources("train-position.proto"));
      ctx.registerMarshaller(new TrainPosition.Marshaller());
      ctx.registerMarshaller(new TimedPosition.Marshaller());
      ctx.registerMarshaller(new GeoLocBearing.Marshaller());
      f.complete(client);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
