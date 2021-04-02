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
import javafx.util.Pair;

/**
 *
 * @author Saqib
 */
public class StopNormalizer {
    
    public HashMap<Integer, Pair<Double, Double>> normalize (HashMap<Integer, Pair<Double, Double>> allStops, double minLon, double minLat, double maxLon, double maxLat){
        // iterate over all the entries in the trajectories, maintained as hashmap of key = anonymized id, value = trajectory object
        HashMap<Integer, Pair<Double, Double>> normalizedStops = new HashMap<>();
        for (HashMap.Entry<Integer, Pair<Double, Double>> entry : allStops.entrySet()) {
            Pair<Double, Double> stopLocation = entry.getValue();
            
            double normLat = (stopLocation.getKey() - minLat)*100/(maxLat-minLat);
            double normLon = (stopLocation.getValue() - minLon)*100/(maxLon-minLon);
                
            if (normLat > 100 || normLat < 0 || normLon > 100 || normLon < 0){
                /*
                // exclude them
                System.out.println("normLat, normLon, lat, lon, id = " + normLat + ", " + normLon + ", "
                        + stopLocation.getKey() + ", " + stopLocation.getValue() + ", " + entry.getKey());
                */
            }
            else{
                normalizedStops.put(entry.getKey(), new Pair<Double, Double>(normLat, normLon));
            }
        }
        System.out.println("Normalized stops size = " + normalizedStops.size());
        return normalizedStops;
    }
}
