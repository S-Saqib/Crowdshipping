/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;

import com.vividsolutions.jts.geom.Coordinate;
import db.TrajStorage;
import ds.qtrajtree.TQIndex;
import ds.qtree.Node;
import ds.qtree.NodeType;
import ds.qtree.Point;
import ds.qtree.QuadTree;
import ds.qtree.SummaryQuadTree;
import ds.trajectory.TrajPoint;
import ds.trajectory.TrajPointComparator;
import ds.trajectory.Trajectory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.TreeSet;
import query.PacketRequest;

/**
 *
 * @author Saqib
 */
public class ServiceQueryProcessor {

    private TQIndex quadTrajTree;
    private double latDisThreshold;
    private double lonDisThreshold;
    private double temporalDisThreshold;
    private TrajStorage trajStorage;

    public ServiceQueryProcessor(TrajStorage trajStorage, TQIndex quadTrajTree, double latDisThreshold, double lonDisThreshold, long temporalDisThreshold) {
        this.quadTrajTree = quadTrajTree;
        this.latDisThreshold = latDisThreshold;
        this.lonDisThreshold = lonDisThreshold;
        this.temporalDisThreshold = temporalDisThreshold;
        this.trajStorage = trajStorage;
    }
    
    public ServiceQueryProcessor(TQIndex quadTrajTree) {
        this.quadTrajTree = quadTrajTree;
        this.latDisThreshold = 0;
        this.lonDisThreshold = 0;
        this.temporalDisThreshold = 0;
    }

    public double getLatDisThreshold() {
        return latDisThreshold;
    }

    public void setLatDisThreshold(double latDisThreshold) {
        this.latDisThreshold = latDisThreshold;
    }

    public double getLonDisThreshold() {
        return lonDisThreshold;
    }

    public void setLonDisThreshold(double lonDisThreshold) {
        this.lonDisThreshold = lonDisThreshold;
    }

    public double getTemporalDisThreshold() {
        return temporalDisThreshold;
    }

    public void setTemporalDisThreshold(double temporalDisThreshold) {
        this.temporalDisThreshold = temporalDisThreshold;
    }
    
    // retrieve summary nodes considering overlap
    
