package org.typeunsafe;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;

import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.Record;

import java.util.Optional;

public class BaseStar extends AbstractVerticle {
  
  private ServiceDiscovery discovery;
  private Record record;

  // Settings to record the service
  private String serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("the-plan");
  private String serviceHost = Optional.ofNullable(System.getenv("SERVICE_HOST")).orElse("localhost"); // domain name
  // this is the visible port from outside
  // for example you run your service with 8080 on a platform (Clever Cloud, Docker, ...)
  // and the visible port is 80
  private Integer servicePort = Integer.parseInt(Optional.ofNullable(System.getenv("SERVICE_PORT")).orElse("80")); // set to 80 on Clever Cloud
  private String serviceRoot = Optional.ofNullable(System.getenv("SERVICE_ROOT")).orElse("/api");

  private Integer httpPort = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
  
  // Redis settings
  private Integer redisPort = Integer.parseInt(Optional.ofNullable(System.getenv("REDIS_PORT")).orElse("6379"));
  private String redisHost = Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("127.0.0.1");
  private String redisAuth = Optional.ofNullable(System.getenv("REDIS_PASSWORD")).orElse(null);
  private String redisRecordsKey = Optional.ofNullable(System.getenv("REDIS_RECORDS_KEY")).orElse("vert.x.ms");  

  public void stop(Future<Void> stopFuture) {
    System.out.println("👋 bye bye " + record.getRegistration());
    discovery.unpublish(record.getRegistration(), ar -> {
      if(ar.succeeded()) {
        System.out.println("unpublished 😀");
        stopFuture.complete();
      } else {
        ar.cause().printStackTrace();
      }
    });
  }

  public void start() {

    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions();
    // Mount the service discovery backend
    discovery = ServiceDiscovery.create(
      vertx,
      serviceDiscoveryOptions.setBackendConfiguration(
        new JsonObject()
          .put("host", redisHost)
          .put("port", redisPort)
          .put("auth", redisAuth)
          .put("key", redisRecordsKey)
      ));

    // create the microservice record
    record = HttpEndpoint.createRecord(
      serviceName,
      serviceHost,
      servicePort,
      serviceRoot
    );

    record.setMetadata(new JsonObject()
      .put("message", "Hello 🌍")
      .put("uri", "/ping")
    );

    discovery.publish(record, res -> {
      System.out.println("😃 published! " + record.getRegistration());
    });

    router.post("/api/ping").handler(context -> {
      // before, you should test that context is not null
      String name = Optional.ofNullable(context.getBodyAsJson().getString("name")).orElse("John Doe");
      System.out.println("🤖 called by " + name);

      context.response()
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(
          new JsonObject().put("message", "👋 hey "+ name + " 😃").toString()
        );
    });

    router.get("/api/ping").handler(context -> {
      context.response()
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(
          new JsonObject().put("message", "🏓 pong!").toString()
        );
    });

    // serve static assets, see /resources/webroot directory
    router.route("/*").handler(StaticHandler.create());

    server.requestHandler(router::accept).listen(httpPort, result -> {
      System.out.println("🌍 Listening on " + httpPort);
    });

  }
}
