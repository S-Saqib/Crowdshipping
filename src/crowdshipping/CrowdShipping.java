/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crowdshipping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import db.TrajStorage;

import ds.qtrajtree.TQIndex;
import ds.qtree.Node;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import ds.trajgraph.TrajGraph;
import ds.trajgraph.TrajGraphNode;
import io.real.InputParser;

import io.real.SimpleParser;
import io.real.TrajProcessor;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import query.PacketDeliveryQuery;
import query.PacketRequest;
import query.service.DistanceConverter;

import query.service.TestServiceQuery;

import query.topk.TestBestKQuery;

public class CrowdShipping {

    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {

        String trajFilePath = "../Data/Myki/2018_June_Last_Week_Trips_All_Days.txt";
        String stopFile1Path = "../Data/Myki/my_stop_locations.txt";
        String stopFile2Path = "../Data/Myki/stop_locations.txt";
        
        TrajProcessor trajProcessor = new TrajProcessor();
        //System.out.println(System.getProperty("user.dir"));
        
        trajProcessor.loadStoppageData(stopFile1Path);
        trajProcessor.loadStoppageData(stopFile2Path);
        trajProcessor.loadTrajectories(trajFilePath);
        //trajProcessor.printTrajs();
        //trajProcessor.printSummary();
        trajProcessor.excludeWeekendUserIds();
        trajProcessor.normalizeTrajectories();
        //trajProcessor.printSummary();
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
            Statistics stats = new Statistics(quadTrajTree);
            stats.printStats();
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
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(1000);
            System.out.println("Summary index (1000 pt/leaf) construction time = " + (System.nanoTime()-from)/1.0e9 + " sec");
            //System.exit(0);
            //quadTrajTree.printSummaryIndex();
            //quadTrajTree.printRevSummaryIndex();
            quadTrajTree.printSummaryIndexSummary();
            System.out.println("Reverse...");
            quadTrajTree.printRevSummaryIndexSummary();
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
        
        // the following proximity, distance etc. are calculated in normalized lat, lon space
        double spatialProximity = 50;
        String proximityUnit = "m"; // it can be "m", "km", "mile" and "ft"
        DistanceConverter distanceConverter = new DistanceConverter(trajProcessor.getMaxLon(), trajProcessor.getMaxLat(), trajProcessor.getMinLon(), trajProcessor.getMinLat());
        double latProximity = distanceConverter.getLatProximity(spatialProximity, proximityUnit);
        double lonProximity = distanceConverter.getLonProximity(spatialProximity, proximityUnit);
        
        System.out.println(latProximity + " and " + lonProximity);
        // merge stops in 0.01m spatial range into a single trajgraph node
        double latClusterRange = 0.01;
        double lonClusterRange = 0.01;
        double clusterRangeInMeters = 250;
        System.out.println("lat cluster range " + latClusterRange + " = " + spatialProximity/latProximity*latClusterRange + " m");
        System.out.println("lon cluster range " + lonClusterRange + " = " + spatialProximity/lonProximity*lonClusterRange + " m");
        
        latClusterRange = latProximity/spatialProximity*clusterRangeInMeters;
        lonClusterRange = lonProximity/spatialProximity*clusterRangeInMeters;
        System.out.println("cluster range in meter " + clusterRangeInMeters + " = " + latClusterRange + " lat cluster range, " + lonClusterRange + " lon cluster range");

        long temporalProximity = 15; // in minutes, may be anything around 5 to 240 for example
        temporalProximity *= 60;    // in seconds
        
        /*
        // stats of stops
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
        
        
        Set <Integer> stopIds = trajProcessor.getStoppageMap().keySet();
        int amongStops = 0;
        int outOfStops = 0;
        for (Trajectory trajectory : trajProcessor.getTrajIdToTrajMap().values()){
            for (TrajPoint trajPoint : trajectory.getPointList()){
                if (stopIds.contains(trajPoint.getStoppage().getStopId())){
                    amongStops++;
                }
                else outOfStops++;
            }
        }
        System.out.println("# of Trajpoints among stoppages = " + amongStops + ", out of stoppages = " + outOfStops);
        */
                
        // need to consider keepers
        double detourDistranceThreshold = 1000; // in meters
        ///
        
        double pktReqMinDisThreshold = clusterRangeInMeters*5;
        PacketDeliveryQuery packetDeliveryQuery = new PacketDeliveryQuery(trajProcessor.getStoppageMap(), trajProcessor.getNormalizedStoppageMap(),
                                                                        distanceConverter, pktReqMinDisThreshold, proximityUnit);
        String pktSrcDestFilePath = "E:\\Education\\Academic\\BUET\\Educational\\MSc_BUET\\Thesis\\Experiments_Insights\\Src_Dest_Successful_Delivery.txt";
        BufferedReader br = null;
        String line = new String();
        br = new BufferedReader(new FileReader(pktSrcDestFilePath));
        br.readLine();
        
        System.out.print("\nSrc\tDest\tDis(" + proximityUnit + ")\tCost (file)\tisDelivered\tCost (hop)\tCost(dis)\tA* Cost(dis)");
        /*
        for (int i=0; i<100; i++){
            packetDeliveryQuery.generatePktDeliveryReq();
        */
        ///*
        while ((line = br.readLine()) != null) {
            String[] data = line.split("\t");
            int srcId = Integer.parseInt(data[0]);
            int destId = Integer.parseInt(data[1]);
            double hopCountCost = Double.parseDouble(data[2]);
            packetDeliveryQuery.generatePktDeliveryReq(srcId, destId);
        //*/
            
            PacketRequest pktReq = packetDeliveryQuery.getPacketRequest();
            //System.out.println("\nGenerated Packet Request:\n" + packetDeliveryQuery);
            System.out.print("\n" + packetDeliveryQuery.getPacketRequest().getSrcId() + "\t" + packetDeliveryQuery.getPacketRequest().getDestId()
                            + "\t" + packetDeliveryQuery.getDistance() + "\t" + hopCountCost + "\t");
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
            boolean validSolution = TestServiceQuery.run(trajStorage, quadTrajTree, pktReq, latProximity, lonProximity, temporalProximity,
                                                        distanceConverter, trajProcessor, packetDeliveryQuery.getDistanceUnit());
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