    public ArrayList<Node> retrieveSummaryNodes(PacketRequest pktRequest){
        int overlapThreshold = 5;
        SummaryQuadTree sqTree = quadTrajTree.getSqTree();
        
        HashMap<Node, Integer> summaryNodes = new HashMap<>();
        PriorityQueue<Pair> outNodes = new PriorityQueue<>();
        PriorityQueue<Pair> inNodes = new PriorityQueue<>();
        HashSet<Node> overlappingNodes = new HashSet<>();
        
        double xMin = pktRequest.getNormSrcLat()-latDisThreshold;
        double xMax = pktRequest.getNormSrcLat()+latDisThreshold;
        double yMin = pktRequest.getNormSrcLon()-lonDisThreshold;
        double yMax = pktRequest.getNormSrcLon()+lonDisThreshold;
        
        Node[] outgoingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        xMin = pktRequest.getNormDestLat()-latDisThreshold;
        xMax = pktRequest.getNormDestLat()+latDisThreshold;
        yMin = pktRequest.getNormDestLon()-lonDisThreshold;
        yMax = pktRequest.getNormDestLon()+lonDisThreshold;
        
        Node[] incomingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        System.out.println("Outgoing nodes size = " + outgoingNodes.length);
        System.out.println("Incoming nodes size = " + incomingNodes.length);
        
        for (Node node : outgoingNodes){
            HashMap <Long, Integer> outNeighbors = sqTree.getSummaryGraph().get(node.getZCode());
            if (outNeighbors == null) continue;
            for (HashMap.Entry<Long,Integer> entry : outNeighbors.entrySet()){
                outNodes.add(new Pair(entry.getValue(), entry.getKey()));
            }
            // the following check is not actually necessary since the nodes are supposed to be unique
            if (summaryNodes.containsKey(node)){
                System.out.println("Outgoing node repeated!! - " + node.getZCode());
                // overlappingNodes.add(node);
            }
            else summaryNodes.put(node, 1); // 1 denotes only outgoing
        }
        
        for (Node node : incomingNodes){
            HashMap <Long, Integer> inNeighbors = sqTree.getReverseSummaryGraph().get(node.getZCode());
            if (inNeighbors == null) continue;
            for (HashMap.Entry<Long,Integer> entry : inNeighbors.entrySet()){
                inNodes.add(new Pair(entry.getValue(), entry.getKey()));
            }
            // the node is in summary nodes from outgoing nodes
            if (summaryNodes.containsKey(node) && summaryNodes.get(node)==1){
                overlappingNodes.add(node);
                summaryNodes.put(node, 3);  // 3 denotes both incoming and outgoing
            }
            // the following check is not actually necessary since the nodes are supposed to be unique
            else if (summaryNodes.containsKey(node)){
                System.out.println("Incoming node repeated!! - " + node.getZCode());
            }
            else summaryNodes.put(node, 2); // 2 denotes only incoming
        }
        // printing overlapping nodes after the initial outgoing and incoming nodes check
        System.out.println("Overlapping nodes initial size = " + overlappingNodes.size());
        for (Node node : overlappingNodes){
            System.out.println(node.getZCode());
        }
        
        boolean explore = true;
        while(overlappingNodes.size() < overlapThreshold){
            if (outNodes.peek() == null && inNodes.peek() == null) break;
            explore = true;
            Pair bestPair = outNodes.poll();
            // System.out.println("Extracted : " + bestPair.getKey() + " ; " + bestPair.getValue());
            if (bestPair!=null){
                Node node = sqTree.getqNodeIndexToNodeMap().get(bestPair.getValue());
                if (summaryNodes.containsKey(node)){
                    if (summaryNodes.get(node)==2){
                        overlappingNodes.add(node);
                        summaryNodes.put(node, 3);
                    }
                    else{
                        explore = false;
                    }
                }
                else summaryNodes.put(node, 1);
                if (explore){
                    HashMap <Long, Integer> outNeighbors = sqTree.getSummaryGraph().get(bestPair.getValue());
                    if (outNeighbors == null) continue;
                    for (HashMap.Entry<Long,Integer> entry : outNeighbors.entrySet()){
                        outNodes.add(new Pair(entry.getValue(), entry.getKey()));
                    }
                }
            }
            
            explore = true;
            bestPair = inNodes.poll();
            if (bestPair!=null){
                Node node = sqTree.getqNodeIndexToNodeMap().get(bestPair.getValue());
                if (summaryNodes.containsKey(node)){
                    if (summaryNodes.get(node)==1){
                        overlappingNodes.add(node);
                        summaryNodes.put(node, 3);
                    }
                    else{
                        explore = false;
                    }
                }
                else summaryNodes.put(node, 2);
                if (explore){
                    HashMap <Long, Integer> inNeighbors = sqTree.getReverseSummaryGraph().get(bestPair.getValue());
                    if (inNeighbors == null) continue;
                    for (HashMap.Entry<Long,Integer> entry : inNeighbors.entrySet()){
                        inNodes.add(new Pair(entry.getValue(), entry.getKey()));
                    }
                }
            }
        }
        
        System.out.println("Retrieved Summary Nodes Size = " + summaryNodes.size());
        /*
        for (Node node : summaryNodes.keySet()){
            System.out.println(node.getZCode());
        }
        */
        // return new ArrayList<Node>(summaryNodes.keySet());
        
        
        System.out.println("Retrieved Overlapping Summary Nodes Size = " + overlappingNodes.size());
        /*
        for (Node node : overlappingNodes){
            System.out.println(node.getZCode());
        }
        */
        //return new ArrayList<Node>(overlappingNodes);
        return new ArrayList<Node>(summaryNodes.keySet());
    }
    
