package workshop.playground;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PlayWithTheCacheTest {

  Vertx vertx;

  @Before
  public void before(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void after(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testDeployAndUndeploy(TestContext context) {
    // Deploy and undeploy a verticle
    vertx.deployVerticle(PlayWithCacheMockedRemoteCalls.class.getName(), context.asyncAssertSuccess(deploymentID ->
      vertx.undeploy(deploymentID, context.asyncAssertSuccess())));
  }

//  @Test
//  public void testPutAndGet(TestContext context) {
//    HttpClient client = vertx.createHttpClient();
//    Async async = context.async();
//    vertx.deployVerticle(PlayWithCacheMockedRemoteCalls.class.getName(), context.asyncAssertSuccess(deploymentID -> {
//
//      client.get(8080, "localhost", "/", resp -> {
//        resp.bodyHandler(body -> {
//          context.assertEquals("Welcome", body.toString());
//          client.close();
//          async.complete();
//        });
//      });
//    }));
//  }
}
