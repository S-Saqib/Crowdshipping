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
import ds.qtree.QuadTree;
import ds.qtree.SummaryQuadTree;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import ds.trajgraph.TrajGraph;
import ds.trajgraph.TrajGraphNode;
import io.real.TrajProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import query.PacketDeliveryQuery;
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
    private TrajGraph trajGraph;
    private DistanceConverter distanceConverter;
    private TrajProcessor trajProcessor;
    private String distanceUnit;
    private double detourDistanceThreshold;
    private HashSet<Integer> normalizedKeeperIds;
    private double deliveryCost;
    private long deliveryTimeInSec;
    private static int overlapThreshold = 5;
    private HashSet<Long> diskBlockSet;

    public ServiceQueryProcessor(TrajStorage trajStorage, TQIndex quadTrajTree, double latDisThreshold, double lonDisThreshold, long temporalDisThreshold,
                        DistanceConverter distanceConverter, TrajProcessor trajProcessor, String distanceUnit, double detourDistanceThrehold, HashSet<Integer> normalizedKeeperIds) {
        this.quadTrajTree = quadTrajTree;
        this.latDisThreshold = latDisThreshold;
        this.lonDisThreshold = lonDisThreshold;
        this.temporalDisThreshold = temporalDisThreshold;
        this.trajStorage = trajStorage;
        this.trajGraph = trajGraph;
        this.distanceConverter = distanceConverter;
        this.trajProcessor = trajProcessor;
        this.distanceUnit = distanceUnit;
        this.detourDistanceThreshold = detourDistanceThrehold;
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.normalizedKeeperIds = normalizedKeeperIds;
        this.diskBlockSet = new HashSet<>();
    }
    
    public ServiceQueryProcessor(TQIndex quadTrajTree) {
        this.quadTrajTree = quadTrajTree;
        this.latDisThreshold = 0;
        this.lonDisThreshold = 0;
        this.temporalDisThreshold = 0;
    }
    // retrieve summary nodes considering overlap
    
    public ArrayList<Node> retrieveSummaryNodes(PacketRequest pktRequest){
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
        
        //System.out.println("Outgoing nodes size = " + outgoingNodes.length);
        //System.out.println("Incoming nodes size = " + incomingNodes.length);
        
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
        //System.out.println("Overlapping nodes initial size = " + overlappingNodes.size());
        /*
        System.out.println("Initial overlapping nodes:");
        for (Node node : overlappingNodes){
            System.out.println(node.getZCode());
        }
        */
        
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
        
        // System.out.println("Retrieved Summary Nodes Size = " + summaryNodes.size());
        /*
        for (Node node : summaryNodes.keySet()){
            System.out.println(node.getZCode());
        }
        */
        // return new ArrayList<Node>(summaryNodes.keySet());
        
        
        // System.out.println("Retrieved Overlapping Summary Nodes Size = " + overlappingNodes.size());
        /*
        for (Node node : overlappingNodes){
            System.out.println(node.getZCode());
        }
        */
        //return new ArrayList<Node>(overlappingNodes);
        return new ArrayList<Node>(summaryNodes.keySet());
    }
    
    public ArrayList<Node> retrieveSummaryNodesOriginal(PacketRequest pktRequest){
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
                    if (exploredInNodes.contains(oNode)){
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
                    if (exploredOutNodes.contains(iNode)){
                        overlapCount++;
                    }
                }
            }
        }
        // System.out.println("Retrieved Summary Nodes (Original) Size = " + summaryNodes.size());
        // System.out.println("Retrieved Overlap Count (Original) = " + overlapCount);
        //System.out.println("Unexplored outnodes = " + potentialOutNodes.size() + " , Unexplored innodes = " + potentialInNodes.size());
        return new ArrayList<Node>(summaryNodes);
    }
    
    public int calculateDirectionalKey(Node node, double lat, double lon){
        double centerX = node.getX()+node.getW()/2;
        double centerY = node.getY()+node.getH()/2;
        double distanceKey = distanceConverter.absDistanceFromNormalizedValues(lat, centerX, lon, centerY, distanceUnit);
        distanceKey *= -1;  // negated because since lower distance should get higher priority
        return (int)(distanceKey);
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
            int key = calculateKey(sqTree.getSummaryGraph().get(node.getZCode()));
            int dirKey = calculateDirectionalKey(node, pktRequest.getNormDestLat(), pktRequest.getNormDestLon());
            int compoundKey = (int)Math.ceil(key/10000.0)*dirKey;
            // if (compoundKey > -1) System.out.println("Alarm!! Alarm!! Compound key = " + compoundKey);
            //potentialOutNodes.add(new Pair(key, node.getZCode()));
            potentialOutNodes.add(new Pair(dirKey, node.getZCode()));
            //potentialOutNodes.add(new Pair(compoundKey, node.getZCode()));
            
            if (!nodeReachabilityMap.containsKey(node)){
                nodeReachabilityMap.put(node, new OverlappingNodes(node));
            }
        }
        
        for (Node node : incomingNodes){
            // add incoming nodes to potential innodes with appropriate key
            int key = calculateKey(sqTree.getReverseSummaryGraph().get(node.getZCode()));
            int dirKey = calculateDirectionalKey(node, pktRequest.getNormSrcLat(), pktRequest.getNormSrcLon());
            int compoundKey = (int)Math.ceil(key/10000.0)*dirKey;
            // if (compoundKey > -1) System.out.println("Alarm!! Alarm!! Compound key = " + compoundKey);
            //potentialInNodes.add(new Pair(key, node.getZCode()));
            potentialInNodes.add(new Pair(dirKey, node.getZCode()));
            //potentialInNodes.add(new Pair(compoundKey, node.getZCode()));
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
                            int key = calculateKey(sqTree.getSummaryGraph().get(neighborNodeZCode));
                            int dirKey = calculateDirectionalKey(outNeighborNode, pktRequest.getNormDestLat(), pktRequest.getNormDestLon());
                            int compoundKey = (int)Math.ceil(key/10000.0)*dirKey;
                            // if (compoundKey > -1) System.out.println("Alarm!! Alarm!! Compound key = " + compoundKey);
                            //potentialOutNodes.add(new Pair(key), neighborNodeZCode));
                            potentialOutNodes.add(new Pair(dirKey, neighborNodeZCode));
                            //potentialOutNodes.add(new Pair(compoundKey, neighborNodeZCode));
                            if (!nodeReachabilityMap.containsKey(outNeighborNode)){
                                nodeReachabilityMap.put(outNeighborNode, new OverlappingNodes(outNeighborNode));
                            }
                            OverlappingNodes n = nodeReachabilityMap.get(outNeighborNode);
                            n.addToFrom(ancestor);
                            nodeReachabilityMap.put(outNeighborNode, n);
                        }
                    }
                    // check if this node overlaps with the explored incoming nodes
                    if (exploredInNodes.contains(oNode)){
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
                            int key = calculateKey(sqTree.getReverseSummaryGraph().get(neighborNodeZCode));
                            int dirKey = calculateDirectionalKey(inNeighborNode, pktRequest.getNormSrcLat(), pktRequest.getNormSrcLon());
                            int compoundKey = (int)Math.ceil(key/10000.0)*dirKey;
                            // if (compoundKey > -1) System.out.println("Alarm!! Alarm!! Compound key = " + compoundKey);
                            //potentialInNodes.add(new Pair(key, neighborNodeZCode));
                            potentialInNodes.add(new Pair(dirKey, neighborNodeZCode));
                            //potentialInNodes.add(new Pair(compoundKey, neighborNodeZCode));
                            if (!nodeReachabilityMap.containsKey(inNeighborNode)){
                                nodeReachabilityMap.put(inNeighborNode, new OverlappingNodes(inNeighborNode));
                            }
                            OverlappingNodes n = nodeReachabilityMap.get(inNeighborNode);
                            n.addToTo(descendent);
                            nodeReachabilityMap.put(inNeighborNode, n);
                        }
                    }
                    // check if this node overlaps with the explored outgoing nodes
                    if (exploredOutNodes.contains(iNode)){
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
        // System.out.println("Retrieved Summary Nodes (Modified) Size = " + summaryNodes.size());
        // System.out.println("Retrieved Overlapping Summary Nodes (Modified) Size = " + overlappingNodes.size() + ", overlap count = " + overlapCount);
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
                //if (!baseQuadTreeNodes.contains(n)) System.out.println(n);
                baseQuadTreeNodes.add(n);
            }
        }
        // System.out.println("Retrieved base quad tree nodes = " + baseQuadTreeNodes.size());
        
        // trajectory retrieval
        HashSet<Trajectory>retrievedTrajs = new HashSet<>();
        for (Node node : baseQuadTreeNodes){
            HashMap<Integer, HashSet<Object>> timeBucketToDiskBlockMap = node.getTimeBucketToDiskBlockIdMap();
            if (timeBucketToDiskBlockMap == null) continue;
            for (HashMap.Entry<Integer, HashSet<Object>> entry : timeBucketToDiskBlockMap.entrySet()){
                
                HashSet<Object> allDiskBlocks = entry.getValue();
                if (allDiskBlocks == null) continue;
                for (Object diskBlockId : allDiskBlocks){
                    // assumption : 16 rtree leaves in each block (2 trajs in each leaf on avg, each traj size around 128B, block size 4k => trajs per block = 32)
                    int diskBlockAccessed = ((Integer)diskBlockId)/16;
                    diskBlockSet.add((long)diskBlockAccessed);
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
        //System.out.println("Retrieved trajectories = " + retrievedTrajs.size());
        
        return new ArrayList<>(retrievedTrajs);
    }
    
    // modify later
    ArrayList<Trajectory> retrieveTrajsBaseline(ArrayList<Node> summaryNodes){
        HashSet<Node> baseQuadTreeNodes = new HashSet<>();
        QuadTree baseQuadTree = quadTrajTree.getQuadTree();
        for (Node node : summaryNodes){
            double xMin = node.getX();
            double yMin = node.getY();
            double xMax = xMin + node.getW();
            double yMax = yMin + node.getH();
            Node[] intersectingNodes = baseQuadTree.searchIntersect(xMin, yMin, xMax, yMax);
            baseQuadTreeNodes.addAll(Arrays.asList(intersectingNodes));
            // pending : temporal filtering
            // check for time bucket intersection of base quadtree node and summary node
            // prune if the time windows are disjoint
            //if (!baseQuadTreeNodes.contains(n)) System.out.println(n);
        }
        // System.out.println("Retrieved base quad tree nodes = " + baseQuadTreeNodes.size());
        
        // trajectory retrieval
        HashSet<Trajectory>retrievedTrajs = new HashSet<>();
        HashMap<Long,Integer> diskBlockAccessMap = new HashMap<>();
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
                        /*
                        // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                        long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                        // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                        diskBlockSet.add((quadtreeBasedDiskBlockId));
                        */
                        // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                        long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                        // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                        if (!diskBlockAccessMap.containsKey(quadtreeBasedDiskBlockId)){
                            diskBlockAccessMap.put(quadtreeBasedDiskBlockId, 0);
                        }
                        diskBlockAccessMap.put(quadtreeBasedDiskBlockId, diskBlockAccessMap.get(quadtreeBasedDiskBlockId)+1);
                        // max leaf id 13046, max pt per leaf 30511 for same coordinates
                        long simulatedBaselineBlockId = quadtreeBasedDiskBlockId*100000 + diskBlockAccessMap.get(quadtreeBasedDiskBlockId)/32;
                        diskBlockSet.add(simulatedBaselineBlockId);
                        retrievedTrajs.add(traj);
                    }
                }
            }
        }
        //System.out.println("Retrieved trajectories = " + retrievedTrajs.size());
        
        return new ArrayList<>(retrievedTrajs);
    }
    
    public ArrayList<Node> retrieveRangeQueryNodes(PacketRequest pktRequest){
        
        double xMin = pktRequest.getNormSrcLat()-latDisThreshold;
        double xMax = pktRequest.getNormSrcLat()+latDisThreshold;
        double yMin = pktRequest.getNormSrcLon()-lonDisThreshold;
        double yMax = pktRequest.getNormSrcLon()+lonDisThreshold;
        
        xMin = Math.min(xMin, pktRequest.getNormDestLat()-latDisThreshold);
        xMax = Math.max(xMax, pktRequest.getNormDestLat()+latDisThreshold);
        yMin = Math.min(yMin, pktRequest.getNormDestLon()-lonDisThreshold);
        yMax = Math.max(yMax, pktRequest.getNormDestLon()+lonDisThreshold);
        
        Node[] baseQuadTreeNodes = quadTrajTree.getSqTree().searchIntersect(xMin, yMin, xMax, yMax);
        /*
        for (Node n : baseQuadTreeNodes){
            System.out.println(n);
        }
        */
        //List<String> list = Arrays.asList(array);
        
        //List<String> list1 = new ArrayList<String>();
        //Collections.addAll(list1, array);
        return new ArrayList<Node>(Arrays.asList(baseQuadTreeNodes));
    }
    
    public TrajGraph constructTrajGraph_join(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = constructTrajGraph(trajectoryList);
        trajGraph = joinNodesByNearbyKeepersIndexed(trajGraph);
        return trajGraph;
    }
    
    public TrajGraph constructTrajGraph_joinWithTime(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = constructTrajGraphWithTime(trajectoryList);
        trajGraph = joinNodesByNearbyKeepersIndexed(trajGraph);
        return trajGraph;
    }
    
    public TrajGraph constructTrajGraph_joinWithTimeDirectional(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = constructTrajGraphWithTimeDirectional(trajectoryList);
        trajGraph = joinNodesByNearbyKeepersIndexed(trajGraph);
        return trajGraph;
    }
    
    public TrajGraph constructTrajGraph(ArrayList<Trajectory> trajectoryList){
        //return constructDummyTrajGraph(trajectoryList);
        TrajGraph trajGraph = new TrajGraph();
        for (Trajectory trajectory : trajectoryList){
            TrajPoint prevPoint = null;
            String trajId = trajectory.getTrajId();
            Integer stopId = null;
            for (TrajPoint trajPoint : trajectory.getPointList()){
                stopId = trajPoint.getStoppage().getStopId();
                if (stopId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajPoint);
                    System.exit(0);
                }
                if (trajId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajectory);
                    System.exit(0);
                }
                if (prevPoint != null){
                    trajGraph.addTrajIdToStop(prevPoint.getStoppage().getStopId(), trajId);
                    TrajGraphNode fromNode = new TrajGraphNode(prevPoint.getStoppage().getStopId(), trajId);
                    TrajGraphNode toNode = new TrajGraphNode(stopId, trajId);
                    trajGraph.addToList(fromNode, toNode);
                }
                prevPoint = trajPoint;
            }
            if (stopId != null){
                trajGraph.addTrajIdToStop(stopId, trajId);
                trajGraph.addStop(stopId);
                trajGraph.addNode(new TrajGraphNode(stopId, trajId));
            }
        }
        //trajGraph.printStats();
        //trajGraph.printDetails();
        //trajGraph.printNeighborsOfNNodes(10);
        //System.out.println("Trajectory join in TrajGraph...");
        //double fromTime = System.nanoTime();
        trajGraph = joinNodesByKeeper(trajGraph);
        //System.out.println("Time = " + (System.nanoTime() - fromTime)/1e9 + " s");
        //trajGraph.printStats();
        //trajGraph.printDetails();
        //trajGraph.printNeighborsOfNNodes(10);
        //trajGraph.printNStopToTrajIds(100);
        return trajGraph;
    }
    
    public TrajGraph constructTrajGraphWithTime(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = new TrajGraph();
        for (Trajectory trajectory : trajectoryList){
            TrajPoint prevPoint = null;
            String trajId = trajectory.getTrajId();
            Integer stopId = null;
            Long timeInSec = null;
            for (TrajPoint trajPoint : trajectory.getPointList()){
                stopId = trajPoint.getStoppage().getStopId();
                timeInSec = trajPoint.getTimeInSec();
                if (stopId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajPoint);
                    System.exit(0);
                }
                if (trajId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajectory);
                    System.exit(0);
                }
                if (prevPoint != null){
                    trajGraph.addTrajIdToStop(prevPoint.getStoppage().getStopId(), trajId);
                    TrajGraphNode fromNode = new TrajGraphNode(prevPoint.getStoppage().getStopId(), trajId, prevPoint.getTimeInSec());
                    TrajGraphNode toNode = new TrajGraphNode(stopId, trajId, timeInSec);
                    trajGraph.addToList(fromNode, toNode);
                }
                prevPoint = trajPoint;
            }
            if (stopId != null){
                trajGraph.addTrajIdToStop(stopId, trajId);
                trajGraph.addStop(stopId);
                trajGraph.addNode(new TrajGraphNode(stopId, trajId, timeInSec));
            }
        }
        trajGraph = joinNodesByKeeper(trajGraph);
        return trajGraph;
    }
    
    public TrajGraph constructTrajGraphWithTimeDirectional(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = new TrajGraph();
        for (Trajectory trajectory : trajectoryList){
            TrajPoint prevPoint = null;
            String trajId = trajectory.getTrajId();
            Integer stopId = null;
            Long timeInSec = null;
            for (TrajPoint trajPoint : trajectory.getPointList()){
                stopId = trajPoint.getStoppage().getStopId();
                timeInSec = trajPoint.getTimeInSec();
                if (stopId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajPoint);
                    System.exit(0);
                }
                if (trajId == null){
                    // should never reach here
                    System.out.println("Terminating abruptly...");
                    System.out.println(trajectory);
                    System.exit(0);
                }
                if (prevPoint != null){
                    trajGraph.addTrajIdToStop(prevPoint.getStoppage().getStopId(), trajId);
                    TrajGraphNode fromNode = new TrajGraphNode(prevPoint.getStoppage().getStopId(), trajId, prevPoint.getTimeInSec());
                    TrajGraphNode toNode = new TrajGraphNode(stopId, trajId, timeInSec);
                    if (fromNode.getTimeInSec() < timeInSec){
                        trajGraph.addToList(fromNode, toNode);
                    }
                    else{
                        System.out.println("Temporal anomaly!! " + fromNode + " - " + toNode);
                    }
                }
                prevPoint = trajPoint;
            }
            if (stopId != null){
                trajGraph.addTrajIdToStop(stopId, trajId);
                trajGraph.addStop(stopId);
                trajGraph.addNode(new TrajGraphNode(stopId, trajId, timeInSec));
            }
        }
        trajGraph = joinNodesByKeeper(trajGraph);
        return trajGraph;
    }
    
    public TrajGraph constructDummyTrajGraph(ArrayList<Trajectory> trajectoryList){
        TrajGraph trajGraph = new TrajGraph();
        String []trajIds = {"black", "blue", "red", "green"};
        int [][]stopIds = {{1,2,3,4},{5,6,3,7},{10,6,11},{8,3,9}};    // test case 1
        //int [][]stopIds = {{1,2,3},{5,6,3,7},{10,6,11},{8,3,9}};    // test case 2
        //int [][]stopIds = {{1,2,3},{5,6,7},{10,6,11},{8,3,9}};    // test case 3
        String trajId;
        int prevStopId, stopId;
        for (int i=0; i<trajIds.length; i++){
            trajId = trajIds[i];
            stopId = -1;
            for (int j=1; j<stopIds[i].length; j++){
                prevStopId = stopIds[i][j-1];
                stopId = stopIds[i][j];
                trajGraph.addTrajIdToStop(prevStopId, trajId);
                TrajGraphNode fromNode = new TrajGraphNode(prevStopId, trajId);
                TrajGraphNode toNode = new TrajGraphNode(stopId, trajId);
                trajGraph.addToList(fromNode, toNode);
            }
            if (stopId != -1){
                trajGraph.addTrajIdToStop(stopId, trajId);
                trajGraph.addStop(stopId);
                trajGraph.addNode(new TrajGraphNode(stopId, trajId));
            }
        }
        trajGraph.printStats();
        trajGraph.printDetails();
        //trajGraph.printTraversalOfNNodes(100);
        //System.out.println("Trajectory join in TrajGraph...");
        //double fromTime = System.nanoTime();
        trajGraph = joinNodesByKeeper(trajGraph);
        //System.out.println("Time = " + (System.nanoTime() - fromTime)/1e9 + " s");
        trajGraph.printStats();
        trajGraph.printDetails();
        //trajGraph.printNeighborsOfNNodes(100);
        //trajGraph.printNStopToTrajIds(100);
        return trajGraph;
    }
    
    TrajGraph joinNodesByKeeper(TrajGraph trajGraph){
        int newEdges = 0;
        HashMap<Integer, HashSet<String>> stopToTrajIdMap = trajGraph.getStopToTrajIdMap();
        for (HashMap.Entry<Integer, HashSet<String>> stopToTrajsEntry : stopToTrajIdMap.entrySet()){
            int stopId = stopToTrajsEntry.getKey();
            // check if stopId is among the keepers, otherwise we should not join
            if (!normalizedKeeperIds.contains(stopId)) continue;
            HashSet <String> stopToTrajs = stopToTrajsEntry.getValue();
            if (stopToTrajs.size() < 2) continue;
            String keeperId = "K-"+stopId;
            //System.out.println("Keeper K-" + stopId + " added");
            TrajGraphNode node1 = new TrajGraphNode(stopId, keeperId, true);
            for (String trajId : stopToTrajs){
                TrajGraphNode node2 = new TrajGraphNode(stopId, trajId);
                trajGraph.addToList(node1, node2);
                trajGraph.addToList(node2, node1);
                newEdges += 2;
            }
            trajGraph.addTrajIdToStop(stopId, keeperId);
        }
        //System.out.println("New edges = " + newEdges);
        return trajGraph;
    }
    
    TrajGraph joinNodesByNearbyKeepers(TrajGraph trajGraph){
        int newEdges = 0;
        HashMap<Integer, HashSet<String>> stopToTrajIdMap = trajGraph.getStopToTrajIdMap();
        HashMap<Integer, javafx.util.Pair<Double, Double>> stoppageMap = trajProcessor.getStoppageMap();
        for (Integer stop1 : stopToTrajIdMap.keySet()){
            for (Integer stop2 : stopToTrajIdMap.keySet()){
                if (stop1 == stop2) continue;
                //if (!normalizedKeeperIds.contains(stop1) && !normalizedKeeperIds.contains(stop2)) continue;
                double lat1 = stoppageMap.get(stop1).getKey();
                double lon1 = stoppageMap.get(stop1).getValue();
                double lat2 = stoppageMap.get(stop2).getKey();
                double lon2 = stoppageMap.get(stop2).getValue();
                double stopDis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
                if (stopDis > detourDistanceThreshold) continue;
                String keeper1 = "K-"+stop1;
                String keeper2 = "K-"+stop2;
                //System.out.println("Keeper K-" + stopId + " added");
                TrajGraphNode node1 = new TrajGraphNode(stop1, keeper1, true);
                TrajGraphNode node2 = new TrajGraphNode(stop2, keeper2, true);
                
                // both are keepers
                if (normalizedKeeperIds.contains(stop1) && normalizedKeeperIds.contains(stop2)){
                    trajGraph.addToList(node1, node2);
                    trajGraph.addToList(node2, node1);
                    newEdges += 2;
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                    trajGraph.addTrajIdToStop(stop2, keeper2);
                }
                // only stop1 is keeper, stop2 is not
                else if (normalizedKeeperIds.contains(stop1)){
                    HashSet<String> trajIds = trajGraph.getStopToTrajIds(stop2);
                    for (String trajId : trajIds){
                        TrajGraphNode node3 = new TrajGraphNode(stop2, trajId);
                        trajGraph.addToList(node1, node3);
                        trajGraph.addToList(node3, node1);
                        newEdges += 2;
                    }
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                }
                // only stop2 is keeper, stop1 is not
                else if (normalizedKeeperIds.contains(stop2)){
                    HashSet<String> trajIds = trajGraph.getStopToTrajIds(stop1);
                    for (String trajId : trajIds){
                        TrajGraphNode node3 = new TrajGraphNode(stop1, trajId);
                        trajGraph.addToList(node2, node3);
                        trajGraph.addToList(node3, node2);
                        newEdges += 2;
                    }
                    trajGraph.addTrajIdToStop(stop2, keeper2);
                }
                else{
                    // do nothing
                }
            }
        }
        //System.out.println("New edges = " + newEdges);
        //return trajGraph;
        return null;
    }
    
    TrajGraph joinNodesByNearbyKeepersReduced(TrajGraph trajGraph){
        int newEdges = 0;
        HashMap<Integer, HashSet<String>> stopToTrajIdMap = trajGraph.getStopToTrajIdMap();
        HashMap<Integer, javafx.util.Pair<Double, Double>> stoppageMap = trajProcessor.getStoppageMap();
        HashSet<Integer> stopToTrajIdMapKeySet = new HashSet<Integer>(stopToTrajIdMap.keySet());
        for (Integer stop1 : normalizedKeeperIds){
            for (Integer stop2 : stopToTrajIdMapKeySet){
                if (stop1 == stop2) continue;
                //if (!normalizedKeeperIds.contains(stop1) && !normalizedKeeperIds.contains(stop2)) continue;
                double lat1 = stoppageMap.get(stop1).getKey();
                double lon1 = stoppageMap.get(stop1).getValue();
                double lat2 = stoppageMap.get(stop2).getKey();
                double lon2 = stoppageMap.get(stop2).getValue();
                double stopDis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
                if (stopDis > detourDistanceThreshold) continue;
                String keeper1 = "K-"+stop1;
                String keeper2 = "K-"+stop2;
                //System.out.println("Keeper K-" + stopId + " added");
                TrajGraphNode node1 = new TrajGraphNode(stop1, keeper1, true);
                TrajGraphNode node2 = new TrajGraphNode(stop2, keeper2, true);
                
                // both are keepers
                if (normalizedKeeperIds.contains(stop2)){
                    trajGraph.addToList(node1, node2);
                    trajGraph.addToList(node2, node1);
                    newEdges += 2;
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                    trajGraph.addTrajIdToStop(stop2, keeper2);
                }
                // only stop1 is keeper, stop2 is not
                else {
                    HashSet<String> trajIds = trajGraph.getStopToTrajIds(stop2);
                    for (String trajId : trajIds){
                        TrajGraphNode node3 = new TrajGraphNode(stop2, trajId);
                        trajGraph.addToList(node1, node3);
                        trajGraph.addToList(node3, node1);
                        newEdges += 2;
                    }
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                }
            }
        }
        //System.out.println("New edges = " + newEdges);
        return trajGraph;
    }
    
    TrajGraph joinNodesByNearbyKeepersIndexed(TrajGraph trajGraph){
        int newEdges = 0;
        HashMap<Integer, HashSet<String>> stopToTrajIdMap = trajGraph.getStopToTrajIdMap();
        HashMap<Integer, javafx.util.Pair<Double, Double>> stoppageMap = trajProcessor.getStoppageMap();
        HashMap<Integer, javafx.util.Pair<Double, Double>> normalizedStoppageMap = trajProcessor.getNormalizedStoppageMap();
        QuadTree stoppageQuadTree = quadTrajTree.getStoppageQuadTree();
        HashSet<Integer> stopToTrajIdMapKeySet = new HashSet<Integer>(stopToTrajIdMap.keySet());
        for (Integer stop1 : normalizedKeeperIds){
            double normalizedLat = normalizedStoppageMap.get(stop1).getKey();
            double normalizedLon = normalizedStoppageMap.get(stop1).getValue();
            double xMin = normalizedLat - latDisThreshold;
            double xMax = normalizedLat + latDisThreshold;
            double yMin = normalizedLon - lonDisThreshold;
            double yMax = normalizedLon + lonDisThreshold;
            ArrayList<Object> stop2IdObjs = stoppageQuadTree.getPointTrajIdsInRange(xMin, yMin, xMax, yMax);
            for (Object stop2Obj : stop2IdObjs){
                if (stop2Obj == null) continue;
                int stop2 = (Integer) stop2Obj; 
                if (stop1 == stop2) continue;
                //if (!normalizedKeeperIds.contains(stop1) && !normalizedKeeperIds.contains(stop2)) continue;
                double lat1 = stoppageMap.get(stop1).getKey();
                double lon1 = stoppageMap.get(stop1).getValue();
                double lat2 = stoppageMap.get(stop2).getKey();
                double lon2 = stoppageMap.get(stop2).getValue();
                double stopDis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
                if (stopDis > detourDistanceThreshold) continue;
                String keeper1 = "K-"+stop1;
                String keeper2 = "K-"+stop2;
                //System.out.println("Keeper K-" + stopId + " added");
                TrajGraphNode node1 = new TrajGraphNode(stop1, keeper1, true);
                TrajGraphNode node2 = new TrajGraphNode(stop2, keeper2, true);
                
                // both are keepers
                if (normalizedKeeperIds.contains(stop2)){
                    trajGraph.addToList(node1, node2);
                    trajGraph.addToList(node2, node1);
                    newEdges += 2;
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                    trajGraph.addTrajIdToStop(stop2, keeper2);
                }
                // only stop1 is keeper, stop2 is not
                else {
                    HashSet<String> trajIds = trajGraph.getStopToTrajIds(stop2);
                    for (String trajId : trajIds){
                        TrajGraphNode node3 = new TrajGraphNode(stop2, trajId);
                        trajGraph.addToList(node1, node3);
                        trajGraph.addToList(node3, node1);
                        newEdges += 2;
                    }
                    trajGraph.addTrajIdToStop(stop1, keeper1);
                }
            }
        }
        //System.out.println("New edges = " + newEdges);
        return trajGraph;
    }
    
    ArrayList<TrajGraphNode> traverseToDeliver(TrajGraph trajGraph, PacketRequest pktRequest){
        
        int srcStopId = pktRequest.getSrcId();
        int destStopId = pktRequest.getDestId();
        PriorityQueue<NodeState> explorableNodes = new PriorityQueue<>();
        HashMap <NodeState, NodeState> parentMap = new HashMap<>();
        HashSet <TrajGraphNode> colorSet = new HashSet<>();
        HashMap <TrajGraphNode, NodeState> enqueuedState = new HashMap<>();
        // construct TrajGraphNodes with stopId, trajId (to be obtained from stopToTraj HashMap)
        // enqueue all those nodes (temporal and other processing based pruning) to be done later
        //System.out.println("src stop id = " + srcStopId + ", dest stop id = " + destStopId);
        HashSet <String> stopTrajIds = trajGraph.getStopToTrajIds(srcStopId);
        if (stopTrajIds == null){
            //System.out.println("No trajs from stop id " + srcStopId);
            stopTrajIds = new HashSet<>();
        }
        //System.out.println("# of traj from src stop id = " + stopTrajIds.size());
        for (String trajId : stopTrajIds){
            TrajGraphNode srcNode = new TrajGraphNode(srcStopId, trajId);
            double gCost = 0;
            //double hCost = computeHeuristicCost(srcStopId, destStopId, distanceUnit);
            double hCost = 0;
            double cost = gCost + hCost;
            // cost of a src state is 0 and parent state of it is null
            if (enqueuedState.containsKey(srcNode) && cost >= enqueuedState.get(srcNode).getCost()){
                // do nothing since a lower cost state for this node is already enqueued
            }
            else{
                NodeState srcState = new NodeState(srcNode, gCost, hCost, null);
                if (enqueuedState.containsKey(srcNode)){
                    NodeState oldState = enqueuedState.get(srcNode);
                    NodeState oldParentState = oldState.getParentState();
                    NodeState newParentState = srcState.getParentState();
                    TrajGraphNode oldParentNode, newParentNode;
                    if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                    else oldParentNode = oldParentState.getTrajGraphNode();
                    if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                    else newParentNode = newParentState.getTrajGraphNode();
                    System.out.print("Node <" + srcNode.getStopId() + "," + srcNode.getTrajId() + ">, Parent = <");
                    System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed ");
                    System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + srcState.getCost() + " added ");
                    explorableNodes.remove(oldState);
                }
                enqueuedState.put(srcNode, srcState);
                explorableNodes.add(srcState);
            }
        }

        NodeState curNodeState = null;
        while(!explorableNodes.isEmpty()) {
            curNodeState = explorableNodes.poll();
            TrajGraphNode curNode = curNodeState.getTrajGraphNode();
            parentMap.put(curNodeState, curNodeState.getParentState());
            colorSet.add(curNode);
            if (curNode.getStopId() == destStopId) break;
            ArrayList <TrajGraphNode> neighbors = trajGraph.getNeighbors(curNode);
            //System.out.println("Neighbors of <" + curNode.getStopId() + "," + curNode.getTrajId() + "> : Size = " + neighbors.size());
            for (TrajGraphNode node : neighbors){
                //System.out.print(node.getStopId() + " , ");
                if (colorSet.contains(node)) continue;
                double gCost = computeIncurredCost(curNodeState, node);
                double hCost = 0;
                double cost = gCost + hCost;
                if (enqueuedState.containsKey(node) && cost >= enqueuedState.get(node).getCost()){
                    // do nothing since a lower cost state for this node is already enqueued
                }
                else{
                    NodeState neighborNodeState = new NodeState(node, gCost, hCost, curNodeState);
                    // do we need to remove the previous nodestate with higher cost?
                    // that may be a bit difficult to do, but trying in the following line
                    if (enqueuedState.containsKey(node)){
                        NodeState oldState = enqueuedState.get(node);
                        NodeState oldParentState = oldState.getParentState();
                        NodeState newParentState = neighborNodeState.getParentState();
                        TrajGraphNode oldParentNode, newParentNode;
                        if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                        else oldParentNode = oldParentState.getTrajGraphNode();
                        if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                        else newParentNode = newParentState.getTrajGraphNode();
                        System.out.print("Node <" + node.getStopId() + "," + node.getTrajId() + ">, Parent = <");
                        System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed and Parent = <");
                        System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + neighborNodeState.getCost() + " added ");
                        explorableNodes.remove(oldState);
                    }
                    enqueuedState.put(node, neighborNodeState);
                    explorableNodes.add(neighborNodeState);
                    //System.out.println("Enqueued : " + neighborNodeState.getTrajGraphNode().getStopId());
                }
            }
            //System.out.println("");
        }
        
        ArrayList<TrajGraphNode> bestDeliverers = new ArrayList<>();
        
        double cost;
        if (curNodeState == null || curNodeState.getTrajGraphNode().getStopId() != destStopId){
            cost = Double.MAX_VALUE;
        }
        else{
            cost = curNodeState.getCost();
            while(curNodeState != null){
                if (!parentMap.containsKey(curNodeState)){
                    System.out.println("Alert : Unreachable i.e. cannot be delivered");
                    break;
                }
                bestDeliverers.add(0, curNodeState.getTrajGraphNode());
                curNodeState = parentMap.get(curNodeState);
            }
        }
        if (bestDeliverers.isEmpty()) System.out.print("false\t" + cost + "\t");
        else System.out.print("true\t" + cost + "\t");
        
        return bestDeliverers;
    }
    
    ArrayList<TrajGraphNode> traverseToDeliver_DistanceCost(TrajGraph trajGraph, PacketRequest pktRequest){
        
        int srcStopId = pktRequest.getSrcId();
        int destStopId = pktRequest.getDestId();
        PriorityQueue<NodeState> explorableNodes = new PriorityQueue<>();
        HashMap <NodeState, NodeState> parentMap = new HashMap<>();
        HashSet <TrajGraphNode> colorSet = new HashSet<>();
        HashMap <TrajGraphNode, NodeState> enqueuedState = new HashMap<>();
        // construct TrajGraphNodes with stopId, trajId (to be obtained from stopToTraj HashMap)
        // enqueue all those nodes (temporal and other processing based pruning) to be done later
        //System.out.println("src stop id = " + srcStopId + ", dest stop id = " + destStopId);
        HashSet <String> stopTrajIds = trajGraph.getStopToTrajIds(srcStopId);
        if (stopTrajIds == null){
            //System.out.println("No trajs from stop id " + srcStopId);
            stopTrajIds = new HashSet<>();
        }
        //System.out.println("# of traj from src stop id = " + stopTrajIds.size());
        for (String trajId : stopTrajIds){
            TrajGraphNode srcNode = new TrajGraphNode(srcStopId, trajId);
            double gCost = 0;
            //double hCost = computeHeuristicCost(srcStopId, destStopId, distanceUnit);
            double hCost = 0;
            double cost = gCost + hCost;
            // cost of a src state is 0 and parent state of it is null
            if (enqueuedState.containsKey(srcNode) && cost >= enqueuedState.get(srcNode).getCost()){
                // do nothing since a lower cost state for this node is already enqueued
            }
            else{
                NodeState srcState = new NodeState(srcNode, gCost, hCost, null);
                if (enqueuedState.containsKey(srcNode)){
                    NodeState oldState = enqueuedState.get(srcNode);
                    NodeState oldParentState = oldState.getParentState();
                    NodeState newParentState = srcState.getParentState();
                    TrajGraphNode oldParentNode, newParentNode;
                    if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                    else oldParentNode = oldParentState.getTrajGraphNode();
                    if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                    else newParentNode = newParentState.getTrajGraphNode();
                    System.out.print("Node <" + srcNode.getStopId() + "," + srcNode.getTrajId() + ">, Parent = <");
                    System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed ");
                    System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + srcState.getCost() + " added ");
                    explorableNodes.remove(oldState);
                }
                enqueuedState.put(srcNode, srcState);
                explorableNodes.add(srcState);
            }
        }

        NodeState curNodeState = null;
        while(!explorableNodes.isEmpty()) {
            curNodeState = explorableNodes.poll();
            TrajGraphNode curNode = curNodeState.getTrajGraphNode();
            parentMap.put(curNodeState, curNodeState.getParentState());
            colorSet.add(curNode);
            if (curNode.getStopId() == destStopId) break;
            ArrayList <TrajGraphNode> neighbors = trajGraph.getNeighbors(curNode);
            //System.out.println("Neighbors of <" + curNode.getStopId() + "," + curNode.getTrajId() + "> : Size = " + neighbors.size());
            for (TrajGraphNode node : neighbors){
                //System.out.print(node.getStopId() + " , ");
                if (colorSet.contains(node)) continue;
                double gCost = computeIncurredCostByDistance(curNodeState, node, distanceUnit); 
                double hCost = 0;
                double cost = gCost + hCost;
                if (enqueuedState.containsKey(node) && cost >= enqueuedState.get(node).getCost()){
                    // do nothing since a lower cost state for this node is already enqueued
                }
                else{
                    NodeState neighborNodeState = new NodeState(node, gCost, hCost, curNodeState);
                    // do we need to remove the previous nodestate with higher cost?
                    // that may be a bit difficult to do, but trying in the following line
                    if (enqueuedState.containsKey(node)){
                        NodeState oldState = enqueuedState.get(node);
                        NodeState oldParentState = oldState.getParentState();
                        NodeState newParentState = neighborNodeState.getParentState();
                        TrajGraphNode oldParentNode, newParentNode;
                        if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                        else oldParentNode = oldParentState.getTrajGraphNode();
                        if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                        else newParentNode = newParentState.getTrajGraphNode();
                        //System.out.print("Node <" + node.getStopId() + "," + node.getTrajId() + ">, Parent = <");
                        //System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed and Parent = <");
                        //System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + neighborNodeState.getCost() + " added ");
                        explorableNodes.remove(oldState);
                    }
                    enqueuedState.put(node, neighborNodeState);
                    explorableNodes.add(neighborNodeState);
                    //System.out.println("Enqueued : " + neighborNodeState.getTrajGraphNode().getStopId());
                }
            }
            //System.out.println("");
        }
        
        ArrayList<TrajGraphNode> bestDeliverers = new ArrayList<>();
        
        double cost;
        if (curNodeState == null || curNodeState.getTrajGraphNode().getStopId() != destStopId){
            cost = Double.MAX_VALUE;
        }
        else{
            cost = curNodeState.getCost();
            while(curNodeState != null){
                if (!parentMap.containsKey(curNodeState)){
                    System.out.println("Alert : Unreachable i.e. cannot be delivered");
                    break;
                }
                bestDeliverers.add(0, curNodeState.getTrajGraphNode());
                curNodeState = parentMap.get(curNodeState);
            }
        }
        if (bestDeliverers.isEmpty()) System.out.print(cost + "\t");
        else System.out.print(cost + "\t");
        
        return bestDeliverers;
    }
    
    ArrayList<TrajGraphNode> traverseToDeliver_AStar(TrajGraph trajGraph, PacketRequest pktRequest){
        
        int srcStopId = pktRequest.getSrcId();
        int destStopId = pktRequest.getDestId();
        PriorityQueue<NodeState> explorableNodes = new PriorityQueue<>();
        HashMap <NodeState, NodeState> parentMap = new HashMap<>();
        HashSet <TrajGraphNode> colorSet = new HashSet<>();
        HashMap <TrajGraphNode, NodeState> enqueuedState = new HashMap<>();
        // construct TrajGraphNodes with stopId, trajId (to be obtained from stopToTraj HashMap)
        // enqueue all those nodes (temporal and other processing based pruning) to be done later
        //System.out.println("src stop id = " + srcStopId + ", dest stop id = " + destStopId);
        HashSet <String> stopTrajIds = trajGraph.getStopToTrajIds(srcStopId);
        if (stopTrajIds == null){
            //System.out.println("No trajs from stop id " + srcStopId);
            stopTrajIds = new HashSet<>();
        }
        //System.out.println("# of traj from src stop id = " + stopTrajIds.size());
        for (String trajId : stopTrajIds){
            TrajGraphNode srcNode = new TrajGraphNode(srcStopId, trajId);
            double gCost = 0;
            double hCost = computeHeuristicCost(srcStopId, destStopId, distanceUnit);
            //double hCost = 0;
            double cost = gCost + hCost;
            // cost of a src state is 0 and parent state of it is null
            if (enqueuedState.containsKey(srcNode) && cost >= enqueuedState.get(srcNode).getCost()){
                // do nothing since a lower cost state for this node is already enqueued
            }
            else{
                NodeState srcState = new NodeState(srcNode, gCost, hCost, null);
                if (enqueuedState.containsKey(srcNode)){
                    NodeState oldState = enqueuedState.get(srcNode);
                    NodeState oldParentState = oldState.getParentState();
                    NodeState newParentState = srcState.getParentState();
                    TrajGraphNode oldParentNode, newParentNode;
                    if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                    else oldParentNode = oldParentState.getTrajGraphNode();
                    if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                    else newParentNode = newParentState.getTrajGraphNode();
                    System.out.print("Node <" + srcNode.getStopId() + "," + srcNode.getTrajId() + ">, Parent = <");
                    System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed ");
                    System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + srcState.getCost() + " added ");
                    explorableNodes.remove(oldState);
                }
                enqueuedState.put(srcNode, srcState);
                explorableNodes.add(srcState);
            }
        }

        NodeState curNodeState = null;
        while(!explorableNodes.isEmpty()) {
            curNodeState = explorableNodes.poll();
            TrajGraphNode curNode = curNodeState.getTrajGraphNode();
            parentMap.put(curNodeState, curNodeState.getParentState());
            colorSet.add(curNode);
            if (curNode.getStopId() == destStopId) break;
            ArrayList <TrajGraphNode> neighbors = trajGraph.getNeighbors(curNode);
            //System.out.println("Neighbors of <" + curNode.getStopId() + "," + curNode.getTrajId() + "> : Size = " + neighbors.size());
            for (TrajGraphNode node : neighbors){
                //System.out.print(node.getStopId() + " , ");
                if (colorSet.contains(node)) continue;
                double gCost = computeIncurredCostByDistance(curNodeState, node, distanceUnit);
                double hCost = computeHeuristicCost(node.getStopId(), destStopId, distanceUnit);
                double cost = gCost + hCost;
                if (enqueuedState.containsKey(node) && cost >= enqueuedState.get(node).getCost()){
                    // do nothing since a lower cost state for this node is already enqueued
                }
                else{
                    NodeState neighborNodeState = new NodeState(node, gCost, hCost, curNodeState);
                    // do we need to remove the previous nodestate with higher cost?
                    // that may be a bit difficult to do, but trying in the following line
                    if (enqueuedState.containsKey(node)){
                        NodeState oldState = enqueuedState.get(node);
                        NodeState oldParentState = oldState.getParentState();
                        NodeState newParentState = neighborNodeState.getParentState();
                        TrajGraphNode oldParentNode, newParentNode;
                        if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                        else oldParentNode = oldParentState.getTrajGraphNode();
                        if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                        else newParentNode = newParentState.getTrajGraphNode();
                        //System.out.print("Node <" + node.getStopId() + "," + node.getTrajId() + ">, Parent = <");
                        //System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed and Parent = <");
                        //System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + neighborNodeState.getCost() + " added ");
                        explorableNodes.remove(oldState);
                    }
                    enqueuedState.put(node, neighborNodeState);
                    explorableNodes.add(neighborNodeState);
                    //System.out.println("Enqueued : " + neighborNodeState.getTrajGraphNode().getStopId());
                }
            }
            //System.out.println("");
        }
        
        ArrayList<TrajGraphNode> bestDeliverers = new ArrayList<>();
        
        double cost;
        long duration;
        if (curNodeState == null || curNodeState.getTrajGraphNode().getStopId() != destStopId){
            cost = Double.MAX_VALUE;
            duration = -1;
        }
        else{
            cost = curNodeState.getCost();
            duration = curNodeState.getTrajGraphNode().getTimeInSec() - curNodeState.getDeliveryStartTimeInSec();
            while(curNodeState != null){
                if (!parentMap.containsKey(curNodeState)){
                    System.out.println("Alert : Unreachable i.e. cannot be delivered");
                    break;
                }
                bestDeliverers.add(0, curNodeState.getTrajGraphNode());
                curNodeState = parentMap.get(curNodeState);
            }
        }
        /*
        if (bestDeliverers.isEmpty()) System.out.print(cost + "\t");
        else System.out.print(cost + "\t");
        */
        this.deliveryCost = cost;
        this.deliveryTimeInSec = duration;
        return bestDeliverers;
    }
    
    double computeIncurredCost(NodeState edgeFromNodeState, TrajGraphNode edgeToNode){
        double gCost = edgeFromNodeState.getGCost() + 1;
        return gCost;
    }
    
    double computeIncurredCostByDistance(NodeState edgeFromNodeState, TrajGraphNode edgeToNode, String distanceUnit){
        double gCost = edgeFromNodeState.getGCost();
        int fromStop = edgeFromNodeState.getTrajGraphNode().getStopId();
        int toStop = edgeToNode.getStopId();
        double additionalCost = 0;
        double detourOverheadCost = 0;
        
        double lat1 = trajProcessor.getStoppageMap().get(fromStop).getKey();
        double lon1 = trajProcessor.getStoppageMap().get(fromStop).getValue();
        double lat2 = trajProcessor.getStoppageMap().get(toStop).getKey();
        double lon2 = trajProcessor.getStoppageMap().get(toStop).getValue();
        additionalCost = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
        if (fromStop == toStop){
            detourOverheadCost += 0;    // ignoring detour overhead in case of handover at the same node
        }
        else if (!edgeFromNodeState.getTrajGraphNode().getTrajId().equals(edgeToNode.getTrajId())
                && edgeFromNodeState.getTrajGraphNode().getIsKeeper() && edgeToNode.getIsKeeper()){
            // simple assumption : the carrier has to travel back to her path
            detourOverheadCost = additionalCost;
        }
        gCost = gCost + additionalCost + detourOverheadCost;
        return gCost;
    }
    
    private double computeHeuristicCost(int fromStop, int toStop, String distanceUnit) {
        double lat1 = trajProcessor.getStoppageMap().get(fromStop).getKey();
        double lon1 = trajProcessor.getStoppageMap().get(fromStop).getValue();
        double lat2 = trajProcessor.getStoppageMap().get(toStop).getKey();
        double lon2 = trajProcessor.getStoppageMap().get(toStop).getValue();
        return distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit); // hCost
    }
    // find the best deliverers only for now
    public ArrayList<TrajGraphNode> deliverPacket(PacketRequest pktRequest){
        // the following one is giving many overlapping nodes (added during enqueue)
        //ArrayList<Node> summaryNodes = retrieveSummaryNodes(pktRequest);
        //ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        ArrayList<Trajectory> reducedTrajs = new ArrayList<>();
        TrajGraph trajGraph = constructTrajGraph(reducedTrajs);
        ArrayList<TrajGraphNode> bestDeliveres = traverseToDeliver(trajGraph, pktRequest);
        return bestDeliveres;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketOriginal(PacketRequest pktRequest){
        // the following ones is giving a bit fewer overlapping nodes (added during dequeue)
        //ArrayList<Node> summaryNodes = retrieveSummaryNodesOriginal(pktRequest);
        //ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        ArrayList<Trajectory> reducedTrajs = new ArrayList<>();
        TrajGraph trajGraph = constructTrajGraph(reducedTrajs);
        ArrayList<TrajGraphNode> bestDeliveres = traverseToDeliver(trajGraph, pktRequest);
        return bestDeliveres;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketModified(PacketDeliveryQuery pktDeliveryQuery){
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();        
        // the following ones is giving a the fewest overlapping nodes (backtracked from overlapping nodes)
        ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        
        trajGraph = constructTrajGraph(reducedTrajs);
        //ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        //TrajGraph trajGraph = constructTrajGraph(allTrajs); // correct later with reducedTrajs
        //bestDeliverers = traverseToDeliver(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_DistanceCost(trajGraph, pktRequest);
        bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        long algoEndTime = System.nanoTime();
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        boolean isDelivered = !bestDeliverers.isEmpty();
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + this.diskBlockSet.size() + "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketModifiedWithJoin(PacketDeliveryQuery pktDeliveryQuery){
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();
        ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        long algoEndTime = System.nanoTime();
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("\n--##--%%--$$--%%--##--");
        //System.out.println("Summary node overlap processing : " + runtimeInSec + " sec");
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        //algoEndTime = System.nanoTime();
        //runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("Trajectory retrival : " + runtimeInSec + " sec");
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        trajGraph = constructTrajGraph_join(reducedTrajs);
        //algoEndTime = System.nanoTime();
        //runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("TrajGraph Construction with Join : " + runtimeInSec + " sec");
        //ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        //TrajGraph trajGraph = constructTrajGraph(allTrajs); // correct later with reducedTrajs
        //bestDeliverers = traverseToDeliver(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_DistanceCost(trajGraph, pktRequest);
        bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        algoEndTime = System.nanoTime();
        runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("Shortest path finding : " + runtimeInSec + " sec");
        //System.out.println("--##--%%--$$--%%--##--");
        boolean isDelivered = !bestDeliverers.isEmpty();
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + this.diskBlockSet.size() + "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        traverseToDeliver(trajGraph, pktRequest);
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketBaselineWithJoin(PacketDeliveryQuery pktDeliveryQuery){
        int io = 0;
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();
        //ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        ArrayList<Node> rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
        // a single node should suffice, but trying to reuse previously written function
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        trajGraph = constructTrajGraph_join(reducedTrajs);
        //ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        //TrajGraph trajGraph = constructTrajGraph(allTrajs); // correct later with reducedTrajs
        //bestDeliverers = traverseToDeliver(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_DistanceCost(trajGraph, pktRequest);
        bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        long algoEndTime = System.nanoTime();
        io = diskBlockSet.size();
        boolean isDelivered = !bestDeliverers.isEmpty();
        if (isDelivered){
            // using lat, lon distance threshold to increase range query rectangle size
            double additionalDistance = this.deliveryCost - pktDeliveryQuery.getDistance();
            double additionalLatDis = distanceConverter.getLatProximity(additionalDistance, distanceUnit);
            double additionalLonDis = distanceConverter.getLonProximity(additionalDistance, distanceUnit);
            latDisThreshold += 0.5*additionalLatDis;
            latDisThreshold += 0.5*additionalLonDis;
            
            // reconstructing trajGraph
            this.deliveryCost = Double.MAX_VALUE;
            this.deliveryTimeInSec = -1;
            this.diskBlockSet.clear();
            rangeQueryNodes.clear();
            rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
            reducedTrajs.clear();
            reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
            trajsRetrieved = reducedTrajs.size();
            trajGraph = null;
            trajGraph = constructTrajGraph_join(reducedTrajs);
            bestDeliverers.clear();
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
            algoEndTime = System.nanoTime();
            io = diskBlockSet.size();
            // revert
            latDisThreshold -= 0.5*additionalLatDis;
            latDisThreshold -= 0.5*additionalLonDis;
        }
        else{
            // trying to increase range query bounds incrementally in case of unsuccessful delivery
            double pktDistance = pktDeliveryQuery.getDistance();
            for(int i=0; i<3 && !isDelivered; i++){
                this.deliveryCost = Double.MAX_VALUE;
                this.deliveryTimeInSec = -1;
                this.diskBlockSet.clear();
                double additionalDistance = pktDistance*(1<<i);
                double additionalLatDis = distanceConverter.getLatProximity(additionalDistance, distanceUnit);
                double additionalLonDis = distanceConverter.getLonProximity(additionalDistance, distanceUnit);
                latDisThreshold += 0.5*additionalLatDis;
                latDisThreshold += 0.5*additionalLonDis;
                // reconstructing trajGraph
                rangeQueryNodes.clear();
                rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
                reducedTrajs.clear();
                reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
                trajsRetrieved = reducedTrajs.size();
                trajGraph = null;
                trajGraph = constructTrajGraph_join(reducedTrajs);
                bestDeliverers.clear();
                bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                algoEndTime = System.nanoTime();
                io = diskBlockSet.size();
                isDelivered = !bestDeliverers.isEmpty();
                // revert
                latDisThreshold -= 0.5*additionalLatDis;
                latDisThreshold -= 0.5*additionalLonDis;
                io = diskBlockSet.size();
            }
            if (!isDelivered){
                // reconstructing trajGraph
                this.deliveryCost = Double.MAX_VALUE;
                this.deliveryTimeInSec = -1;
                this.diskBlockSet.clear();
                reducedTrajs.clear();
                reducedTrajs = trajStorage.getTrajDataAsList();
                trajsRetrieved = reducedTrajs.size();
                // assumption : 32 trajs in each block (each traj size around 128B, block size 4k => trajs per block = 32)
                trajGraph = null;
                trajGraph = constructTrajGraph_join(reducedTrajs);
                bestDeliverers.clear();
                bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                algoEndTime = System.nanoTime();
                isDelivered = !bestDeliverers.isEmpty();
                
                /* the following segment is to get insight of number of quadtree nodes and points in them (in case of same location, node capacity is exceeded)
                HashMap<Long,Integer> diskBlockAccessMap = new HashMap<>();
                for (Trajectory traj : reducedTrajs){
                    String trajId=  traj.getTrajId();
                    // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                    long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                    // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                    if (!diskBlockAccessMap.containsKey(quadtreeBasedDiskBlockId)){
                        diskBlockAccessMap.put(quadtreeBasedDiskBlockId, 0);
                    }
                    diskBlockAccessMap.put(quadtreeBasedDiskBlockId, diskBlockAccessMap.get(quadtreeBasedDiskBlockId)+1);
                }
                long maxQuadtreeBlockId = Long.MIN_VALUE;
                int maxQuadtreeBlockPoints = Integer.MIN_VALUE;
                for (HashMap.Entry<Long,Integer> entry : diskBlockAccessMap.entrySet()){
                    maxQuadtreeBlockId = Math.max(maxQuadtreeBlockId, entry.getKey());
                    maxQuadtreeBlockPoints = Math.max(maxQuadtreeBlockPoints, entry.getValue());
                    long blockId = entry.getKey()*1000000L + entry.getValue();
                    diskBlockSet.add(blockId);
                }
                io = diskBlockSet.size();
                System.out.print("[Ignore : " + maxQuadtreeBlockId + "-" + maxQuadtreeBlockPoints + "] ");
                */
                HashMap<Long,Integer> diskBlockAccessMap = new HashMap<>();
                for (Trajectory traj : reducedTrajs){
                    String trajId=  traj.getTrajId();
                    // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                    long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                    // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                    if (!diskBlockAccessMap.containsKey(quadtreeBasedDiskBlockId)){
                        diskBlockAccessMap.put(quadtreeBasedDiskBlockId, 0);
                    }
                    diskBlockAccessMap.put(quadtreeBasedDiskBlockId, diskBlockAccessMap.get(quadtreeBasedDiskBlockId)+1);
                    long simulatedBaselineBlockId = quadtreeBasedDiskBlockId*100000 + diskBlockAccessMap.get(quadtreeBasedDiskBlockId)/32;
                    diskBlockSet.add(simulatedBaselineBlockId);
                }
                io = diskBlockSet.size();
            }
        }
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec+ "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec+ "\t" + io +  "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        traverseToDeliver(trajGraph, pktRequest);
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
        
    public ArrayList<TrajGraphNode> deliverPacketAllTrajWithJoin(PacketDeliveryQuery pktDeliveryQuery){
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();
        ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        ArrayList<Trajectory> reducedTrajs = allTrajs;
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        trajGraph = constructTrajGraph_join(reducedTrajs);
        //ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        //TrajGraph trajGraph = constructTrajGraph(allTrajs); // correct later with reducedTrajs
        //bestDeliverers = traverseToDeliver(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_DistanceCost(trajGraph, pktRequest);
        bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        long algoEndTime = System.nanoTime();
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        boolean isDelivered = !bestDeliverers.isEmpty();
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec+ "\t" + trajsRetrieved + "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        traverseToDeliver(trajGraph, pktRequest);
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
    // temporal processing
    
    ArrayList<TrajGraphNode> traverseToDeliver_AStarWithDuration(TrajGraph trajGraph, PacketRequest pktRequest){
        
        int srcStopId = pktRequest.getSrcId();
        int destStopId = pktRequest.getDestId();
        long pktDuration = pktRequest.getDurationInSeconds();
        PriorityQueue<NodeState> explorableNodes = new PriorityQueue<>();
        HashMap <NodeState, NodeState> parentMap = new HashMap<>();
        HashSet <TrajGraphNode> colorSet = new HashSet<>();
        HashMap <TrajGraphNode, NodeState> enqueuedState = new HashMap<>();
        // construct TrajGraphNodes with stopId, trajId (to be obtained from stopToTraj HashMap)
        // enqueue all those nodes (temporal and other processing based pruning) to be done later
        //System.out.println("src stop id = " + srcStopId + ", dest stop id = " + destStopId);
        HashSet <String> stopTrajIds = trajGraph.getStopToTrajIds(srcStopId);
        if (stopTrajIds == null){
            //System.out.println("No trajs from stop id " + srcStopId);
            stopTrajIds = new HashSet<>();
        }
        //System.out.println("# of traj from src stop id = " + stopTrajIds.size());
        for (String trajId : stopTrajIds){
            TrajGraphNode srcNode = new TrajGraphNode(srcStopId, trajId, pktRequest.getSrcTimeInSec());
            double gCost = 0;
            double hCost = computeHeuristicCost(srcStopId, destStopId, distanceUnit);
            //double hCost = 0;
            double fCost = gCost + hCost;
            // cost of a src state is 0 and parent state of it is null
            if (enqueuedState.containsKey(srcNode) && fCost >= enqueuedState.get(srcNode).getCost()){
                // do nothing since a lower cost state for this node is already enqueued
            }
            else{
                NodeState srcState = new NodeState(srcNode, gCost, hCost, null, pktRequest.getSrcTimeInSec());
                if (enqueuedState.containsKey(srcNode)){
                    NodeState oldState = enqueuedState.get(srcNode);
                    NodeState oldParentState = oldState.getParentState();
                    NodeState newParentState = srcState.getParentState();
                    TrajGraphNode oldParentNode, newParentNode;
                    if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                    else oldParentNode = oldParentState.getTrajGraphNode();
                    if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                    else newParentNode = newParentState.getTrajGraphNode();
                    System.out.print("Node <" + srcNode.getStopId() + "," + srcNode.getTrajId() + ">, Parent = <");
                    System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed ");
                    System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + srcState.getCost() + " added ");
                    explorableNodes.remove(oldState);
                }
                enqueuedState.put(srcNode, srcState);
                explorableNodes.add(srcState);
            }
        }

        NodeState curNodeState = null;
        while(!explorableNodes.isEmpty()) {
            curNodeState = explorableNodes.poll();
            TrajGraphNode curNode = curNodeState.getTrajGraphNode();
            parentMap.put(curNodeState, curNodeState.getParentState());
            colorSet.add(curNode);
            // temporal pruning of states
            long duration =  curNode.getTimeInSec() - curNodeState.getDeliveryStartTimeInSec();
            // comment out the following line for ignoring temporal info
            if (duration > pktDuration) continue;
            
            if (curNode.getStopId() == destStopId) break;
            
            ArrayList <TrajGraphNode> neighbors = trajGraph.getNeighbors(curNode);
            //System.out.println("Neighbors of <" + curNode.getStopId() + "," + curNode.getTrajId() + "> : Size = " + neighbors.size());
            for (TrajGraphNode node : neighbors){
                //System.out.print(node.getStopId() + " , ");
                if (colorSet.contains(node)) continue;
                // comment out the following block for ignoring temporal info
                
                // the node to be enqueued is keeper
                if (node.getIsKeeper()) node.setTimeInSec(curNode.getTimeInSec());
                // the parent node is a keeper, so check the validity of node wrt time
                else if (curNode.getIsKeeper() && curNode.getTimeInSec() > node.getTimeInSec()){
                    // invalid node as parent's time is greater, so do not process it
                    continue;
                }
                else if (node.getTimeInSec() < curNode.getTimeInSec()){
                    continue;
                }
                else if (node.getTimeInSec() - curNodeState.getDeliveryStartTimeInSec() > pktDuration){
                    continue;
                }
                
                double gCost = computeIncurredCostByDistance(curNodeState, node, distanceUnit);
                double hCost = computeHeuristicCost(node.getStopId(), destStopId, distanceUnit);
                double fcost = gCost + hCost;
                if (enqueuedState.containsKey(node) && fcost >= enqueuedState.get(node).getCost()){
                    // do nothing since a lower cost state for this node is already enqueued
                }
                else{
                    
                    NodeState neighborNodeState = new NodeState(node, gCost, hCost, curNodeState, pktRequest.getSrcTimeInSec());
                    // do we need to remove the previous nodestate with higher cost?
                    // that may be a bit difficult to do, but trying in the following line
                    if (enqueuedState.containsKey(node)){
                        NodeState oldState = enqueuedState.get(node);
                        NodeState oldParentState = oldState.getParentState();
                        NodeState newParentState = neighborNodeState.getParentState();
                        TrajGraphNode oldParentNode, newParentNode;
                        if (oldParentState == null) oldParentNode = new TrajGraphNode(-1, "-1");
                        else oldParentNode = oldParentState.getTrajGraphNode();
                        if (newParentState == null) newParentNode = new TrajGraphNode(-1, "-1");
                        else newParentNode = newParentState.getTrajGraphNode();
                        //System.out.print("Node <" + node.getStopId() + "," + node.getTrajId() + ">, Parent = <");
                        //System.out.print(oldParentNode.getStopId() + "," + oldParentNode.getTrajId() + ">, Cost = " + oldState.getCost() + " removed and Parent = <");
                        //System.out.println(newParentNode.getStopId() + "," + newParentNode.getTrajId() + ">, Cost = " + neighborNodeState.getCost() + " added ");
                        explorableNodes.remove(oldState);
                    }
                    enqueuedState.put(node, neighborNodeState);
                    explorableNodes.add(neighborNodeState);
                    //System.out.println("Enqueued : " + neighborNodeState.getTrajGraphNode().getStopId());
                }
            }
            //System.out.println("");
        }
        
        ArrayList<TrajGraphNode> bestDeliverers = new ArrayList<>();
        double cost;
        long duration;
        
        if (curNodeState == null || curNodeState.getTrajGraphNode().getStopId() != destStopId){
            cost = Double.MAX_VALUE;
            duration = -1;
        }
        else{
            cost = curNodeState.getCost();
            duration = curNodeState.getTrajGraphNode().getTimeInSec() - curNodeState.getDeliveryStartTimeInSec();
            while(curNodeState != null){
                if (!parentMap.containsKey(curNodeState)){
                    System.out.println("Alert : Unreachable i.e. cannot be delivered");
                    break;
                }
                bestDeliverers.add(0, curNodeState.getTrajGraphNode());
                curNodeState = parentMap.get(curNodeState);
            }
        }
        /*
        if (bestDeliverers.isEmpty()) System.out.print(cost + "\t");
        else System.out.print(cost + "\t");
        */
        this.deliveryCost = cost;
        this.deliveryTimeInSec = duration;
        return bestDeliverers;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketModifiedWithDuration(PacketDeliveryQuery pktDeliveryQuery, int temporalProcess){
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();        
        // the following ones is giving a the fewest overlapping nodes (backtracked from overlapping nodes)
        ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        if (temporalProcess == 0){
            trajGraph = constructTrajGraph(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 1){
            trajGraph = constructTrajGraphWithTime(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 2){
            trajGraph = constructTrajGraphWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else{
            trajGraph = constructTrajGraphWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
        }
        //ArrayList<Trajectory> allTrajs = trajStorage.getTrajDataAsList();
        //TrajGraph trajGraph = constructTrajGraph(allTrajs); // correct later with reducedTrajs
        //bestDeliverers = traverseToDeliver(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_DistanceCost(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
        //bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        long algoEndTime = System.nanoTime();
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        boolean isDelivered = !bestDeliverers.isEmpty();
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + this.diskBlockSet.size() + "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketModifiedWithJoinWithDuration(PacketDeliveryQuery pktDeliveryQuery, int temporalProcess){
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();
        ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        long algoEndTime = System.nanoTime();
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("\n--##--%%--$$--%%--##--");
        //System.out.println("Summary node overlap processing : " + runtimeInSec + " sec");
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsFromSummaryNodes(summaryNodes);
        //algoEndTime = System.nanoTime();
        //runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("Trajectory retrival : " + runtimeInSec + " sec");
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        if (temporalProcess == 0){
            trajGraph = constructTrajGraph_join(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 1){
            trajGraph = constructTrajGraph_joinWithTime(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 2){
            trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else{
            trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
        }
        algoEndTime = System.nanoTime();
        runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.println("Shortest path finding : " + runtimeInSec + " sec");
        //System.out.println("--##--%%--$$--%%--##--");
        boolean isDelivered = !bestDeliverers.isEmpty();
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec + "\t" + this.diskBlockSet.size() + "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        traverseToDeliver(trajGraph, pktRequest);
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
    public ArrayList<TrajGraphNode> deliverPacketBaselineWithJoinWithDuration(PacketDeliveryQuery pktDeliveryQuery, int temporalProcess){
        int io = 0;
        PacketRequest pktRequest = pktDeliveryQuery.getPacketRequest();
        long algoStartTime = System.nanoTime();
        //ArrayList<Node> summaryNodes = retrieveSummaryNodesModified(pktRequest);
        ArrayList<Node> rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
        // a single node should suffice, but trying to reuse previously written function
        ArrayList<Trajectory> reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
        int trajsRetrieved = reducedTrajs.size();
        TrajGraph trajGraph;
        ArrayList<TrajGraphNode> bestDeliverers;
        if (temporalProcess == 0){
            trajGraph = constructTrajGraph_join(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 1){
            trajGraph = constructTrajGraph_joinWithTime(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else if (temporalProcess == 2){
            trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
        }
        else{
            trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
            bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
        }
        long algoEndTime = System.nanoTime();
        io = diskBlockSet.size();
        boolean isDelivered = !bestDeliverers.isEmpty();
        if (isDelivered){
            // using lat, lon distance threshold to increase range query rectangle size
            double additionalDistance = this.deliveryCost - pktDeliveryQuery.getDistance();
            double additionalLatDis = distanceConverter.getLatProximity(additionalDistance, distanceUnit);
            double additionalLonDis = distanceConverter.getLonProximity(additionalDistance, distanceUnit);
            latDisThreshold += 0.5*additionalLatDis;
            latDisThreshold += 0.5*additionalLonDis;
            
            // reconstructing trajGraph
            this.deliveryCost = Double.MAX_VALUE;
            this.deliveryTimeInSec = -1;
            this.diskBlockSet.clear();
            rangeQueryNodes.clear();
            rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
            reducedTrajs.clear();
            reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
            trajsRetrieved = reducedTrajs.size();
            trajGraph = null;
            bestDeliverers.clear();
            if (temporalProcess == 0){
                trajGraph = constructTrajGraph_join(reducedTrajs);
                bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
            }
            else if (temporalProcess == 1){
                trajGraph = constructTrajGraph_joinWithTime(reducedTrajs);
                bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
            }
            else if (temporalProcess == 2){
                trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
            }
            else{
                trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
            }
            algoEndTime = System.nanoTime();
            io = diskBlockSet.size();
            // revert
            latDisThreshold -= 0.5*additionalLatDis;
            latDisThreshold -= 0.5*additionalLonDis;
        }
        else{
            // trying to increase range query bounds incrementally in case of unsuccessful delivery
            double pktDistance = pktDeliveryQuery.getDistance();
            for(int i=0; i<3 && !isDelivered; i++){
                this.deliveryCost = Double.MAX_VALUE;
                this.deliveryTimeInSec = -1;
                this.diskBlockSet.clear();
                double additionalDistance = pktDistance*(1<<i);
                double additionalLatDis = distanceConverter.getLatProximity(additionalDistance, distanceUnit);
                double additionalLonDis = distanceConverter.getLonProximity(additionalDistance, distanceUnit);
                latDisThreshold += 0.5*additionalLatDis;
                latDisThreshold += 0.5*additionalLonDis;
                // reconstructing trajGraph
                rangeQueryNodes.clear();
                rangeQueryNodes = retrieveRangeQueryNodes(pktRequest);
                reducedTrajs.clear();
                reducedTrajs = retrieveTrajsBaseline(rangeQueryNodes);
                trajsRetrieved = reducedTrajs.size();
                trajGraph = null;
                bestDeliverers.clear();
                if (temporalProcess == 0){
                    trajGraph = constructTrajGraph_join(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else if (temporalProcess == 1){
                    trajGraph = constructTrajGraph_joinWithTime(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else if (temporalProcess == 2){
                    trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else{
                    trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
                }
                algoEndTime = System.nanoTime();
                io = diskBlockSet.size();
                isDelivered = !bestDeliverers.isEmpty();
                // revert
                latDisThreshold -= 0.5*additionalLatDis;
                latDisThreshold -= 0.5*additionalLonDis;
                io = diskBlockSet.size();
            }
            if (!isDelivered){
                // reconstructing trajGraph
                this.deliveryCost = Double.MAX_VALUE;
                this.deliveryTimeInSec = -1;
                this.diskBlockSet.clear();
                reducedTrajs.clear();
                reducedTrajs = trajStorage.getTrajDataAsList();
                trajsRetrieved = reducedTrajs.size();
                // assumption : 32 trajs in each block (each traj size around 128B, block size 4k => trajs per block = 32)
                trajGraph = null;
                bestDeliverers.clear();
                if (temporalProcess == 0){
                    trajGraph = constructTrajGraph_join(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else if (temporalProcess == 1){
                    trajGraph = constructTrajGraph_joinWithTime(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else if (temporalProcess == 2){
                    trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStar(trajGraph, pktRequest);
                }
                else{
                    trajGraph = constructTrajGraph_joinWithTimeDirectional(reducedTrajs);
                    bestDeliverers = traverseToDeliver_AStarWithDuration(trajGraph, pktRequest);
                }
                algoEndTime = System.nanoTime();
                isDelivered = !bestDeliverers.isEmpty();
                
                /* the following segment is to get insight of number of quadtree nodes and points in them (in case of same location, node capacity is exceeded)
                HashMap<Long,Integer> diskBlockAccessMap = new HashMap<>();
                for (Trajectory traj : reducedTrajs){
                    String trajId=  traj.getTrajId();
                    // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                    long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                    // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                    if (!diskBlockAccessMap.containsKey(quadtreeBasedDiskBlockId)){
                        diskBlockAccessMap.put(quadtreeBasedDiskBlockId, 0);
                    }
                    diskBlockAccessMap.put(quadtreeBasedDiskBlockId, diskBlockAccessMap.get(quadtreeBasedDiskBlockId)+1);
                }
                long maxQuadtreeBlockId = Long.MIN_VALUE;
                int maxQuadtreeBlockPoints = Integer.MIN_VALUE;
                for (HashMap.Entry<Long,Integer> entry : diskBlockAccessMap.entrySet()){
                    maxQuadtreeBlockId = Math.max(maxQuadtreeBlockId, entry.getKey());
                    maxQuadtreeBlockPoints = Math.max(maxQuadtreeBlockPoints, entry.getValue());
                    long blockId = entry.getKey()*1000000L + entry.getValue();
                    diskBlockSet.add(blockId);
                }
                io = diskBlockSet.size();
                System.out.print("[Ignore : " + maxQuadtreeBlockId + "-" + maxQuadtreeBlockPoints + "] ");
                */
                HashMap<Long,Integer> diskBlockAccessMap = new HashMap<>();
                for (Trajectory traj : reducedTrajs){
                    String trajId=  traj.getTrajId();
                    // assumption : a trajectory is stored in a disk block enumerated by the z-id of its first point
                    long quadtreeBasedDiskBlockId = trajStorage.getTransformedTrajectoryById(trajId).getTransformedPointList().first().getqNodeIndex();
                    // assumption : at most 32 trajs in each block as node capacity is 32 points (each traj size around 128B, block size 4k => trajs per block = 32)
                    if (!diskBlockAccessMap.containsKey(quadtreeBasedDiskBlockId)){
                        diskBlockAccessMap.put(quadtreeBasedDiskBlockId, 0);
                    }
                    diskBlockAccessMap.put(quadtreeBasedDiskBlockId, diskBlockAccessMap.get(quadtreeBasedDiskBlockId)+1);
                    long simulatedBaselineBlockId = quadtreeBasedDiskBlockId*100000 + diskBlockAccessMap.get(quadtreeBasedDiskBlockId)/32;
                    diskBlockSet.add(simulatedBaselineBlockId);
                }
                io = diskBlockSet.size();
            }
        }
        double runtimeInSec = (algoEndTime - algoStartTime)/1.0e9;
        //System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec+ "\t" + trajsRetrieved + "\t");
        System.out.print(isDelivered + "\t" + this.deliveryCost + "\t" + runtimeInSec+ "\t" + io +  "\t" + this.deliveryTimeInSec + "\t");
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        traverseToDeliver(trajGraph, pktRequest);
        this.deliveryCost = Double.MAX_VALUE;
        this.deliveryTimeInSec = -1;
        this.diskBlockSet.clear();
        return bestDeliverers;
    }
    
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
    
}

class NodeState implements Comparable<NodeState> {
    private NodeState parentState;
    private TrajGraphNode trajGraphNode;
    private double gCost, hCost;
    private long deliveryStartTimeInSec;

    public NodeState(TrajGraphNode trajGraphNode, double gCost, double hCost, NodeState parentState) {
        this.trajGraphNode = trajGraphNode;
        this.gCost = gCost;
        this.hCost = hCost;
        this.parentState = parentState;
        this.deliveryStartTimeInSec = 0;
    }

    public NodeState(TrajGraphNode trajGraphNode, double gCost, double hCost, NodeState parentState, long deliveryStartTimeInSec) {
        this.parentState = parentState;
        this.trajGraphNode = trajGraphNode;
        this.gCost = gCost;
        this.hCost = hCost;
        this.deliveryStartTimeInSec = deliveryStartTimeInSec;
    }
    
    
    public TrajGraphNode getTrajGraphNode() {
        return trajGraphNode;
    }
    
    public double getGCost(){
        return gCost;
    }
    
    public double getHCost(){
        return hCost;
    }
    
    public double getCost() {
        return gCost + hCost;
    }

    public NodeState getParentState() {
        return parentState;
    }

    public long getDeliveryStartTimeInSec() {
        return deliveryStartTimeInSec;
    }
            
    @Override
    public int compareTo(NodeState o) {
        double cost = gCost + hCost;
        double otherNodeStateCost = o.gCost + o.hCost;
        if (cost < otherNodeStateCost) return -1;
        if (cost > otherNodeStateCost) return 1;
        return 0;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.parentState);
        hash = 59 * hash + Objects.hashCode(this.trajGraphNode);
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.gCost) ^ (Double.doubleToLongBits(this.gCost) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.hCost) ^ (Double.doubleToLongBits(this.hCost) >>> 32));
        hash = 59 * hash + (int) (this.deliveryStartTimeInSec ^ (this.deliveryStartTimeInSec >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeState other = (NodeState) obj;
        if (Double.doubleToLongBits(this.gCost) != Double.doubleToLongBits(other.gCost)) {
            return false;
        }
        if (Double.doubleToLongBits(this.hCost) != Double.doubleToLongBits(other.hCost)) {
            return false;
        }
        if (this.deliveryStartTimeInSec != other.deliveryStartTimeInSec) {
            return false;
        }
        if (!Objects.equals(this.parentState, other.parentState)) {
            return false;
        }
        if (!Objects.equals(this.trajGraphNode, other.trajGraphNode)) {
            return false;
        }
        return true;
    }

}

class OverlappingNodes{
    private Node node;
    private HashSet<Node> from;
    private HashSet<Node> to;

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
    private Integer key;
    private Long value;

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