    public ArrayList<Node> retrieveSummaryNodesOriginal(PacketRequest pktRequest){
        int overlapThreshold = 10;
        SummaryQuadTree sqTree = quadTrajTree.getSqTree();
        
        PriorityQueue<Pair> potentialOutNodes = new PriorityQueue<>();
        PriorityQueue<Pair> potentialInNodes = new PriorityQueue<>();
        HashSet<Node> exploredOutNodes = new HashSet<>();
        HashSet<Node> exploredInNodes = new HashSet<>();
        
        //HashMap<Node, Integer> summaryNodes = new HashMap<>();
        HashSet<Node> summaryNodes = new HashSet<>();
        
        double xMin = pktRequest.getNormSrcLat()-latDisThreshold;
        double xMax = pktRequest.getNormSrcLat()+latDisThreshold;
        double yMin = pktRequest.getNormSrcLon()-lonDisThreshold;
        double yMax = pktRequest.getNormSrcLon()+lonDisThreshold;
        
        // nearby blocks Ns
        Node[] outgoingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        xMin = pktRequest.getNormDestLat()-latDisThreshold;
        xMax = pktRequest.getNormDestLat()+latDisThreshold;
        yMin = pktRequest.getNormDestLon()-lonDisThreshold;
        yMax = pktRequest.getNormDestLon()+lonDisThreshold;
        
        // nearby blocks Nd
        Node[] incomingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        // O gets Ns (say key = sum of outdegree
        // we know every node is distinct in this array, no need to check color
        for (Node node : outgoingNodes){
            // add outgoing nodes to potential outnodes with appropriate key
            potentialOutNodes.add(new Pair(calculateKey(sqTree.getSummaryGraph().get(node.getZCode())), node.getZCode()));
        }
        
        for (Node node : incomingNodes){
            // add incoming nodes to potential innodes with appropriate key
            potentialInNodes.add(new Pair(calculateKey(sqTree.getReverseSummaryGraph().get(node.getZCode())), node.getZCode()));
        }
        
        int overlapCount = 0;
        
        while(overlapCount < overlapThreshold){
            if ((potentialOutNodes == null || potentialOutNodes.isEmpty()) && (potentialInNodes == null || potentialInNodes.isEmpty())){
                // neither of the priority queues have any more elements
                break;
            }
            if (potentialOutNodes != null && !potentialOutNodes.isEmpty()){
                // next potential out block
                Pair o = potentialOutNodes.poll();
                Node oNode = sqTree.getqNodeIndexToNodeMap().get(o.getValue());
                // need to do a color check
                if (!exploredOutNodes.contains(oNode)){
                    // add o to the list of nodes to be retrieved
                    summaryNodes.add(oNode);
                    // add o to explored outnodes
                    exploredOutNodes.add(oNode);
                    // add neighbors of o to potential outnodes with appropriate keys
                    HashMap <Long, Integer> neighbors = sqTree.getSummaryGraph().get(o.getValue());
                    if (neighbors != null){
                        for (HashMap.Entry<Long, Integer> entry : neighbors.entrySet()){
                            long neighborNodeZCode = entry.getKey();
                            potentialOutNodes.add(new Pair(calculateKey(sqTree.getSummaryGraph().get(neighborNodeZCode)), neighborNodeZCode));
                        }
                    }
                    // check if this node overlaps with the explored incoming nodes
                    if (exploredInNodes.contains(o)){
                        overlapCount++;
                    }
                }
            }
            if (potentialInNodes != null && !potentialInNodes.isEmpty() && overlapCount < overlapThreshold){
                // next potential in block
                Pair i = potentialInNodes.poll();
                Node iNode = sqTree.getqNodeIndexToNodeMap().get(i.getValue());
                // need to do a color check
                if (!exploredInNodes.contains(iNode)){
                    // add i to the list of nodes to be retrieved
                    summaryNodes.add(iNode);
                    // add i to explored innodes
                    exploredInNodes.add(iNode);
                    // add neighbors of i to potential innodes with appropriate keys
                    HashMap <Long, Integer> neighbors = sqTree.getReverseSummaryGraph().get(i.getValue());
                    if (neighbors != null){
                        for (HashMap.Entry<Long, Integer> entry : neighbors.entrySet()){
                            long neighborNodeZCode = entry.getKey();
                            potentialInNodes.add(new Pair(calculateKey(sqTree.getReverseSummaryGraph().get(neighborNodeZCode)), neighborNodeZCode));
                        }
                    }
                    // check if this node overlaps with the explored outgoing nodes
                    if (exploredOutNodes.contains(i)){
                        overlapCount++;
                    }
                }
            }
        }
        System.out.println("Retrieved Summary Nodes (Original) Size = " + summaryNodes.size());
        System.out.println("Retrieved Overlap Count (Original) = " + overlapCount);
        System.out.println("Unexplored outnodes = " + potentialOutNodes.size() + " , Unexplored innodes = " + potentialInNodes.size());
        return new ArrayList<Node>(summaryNodes);
    }
    
