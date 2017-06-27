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
import java.util.function.*;

public class BaseStar extends AbstractVerticle {
  
  private ServiceDiscovery discovery;
  private Record record;
  
  private void setDiscovery() {
    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions();

    // Redis settings
    Integer redisPort = Integer.parseInt(Optional.ofNullable(System.getenv("REDIS_PORT")).orElse("6379"));
    String redisHost = Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("127.0.0.1");
    String redisAuth = Optional.ofNullable(System.getenv("REDIS_PASSWORD")).orElse(null);
    String redisRecordsKey = Optional.ofNullable(System.getenv("REDIS_RECORDS_KEY")).orElse("vert.x.ms");    // the redis hash
    
    discovery = ServiceDiscovery.create(
      vertx,
      serviceDiscoveryOptions.setBackendConfiguration(
        new JsonObject()
          .put("host", redisHost)
          .put("port", redisPort)
          .put("auth", redisAuth)
          .put("key", redisRecordsKey)
      ));
  }

  private void setRecord() {

    // Settings to record the service
    String serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("unknown-basestar");
    String serviceHost = Optional.ofNullable(System.getenv("SERVICE_HOST")).orElse("localhost"); // domain name
    // this is the visible port from outside
    // for example you run your service with 8080 on a platform (Clever Cloud, Docker, ...)
    // and the visible port is 80
    Integer servicePort = Integer.parseInt(Optional.ofNullable(System.getenv("SERVICE_PORT")).orElse("80")); // set to 80 on Clever Cloud
    String serviceRoot = Optional.ofNullable(System.getenv("SERVICE_ROOT")).orElse("/api");

    // create the microservice record
    record = HttpEndpoint.createRecord(
      serviceName,
      serviceHost,
      servicePort,
      serviceRoot
    );

    record.setMetadata(new JsonObject()
      .put("kind", "basestar")
      .put("message", "Hello 🌍")
      .put("uri", "/coordinates") 
    );
    // ... perphaps a set of uries
  }

  public void stop(Future<Void> stopFuture) {
    System.out.println("Unregistration process is started...");
    // unpublish the microservice before quit
    discovery.unpublish(record.getRegistration(), asyncResult -> {
      if(asyncResult.succeeded()) {
        System.out.println("👋 bye bye " + record.getRegistration());
      } else {
        System.out.println("😡 Not able to unpublish the microservice: " + asyncResult.cause().getMessage());
        //asyncResult.cause().printStackTrace();
      }
      stopFuture.complete();
    });
  }

  private Router defineRoutes(Router router) {
    
    router.route().handler(BodyHandler.create());

    BiFunction<Double, Double, Double> random = (min, max) -> (Math.random() * (max+1-min)) + min;

    // TODO: add a route to share coordinates
    // How to share or synchronize the data (coordinates list) ?
    // Each time a raider connect -> add to the list
    // the base star can have the list with discovery
    // this is the same for the raiders
      router.get("/api/coordinates/:raiderid").handler(context -> {

        System.out.println("👋  raider " + context.request().getParam("raiderid") + " I'am " + record.getName()); // test if null

        context.response()
          .putHeader("content-type", "application/json;charset=UTF-8")
          .end(
            new JsonObject()
              .put("x", random.apply(1.0, 50.0))
              .put("y", random.apply(1.0, 50.0))
              .toString()
          );
      });

      // serve static assets, see /resources/webroot directory
      router.route("/*").handler(StaticHandler.create());

      return router;
  }

  public void start() {

    setDiscovery();
    setRecord();

    /* === Define routes and start the server ==

    */
    Router router = Router.router(vertx);
    defineRoutes(router);
    Integer httpPort = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
    HttpServer server = vertx.createHttpServer();

    server.requestHandler(router::accept).listen(httpPort, result -> {

      if(result.succeeded()) {
        System.out.println("🌍 Listening on " + httpPort);
        /* === publication ===
          publish the microservice to the discovery backend
        */
        discovery.publish(record, asyncResult -> {

          if(asyncResult.succeeded()) {
            System.out.println("😃 Microservice is published! " + record.getRegistration());
          } else {
            System.out.println("😡 Not able to publish the microservice: " + asyncResult.cause().getMessage());
            //TODO: retry ...
          }

        });

      } else {
        System.out.println("😡 Houston, we have a problem: " + result.cause().getMessage());
      }

    });

  }
}
