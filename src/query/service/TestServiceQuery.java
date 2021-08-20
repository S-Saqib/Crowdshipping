package query.service;

import db.TrajStorage;
import java.util.ArrayList;
import ds.qtrajtree.TQIndex;
import ds.trajgraph.TrajGraphNode;
import io.real.TrajProcessor;
import java.util.HashSet;
import query.PacketDeliveryQuery;

public class TestServiceQuery {

    public static boolean run(TrajStorage trajStorage, TQIndex quadTrajTree, PacketDeliveryQuery pktRequest, double latDisThreshold, double lonDisThreshold, long temporalDisThreshold,
                            DistanceConverter distanceConverter, TrajProcessor trajProcessor, double detourDistanceThreshold, HashSet<Integer> normalizedKeepers, int temporalProcess){

        //int numberOfRuns = 10;
        //double naiveTime = 0, zOrderTime = 0;
        //for (int i = 0; i < numberOfRuns; i++) {
            ServiceQueryProcessor processQuery = new ServiceQueryProcessor(trajStorage, quadTrajTree, latDisThreshold, lonDisThreshold, temporalDisThreshold, distanceConverter,
                                                                            trajProcessor, pktRequest.getDistanceUnit(), detourDistanceThreshold, normalizedKeepers);
            //System.out.println("--Service Query--");
            //System.out.println("Optimal:");
            //double from = System.nanoTime();
            
            // assuming only the best one is returned
            /*
            ArrayList <TrajGraphNode> bestDeliverers = processQuery.deliverPacket(pktRequest);
            System.out.println("Best deliverers size = " + bestDeliverers.size());
            for (TrajGraphNode deliverer : bestDeliverers){
                System.out.print(deliverer.getStopId() + " " + deliverer.getTrajId() + " - ");
            }
            System.out.println("\n------------------------------------------------------");
            ArrayList <TrajGraphNode> bestDeliverersOriginal = processQuery.deliverPacketOriginal(pktRequest);
            System.out.println("Best deliverers (original) size = " + bestDeliverersOriginal.size());
            for (TrajGraphNode deliverer : bestDeliverersOriginal){
                System.out.print(deliverer.getStopId() + " " + deliverer.getTrajId() + " - ");
            }
            */
            //System.out.println("\n------------------------------------------------------");
            ArrayList <TrajGraphNode> bestDeliverersModified;
            /*
            if (!temporal){
                bestDeliverersModified = processQuery.deliverPacketModified(pktRequest);
                bestDeliverersModified = processQuery.deliverPacketModifiedWithJoin(pktRequest);

                bestDeliverersModified = processQuery.deliverPacketBaselineWithJoin(pktRequest);
            }
            else{
            */
                // go the the following methods and update the call to the appropriate A* method after testing is done
                bestDeliverersModified = processQuery.deliverPacketModifiedWithDuration(pktRequest, temporalProcess);
                bestDeliverersModified = processQuery.deliverPacketModifiedWithJoinWithDuration(pktRequest, temporalProcess);

                bestDeliverersModified = processQuery.deliverPacketBaselineWithJoinWithDuration(pktRequest, temporalProcess);

                /*
                bestDeliverersModified = processQuery.deliverPacketModifiedWithTimeStamp(pktRequest);
                bestDeliverersModified = processQuery.deliverPacketModifiedWithJoinWithTimeStamp(pktRequest);

                bestDeliverersModified = processQuery.deliverPacketBaselineWithJoinWithTimeStamp(pktRequest);
                */
            //}
            //bestDeliverersModified = processQuery.deliverPacketAllTrajWithJoin(pktRequest);
            /*
            System.out.println("Best deliverers (modified) size = " + bestDeliverersModified.size());
            for (TrajGraphNode deliverer : bestDeliverersModified){
                System.out.print("<" + deliverer.getStopId() + ", " + deliverer.getTrajId() + "> - ");
            }
            System.out.println("\n------------------------------------------------------");
            */
            
            // HashMap<String, TreeSet<TrajPoint>> infectedContacts = new HashMap<String, TreeSet<TrajPoint>>();
            // infectedContacts = processQuery.evaluateService(quadTrajTree.getQuadTree().getRootNode(), facilityGraph, infectedContacts);
            //infectedContacts = processQuery.calculateCover(quadTrajTree.getQuadTree(), facilityGraph, infectedContacts);
            //System.out.println("Infected = " + infectedContacts.size() + " persons");
            /*
            for (HashMap.Entry<String, TreeSet<TrajPoint>> entry : infectedContacts.entrySet()){
                System.out.print(entry.getKey() + " : ");
                for (TrajPoint trajPoint : entry.getValue()){
                    System.out.print(trajPoint.toString() + " ");
                }
                System.out.println("");
            }
            */
            //double to = System.nanoTime();
            //System.out.println("Number of routes: " + facilityQuery.size() + "\nNumber of users served = " + (int) serviceValue + "\nTime: " + (to - from) / 1e9 + "s");
            /*
            if (serviceValue < 1){
                i--;
                continue;
            }
            */
            //zOrderTime += (to - from) / 1e9;
            //System.out.println("Brute Force:");
            //from = System.nanoTime();
            //serviceValue = processQuery.evaluateServiceBruteForce(quadTrajTree.getQuadTree().getRootNode(), facilityQuery);
            //to = System.nanoTime();
            //System.out.println("Number of routes: " + facilityQuery.size() + "\nNumber of users served = " + (int) serviceValue
            //        + "\nTime: " + (to - from) / 1e9 + "s");
            //naiveTime += (to - from) / 1e9;
        //}
        //naiveTime /= numberOfRuns;
        //zOrderTime /= numberOfRuns;
        //System.out.println (naiveTime + "\n" + zOrderTime);
        return !bestDeliverersModified.isEmpty();
    }
}