    public int calculateKey(HashMap<Long, Integer> neighbors){
        return calculateKeyFromTrajCount(neighbors);
        // return calculateKeyFromNeighborCount(neighbors);
    }
    
    public int calculateKeyFromNeighborCount(HashMap<Long, Integer> neighbors){
        if (neighbors == null) return 0;
        return neighbors.size();
    }
    
    public int calculateKeyFromTrajCount(HashMap<Long, Integer> neighbors){
        int key = 0;
        if (neighbors != null){
            for (HashMap.Entry<Long,Integer> entry : neighbors.entrySet()){
                key += entry.getValue();
            }
        }
        return key;
    }
    
    public ArrayList<Node> retrieveSummaryNodesModified(PacketRequest pktRequest){
        int overlapThreshold = 10;
        SummaryQuadTree sqTree = quadTrajTree.getSqTree();
        
        PriorityQueue<Pair> potentialOutNodes = new PriorityQueue<>();
        PriorityQueue<Pair> potentialInNodes = new PriorityQueue<>();
        HashSet<Node> exploredOutNodes = new HashSet<>();
        HashSet<Node> exploredInNodes = new HashSet<>();
        
        HashMap<Node, OverlappingNodes> nodeReachabilityMap = new HashMap<>();
        HashSet<Node> summaryNodes = new HashSet<>();
        HashSet<Node> overlappingNodes = new HashSet<>();
        
        Node ancestor = null;
        Node descendent = null;
        
        double xMin = pktRequest.getNormSrcLat()-latDisThreshold;
        double xMax = pktRequest.getNormSrcLat()+latDisThreshold;
        double yMin = pktRequest.getNormSrcLon()-lonDisThreshold;
        double yMax = pktRequest.getNormSrcLon()+lonDisThreshold;
        
        // nearby blocks Ns
        Node[] outgoingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        xMin = pktRequest.getNormDestLat()-latDisThreshold;
        xMax = pktRequest.getNormDestLat()+latDisThreshold;
        yMin = pktRequest.getNormDestLon()-lonDisThreshold;
        yMax = pktRequest.getNormDestLon()+lonDisThreshold;
        
        // nearby blocks Nd
        Node[] incomingNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        
        // O gets Ns (say key = sum of outdegree
        // we know every node is distinct in this array, no need to check color
        for (Node node : outgoingNodes){
            // add outgoing nodes to potential outnodes with appropriate key
            potentialOutNodes.add(new Pair(calculateKey(sqTree.getSummaryGraph().get(node.getZCode())), node.getZCode()));
            if (!nodeReachabilityMap.containsKey(node)){
                nodeReachabilityMap.put(node, new OverlappingNodes(node));
            }
        }
        
        for (Node node : incomingNodes){
            // add incoming nodes to potential innodes with appropriate key
            potentialInNodes.add(new Pair(calculateKey(sqTree.getReverseSummaryGraph().get(node.getZCode())), node.getZCode()));
            if (!nodeReachabilityMap.containsKey(node)){
                nodeReachabilityMap.put(node, new OverlappingNodes(node));
            }
        }
        
        int overlapCount = 0;
        
        while(overlapCount < overlapThreshold){
            if ((potentialOutNodes == null || potentialOutNodes.isEmpty()) && (potentialInNodes == null || potentialInNodes.isEmpty())){
                // neither of the priority queues have any more elements
                break;
            }
            if (potentialOutNodes != null && !potentialOutNodes.isEmpty()){
                // next potential out block
                Pair o = potentialOutNodes.poll();
                Node oNode = sqTree.getqNodeIndexToNodeMap().get(o.getValue());
                // need to do a color check
                if (!exploredOutNodes.contains(oNode)){
                    // add o to the list of nodes to be retrieved
                    // summaryNodes.add(oNode);
                    // add o to explored outnodes
                    exploredOutNodes.add(oNode);
                    // add neighbors of o to potential outnodes with appropriate keys
                    HashMap <Long, Integer> neighbors = sqTree.getSummaryGraph().get(o.getValue());
                    if (neighbors != null){
                        ancestor = oNode;
                        for (HashMap.Entry<Long, Integer> entry : neighbors.entrySet()){
                            long neighborNodeZCode = entry.getKey();
                            Node outNeighborNode = sqTree.getqNodeIndexToNodeMap().get(neighborNodeZCode);
                            potentialOutNodes.add(new Pair(calculateKey(sqTree.getSummaryGraph().get(neighborNodeZCode)), neighborNodeZCode));
                            if (!nodeReachabilityMap.containsKey(outNeighborNode)){
                                nodeReachabilityMap.put(outNeighborNode, new OverlappingNodes(outNeighborNode));
                            }
                            OverlappingNodes n = nodeReachabilityMap.get(outNeighborNode);
                            n.addToFrom(ancestor);
                            nodeReachabilityMap.put(outNeighborNode, n);
                        }
                    }
                    // check if this node overlaps with the explored incoming nodes
                    if (exploredInNodes.contains(o)){
                        overlapCount++;
                        overlappingNodes.add(oNode);
                    }
                }
            }
            if (potentialInNodes != null && !potentialInNodes.isEmpty() && overlapCount < overlapThreshold){
                // next potential in block
                Pair i = potentialInNodes.poll();
                Node iNode = sqTree.getqNodeIndexToNodeMap().get(i.getValue());
                // need to do a color check
                if (!exploredInNodes.contains(iNode)){
                    // add i to the list of nodes to be retrieved
                    // summaryNodes.add(iNode);
                    // add i to explored innodes
                    exploredInNodes.add(iNode);
                    // add neighbors of i to potential innodes with appropriate keys
                    HashMap <Long, Integer> neighbors = sqTree.getReverseSummaryGraph().get(i.getValue());
                    if (neighbors != null){
                        descendent = iNode;
                        for (HashMap.Entry<Long, Integer> entry : neighbors.entrySet()){
                            long neighborNodeZCode = entry.getKey();
                            Node inNeighborNode = sqTree.getqNodeIndexToNodeMap().get(neighborNodeZCode);
                            potentialInNodes.add(new Pair(calculateKey(sqTree.getReverseSummaryGraph().get(neighborNodeZCode)), neighborNodeZCode));
                            if (!nodeReachabilityMap.containsKey(inNeighborNode)){
                                nodeReachabilityMap.put(inNeighborNode, new OverlappingNodes(inNeighborNode));
                            }
                            OverlappingNodes n = nodeReachabilityMap.get(inNeighborNode);
                            n.addToTo(descendent);
                            nodeReachabilityMap.put(inNeighborNode, n);
                        }
                    }
                    // check if this node overlaps with the explored outgoing nodes
                    if (exploredOutNodes.contains(i)){
                        overlapCount++;
                        overlappingNodes.add(iNode);
                    }
                }
            }
        }
        for (Node node : overlappingNodes){
            OverlappingNodes connectedNodes = nodeReachabilityMap.get(node);
            for (Node n : connectedNodes.getAllNodes()){
                summaryNodes.add(n);
            }
        }
        System.out.println("Retrieved Summary Nodes (Modified) Size = " + summaryNodes.size());
        System.out.println("Retrieved Overlapping Summary Nodes (Modified) Size = " + overlappingNodes.size() + ", overlap count = " + overlapCount);
        return new ArrayList<Node>(summaryNodes);
    }
    
