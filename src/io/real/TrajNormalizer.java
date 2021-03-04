/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.real;

import com.vividsolutions.jts.geom.Coordinate;
import ds.trajectory.Stoppage;
import ds.trajectory.TrajEdge;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

/**
 *
 * @author Saqib
 */
public class TrajNormalizer {
    
    public HashMap<String, Trajectory> normalize (HashMap<String, Trajectory> allTrajectories, double minLon, double minLat, double maxLon, double maxLat){
        // iterate over all the entries in the trajectories, maintained as hashmap of key = anonymized id, value = trajectory object
        HashMap<String, Trajectory> normalizedTrajectories = new HashMap<>();
        for (HashMap.Entry<String, Trajectory> entry : allTrajectories.entrySet()) {
            Trajectory trajectory = entry.getValue();
            
            Trajectory normalizedTrajectory = new Trajectory(trajectory.getUserId(), trajectory.getTrajId());
            normalizedTrajectory.setFromSample(trajectory.getFromSample());
            normalizedTrajectory.setProbability(trajectory.getProbability());
            
            ArrayList <TrajEdge> trajEdges = trajectory.getTrajEdges();
            ArrayList <TrajEdge> normTrajEdges = new ArrayList<>();
            // normalize latitude, longitude values of each point of each trajectory 
            for (TrajEdge trajEdge: trajEdges) {
                TrajPoint trajPoint = trajEdge.getStartsFrom();
                Coordinate trajPointLocation = trajPoint.getPointLocation();
                double normLat = (trajPointLocation.x - minLat)*100/(maxLat-minLat);
                double normLon = (trajPointLocation.y - minLon)*100/(maxLon-minLon);
                
                TrajEdge normTrajEdge = new TrajEdge();
                normTrajEdge.setStartsFrom(new TrajPoint(trajPoint.getStoppage().getStopId(), trajPoint.getRouteId(), trajPoint.getVehicleId(),
                                                        trajPoint.getTransportMode(), trajPoint.isTouchOn(), trajPoint.getDatetime(),
                                                        trajPoint.getTimeInSec(), normLat, normLon));
                
                
                if (normLat > 100 || normLat < 0 || normLon > 100 || normLon < 0){
                    System.out.println("normLat, normLon, lat, lon = " + normLat + ", " + normLon + ", " + trajPointLocation.x + ", " + trajPointLocation.y);
                }
                
                
                trajPoint = trajEdge.getEndsAt();
                trajPointLocation = trajPoint.getPointLocation();
                normLat = (trajPointLocation.x - minLat)*100/(maxLat-minLat);
                normLon = (trajPointLocation.y - minLon)*100/(maxLon-minLon);
                
                normTrajEdge.setEndsAt(new TrajPoint(trajPoint.getStoppage().getStopId(), trajPoint.getRouteId(), trajPoint.getVehicleId(),
                                                        trajPoint.getTransportMode(), trajPoint.isTouchOn(), trajPoint.getDatetime(),
                                                        trajPoint.getTimeInSec(), normLat, normLon));
                
                
                if (normLat > 100 || normLat < 0 || normLon > 100 || normLon < 0){
                    System.out.println("normLat, normLon, lat, lon = " + normLat + ", " + normLon + ", " + trajPointLocation.x + ", " + trajPointLocation.y);
                }
                
                
                normTrajEdges.add(normTrajEdge);
            }
            // update the trajPoint list in the trajectory object
            normalizedTrajectory.setTrajEdges(normTrajEdges);
            //System.out.println(normalizedTrajectory);
            normalizedTrajectories.put(normalizedTrajectory.getTrajId(), normalizedTrajectory);
        }
        return normalizedTrajectories;
    }
}
