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
public class TrajPoint {
    private Stoppage stoppage;
    private int routeId;
    private int vehicleId;
    private int transportMode;
    private boolean touchOn;
    private String datetime;
    private long timeInSec;

    public TrajPoint() {
    
    }

    public TrajPoint(int stopid, int routeId, int vehicleId, int transportMode, boolean touchOn, String datetime, long timeInSec) {
        this.stoppage = new Stoppage(stopid);
        this.routeId = routeId;
        this.vehicleId = vehicleId;
        this.transportMode = transportMode;
        this.touchOn = touchOn;
        this.datetime = datetime;
        this.timeInSec = timeInSec;
    }
    
    public TrajPoint(int stopid, int routeId, int vehicleId, int transportMode, boolean touchOn, String datetime, long timeInSec, double lat, double lon) {
        this.stoppage = new Stoppage(stopid, lat, lon);
        this.routeId = routeId;
        this.vehicleId = vehicleId;
        this.transportMode = transportMode;
        this.touchOn = touchOn;
        this.datetime = datetime;
        this.timeInSec = timeInSec;
    }
    
    public Stoppage getStoppage() {
        return stoppage;
    }

    public void setStoppage(Stoppage stoppage) {
        this.stoppage = stoppage;
    }
    
    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(int transportMode) {
        this.transportMode = transportMode;
    }
    
    public boolean isTouchOn() {
        return touchOn;
    }

    public void setTouchOn(boolean touchOn) {
        this.touchOn = touchOn;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public long getTimeInSec() {
        return timeInSec;
    }

    public void setTimeInSec(long timeInSec) {
        this.timeInSec = timeInSec;
    }
    
    public Coordinate getPointLocation(){
        return stoppage.getStopLocation();
    }

    @Override
    public String toString() {
        return "(Stop = " + stoppage.getStopId() + ", Lat = " + stoppage.getStopLocation().x + ", Lon = " + stoppage.getStopLocation().y + 
                ", Vehicle = " + vehicleId + ", Mode = " + transportMode + ", Route = " + routeId + ", Time = " + datetime + ", Touch-on = " + touchOn + ")";
    }
    
}