    ArrayList<Trajectory> retrieveTrajsFromSummaryNodes(ArrayList<Node> summaryNodes){
        HashSet<Node> baseQuadTreeNodes = new HashSet<>();
        QuadTree baseQuadTree = quadTrajTree.getQuadTree();
        for (Node node : summaryNodes){
            double xMin = node.getX();
            double yMin = node.getY();
            double xMax = xMin + node.getW();
            double yMax = yMin + node.getH();
            Node[] intersectingNodes = baseQuadTree.searchIntersect(xMin, yMin, xMax, yMax);
            for (Node n : intersectingNodes){
                // pending : temporal filtering
                // check for time bucket intersection of base quadtree node and summary node
                // prune if the time windows are disjoint
                baseQuadTreeNodes.add(n);
            }
        }
        System.out.println("Retrieved base quad tree nodes = " + baseQuadTreeNodes.size());
        
        // trajectory retrieval
        HashSet<Trajectory>retrievedTrajs = new HashSet<>();
        for (Node node : baseQuadTreeNodes){
            HashMap<Integer, HashSet<Object>> timeBucketToDiskBlockMap = node.getTimeBucketToDiskBlockIdMap();
            if (timeBucketToDiskBlockMap == null) continue;
            for (HashMap.Entry<Integer, HashSet<Object>> entry : timeBucketToDiskBlockMap.entrySet()){
                
                HashSet<Object> allDiskBlocks = entry.getValue();
                if (allDiskBlocks == null) continue;
                for (Object diskBlockId : allDiskBlocks){
                    
                    ArrayList<String> allTrajIds = trajStorage.getTrajIdListByBlockId((Integer)diskBlockId);
                    if (allTrajIds == null) continue;
                    for (String trajId : allTrajIds){
                        Trajectory traj = trajStorage.getTrajectoryById(trajId);
                        if (traj == null){
                            System.out.println("Why null? " + trajId);
                        }
                        retrievedTrajs.add(traj);
                        
                    }
                }
            }
        }
        System.out.println("Retrieved trajectories = " + retrievedTrajs.size());
        
        return new ArrayList<>(retrievedTrajs);
    }
    
