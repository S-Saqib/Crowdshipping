/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crowdshipping;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import ds.qtrajtree.TQIndex;
import ds.qtree.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author Saqib
 */
public class Statistics {

    private TQIndex quadTrajTree;
    
    public Statistics(TQIndex tQIndex) {
        quadTrajTree = tQIndex;
    }
    
    public void printStats(){
        
        int trajCount = quadTrajTree.getTotalNodeTraj(quadTrajTree.getQuadTree().getRootNode());
        int nodeCount = quadTrajTree.getQuadTree().getNodeCount();
        int nodesHavingTrajectories = quadTrajTree.qNodeToTrajsMap.size();
            
        int []depthWiseNodeCount = new int [quadTrajTree.getQuadTree().getHeight()+1];
        int []depthWiseTrajCount = new int[quadTrajTree.getQuadTree().getHeight()+1];
        Arrays.fill(depthWiseNodeCount, 0);
        Arrays.fill(depthWiseTrajCount, 0);

        for (Map.Entry<Node, ArrayList<CoordinateArraySequence>> entry : quadTrajTree.qNodeToTrajsMap.entrySet()) {
            Node node = entry.getKey();
            depthWiseNodeCount[node.getDepth()]++;
            ArrayList<CoordinateArraySequence> trajectories = entry.getValue();
            depthWiseTrajCount[node.getDepth()] += trajectories.size();
        }
        
        System.out.println("Number of trajectories = " + trajCount);
        System.out.println("Number of total qNodes = " + nodeCount);
        System.out.println("Number of qNodes having trajs = " + nodesHavingTrajectories);
        
        System.out.println("QuadTree height = " + quadTrajTree.getQuadTree().getHeight());
        
        for (int i=0; i<depthWiseNodeCount.length; i++){
            System.out.println("Depth = " + i + " : Node Count = " + depthWiseNodeCount[i] + " , Trajectory Count = " + depthWiseTrajCount[i]);
        }
        
    }
}
