/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.real;

import com.vividsolutions.jts.geom.Coordinate;
import ds.trajectory.TrajEdge;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.abs;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javafx.util.Pair;

/**
 *
 * @author Saqib
 */
public class TrajProcessor {
    
    private double minLon, maxLon, minLat, maxLat, latCoeff, latConst, lonCoeff, lonConst;
    private final double  latLowerLimit, latUpperLimit, lonLowerLimit, lonUpperLimit;
    private long minTimeInSec, maxTimeInSec;
    
    private HashSet<Integer> excludeUserIds;
    private HashMap<String, Trajectory> trajIdToTrajMap;
    private HashMap<String, Trajectory> trajIdToNormalizedTrajMap;
    private HashMap<Integer, ArrayList<String>> userIdToTrajIdMap;
    private HashMap<Integer, Pair<Double,Double>> stoppageMap;
    private HashMap<Integer, Pair<Double,Double>> normalizedStoppageMap;
    
    public TrajProcessor(){
        excludeUserIds = new HashSet<>();
        trajIdToTrajMap = new HashMap<>();
        trajIdToNormalizedTrajMap = new HashMap<>();
        userIdToTrajIdMap = new HashMap<>();
        stoppageMap = new HashMap<>();
        normalizedStoppageMap = new HashMap<>();
        // the following variables are used in spatial normalization
        minLon = minLat = 1000;
        maxLon = maxLat = -1000;
        // the following variables can be used in spatial denormalization if needed
        latCoeff = 0;
        latConst = 0;
        lonCoeff = 0;
        lonConst = 0;
        // the following limits are used to remove spatially noisy data (if any)
        latLowerLimit = -1e9;
        latUpperLimit = 1e9;
        lonLowerLimit = -1e9;
        lonUpperLimit = 1e9;
        // there may be some temporal noise which we do not know about, if there is, it should be cleaned as well
        minTimeInSec = (long) 1e18;
        maxTimeInSec = -1;
    }
    