    // find the best deliverers only for now
    public ArrayList<Trajectory> deliverPacket(PacketRequest pktRequest){
        // the correctness of the following methods should be rechecked
        // the following one is giving some overlapping nodes, so using it
        ArrayList<Node> summaryNodes = retrieveSummaryNodes(pktRequest);
        // the following ones are not giving any overlapping nodes
        //retrieveSummaryNodesOriginal(pktRequest);
        //retrieveSummaryNodesModified(pktRequest);
        ArrayList<Trajectory> bestDeliveres = retrieveTrajsFromSummaryNodes(summaryNodes);
        return bestDeliveres;
    }
    
    // the following method deals with inter node trajectories organized hierarchically
    // it traverses from root to leaves of the first level quadtree and processes internode trajectories for each node by calling evaluateNodeTraj method
    // should not be needed for QR-tree
    
    /*
    public HashMap <String, TreeSet<TrajPoint>> evaluateService(Node qNode, ArrayList<Trajectory> facilityQuery, HashMap <String, TreeSet<TrajPoint>> contactInfo) {
        if (facilityQuery == null || facilityQuery.isEmpty() || qNode.getNodeType() == NodeType.EMPTY) {
            return null;
        }
        
        HashMap <String, TreeSet<TrajPoint>> newContactInfo = evaluateNodeTraj(qNode, facilityQuery, contactInfo);
        
        if (newContactInfo != null) {
            for (HashMap.Entry<String, TreeSet<TrajPoint>> entry : newContactInfo.entrySet()) {
                String trajId = entry.getKey();
                TreeSet<TrajPoint> newContactPoints = entry.getValue();
                if (!contactInfo.containsKey(trajId)){
                    contactInfo.put(trajId, newContactPoints);
                }
                else{
                    for (TrajPoint trajPoint: newContactPoints) {
                        contactInfo.get(trajId).add(trajPoint);
                    }
                }
            }
        }
        
        if (qNode.getNodeType() != NodeType.LEAF){
            Node[] qChildren = new Node[4];
            qChildren[0] = qNode.getNe();
            qChildren[1] = qNode.getSe();
            qChildren[2] = qNode.getSw();
            qChildren[3] = qNode.getNw();
            newContactInfo = null;
            for (int k = 0; k < 4; k++) {
                ArrayList<Trajectory> querySubgraphs = clipGraph(qChildren[k], facilityQuery);
                newContactInfo = evaluateService(qChildren[k], querySubgraphs, contactInfo);
                if (newContactInfo != null) {
                    for (HashMap.Entry<String, TreeSet<TrajPoint>> entry : newContactInfo.entrySet()) {
                        String trajId = entry.getKey();
                        TreeSet<TrajPoint> newContactPoints = entry.getValue();
                        if (!contactInfo.containsKey(trajId)){
                            contactInfo.put(trajId, newContactPoints);
                        }
                        else{
                            for (TrajPoint trajPoint: newContactPoints) {
                                contactInfo.get(trajId).add(trajPoint);
                            }
                        }
                    }
                }
            }
        }
        return contactInfo;
    }
    */
    /*
    private ArrayList<Trajectory> clipGraph(Node node, ArrayList<Trajectory> facilityQuery) {
        ArrayList<Trajectory> clippedSubgraphs = new ArrayList<Trajectory>();
        for (Trajectory trajectory : facilityQuery){
            Trajectory clippedFacility = new Trajectory(trajectory.getAnonymizedId(), trajectory.getUserId());
            for (TrajPoint trajPoint : trajectory.getPointList()){
                if (containsExtended(node, trajPoint)){
                    clippedFacility.addTrajPoint(trajPoint);
                }
            }
            clippedSubgraphs.add(clippedFacility);
        }
        return clippedSubgraphs;
    }
    */
    
