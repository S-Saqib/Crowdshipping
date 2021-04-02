package query.service;

import db.TrajStorage;
import java.util.ArrayList;
import ds.qtrajtree.TQIndex;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import java.util.HashMap;
import java.util.TreeSet;
import query.PacketRequest;

public class TestServiceQuery {

    public static void run(TrajStorage trajStorage, TQIndex quadTrajTree, PacketRequest pktRequest,
                            double latDisThreshold, double lonDisThreshold, long temporalDisThreshold) {

        //int numberOfRuns = 10;
        //double naiveTime = 0, zOrderTime = 0;
        //for (int i = 0; i < numberOfRuns; i++) {
            ServiceQueryProcessor processQuery = new ServiceQueryProcessor(trajStorage, quadTrajTree, latDisThreshold, lonDisThreshold, temporalDisThreshold);
            //System.out.println("--Service Query--");
            //System.out.println("Optimal:");
            //double from = System.nanoTime();
            
            // assuming only the best one is returned
            ArrayList <Trajectory> bestDeliverers = processQuery.deliverPacket(pktRequest);
            for (Trajectory deliverer : bestDeliverers){
                System.out.println(deliverer.getUserId() + " " + deliverer.getTrajId());
            }
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
    }
}
