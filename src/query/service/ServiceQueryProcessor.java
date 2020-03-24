/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import ds.qtrajtree.QuadTrajTree;
import ds.qtree.Node;
import ds.qtree.NodeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import result.ResultPlotter;
import query.topk.DiskIO;

/**
 *
 * @author Saqib
 */
public class ServiceQueryProcessor {

    QuadTrajTree quadTrajTree;

    public ServiceQueryProcessor(QuadTrajTree quadTrajTree) {
        this.quadTrajTree = quadTrajTree;
    }
    
    public double evaluateServiceBruteForce(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery) {
//    		return quadTrajTree.evaluateNodeTrajBruteForce(qNode, facilityQuery);
    		
    		if (facilityQuery == null || facilityQuery.isEmpty()) {
    			return 0;
    	    }
    	    ArrayList<CoordinateArraySequence> allTrajs = quadTrajTree.getQNodeAllTrajs(qNode);
    	    ArrayList<Integer> allTrajIds = quadTrajTree.getQNodeAllTrajsId(qNode);
    		return quadTrajTree.calculateCover(allTrajs, allTrajIds, facilityQuery);
    }

    public double evaluateService(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery, HashSet <Integer> served) {
        if (facilityQuery == null || facilityQuery.isEmpty() || qNode.getNodeType() == NodeType.LEAF
                || qNode.getNodeType() == NodeType.EMPTY) {
            return 0;
        }
        Node[] qChildren = new Node[4];
        qChildren[0] = qNode.getNe();
        qChildren[1] = qNode.getSe();
        qChildren[2] = qNode.getSw();
        qChildren[3] = qNode.getNw();
        // Should pass two Integer variables instead of 0, 0 and use their values later for disk I/O comparison
        DiskIO diskIO = new DiskIO();
        double serviceValue = quadTrajTree.evaluateNodeTrajWithIndexBinary(qNode, facilityQuery, served, new ResultPlotter(), diskIO);
        for (int k = 0; k < 4; k++) {
            ArrayList<CoordinateArraySequence> querySubgraphs
                    = quadTrajTree.clipGraph(qChildren[k], facilityQuery);
            serviceValue += evaluateService(qChildren[k], querySubgraphs, served);
        }
        //System.out.println("Service Value = " + serviceValue + ", actually served = " + served.size());
        //return serviceValue;
        return served.size();
    }

}