    boolean containsExtended(Node qNode, TrajPoint trajPoint) {
        Coordinate coord = trajPoint.getPointLocation();
        // checking whether an extended qNode contains a point
        double minX = qNode.getX() - latDisThreshold;
        double minY = qNode.getY() - lonDisThreshold;
        double maxX = minX + qNode.getW() + 2 * latDisThreshold;
        double maxY = minY + qNode.getH() + 2 * lonDisThreshold;
        if (coord.x < minX || coord.y < minY || coord.x > maxX || coord.y > maxY) {
            return false;
        }
        return true;
    }
    
    // the following method obtains the corresponding quadtree for the inter node trajectories of a node and passes to calculate cover for processing them
    // should not be needed for QR tree as we have a single quadtree
    private HashMap <String, TreeSet<TrajPoint>> evaluateNodeTraj(Node qNode, ArrayList<Trajectory> facilityQuery, HashMap<String, TreeSet<TrajPoint>> contactInfo) {
        if (facilityQuery == null || facilityQuery.isEmpty()) {
            return null;
        }
        QuadTree interNodeQuadTree = quadTrajTree.getQNodeQuadTree(qNode);
        if (interNodeQuadTree == null || interNodeQuadTree.isEmpty()) {
            return null;
        }
        return calculateCover(interNodeQuadTree, facilityQuery, contactInfo);
    }
    
