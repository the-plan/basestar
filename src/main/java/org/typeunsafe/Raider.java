package org.typeunsafe;


import io.vertx.servicediscovery.Record;

import java.util.List;

public class Raider {

  public String id;
  public String name;

  public Double x;
  public Double y;

  public Double xVelocity = 1.0;
  public Double yVelocity = -1.0;

  public Constraints constraints;


  public Raider(String id, String name, Double x, Double y) {
    this.id = id;
    this.name = name;
    this.x = x;
    this.y = y;
  }

  public Raider(String id, String name, Double x, Double y, Constraints constraints) {
    this.id = id;
    this.name = name;
    this.x = x;
    this.y = y;
    this.constraints = constraints;
  }

  public Raider(Double x, Double y) {
    this.x = x;
    this.y = y;
  }


  public void move() {
    this.x += this.xVelocity;
    this.y += this.yVelocity;
    if(this.x <= this.constraints.border || this.x >= this.constraints.width - this.constraints.border) {
      this.x -= this.xVelocity;
      this.x = Math.max(this.x, this.constraints.border);
      this.x = Math.min(this.x, this.constraints.width - this.constraints.border);
      this.xVelocity = -this.xVelocity;
      this.x += this.xVelocity;
    }
    if(this.y <= this.constraints.border || this.y >= this.constraints.height - this.constraints.border) {
      this.y -= this.yVelocity;
      this.y = Math.max(this.y, this.constraints.border);
      this.y = Math.min(this.y, this.constraints.height - this.constraints.border);
      this.yVelocity = -this.yVelocity;
      this.y += this.yVelocity;
    }
  }

  public Double distance(Raider raider) {
    Double distX = this.x - raider.x;
    Double distY = this.y - raider.y;
    return Math.sqrt(distX * distX + distY * distY);
  }

  public void moveAway(List<Record> raiders, Double minDistance) {
    Double distanceX = 0.0;
    Double distanceY = 0.0;
    Integer numClose = 0;

    for(int i = 0; i < raiders.size(); i++) {

      Raider raider = new Raider(
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("x"),
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("y")
      );

      if(raider.x.equals(this.x) && raider.y.equals(this.y)) continue;

      Double distance = this.distance(raider);

      if(distance < minDistance) {
        numClose++;
        Double xdiff = (this.x - raider.x);
        Double ydiff = (this.y - raider.y);

        if(xdiff >= 0) xdiff = Math.sqrt(minDistance) - xdiff;
        else if(xdiff < 0) xdiff = -Math.sqrt(minDistance) - xdiff;

        if(ydiff >= 0) ydiff = Math.sqrt(minDistance) - ydiff;
        else if(ydiff < 0) ydiff = -Math.sqrt(minDistance) - ydiff;

        distanceX += xdiff;
        distanceY += ydiff;
      }
    }

    if(numClose == 0) return;

    this.xVelocity -= distanceX / 5.0;
    this.yVelocity -= distanceY / 5.0;

  }


  public void moveCloser(List<Record> raiders, Double distance) {
    if(raiders.size() < 1) return;

    Double avgX = 0.0;
    Double avgY = 0.0;

    for(int i = 0; i < raiders.size(); i++) {

      Raider raider = new Raider(
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("x"),
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("y")
      );

      if(raider.x.equals(this.x) && raider.y.equals(this.y)) continue;
      if(this.distance(raider) > distance) continue;

      avgX += (this.x - raider.x);
      avgY += (this.y - raider.y);
    }

    avgX /= raiders.size();
    avgY /= raiders.size();

    distance = Math.sqrt((avgX * avgX) + (avgY * avgY)) * -1.0;
    if(distance == 0) return;

    this.xVelocity= Math.min(this.xVelocity + (avgX / distance) * 0.15, this.constraints.maxVelocity);
    this.yVelocity = Math.min(this.yVelocity + (avgY / distance) * 0.15, this.constraints.maxVelocity);

  }

  public void moveWith(List<Record> raiders, Double distance) {
    if(raiders.size() < 1) return;
    // calculate the average velocity of the other boids

    Double avgX = 0.0;
    Double avgY = 0.0;

    for(int i = 0; i < raiders.size(); i++) {

      Raider raider = new Raider(
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("x"),
        raiders.get(i).getMetadata().getJsonObject("coordinates").getDouble("y")
      );

      if(raider.x.equals(this.x) && raider.y.equals(this.y)) continue;
      if(this.distance(raider) > distance) continue;

      avgX += raider.xVelocity;
      avgY += raider.yVelocity;
    }

    avgX /= raiders.size();
    avgY /= raiders.size();

    distance = Math.sqrt((avgX * avgX) + (avgY * avgY)) * 1.0;
    if(distance == 0) return;

    this.xVelocity= Math.min(this.xVelocity + (avgX / distance) * 0.05, this.constraints.maxVelocity);
    this.yVelocity = Math.min(this.yVelocity + (avgY / distance) * 0.05, this.constraints.maxVelocity);

  }

}