    public void loadStoppageData(String path) throws FileNotFoundException, IOException{
        System.out.println("Loading stoppages from file...");
        File stoppageFile = new File(path);
        if (stoppageFile == null){
            System.out.println("Stoppage file at " + path + " not found");
            System.exit(0);
        }
        
        BufferedReader br = null;
        String line = new String();

        br = new BufferedReader(new FileReader(path));
        
        while ((line = br.readLine()) != null) {
            //System.out.println(line);
            String[] data = line.split("\\|");
            //for (String splittedData : data) System.out.println(splittedData);
            int stopId = Integer.parseInt(data[0]);
            double lat = Double.parseDouble(data[9]);
            double lon = Double.parseDouble(data[10]);
            
            if (stoppageMap.containsKey(stopId)){
                if (abs(stoppageMap.get(stopId).getKey()-lat)<1e-6 && abs(stoppageMap.get(stopId).getValue()-lon)<1e-6){
                    System.out.println("OK!! Already found this stoppage with id = " + stopId + " and lat-lon matched");
                }
                else{
                    System.out.println("Warning!! Already found this stoppage with id = " + stopId + " but lat-lon mismatched");
                }
            }
            else{
                stoppageMap.put(stopId, new Pair<>(lat,lon));
            }
        }
        System.out.println(stoppageMap.size() + " stoppages processed");
    }
    
    
    public void loadTrajectories(String path) throws FileNotFoundException, IOException, ParseException{
        File userTrajectoryFile = new File(path);
        if (userTrajectoryFile == null){
            System.out.println("User trajectory file at " + path + " not found");
            System.exit(0);
        }
        
        BufferedReader br = null;
        String line = new String();

        br = new BufferedReader(new FileReader(path));
        // ignore the first line that contains the headers
        br.readLine();
        
        System.out.println("Loading trajectories from file ...");
        
        // used to assign trajectory id
        int trajCount = 0;
        
        int userCount = 0;
        int currentTrajUserId = -1;
        TrajEdge trajEdge = new TrajEdge();
        Trajectory trajectory = new Trajectory();
        
        // parse each line and add its point (location, time) to the appropriate trajectory
        while ((line = br.readLine()) != null/* && trajCount < 40*/) {
            // different fields in each line are extracted and quotes are trimmed
            String[] data = line.split(";");
            int userId = Integer.parseInt(data[0]);
            
            if (userId == -1){
                trajectory.setUserId(currentTrajUserId);
                trajectory.setTrajId(Integer.toString(trajCount));
                userIdToTrajIdMap.get(currentTrajUserId).add(trajectory.getTrajId());
                trajIdToTrajMap.put(trajectory.getTrajId(), trajectory);
                trajCount++;
                trajectory = new Trajectory();
                continue;
            }
            String datetime = data[1];
            int transportMode = Integer.parseInt(data[2]);
            Boolean touchOn = data[3].equals("on")?true :false;
            int routeId = Integer.parseInt(data[4]);
            int fromSample = Integer.parseInt(data[5]);
            int stopId = Integer.parseInt(data[6]);
            int vehicleId = Integer.parseInt(data[7]);
            
            // when a new cardid i.e. userId is encountered the first time, a new entry in the userIdToTrajIdMap is generated
            if (!userIdToTrajIdMap.containsKey(userId)){
                userIdToTrajIdMap.put(userId, new ArrayList<>());
                currentTrajUserId = userId;
                userCount++;
            }
            
            if (datetime.charAt(9)=='0' || datetime.charAt(9)=='3') excludeUserIds.add(currentTrajUserId);
            
            // calculate timestamp (timeInSec) with simple date format, its parse and getTime methods and converting obtained ms value to seconds
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long timeInSec = dateTimeFormat.parse(datetime).getTime()/1000;
            double lat, lon;
            lat = lon = -1;
            if (stoppageMap.containsKey(stopId)){
                lat = stoppageMap.get(stopId).getKey();
                lon = stoppageMap.get(stopId).getValue();
                //System.out.println("(Stopid, lat, lon) = (" + stopId + "," + lat + "," + lon + ")");
                // remove spatial noise (outside our desired geographical zone)
                Coordinate trajPointCoord = new Coordinate(lat, lon);
                if (trajPointCoord.x < latLowerLimit || trajPointCoord.x > latUpperLimit){
                    excludeUserIds.add(userId);
                }
                else if (trajPointCoord.y < lonLowerLimit || trajPointCoord.y > lonUpperLimit){
                    excludeUserIds.add(userId);
                }

                // update the boundary values for normalization
                if (trajPointCoord.x < minLat){
                    minLat = trajPointCoord.x;
                }
                else if (trajPointCoord.x > maxLat){
                    maxLat = trajPointCoord.x;
                }
                if (trajPointCoord.y < minLon){
                    minLon = trajPointCoord.y;
                }
                else if (trajPointCoord.y > maxLon){
                    maxLon = trajPointCoord.y;
                }

                if (timeInSec < minTimeInSec){
                    minTimeInSec = timeInSec;
                }
                else if (timeInSec > maxTimeInSec){
                    maxTimeInSec = timeInSec;
                }
            }
            else{
                //System.out.println("Stop id " + stopId + " of trajectory " + trajCount + " of user " + userId + " not found");
                excludeUserIds.add(userId);
            }
            TrajPoint trajPoint = new TrajPoint(stopId, routeId, vehicleId, transportMode, touchOn, datetime, timeInSec, lat, lon);
            if (touchOn){
                trajEdge.setStartsFrom(trajPoint);
            }
            else{
                trajEdge.setEndsAt(trajPoint);
                trajectory.getTrajEdges().add(trajEdge);
                trajEdge = new TrajEdge();
            }           
            // remove spatial noise (outside our desired geographical zone)
            /*
            if (trajPointCoord.x < latLowerLimit || trajPointCoord.x > latUpperLimit) continue;
            if (trajPointCoord.y < lonLowerLimit || trajPointCoord.y > lonUpperLimit) continue;
            
            // update the boundary values for normalization
            if (trajPointCoord.x < minLat){
                minLat = trajPointCoord.x;
            }
            else if (trajPointCoord.x > maxLat){
                maxLat = trajPointCoord.x;
            }
            if (trajPointCoord.y < minLon){
                minLon = trajPointCoord.y;
            }
            else if (trajPointCoord.y > maxLon){
                maxLon = trajPointCoord.y;
            }
            
            if (timeInSec < minTimeInSec){
                minTimeInSec = timeInSec;
            }
            else if (timeInSec > maxTimeInSec){
                maxTimeInSec = timeInSec;
            }
            
            // a point containing location and time is constructed from the recently processed values and inserted into the trajectory
            TrajPoint trajPoint = new TrajPoint(trajPointCoord, timeInSec);
            allTrajectories.get(anonymizedId).addTrajPoint(trajPoint);
            */
        }
        br.close();
        
        // 1% increase in spatio-temporal boundaries
        maxLat += (maxLat-minLat)/100;
        minLat -= (maxLat-minLat)/100;
        maxLon += (maxLon-minLon)/100;
        minLon -= (maxLon-minLon)/100;
        maxTimeInSec += (maxTimeInSec-minTimeInSec)/100;
        minTimeInSec -= (maxTimeInSec-minTimeInSec)/100;
    }
    
