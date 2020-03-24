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

import ds.qtrajtree.QuadTrajTree;
import ds.qtree.Node;
import io.real.InputParser;

import io.real.SimpleParser;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import query.service.TestServiceQuery;

import query.topk.TestBestKQuery;

public class CrowdShipping {

    public static void main(String[] args) throws IOException {

        //System.out.println(System.getProperty("user.dir"));
        //String routeFilePath = "../../maxtrajcover/src/main/resources/io/real/routelist.xlsx";
        //String userTrajectoryFilePath = "../../maxtrajcover/src/main/resources/io/real/staypoints.xlsx";
        String routeFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Facility\\NYC_transport\\NYC_routes.txt";
        String stoppageFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Facility\\NYC_transport\\NYC_stopid.txt";
        String userTrajectoryFilePath = "E:\\Education\\Academic\\BUET\\Educational\\Departmental\\4-2\\Thesis\\Data\\New York\\Taxi\\user_traj_for_temporal_processing.csv";
        
        File routeFile = new File(routeFilePath);
        File userTrajectoryFile = new File(userTrajectoryFilePath);


        //System.out.println(routeFile.exists());
        //System.out.println(userTrajectoryFile.exists());
        //InputParser inParser = new InputParser();
        SimpleParser inParser = new SimpleParser();

        //System.out.println("Route File Found: "+ routeFile.exists());
        //System.out.println("Trajectory File Found: "+ userTrajectoryFile.exists());
        
        /*
        if(!routeFile.exists() || !userTrajectoryFile.exists()) {
            System.exit(0);
        }
        */
        
        
        ArrayList<CoordinateArraySequence> userTrajectories = null;
        ArrayList<CoordinateArraySequence> facilityGraph = null;

        userTrajectories = new ArrayList<CoordinateArraySequence>(inParser.parseUserTrajectories(userTrajectoryFilePath));
        //System.out.println(inParser.minLat+"\t"+inParser.maxLat+"\t"+inParser.minLon+"\t"+inParser.maxLon);
        facilityGraph = new ArrayList<CoordinateArraySequence>(inParser.parseRoutes(stoppageFilePath, routeFilePath));
        
        routeFile = null;
        userTrajectoryFile = null;
        
        double indexTime = 0;
        int numberOfRuns = 10;
        
        /*
        try {
            //System.out.println(userTrajectories.size());
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (int i=0; i<numberOfRuns; i++){
            
            Collections.shuffle(userTrajectories);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
          */  
            QuadTrajTree quadTrajTree = new QuadTrajTree(userTrajectories, inParser.latCoeff, inParser.latConst, inParser.lonCoeff, inParser.lonConst);
            System.out.println(userTrajectories.size());
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
        //System.exit(0);
        
        //TestServiceQuery.run(quadTrajTree, facilityGraph);
        //TestBestKQuery.run(quadTrajTree, facilityGraph);

        //RandomGenerator randomGenerator = new RandomGenerator();
        //QuadTrajTree quadTrajTree = new QuadTrajTree(randomGenerator.generateTrajectory(10));		
        quadTrajTree.draw();
    }

}
