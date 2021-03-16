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
import ds.trajectory.Trajectory;
import io.real.InputParser;

import io.real.SimpleParser;
import io.real.TrajProcessor;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        //trajProcessor.printInfo();
        
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
            from = System.nanoTime();
            quadTrajTree.buildSummaryIndex(1000);
            System.out.println("Summary index (1000 pt/leaf) construction time = " + (System.nanoTime()-from)/1.0e9 + " sec");
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
        //quadTrajTree.draw();
    }

}
