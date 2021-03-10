package ds.qtrajtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.util.Assert;
import db.TrajStorage;

import ds.qtree.Node;
import ds.qtree.NodeType;
import ds.qtree.QuadTree;
import ds.qtree.SummaryQuadTree;
import ds.rtree.Rtree;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;
import java.util.TreeSet;

public class TQIndex {

    private final QuadTree quadTree;
    
    private SummaryQuadTree sqTree;
    
    private final TrajStorage trajStorage;
    
    // trying to maintain a map of trajectories contained in qNode, this is analogous to the block number of the trajectory
    // public Map<Node, ArrayList<Trajectory>> qNodeToTrajsMap;
    // cannot maintain the aforementioned map for scalability
    
    // maintaining a map of number of trajectories contained in qNode, will remove later unless needed
    public Map<Node, Integer> qNodeTrajsCount;
    
    // maintaining a map of trajectory ids contained in qNode, will remove later unless needed
    public Map<Node, ArrayList<String>> qNodeToAnonymizedTrajIdsMap;
    // cannot maintain the aforementioned map with the value as arraylist, here we need a quadtree as value, as follows
    public Map<Node, QuadTree> qNodeToNextLevelIndexMap;
    
    //public double latCoeff = 0, latConst = 0, lonCoeff = 0, lonConst = 0;
    // these coefficients and constants may be needed to get back the actual longitudes, latitudes of trajectories later
    private double latCoeff, latConst, lonCoeff, lonConst;
    private double maxLat, maxLon, minLat, minLon;
    private long minTimeInSec;
    private int timeWindowInSec;
    
    public TQIndex(TrajStorage trajStorage, double latCoeff, double latConst, double lonCoeff, double lonConst,
                    double maxLat, double maxLon, double minLat, double minLon, long minTimeInSec, int timeWindowInSec) {
        
        this.trajStorage = trajStorage;
        
        this.latCoeff = latCoeff;
        this.latConst = latConst;
        this.lonCoeff = lonCoeff;
        this.lonConst = lonConst;
        
        this.maxLat = maxLat;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.minLon = minLon;
        
        this.minTimeInSec = minTimeInSec;
        this.timeWindowInSec = timeWindowInSec;
        
        qNodeTrajsCount = new HashMap<Node, Integer>();
        qNodeToAnonymizedTrajIdsMap = new HashMap<Node, ArrayList<String>>();
        qNodeToNextLevelIndexMap = new HashMap<Node, QuadTree>();
        
        quadTree = new QuadTree(trajStorage, 0.0, 0.0, 100.0, 100.0, minTimeInSec, timeWindowInSec);    // since trajectories are already normalized in this range
        
        int pointsInSummaryNode = 0;
        sqTree = new SummaryQuadTree(trajStorage, 0.0, 0.0, 100.0, 100.0, minTimeInSec, timeWindowInSec, pointsInSummaryNode);
        
        // now read data in chunks and build the first level quadtree
        ArrayList<Trajectory> trajectories = this.trajStorage.getNextChunkAsList();
        while(trajectories != null){
            for (Trajectory trajectory : trajectories) {
                TreeSet <TrajPoint> trajPointList = trajectory.getPointList();
                int pointCount = 0;
                // long trajId = trajectory.getUserId();
                String anonymizedTrajId = trajectory.getAnonymizedId();
                for (TrajPoint trajPoint: trajPointList) {
                    Coordinate trajPointLocation = trajPoint.getPointLocation();
                    long trajPointTimeInSec = trajPoint.getTimeInSec();
                    quadTree.set(trajPointLocation.x, trajPointLocation.y, trajPointTimeInSec, new Integer(pointCount++), new String(anonymizedTrajId));
                    // sqTree.set(trajPointLocation.x, trajPointLocation.y, trajPointTimeInSec, new Integer(pointCount++), new String(anonymizedTrajId));
                }
            }
            trajectories = this.trajStorage.getNextChunkAsList();
        }
        
        // assuming zCode starts from 0 (the second argument)
        quadTree.assignZCodesToLeaves(quadTree.getRootNode(), 0);
        quadTree.transformTrajectories(quadTree.getRootNode());
        // trajStorage.printTrajectories();
        // the following will be done when we have the disk block ids in trajStorage
        Rtree rTree = new Rtree(trajStorage.getTransformedTrajData());
        trajStorage.setTrajIdToDiskBlockIdMap(rTree.getTrajectoryToLeafMapping());
        quadTree.tagDiskBlockIdsToNodes(quadTree.getRootNode());
        trajStorage.setDiskBlockIdToTrajIdListMap();
        
        /* need to work on the following part for (Q^2)R tree
        // cursor set to beginning automatically, so reading next chunk will not return null
        trajectories = this.trajStorage.getNextChunkAsList();
        while(trajectories != null){
            addTrajectories(trajectories);
            trajectories = this.trajStorage.getNextChunkAsList();
        }
        */        
        trajStorage.clearQNodeToPointListMap();
    }
    
