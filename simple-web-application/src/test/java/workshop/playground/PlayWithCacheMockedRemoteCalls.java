package workshop.playground;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import io.vertx.core.Vertx;

public class PlayWithCacheMockedRemoteCalls extends PlayWithTheCache {

  @Override
  protected void initCache(Vertx vertx) {
    client = mock(RemoteCacheManager.class);
    defaultCache = mock(RemoteCache.class);
    when(defaultCache.putAsync(1, "Oihana")).thenReturn(CompletableFuture.completedFuture("Oihana"));
    when(defaultCache.getAsync(1)).thenReturn(CompletableFuture.completedFuture("Oihana"));
    when(client.stopAsync()).thenReturn(CompletableFuture.completedFuture(null));
  }

}

