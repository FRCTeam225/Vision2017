package org.techfire225.firevision2017;

import java.io.Serializable;

public class VisionMessage implements Serializable {
    static final long serialVersionUID = 2694922774567409174L;
    public boolean found;
    public long age;
    public double theta;
    public double distance;

    public transient double timestamp;

    public VisionMessage(boolean found, long age, double theta, double distance) {
        this.found = found;
        this.age = age;
        this.theta = theta;
        this.distance = distance;
    }
}