    // actually calculates the overlaps with facility trajectory
    // should be called directly for QR-tree
    public HashMap <String, TreeSet<TrajPoint>> calculateCover(QuadTree quadTree, ArrayList<Trajectory> facilityQuery, HashMap<String, TreeSet<TrajPoint>> contactInfo) {
        for (Trajectory trajectory : facilityQuery) {
            String infectedAnonymizedId = trajectory.getAnonymizedId();
            for (TrajPoint trajPoint : trajectory.getPointList()) {
                // trajPointCoordinate of a facility point
                Coordinate trajPointCoordinate = trajPoint.getPointLocation();
                double infectedX = trajPointCoordinate.x;
                double infectedY = trajPointCoordinate.y;
                double infectedT = trajPoint.getTimeInSec();
                // taking each point of facility subgraph we are checking against the points of inter node trajectories, indexed in the quadtree
                double xMin = infectedX - latDisThreshold;
                double xMax = infectedX + latDisThreshold;
                double yMin = infectedY - lonDisThreshold;
                double yMax = infectedY + lonDisThreshold;
                
                Node[] relevantNodes = quadTree.searchIntersect(xMin, yMin, xMax, yMax);
                
                // calculating time index for the trajectory points
                ArrayList<Integer> timeBuckets = new ArrayList<Integer>();
                int timeIndexFrom = quadTree.getTimeIndex(trajPoint.getTimeInSec());
                int timeIndexTo = quadTree.getTimeIndex(trajPoint.getTimeInSec() + (long) temporalDisThreshold);
                for (int timeIndex = timeIndexFrom; timeIndex <= timeIndexTo; timeIndex++){
                    timeBuckets.add(timeIndex);
                }
                
                HashSet<Integer> relevantDiskBlocks = new HashSet<Integer>();
                for (Node node : relevantNodes){
                    for (int timeIndex : timeBuckets){
                        ArrayList<Object> mappedDiskBlocks = node.getDiskBlocksByQNodeTimeIndex(timeIndex);
                        if (mappedDiskBlocks == null) continue;
                        for (Object blockId : mappedDiskBlocks){
                            relevantDiskBlocks.add((Integer) blockId);
                        }
                    }
                }
                
                ArrayList<Trajectory> relevantTrajectories = new ArrayList<Trajectory>();
                // need a map for disk block id to trajectory (the reverse of traj to disk block map
                for (Integer blockId : relevantDiskBlocks){
                    for (String trajId : trajStorage.getTrajIdListByBlockId(blockId)){
                        relevantTrajectories.add(trajStorage.getTrajectoryById(trajId));
                    }
                }
                
                for (Trajectory traj : relevantTrajectories){
                    String checkId = traj.getAnonymizedId();
                    for (TrajPoint point : traj.getPointList()){
                        // checking if the point belongs to the same trajectory, if so, it should be ignored
                        if (checkId.equals(infectedAnonymizedId)){
                            continue;
                        }
                        // spatial matching: checking if eucliean distance is within spatialDistanceThreshold
                        double checkX = point.getPointLocation().x;
                        double checkY = point.getPointLocation().y;
                        // need to calculate geodesic distance here
                        double euclideanDistance = Math.sqrt(Math.pow((infectedX - checkX), 2) + Math.pow((infectedY - checkY), 2));
                        if (euclideanDistance <= (latDisThreshold+lonDisThreshold)/2){
                            double checkT = point.getTimeInSec();
                            // temporal matching: checkT should be in [t, t+temporalDistanceThreshold] window for a contact to be affected
                            if (checkT - infectedT >= 0 && checkT - infectedT <= temporalDisThreshold){
                                if (!contactInfo.containsKey(checkId)){
                                    contactInfo.put((String)checkId, new TreeSet<TrajPoint>(new TrajPointComparator()));
                                }
                                contactInfo.get(checkId).add(point);
                            }
                        }
                    }
                }
            }
        }
        return contactInfo;
    }
}

class OverlappingNodes{
    Node node;
    HashSet<Node> from;
    HashSet<Node> to;

    public OverlappingNodes() {
        from = new HashSet<>();
        to = new HashSet<>();
    }
    
    public OverlappingNodes(Node node) {
        this.node = node;
        from = new HashSet<>();
        to = new HashSet<>();
    }
    
    public void addToFrom(Node node){
        from.add(node);
    }
    
    public void addToTo(Node node){
        to.add(node);
    }
    
    public ArrayList<Node> getAllNodes(){
        HashSet <Node> allNodes = new HashSet<>();
        allNodes.add(node);
        for (Node node : from) allNodes.add(node);
        for (Node node : to) allNodes.add(node);
        return new ArrayList<>(allNodes);
    }
    
}

class Pair implements Comparable<Pair>{
    Integer key;
    Long value;

    public Pair(Integer key, Long value) {
        this.key = key;
        this.value = value;
    }

    public Integer getKey() {
        return key;
    }

    public Long getValue() {
        return value;
    }
    
    @Override
    public int compareTo(Pair o) {
        return o.key - key;
    }
}