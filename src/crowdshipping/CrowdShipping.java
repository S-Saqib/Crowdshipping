/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crowdshipping;

import java.io.IOException;

import db.TrajStorage;

import ds.qtrajtree.TQIndex;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import io.real.TrajProcessor;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import javafx.util.Pair;
import query.PacketDeliveryQuery;
import query.PacketRequest;
import query.service.DistanceConverter;

import query.service.TestServiceQuery;


public class CrowdShipping {

    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {

        /*
        String trajFilePath = "../Data/Myki/2018_June_Last_Week_Trips_All_Days.txt";
        String stopFile1Path = "../Data/Myki/my_stop_locations.txt";
        String stopFile2Path = "../Data/Myki/stop_locations.txt";
        */
        
        String routeFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Facility\\NYC_transport\\NYC_routes.txt";
        String stoppageFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Facility\\NYC_transport\\NYC_stopid.txt";
        String userTrajectoryFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Taxi\\user_traj_for_crowdshipping.csv";
        
        if (routeFilePath == null || stoppageFilePath == null || userTrajectoryFilePath == null){
            System.out.println("File not found");
            System.exit(0);
        }
        
        /// Param : Keeper Percentage : 10, 25, 50 (default), 100
        int keeperPercentage = 50;
        
        TrajProcessor trajProcessor = new TrajProcessor();
        //System.out.println(System.getProperty("user.dir"));
        
        //trajProcessor.loadStoppageData(stopFile1Path);
        //trajProcessor.loadStoppageData(stopFile2Path);
        //trajProcessor.loadTrajectories(trajFilePath);
        
        trajProcessor.loadNYCStoppageData(stoppageFilePath);
        trajProcessor.loadNYCTrajectories(userTrajectoryFilePath);
        
        //trajProcessor.printTrajs();
        trajProcessor.printSummary();
        //trajProcessor.excludeWeekendUserIds();
        
        //trajProcessor.useNTrajsAsDataSet(50);     // for testing rtree leaves traversal order
        /// Param : Traj Count 100k, 250k, 400k, 550k (default)
        int trajCount = 1000000;
        trajProcessor.useNTrajsAsDataSet(trajCount);
        
        trajProcessor.normalizeTrajectories();
        trajProcessor.printSummary();
        //trajProcessor.printTrajs(5);
        //trajProcessor.printNormalizedTrajs(5);
        trajProcessor.printInfo();
        // create an object of TrajStorage to imitate database functionalities
        TrajStorage trajStorage = new TrajStorage(trajProcessor.getTrajIdToNormalizedTrajMap(),trajProcessor.getTrajIdToTrajMap());
        
            // build index on the trajectory data (assuming we have all of it in memory)
            int timeWindowInSec = 15*60;
            long from = System.nanoTime();
            TQIndex quadTrajTree = new TQIndex(trajStorage, trajProcessor.getLatCoeff(), trajProcessor.getLatConst(),
                                                trajProcessor.getLonCoeff(), trajProcessor.getLonConst(), 
                                                trajProcessor.getMaxLat(), trajProcessor.getMaxLon(), trajProcessor.getMinLat(), trajProcessor.getMinLon(),
                                                trajProcessor.getMinTimeInSec(), timeWindowInSec);
            System.out.println("TQ-tree construction time = " + (System.nanoTime()-from)/1.0e9 + " sec");
            //System.exit(0);
            //Statistics stats = new Statistics(quadTrajTree);
            //stats.printStats();
            //System.out.println(userTrajectories.size());
            /*
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(100);
            System.out.println("Summary index (100 pt /leaf) construction time = " + (System.nanoTime()-from)/1.0e9);
            //quadTrajTree.printSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(200);
            System.out.println("Summary index (200 pt /leaf) construction time = " + (System.nanoTime()-from)/1.0e9);
            //quadTrajTree.printSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(500);
            System.out.println("Summary index (500 pt /leaf) construction time = " + (System.nanoTime()-from)/1.0e9);
            //quadTrajTree.printSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            */
            
            /*
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(2000);
            System.out.println("Summary index (2000 pt /leaf) construction time = " + (System.nanoTime()-from)/1.0e9);
            //quadTrajTree.printSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(5000);
            System.out.println("Summary index (5000 pt /leaf) construction time = " + (System.nanoTime()-from)/1.0e9);
            //quadTrajTree.printSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            */
            //quadTrajTree = null;
            //quadTrajTree.getAllInterNodeTrajsId(quadTrajTree.getQuadTree().getRootNode());
        /*
        }
        //System.out.println(indexTime/numberOfRuns);
        try {
            //System.out.println(userTrajectories.size());
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        trajProcessor.normalizeStops();
        
        quadTrajTree.indexStoppages(trajProcessor.getNormalizedStoppageMap());
        
        trajProcessor.useNPercentStopsAsKeepers(keeperPercentage);
        
        // the following proximity, distance etc. are calculated in normalized lat, lon space
        double spatialProximity;
        // need to consider keepers
        /// Param : Max Detour in KM 500, 1000 (default), 2000, 4000
        double detourDistanceThreshold = 1000; // in meters
        spatialProximity = detourDistanceThreshold;
        ///
        String proximityUnit = "m"; // it can be "m", "km", "mile" and "ft"
        DistanceConverter distanceConverter = new DistanceConverter(trajProcessor.getMaxLon(), trajProcessor.getMaxLat(), trajProcessor.getMinLon(), trajProcessor.getMinLat());
        double latProximity = distanceConverter.getLatProximity(spatialProximity, proximityUnit);
        double lonProximity = distanceConverter.getLonProximity(spatialProximity, proximityUnit);
        
        System.out.println(latProximity + " and " + lonProximity);
        /*
        // merge stops in 0.01m spatial range into a single trajgraph node
        double latClusterRange = 0.01;
        double lonClusterRange = 0.01;
        double clusterRangeInMeters = 250;
        System.out.println("lat cluster range " + latClusterRange + " = " + spatialProximity/latProximity*latClusterRange + " m");
        System.out.println("lon cluster range " + lonClusterRange + " = " + spatialProximity/lonProximity*lonClusterRange + " m");
        
        latClusterRange = latProximity/spatialProximity*clusterRangeInMeters;
        lonClusterRange = lonProximity/spatialProximity*clusterRangeInMeters;
        System.out.println("cluster range in meter " + clusterRangeInMeters + " = " + latClusterRange + " lat cluster range, " + lonClusterRange + " lon cluster range");
        */
        long temporalProximity = 15;    // in minutes, may be anything around 5 to 240 for example
        temporalProximity *= 60;        // in seconds
        
        
        from = System.nanoTime();
        // in the following function, keeper join is done
        quadTrajTree.buildSummaryIndex(1000, trajProcessor.getNormalizedKeeperMap(), latProximity, lonProximity);
        System.out.println("Summary index (1000 pt/leaf) construction time = " + (System.nanoTime()-from)/1.0e9 + " sec");
        //quadTrajTree.printSummaryIndex();
        //quadTrajTree.printRevSummaryIndex();
        quadTrajTree.printSummaryIndexSummary();
        System.out.println("Reverse...");
        quadTrajTree.printRevSummaryIndexSummary();
        //System.exit(0);
        
        double clusterRangeInMeters = 100;
        double latClusterRange = distanceConverter.getLatProximity(clusterRangeInMeters, proximityUnit);
        double lonClusterRange = distanceConverter.getLonProximity(clusterRangeInMeters, proximityUnit);
        // note that this cluster range along latitude and longitude is in normalized space
        
        // stats of stops
        /*
        HashMap<Integer, Integer> distanceWiseStopCount = new HashMap<>();
        for (HashMap.Entry<Integer, Pair<Double,Double>> fromEntry : trajProcessor.getStoppageMap().entrySet()){
            for (HashMap.Entry<Integer, Pair<Double,Double>> toEntry : trajProcessor.getStoppageMap().entrySet()){
                if (fromEntry.getKey() == toEntry.getKey()) continue;
                double absDistance = distanceConverter.absDistance(fromEntry.getValue().getKey(), toEntry.getValue().getKey(),
                                                        fromEntry.getValue().getValue(), toEntry.getValue().getValue(), proximityUnit);
                //System.out.println(fromEntry.getKey() + "\t" + toEntry.getKey() + "\t" + absDistance);
                int distKey = (int)(absDistance/clusterRangeInMeters);
                if (!distanceWiseStopCount.containsKey(distKey)){
                    distanceWiseStopCount.put(distKey, 0);
                }
                distanceWiseStopCount.put(distKey, distanceWiseStopCount.get(distKey)+1);
            }
        }
        
        System.out.println("Distance wise (m) stats of stops...");
        for (HashMap.Entry<Integer, Integer> entry : distanceWiseStopCount.entrySet()){
            System.out.println(entry.getKey()*clusterRangeInMeters + "\t" + (entry.getKey()+1)*clusterRangeInMeters + "\t" + entry.getValue());
        }
        */
        /*
        // stats of trajs
        HashMap<Integer, Integer> fromToTrajPointDis = new HashMap<>();
        HashMap<Integer, Integer> toFromTrajPointDis = new HashMap<>();
        for (Trajectory trajectory : trajProcessor.getTrajIdToTrajMap().values()){
            TrajPoint prevPoint = null;
            for (TrajPoint trajPoint : trajectory.getPointList()){
                if (prevPoint != null){
                    double absDistance = distanceConverter.absDistance(prevPoint.getPointLocation().x, trajPoint.getPointLocation().x,
                                                            prevPoint.getPointLocation().y, trajPoint.getPointLocation().y, proximityUnit);
                    int distKey = (int)(absDistance/clusterRangeInMeters);
                    boolean fromTo = true;
                    if (prevPoint.isTouchOn() && trajPoint.isTouchOn()){
                        System.out.println("on-on : shouldn't come here");
                        continue;
                    }
                    if (!prevPoint.isTouchOn() && !trajPoint.isTouchOn()){
                        System.out.println("off-off : shouldn't come here");
                        continue;
                    }
                    if (!prevPoint.isTouchOn() && trajPoint.isTouchOn()){
                        // off-on
                        fromTo = false;
                    }
                    // else on-off => fromTo = true, as already set
                    if (fromTo){
                        if (!fromToTrajPointDis.containsKey(distKey)){
                            fromToTrajPointDis.put(distKey, 0);
                        }
                        fromToTrajPointDis.put(distKey, fromToTrajPointDis.get(distKey)+1);
                    }
                    else{
                        if (!toFromTrajPointDis.containsKey(distKey)){
                            toFromTrajPointDis.put(distKey, 0);
                        }
                        toFromTrajPointDis.put(distKey, toFromTrajPointDis.get(distKey)+1);
                    }
                }
                prevPoint = trajPoint;
            }
        }
        
        System.out.println("Distance wise (m) consecutive trajpoint pairs...");
        System.out.println("Along an edge i.e. on-off");
        for (HashMap.Entry<Integer, Integer> entry : fromToTrajPointDis.entrySet()){
            System.out.println(entry.getKey()*clusterRangeInMeters + "\t" + (entry.getKey()+1)*clusterRangeInMeters + "\t" + entry.getValue());
        }
        System.out.println("One edge to next edge i.e. off-on");
        for (HashMap.Entry<Integer, Integer> entry : toFromTrajPointDis.entrySet()){
            System.out.println(entry.getKey()*clusterRangeInMeters + "\t" + (entry.getKey()+1)*clusterRangeInMeters + "\t" + entry.getValue());
        }
        */
        
        /*
        Set <Integer> stopIds = trajProcessor.getStoppageMap().keySet();
        int amongStops = 0;
        int outOfStops = 0;
        for (Trajectory trajectory : trajProcessor.getTrajIdToTrajMap().values()){
            for (TrajPoint trajPoint : trajectory.getPointList()){
                for (Pair<Double,Double> stopCoords : trajProcessor.getStoppageMap().values()){
                    
                }
                if (stopIds.contains(trajPoint.getStoppage().getStopId())){
                    amongStops++;
                }
                else outOfStops++;
            }
        }
        System.out.println("# of Trajpoints among stoppages = " + amongStops + ", out of stoppages = " + outOfStops);
        */
        
        // stopwise and timewise trajectory point distribution
        int trajNotMappedToStopCount = 0;
                
        HashMap <Integer, Integer> stopWiseTrajs = new HashMap<>();
        HashMap <Integer, Integer> timeHourWiseTrajs = new HashMap<>();
        HashMap <Integer, Integer> timeMinuteWiseTrajs = new HashMap<>();
        for (Trajectory trajectory : trajProcessor.getTrajIdToTrajMap().values()){
            TrajPoint trajStartPoint = trajectory.getPointList().first();
            TrajPoint trajEndPoint = trajectory.getPointList().last();
            double lat = trajStartPoint.getStoppage().getStopLocation().x;
            double lon = trajStartPoint.getStoppage().getStopLocation().y;
            double xMin = lat - latClusterRange/2;
            double xMax = lat + latClusterRange/2;
            double yMin = lon - lonClusterRange/2;
            double yMax = lon + lonClusterRange/2;
            //System.out.println("startLat, startLon = " + lat + ", " + lon);
            int startStopId = quadTrajTree.getStoppageQuadTree().getNearestPointTrajId(xMin, yMin, xMax, yMax, lat, lon);
            
            lat = trajEndPoint.getStoppage().getStopLocation().x;
            lon = trajEndPoint.getStoppage().getStopLocation().y;
            xMin = lat - latClusterRange/2;
            xMax = lat + latClusterRange/2;
            yMin = lon - lonClusterRange/2;
            yMax = lon + lonClusterRange/2;
            //System.out.println("endLat, endLon = " + lat + ", " + lon);
            int endStopId = quadTrajTree.getStoppageQuadTree().getNearestPointTrajId(xMin, yMin, xMax, yMax, lat, lon);
            
            if (startStopId == -1 || endStopId == -1){
                trajNotMappedToStopCount++;
                continue;
            }
            trajStartPoint.getStoppage().setStopId(startStopId);
            trajEndPoint.getStoppage().setStopId(endStopId);
            
            if (!stopWiseTrajs.containsKey(startStopId)){
                stopWiseTrajs.put(startStopId, 0);
            }
            stopWiseTrajs.put(startStopId, stopWiseTrajs.get(startStopId)+1);
            
            if (!stopWiseTrajs.containsKey(endStopId)){
                stopWiseTrajs.put(endStopId, 0);
            }
            stopWiseTrajs.put(endStopId, stopWiseTrajs.get(endStopId)+1);
            
            int timeHourId = (int)(trajStartPoint.getTimeInSec() - trajProcessor.getMinTimeInSec())/3600;
            if (!timeHourWiseTrajs.containsKey(timeHourId)){
                timeHourWiseTrajs.put(timeHourId, 0);
            }
            timeHourWiseTrajs.put(timeHourId, timeHourWiseTrajs.get(timeHourId)+1);

            int timeMinuteId = (int)(trajStartPoint.getTimeInSec() - trajProcessor.getMinTimeInSec())/60;
            if (!timeMinuteWiseTrajs.containsKey(timeMinuteId)){
                timeMinuteWiseTrajs.put(timeMinuteId, 0);
            }
            timeMinuteWiseTrajs.put(timeMinuteId, timeMinuteWiseTrajs.get(timeMinuteId)+1);
            
            timeHourId = (int)(trajEndPoint.getTimeInSec() - trajProcessor.getMinTimeInSec())/3600;
            if (!timeHourWiseTrajs.containsKey(timeHourId)){
                timeHourWiseTrajs.put(timeHourId, 0);
            }
            timeHourWiseTrajs.put(timeHourId, timeHourWiseTrajs.get(timeHourId)+1);

            timeMinuteId = (int)(trajEndPoint.getTimeInSec() - trajProcessor.getMinTimeInSec())/60;
            if (!timeMinuteWiseTrajs.containsKey(timeMinuteId)){
                timeMinuteWiseTrajs.put(timeMinuteId, 0);
            }
            timeMinuteWiseTrajs.put(timeMinuteId, timeMinuteWiseTrajs.get(timeMinuteId)+1);

        }
        
        System.out.println("Trajectories not mapped to stops (one or both points) = " + trajNotMappedToStopCount);
        
        System.out.println("# of Stops having traj points = " + stopWiseTrajs.size());
        System.out.println("Stop Id\tCount");
        for (HashMap.Entry<Integer, Integer> entry : stopWiseTrajs.entrySet()){
            int stopId = entry.getKey();
            int count = entry.getValue();
            System.out.println(stopId + "\t" + count);
        }
        
        System.out.println("# of Times (hours) having traj points = " + timeHourWiseTrajs.size());
        System.out.println("Hour Id\tCount");
        for (HashMap.Entry<Integer, Integer> entry : timeHourWiseTrajs.entrySet()){
            int timeHourId = entry.getKey();
            int count = entry.getValue();
            System.out.println(timeHourId + "\t" + count);
        }
        
        System.out.println("# of Times (minutes) having traj points = " + timeMinuteWiseTrajs.size());
        System.out.println("Minute Id\tCount");
        for (HashMap.Entry<Integer, Integer> entry : timeMinuteWiseTrajs.entrySet()){
            int timeMinuteId = entry.getKey();
            int count = entry.getValue();
            System.out.println(timeMinuteId + "\t" + count);
        }
        
        System.exit(0);
        /*
        HashMap <Integer, HashMap<Integer, Integer>> stopWiseHourlyTrajs = new HashMap<>();
        for (Trajectory trajectory : trajProcessor.getTrajIdToTrajMap().values()){
            for (TrajPoint trajPoint : trajectory.getPointList()){
                int stopId = trajPoint.getStoppage().getStopId();
                if (!stopWiseHourlyTrajs.containsKey(stopId)){
                    stopWiseHourlyTrajs.put(stopId, new HashMap<>());
                }
                int timeHourId = (int)(trajPoint.getTimeInSec() - trajProcessor.getMinTimeInSec())/3600;
                if (!stopWiseHourlyTrajs.get(stopId).containsKey(timeHourId)){
                    stopWiseHourlyTrajs.get(stopId).put(timeHourId, 0);
                }
                stopWiseHourlyTrajs.get(stopId).put(timeHourId, stopWiseHourlyTrajs.get(stopId).get(timeHourId)+1);
            }
        }
        System.out.println("# of (Stops, Hour) having traj points = " + stopWiseHourlyTrajs.size());
        System.out.println("Stop Id\tHour Id\tCount");
        for (HashMap.Entry<Integer, HashMap<Integer, Integer>> entry : stopWiseHourlyTrajs.entrySet()){
            int stopId = entry.getKey();
            for (HashMap.Entry<Integer, Integer> entry2 : entry.getValue().entrySet()){
                int hourId = entry2.getKey();
                int count = entry2.getValue();
                System.out.println(stopId + "\t" + hourId + "\t" + count);
            }
        }
        System.exit(0);
        */
        // the following two variables are perhaps no longer required
        double pktDisByDetourCoeff = 2;
        double pktReqMinDisThreshold = detourDistanceThreshold*pktDisByDetourCoeff;
        
        // query can be generated from anywhere (i.e. any stop) not necessarily from a keeper
        // keeper is a subset of stoppages
        PacketDeliveryQuery packetDeliveryQuery = new PacketDeliveryQuery(trajProcessor.getStoppageMap(), trajProcessor.getNormalizedStoppageMap(), distanceConverter,
                                                                        pktReqMinDisThreshold, proximityUnit, trajProcessor.getMinTimeInSec(), trajProcessor.getMaxTimeInSec());
        //String pktSrcDestFilePath = "E:\\Education\\Academic\\BUET\\Educational\\MSc_BUET\\Thesis\\Experiments_Insights\\Src_Dest_BL_Successful.txt";
        
        /*
        System.out.print("\nSrc\tDest\tDis(" + proximityUnit + ")\tCost (file)\tisDelivered\tCost (hop)\tCost (dis)\tA* Cost (dis)\t"
                        + "isDelivered_joined\tCost_joined (hop)\tCost_joined (dis)\tA* Cost_joined (dis)");
        */
        
                        //+ "isDelivered_all-traj\tAll-traj Cost (dis)\tAll-traj Time (sec)\tAll-traj Trajs(I/O)\tisDelivered_joined_AT_hop\tAT_Cost_Hop_joined\t");
        int noOfPktsForDelivery = 1000;
        // generates some random src, dest ids and puts them in lists
        // packetDeliveryQuery.populateRandomSrcDestIds(noOfPktsForDelivery);
        
        // distance based packet generation
        int bucketId = 2;   // 0 : <= 2.5km, 1 : <= 5km, 2: <= 10km, 3: <= 20km, 4: <= 40km, 5: > 40km
        //packetDeliveryQuery.groupDistanceWiseSrcDest();
        //packetDeliveryQuery.populateRandomBucketedPackets(noOfPktsForDelivery, bucketId);
        //packetDeliveryQuery.populateCertainDistanceSrcDestIds(noOfPktsForDelivery, bucketId);
        // time info in packet (experiment with it
        /// Param : Pkt Duration (Hour) 0: 0.5-1, 1: 1-2, 2: 2-4 (default), 3: 4-8, 4: 8-24, 5: 24-72
        int timeBucketId = 4;    // 0: <= 30 min to 1 hour, 1: 1 to 2 hours, 2: 2 to 4 hours, 3: 4 to 8 hours, 4: 8 hours to 1 day, 5: 1 to 3 days
        
        // dataset and time range i.e. hour/minute specific processing
        TreeMap<Integer, ArrayList<Integer>> timeHourIdWiseStops = new TreeMap<>();
        TreeMap<Integer, ArrayList<Integer>> stopWiseHourId = new TreeMap<>();
        int maxHourId = Integer.MIN_VALUE;
        String pktSrcDestFilePath = "E:\\Education\\Academic\\BUET\\Educational\\MSc_BUET\\Thesis\\Experiments_Insights\\550k_Hotspots_TS_25%.txt";
        BufferedReader br = null;
        String line = new String();
        br = new BufferedReader(new FileReader(pktSrcDestFilePath));
        while ((line = br.readLine()) != null) {
            String[] data = line.split("\t");
            int stopId = Integer.parseInt(data[0]);
            int timeHourId = Integer.parseInt(data[1]);
            if (!timeHourIdWiseStops.containsKey(timeHourId)){
                timeHourIdWiseStops.put(timeHourId, new ArrayList<>());
            }
            timeHourIdWiseStops.get(timeHourId).add(stopId);
            if (!stopWiseHourId.containsKey(stopId)){
                stopWiseHourId.put(stopId, new ArrayList<>());
            }
            stopWiseHourId.get(stopId).add(timeHourId);
            maxHourId = Math.max(maxHourId, timeHourId);
        }
        //System.out.println("\nHotspot : " + timeHourIdWiseStops.size() + " time buckets (hour), " + stopWiseHourId.size() + " stops");
        // Random packet generation
        // packetDeliveryQuery.populateCertainDistTimeSrcDestIds(noOfPktsForDelivery, bucketId, timeBucketId);
        // Hotspot based packet generation
        packetDeliveryQuery.populatePktsFromHotspot(noOfPktsForDelivery, bucketId, timeBucketId, timeHourIdWiseStops, maxHourId);
        
        /*
        System.out.print("\nSrc\tDest\tDis(" + proximityUnit + ")\tSrc time(sec)\tDest time(sec)\tDuration(sec)\tisDelivered\tA* Cost (dis)\tA* Time (sec)\tA* Trajs I/O\tA* Duration(sec)\t"
                    + "isDelivered_joined\tA* Cost_joined (dis)\tA* Time_joined (sec)\tA* Trajs_joined I/O\tA* Duration_joined(sec)\tisDelivered_joined_A*_hop\tA* Cost Hop_joined\t"
                    + "isDelivered_baseline\tBaseline Cost (dis)\tBaseline Time (sec)\tBaseline I/O\tBaseline Duration_joined(sec)\tisDelivered_joined_BL_hop\tBL_Cost_Hop_joined\t");
        */
        
        System.out.println("\nParameters:");
        //System.out.print("Traj_Count\t" + trajCount + "\tKeeper %\t" + keeperPercentage + "\tPkt_Duration(H)_Id\t" + timeBucketId + "\tMax_Detour(m)\t" + detourDistanceThreshold);
        System.out.print("Traj_Count\t" + trajCount + "\tKeeper %\t" + keeperPercentage + "\tPkt_Dis_Id(km)" + bucketId + "\tPkt_Duration(H)_Id\t" + timeBucketId + "\tMax_Detour(m)\t" + detourDistanceThreshold);
        System.out.print("\nSrc\tDest\tDis(" + proximityUnit + ")\tSrc time(sec)\tDest time(sec)\tDuration(sec)\tDuration(H:M)\t"
                    + "SQ_S Delivered\tSQ_S Cost(dis)\tSQ_S Duration(sec)\tSQ_S Runtime(sec)\tSQ_S I/O\t"
                    + "BL_S Delivered\tBL_S Cost(dis)\tBL_S Duration(sec)\tBL_S Runtime(sec)\tBL_S I/O\t"
                    + "SQ_ST Delivered\tSQ_ST Cost(dis)\tSQ_ST Duration(sec)\tSQ_ST Runtime(sec)\tSQ_ST I/O\t"
                    + "BL_ST Delivered\tBL_ST Cost(dis)\tBL_ST Duration(sec)\tBL_ST Runtime(sec)\tBL_ST I/O\t"
                    + "SQ_ST_t-QG Delivered\tSQ_ST_t-QG Cost(dis)\tSQ_ST_t-QG Duration(sec)\tSQ_ST_t-QG Runtime(sec)\tSQ_ST_t-QG I/O\t"
                    + "BL_ST_t-QG Delivered\tBL_ST_t-QG Cost(dis)\tBL_ST_t-QG Duration(sec)\tBL_ST_t-QG Runtime(sec)\tBL_ST_t-QG I/O\t");
        
        // Experiment
        for (int i=0; i<noOfPktsForDelivery; i++){
            // checks each stop probabilistically
            // packetDeliveryQuery.generatePktDeliveryReq();
            
            // picks from the lists of random src, dest stop ids
            //packetDeliveryQuery.generatePktDeliveryReq(i);
            // hotspot based packet request generation
            packetDeliveryQuery.generateSTHotspotPktDeliveryReq(i);
            PacketRequest pktReq = packetDeliveryQuery.getPacketRequest();
            System.out.print("\n" + packetDeliveryQuery.getPacketRequest().getSrcId() + "\t" + packetDeliveryQuery.getPacketRequest().getDestId() + "\t" +
                packetDeliveryQuery.getDistance() + "\t" + packetDeliveryQuery.getPacketRequest().getSrcTimeInSec() + "\t" + packetDeliveryQuery.getPacketRequest().getDestTimeInSec()
                + "\t" + packetDeliveryQuery.getDurationInSec() + "\t" + packetDeliveryQuery.getDurationInHourMinute() + "\t");
            //System.out.println("\nGenerated Packet Request:\n" + packetDeliveryQuery);
            // temporalProcess: 0 - Only spatial traj retrieval (QG timebucket ignored), spatial trajgraph, spatial A* search (no temporal processing)
            // temporalProcess: 1 - Only spatial traj retrieval (QG timebucket ignored), temporal directional trajgraph, temporal processing in A* search
            // temporalProcess: 2 - Spatio-temporal traj retrieval (QG timebucket considered), temporal directional trajgraph, temporal processing in A* search
            // SQ time bitmap not implemented, if done, can perhaps be put under temporalProcess 3
            for (int temporalProcess=0; temporalProcess<3; temporalProcess++){
                // latProximity and lonProximity are used only in summary node retrieval (or range query in baseline)
                boolean validSolution = TestServiceQuery.run(trajStorage, quadTrajTree, packetDeliveryQuery, latProximity, lonProximity, temporalProximity, distanceConverter,
                                                            trajProcessor, detourDistanceThreshold, trajProcessor.getNormalizedKeeperSet(), temporalProcess);
            }
            /*
            System.out.println("");
            if (!validSolution) i--;
            else{}
            */
        }
        System.out.println("");
        System.exit(0);
        // End of experiment
        
        for (int i=0; i<noOfPktsForDelivery; i++){
            // checks each stop probabilistically
            // packetDeliveryQuery.generatePktDeliveryReq();
            
            // picks from the lists of random src, dest stop ids
            //packetDeliveryQuery.generatePktDeliveryReq(i);
            // hotspot based packet request generation
            packetDeliveryQuery.generateSTHotspotPktDeliveryReq(i);
        
        /*
        while ((line = br.readLine()) != null) {
            String[] data = line.split("\t");
            int srcId = Integer.parseInt(data[0]);
            int destId = Integer.parseInt(data[1]);
            double hopCountCost = Double.parseDouble(data[2]);
            packetDeliveryQuery.generatePktDeliveryReq(srcId, destId);
        */
            
            PacketRequest pktReq = packetDeliveryQuery.getPacketRequest();
            //System.out.println("\nGenerated Packet Request:\n" + packetDeliveryQuery);
            // temporalProcess: 0 - Only spatial traj retrieval (QG timebucket ignored), spatial trajgraph, spatial A* search (no temporal processing)
            // temporalProcess: 1 - Only spatial traj retrieval (QG timebucket ignored), temporal directional trajgraph, temporal processing in A* search
            // temporalProcess: 2 - Spatio-temporal traj retrieval (QG timebucket considered), temporal directional trajgraph, temporal processing in A* search
            // SQ time bitmap not implemented, if done, can perhaps be put under temporalProcess 3
            for (int temporalProcess=0; temporalProcess<3; temporalProcess++){
                System.out.print("\n" + packetDeliveryQuery.getPacketRequest().getSrcId() + "\t" + packetDeliveryQuery.getPacketRequest().getDestId()
                                + "\t" + packetDeliveryQuery.getDistance() + "\t" + packetDeliveryQuery.getPacketRequest().getSrcTimeInSec() + "\t" +
                                    packetDeliveryQuery.getPacketRequest().getDestTimeInSec() + "\t" + packetDeliveryQuery.getDurationInHourMinute() + "\t");
                //facilityGraph = quadTrajTree.makeUnionSet(facilityGraph);
                //quadTrajTree.draw();

                /*
                int tot = 0;
                int id = 0;
                for (Entry<Node, Integer> entry : quadTrajTree.nodeToAllTrajsCount.entrySet()) {
                    //System.out.println(entry.getKey() + " " + entry.getValue());
                    tot += entry.getValue();
                }
                System.out.println(tot); */
                /*
                for (int srcId = 1; srcId <=11; srcId++){
                    for (int destId = 1; destId <=11; destId++){
                        pktReq.setSrcId(srcId); //11
                        pktReq.setDestId(destId);//6
                        boolean result = TestServiceQuery.run(trajStorage, quadTrajTree, pktReq, latProximity, lonProximity, temporalProximity);
                        System.exit(0);
                    }
                }
                */
                // latProximity and lonProximity are used only in summary node retrieval (or range query in baseline)
                boolean validSolution = TestServiceQuery.run(trajStorage, quadTrajTree, packetDeliveryQuery, latProximity, lonProximity, temporalProximity, distanceConverter,
                                                            trajProcessor, detourDistanceThreshold, trajProcessor.getNormalizedKeeperSet(), temporalProcess);
            }
            System.out.println("");
            /*
            if (!validSolution) i--;
            else{
                
            }
            */
            //System.exit(0);
        }
        
        //TestServiceQuery.run(quadTrajTree, facilityGraph);
        //TestBestKQuery.run(quadTrajTree, facilityGraph);

        //RandomGenerator randomGenerator = new RandomGenerator();
        //QuadTrajTree quadTrajTree = new QuadTrajTree(randomGenerator.generateTrajectory(10));		
        //quadTrajTree.draw();
    }

}
