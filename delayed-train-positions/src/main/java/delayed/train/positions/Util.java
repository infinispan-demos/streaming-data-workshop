package delayed.train.positions;

import datamodel.GeoLocBearing;
import datamodel.TimedPosition;
import datamodel.TrainPosition;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;

public class Util {

  static RemoteCacheManager createRemoteCacheManager() {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host("datagrid-hotrod")
        .port(11222)
        .marshaller(ProtoStreamMarshaller.class)
        .build());

    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);
    try {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources("train-position.proto"));
      ctx.registerMarshaller(new TrainPosition.Marshaller());
      ctx.registerMarshaller(new TimedPosition.Marshaller());
      ctx.registerMarshaller(new GeoLocBearing.Marshaller());
      return client;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
