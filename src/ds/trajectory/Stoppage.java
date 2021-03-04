/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.trajectory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * @author Saqib
 */
public class Stoppage {
    private int stopId;
    private Coordinate stopLocation;

    public Stoppage() {
        stopLocation = new Coordinate();
    }

    public Stoppage(int stopId) {
        this.stopId = stopId;
        stopLocation = new Coordinate();
    }

    public Stoppage(int stopId, double lat, double lon) {
        this.stopId = stopId;
        stopLocation = new Coordinate(lat, lon);
    }
    
    public int getStopId() {
        return stopId;
    }

    public void setStopId(int stopId) {
        this.stopId = stopId;
    }

    public Coordinate getStopLocation() {
        return stopLocation;
    }

    public void setStopLocation(Coordinate stopLocation) {
        this.stopLocation = stopLocation;
    }
    
}