    public void normalizeTrajectories(){
        // location values are normalized in [0, 100] range
        TrajNormalizer trajNormalizer = new TrajNormalizer();
        trajIdToNormalizedTrajMap = trajNormalizer.normalize(trajIdToTrajMap, minLon, minLat, maxLon, maxLat);
        
        // the denormalizing variables are updated
        latCoeff = (maxLat/100.0-minLat/100.0);
        latConst = minLat;
        lonCoeff = (maxLon/100.0-minLon/100.0);
        lonConst = minLon;
    }
    
    public void normalizeStops(){
        StopNormalizer stopNormalizer = new StopNormalizer();
        normalizedStoppageMap = stopNormalizer.normalize(stoppageMap, minLon, minLat, maxLon, maxLat);
    }
    
    public void excludeWeekendUserIds(){
        int excludedTrajCount = 0;
        int excludedUserCount = 0;
        for (Integer excludeUserId : excludeUserIds){
            for (String trajId : userIdToTrajIdMap.get(excludeUserId)){
                trajIdToTrajMap.remove(trajId);
                excludedTrajCount++;
            }
            userIdToTrajIdMap.remove(excludeUserId);
            excludedUserCount++;
        }
        System.out.println("Excluded users = " + excludedUserCount + " , excluded trajs = " + excludedTrajCount);
    }
    
    public void printTrajs(int count){
        System.out.println("No. of Users = " + userIdToTrajIdMap.size());
        for (Map.Entry<Integer, ArrayList<String>> entry : userIdToTrajIdMap.entrySet()){
            System.out.println("User " + entry.getKey() + " has " + entry.getValue().size() + " trajectories:");
            for (String trajId : entry.getValue()){
                System.out.println(trajIdToTrajMap.get(trajId));
                count--;
                if (count==0) return;
            }
        }
    }
    
    public void printNormalizedTrajs(int count){
        System.out.println("After normalization");
        System.out.println("No. of Users = " + userIdToTrajIdMap.size());
        for (Map.Entry<Integer, ArrayList<String>> entry : userIdToTrajIdMap.entrySet()){
            System.out.println("User " + entry.getKey() + " has " + entry.getValue().size() + " trajectories:");
            for (String trajId : entry.getValue()){
                System.out.println(trajIdToNormalizedTrajMap.get(trajId));
                count--;
                if (count==0) return;
            }
        }
    }
    
    public void printInfo(){
        System.out.println("minLat = " + minLat);
        System.out.println("maxLat = " + maxLat);
        System.out.println("minLon = " + minLon);
        System.out.println("maxLon = " + maxLon);
        System.out.println("latCoeff = " + latCoeff);
        System.out.println("latConst = " + latConst);
        System.out.println("lonCoeff = " + lonCoeff);
        System.out.println("lonConst = " + lonConst);
        System.out.println("latLowerLimit = " + latLowerLimit);
        System.out.println("latUpperLimit = " + latUpperLimit);
        System.out.println("lonLowerLimit = " + lonLowerLimit);
        System.out.println("lonUpperLimit = " + lonUpperLimit);
        System.out.println("minTimeInSec = " + minTimeInSec);
        System.out.println("maxTimeInSec = " + maxTimeInSec);
    }
    
    public void printSummary(){
        System.out.println("Users = " + userIdToTrajIdMap.size());
        int[] trajCountWiseUserCount = new int[100];
        Arrays.fill(trajCountWiseUserCount, 0);
        for (Map.Entry<Integer, ArrayList<String>> entry : userIdToTrajIdMap.entrySet()){
            int trajCount = entry.getValue().size();
            trajCountWiseUserCount[trajCount]++;
        }
        for (int i=0; i<trajCountWiseUserCount.length; i++){
            if (trajCountWiseUserCount[i] == 0) continue;
            System.out.println(i + " Trajs : " + trajCountWiseUserCount[i] + " Users");
        }
    }
    
    public HashMap<String, Trajectory> getTrajIdToTrajMap() {
        return trajIdToTrajMap;
    }

    public HashMap<String, Trajectory> getTrajIdToNormalizedTrajMap() {
        return trajIdToNormalizedTrajMap;
    }
        
    public double getLatCoeff() {
        return latCoeff;
    }

    public double getLatConst() {
        return latConst;
    }

    public double getLonCoeff() {
        return lonCoeff;
    }

    public double getLonConst() {
        return lonConst;
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public long getMinTimeInSec() {
        return minTimeInSec;
    }

    public long getMaxTimeInSec() {
        return maxTimeInSec;
    }

    public HashMap<Integer, Pair<Double, Double>> getStoppageMap() {
        return stoppageMap;
    }

    public HashMap<Integer, Pair<Double, Double>> getNormalizedStoppageMap() {
        return normalizedStoppageMap;
    }
    
}