    public void buildSummaryIndex(int pointsInSummaryNode){
        sqTree = new SummaryQuadTree(trajStorage, 0.0, 0.0, 100.0, 100.0, minTimeInSec, timeWindowInSec, pointsInSummaryNode);
        trajStorage.resetSummaryTrajData();
        // now read data in chunks and build the first level quadtree
        ArrayList<Trajectory> trajectories = this.trajStorage.getNextChunkAsList();
        while(trajectories != null){
            for (Trajectory trajectory : trajectories) {
                //System.out.println("Processing trajectory for summary index...");
                TreeSet <TrajPoint> trajPointList = trajectory.getPointList();
                int pointCount = 0;
                // long trajId = trajectory.getUserId();
                String anonymizedTrajId = trajectory.getAnonymizedId();
                for (TrajPoint trajPoint: trajPointList) {
                    Coordinate trajPointLocation = trajPoint.getPointLocation();
                    long trajPointTimeInSec = trajPoint.getTimeInSec();
                    sqTree.set(trajPointLocation.x, trajPointLocation.y, trajPointTimeInSec, new Integer(pointCount++), new String(anonymizedTrajId));
                }
            }
            trajectories = this.trajStorage.getNextChunkAsList();
        }
        sqTree.assignZCodesToLeaves(sqTree.getRootNode(), 0);
        sqTree.transformTrajSummary(sqTree.getRootNode());
        sqTree.buildSummaryNetwork();
        
        trajStorage.clearQNodeToPointListMap();
    }
    
    public void printSummaryIndex(){
        sqTree.printSummaryGraph();
    }
    
    public void printSummaryIndexSummary(){
        sqTree.printSummaryGraphSummary();
    }
    
    private void addTrajectories(ArrayList<Trajectory> trajectories) {
        for (Trajectory trajectory : trajectories) {
            Node node = addTrajectory(quadTree.getRootNode(), trajectory);
            
            if (!qNodeToAnonymizedTrajIdsMap.containsKey(node)) {
                qNodeToAnonymizedTrajIdsMap.put(node, new ArrayList<String>());
                qNodeToNextLevelIndexMap.put(node, new QuadTree(trajStorage, node.getX(), node.getY(), node.getX() + node.getW(), node.getY() + node.getH(),
                                                                minTimeInSec, timeWindowInSec));
            }
            String anonymizedTrajId = trajectory.getAnonymizedId();
            qNodeToAnonymizedTrajIdsMap.get(node).add(anonymizedTrajId);
            int pointCount = 0;
            for (TrajPoint trajPoint : trajectory.getPointList()){
                double trajPointX = trajPoint.getPointLocation().x;
                double trajPointY = trajPoint.getPointLocation().y;
                long trajPointTimeInSec = trajPoint.getTimeInSec();
                qNodeToNextLevelIndexMap.get(node).set(trajPointX, trajPointY, trajPointTimeInSec, new Integer(pointCount++), anonymizedTrajId);
            }
        }
    }

