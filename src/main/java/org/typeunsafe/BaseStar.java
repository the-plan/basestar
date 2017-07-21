package org.typeunsafe;


import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;

import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.Record;
import me.atrox.haikunator.Haikunator;
import me.atrox.haikunator.HaikunatorBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

public class BaseStar extends AbstractVerticle {
  
  private ServiceDiscovery discovery;
  private Record record;
  private Integer raidersCounter = 0;


  public void stop(Future<Void> stopFuture) {
    System.out.println("Unregistration process is started ("+record.getRegistration()+")...");

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
  private void raiderWorker(Record raiderRecord, WebClient raiderClient) {
    // CircuitBreaker raidersCircuitBreaker

    Raider thatRaider = new Raider(
      raiderRecord.getRegistration(),
      raiderRecord.getName(),
      raiderRecord.getMetadata().getJsonObject("coordinates").getDouble("x"),
      raiderRecord.getMetadata().getJsonObject("coordinates").getDouble("y"),
      new Constraints(
        5.0, 600.0, 600.0, 5.0
      )
    );


    // 👷 this is a worker
    vertx.setPeriodic(500, id ->{
      // This handler will get called every 500 ms

      discovery.getRecords(r -> r.getMetadata().getString("kind").equals("raider") , ar -> {

        if(ar.succeeded()) {
          //
          List<Record> raidersRecords = ar.result();
          thatRaider.moveWith(raidersRecords, 300.0);
          thatRaider.moveCloser(raidersRecords, 300.0);
          thatRaider.moveAway(raidersRecords, 15.0);

          thatRaider.move();

          // 👋 === try to contact the raider ===
          // 🙋 -> Circuit Breaker - useful ? better to use from the raider to detect if its basestar is dead
          //raidersCircuitBreaker.executeCommandWithFallback();
          raiderClient.post("/api/coordinates").sendJsonObject(
            new JsonObject()
              .put("x",thatRaider.x)
              .put("y",thatRaider.y)
              .put("xVelocity",thatRaider.xVelocity)
              .put("yVelocity",thatRaider.yVelocity),
            asyncPostRes -> {
              if(asyncPostRes.succeeded()) {
                System.out.println("😀 " + asyncPostRes.result().bodyAsJsonObject().encodePrettily());
              } else { // ouch
                this.raidersCounter -= 1; //TODO check if this is the good place
                record.getMetadata().put("raiders_counter", raidersCounter);
                discovery.update(record, asyncRecUpdateRes -> {
                  System.out.println("🤖 😡 (" + record.getName() + ") I'm updated with " + raiderRecord.getName());
                });
                System.out.println("😡 " + asyncPostRes.cause().getMessage());
              }
            }
          );


        } else { // unable to fetch the raiders list
          //TODO: foo....
        }
      });
    });
  }



  public void start() {

    // === Discovery settings ===
    ServiceDiscoveryOptions serviceDiscoveryOptions = new ServiceDiscoveryOptions();

    // Redis settings with the standard Redis Backend

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

    /**
     * Define microservice options
     * Aka Settings to record the service
     *
     * servicePort: this is the visible port from outside
     * for example you run your service with 8080 on a platform (Clever Cloud, Docker, ...)
     * and the visible port is 80
     */

    Haikunator haikunator = new HaikunatorBuilder().setTokenLength(6).build();

    String niceName = haikunator.haikunate();

    String serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("the-plan")+"-"+niceName;
    String serviceHost = Optional.ofNullable(System.getenv("SERVICE_HOST")).orElse("localhost"); // domain name
    Integer servicePort = Integer.parseInt(Optional.ofNullable(System.getenv("SERVICE_PORT")).orElse("80")); // set to 80 on Clever Cloud
    String serviceRoot = Optional.ofNullable(System.getenv("SERVICE_ROOT")).orElse("/api");

    // create the microservice record
    record = HttpEndpoint.createRecord(
      serviceName,
      serviceHost,
      servicePort,
      serviceRoot
    );

    System.out.println("🎃  " + record.toJson().encodePrettily());

    // add some metadata
    record.setMetadata(new JsonObject()
      .put("kind", "basestar")
      .put("message", "Hello 🌍")
      .put("uri", "/coordinates")
      .put("raiders_counter", raidersCounter)
      .put("app_id", Optional.ofNullable(System.getenv("APP_ID")).orElse("🤖"))
      .put("instance_id", Optional.ofNullable(System.getenv("INSTANCE_ID")).orElse("🤖"))
      .put("instance_type", Optional.ofNullable(System.getenv("INSTANCE_TYPE")).orElse("production")) // build or production
      .put("instance_number", Integer.parseInt(Optional.ofNullable(System.getenv("INSTANCE_NUMBER")).orElse("0")))
    );
    /*
    This variable allows your application to differentiate each running node on the application level.
    It will contain a different number for each instance of your application.
    For example, if three instances are running, it will contain 0 for the first, 1 for the second and 2 for the third. It's handy if you want to only run crons on 1 instance (e.g. only on instance 0)
   */

    System.out.println("🤖 " + record.getName() + " is starting... ");


    /* === Define routes and start the server ==

    */
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.get("/api/raiders").handler(context -> {
      discovery.getRecords(r -> r.getMetadata().getString("kind").equals("raider") , ar -> {
        if (ar.succeeded()) {
          context.response()
            .putHeader("content-type", "application/json;charset=UTF-8")
            .end(new JsonArray(ar.result()).encodePrettily());
        } else {
          //TODO: foo...
        }
      });
    });

    router.post("/api/raiders").handler(context -> {
      // 👋 I'm a new 🚀 raider
      String registationId = Optional.ofNullable(context.getBodyAsJson().getString("registration")).orElse("unknown");

      discovery.getRecord(r -> r.getRegistration().equals(registationId), asyncResRecord -> { // = raider's record
        // get a web client

        if(asyncResRecord.succeeded()) {
          Record raiderRecord = asyncResRecord.result();
          ServiceReference reference = discovery.getReference(raiderRecord);
          WebClient raiderClient = reference.getAs(WebClient.class);

          this.raidersCounter += 1;
          record.getMetadata().put("raiders_counter", raidersCounter);

          discovery.update(record, ar -> {
            System.out.println("🤖 (" + record.getName() + ") I'm updated with " + raiderRecord.getName());
          });

          // 👷 this is a worker

          this.raiderWorker(
            raiderRecord,
            raiderClient
          );

          // message to the raider
          context.response()
            .putHeader("content-type", "application/json;charset=UTF-8")
            .end(new JsonObject().put("message", "ok, registered").encodePrettily());


        } else {
          System.out.println("😡 Unable to get a WebClient for the service: " + asyncResRecord.cause().getMessage());
          System.out.println("😡, Houston, we have a problem with this raider");
          context.response()
            .putHeader("content-type", "application/json;charset=UTF-8")
            .end(new JsonObject().put("error", "ouch").encodePrettily());
        }

      });

    });

    // 🤖 === health check of existing basestars
    // only to say "hey I'm bad"

    System.out.println("🚀 healthChecker ...");

    HealthCheckHandler hch = HealthCheckHandler.create(vertx);

    hch.register("iamok", future ->
      discovery.getRecord(r -> r.getRegistration().equals(record.getRegistration()), ar -> {
        if(ar.succeeded()) {
          future.complete();
        } else {
          System.out.println("😡 not in a good shape");
          ar.cause().printStackTrace();
          future.fail(ar.cause());
        }
      })
    );

    router.get("/health").handler(hch);
    //TODO a route to kill or stop or restart the service

    // use me with other microservices
    ServiceDiscoveryRestEndpoint.create(router, discovery);



    // serve static assets, see /resources/webroot directory
    router.route("/*").handler(StaticHandler.create());


    Integer httpPort = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
    System.out.println("😛 " + httpPort);
    System.out.println("😛 " + System.getenv("PORT"));

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
