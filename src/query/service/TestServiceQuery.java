package query.service;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import ds.qtrajtree.QuadTrajTree;
import java.util.HashSet;
import query.QueryGraphGenerator;

public class TestServiceQuery {

    public static void run(QuadTrajTree quadTrajTree, ArrayList<CoordinateArraySequence> facilityGraph) {

        QueryGraphGenerator newQuery = new QueryGraphGenerator();
        int numberOfRuns = 10;
        double naiveTime = 0, zOrderTime = 0;
        //for (int i = 0; i < numberOfRuns; i++) {
            ArrayList<CoordinateArraySequence> facilityQuery = new ArrayList<CoordinateArraySequence>(newQuery.generateQuery(facilityGraph));
            
            ServiceQueryProcessor processQuery = new ServiceQueryProcessor(quadTrajTree);
            //System.out.println("--Service Query--");
            //System.out.println("Optimal:");
            HashSet <Integer> served = new HashSet<Integer>();
            double from = System.nanoTime();
            double serviceValue = processQuery.evaluateService(quadTrajTree.getQuadTree().getRootNode(), facilityQuery, served);
            double to = System.nanoTime();
            System.out.println("Number of routes: " + facilityQuery.size() + "\nNumber of users served = " + (int) serviceValue
                    + "\nTime: " + (to - from) / 1e9 + "s");
            /*
            if (serviceValue < 1){
                i--;
                continue;
            }
            */
            zOrderTime += (to - from) / 1e9;
            //System.out.println("Brute Force:");
            //from = System.nanoTime();
            //serviceValue = processQuery.evaluateServiceBruteForce(quadTrajTree.getQuadTree().getRootNode(), facilityQuery);
            //to = System.nanoTime();
            //System.out.println("Number of routes: " + facilityQuery.size() + "\nNumber of users served = " + (int) serviceValue
            //        + "\nTime: " + (to - from) / 1e9 + "s");
            //naiveTime += (to - from) / 1e9;
        //}
        naiveTime /= numberOfRuns;
        zOrderTime /= numberOfRuns;
        System.out.println (naiveTime + "\n" + zOrderTime);
    }
}