    private Node addTrajectory(Node node, Trajectory trajectory) {

        if (qNodeTrajsCount.get(node) == null) {
            qNodeTrajsCount.put(node, 0);
        }
        qNodeTrajsCount.put(node, qNodeTrajsCount.get(node) + 1);
        
        if (node.getNodeType() == NodeType.LEAF){
            return node;
        }
        
        Envelope trajEnv = new Envelope();

        for (TrajPoint trajPoint: trajectory.getPointList()) {
            trajEnv.expandToInclude(trajPoint.getPointLocation());
        }
        
        Envelope envNe = getNodeEnvelop(node.getNe());
        if (!envNe.contains(trajEnv) && envNe.intersects(trajEnv)) {
            return node;
        }
        
        Envelope envNw = getNodeEnvelop(node.getNw());
        if (!envNw.contains(trajEnv) && envNw.intersects(trajEnv)) {
            return node;
        }
        
        Envelope envSe = getNodeEnvelop(node.getSe());
        if (!envSe.contains(trajEnv) && envSe.intersects(trajEnv)) {
            return node;
        }
        
        Envelope envSw = getNodeEnvelop(node.getSw());
        if (!envSw.contains(trajEnv) && envSw.intersects(trajEnv)) {
            return node;
        }

        if (envNe.contains(trajEnv)) {
            return addTrajectory(node.getNe(), trajectory);
        }
        if (envNw.contains(trajEnv)) {
            return addTrajectory(node.getNw(), trajectory);
        }
        if (envSe.contains(trajEnv)) {
            return addTrajectory(node.getSe(), trajectory);
        }
        if (envSw.contains(trajEnv)) {
            return addTrajectory(node.getSw(), trajectory);
        }

        System.out.println(trajEnv);

        System.out.println("QuadTrajTree.addTrajectory()");
        Assert.shouldNeverReachHere();

        return null;
    }

    private Envelope getNodeEnvelop(Node node) {
        Envelope nodeEnv = new Envelope();
        nodeEnv.expandToInclude(node.getX(), node.getY());
        nodeEnv.expandToInclude(node.getX() + node.getW(), node.getY() + node.getH());
        return nodeEnv;
    }
    
    public QuadTree getQuadTree() {
        return quadTree;
    }

    public ArrayList<String> getQNodeTrajsId(Node node) {
        ArrayList<String> empty = new ArrayList<String>();
        if (node == null) {
            return empty;
        }
        ArrayList<String> ret = qNodeToAnonymizedTrajIdsMap.get(node);
        ////ArrayList<Integer> ret = null;
        return ret == null ? empty : ret;
    }

    public ArrayList<Trajectory> getQNodeTrajs(Node node) {
        ArrayList<String> retIds = getQNodeTrajsId(node);
        ArrayList<Trajectory> empty = new ArrayList<Trajectory>();
        ArrayList<Trajectory> ret = new ArrayList<Trajectory>();
        for (int i=0; i<retIds.size(); i++){
            ret.add(trajStorage.getTrajectoryById(retIds.get(i)));
        }
        return ret == null ? empty : ret;
    }

    public QuadTree getQNodeQuadTree(Node node) {
        if (qNodeToNextLevelIndexMap.containsKey(node)){
            return qNodeToNextLevelIndexMap.get(node);
        }
        return null;
    }
    
    public int getTotalNodeTraj(Node qNode) {
        if (qNodeTrajsCount.get(qNode) != null) {
            return qNodeTrajsCount.get(qNode);
        }
        return 0;
    }
    
    public double getLatCoeff() {
        return latCoeff;
    }

    public double getLatConst() {
        return latConst;
    }

    public double getLonCoeff() {
        return lonCoeff;
    }

    public double getLonConst() {
        return lonConst;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMinLon() {
        return minLon;
    }
    
    public void draw() {
        IndexCanvas quadTrajTreeCanvas = new IndexCanvas(this);
        quadTrajTreeCanvas.draw();
    }
    
}