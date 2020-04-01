package ds.qtrajtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.javatuples.Pair;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.util.Assert;

import ds.qtree.Node;
import ds.qtree.NodeType;
import ds.qtree.Point;
import ds.qtree.QuadTree;
import result.ResultPlotter;

import query.topk.DiskIO;

public class TQIndex {

    final double proximity;
    private QuadTree quadTree;
    private ArrayList<CoordinateArraySequence> trajectories;

    // a trajectory belongs to which node
    //public Map<String, String> trajToNodeStrMap = new HashMap<String, String>();
    //public Map<CoordinateArraySequence, Node> intraTrajToNodeMap = new HashMap<CoordinateArraySequence, Node>();
    
    //public Map<Node, ArrayList<CoordinateArraySequence>> nodeToIntraTrajsMap = new HashMap<Node, ArrayList<CoordinateArraySequence>>();
    // maintaining a map of trajectories contained in qNode, this is analogous to the block number of the trajectory
    public Map<Node, ArrayList<CoordinateArraySequence>> qNodeToTrajsMap;
    // maintaining a map of number of trajectories contained in qNode, will remove later unless needed
    public Map<Node, Integer> qNodeTrajsCount;
    // maintaining a map of trajectory ids contained in qNode, will remove later unless needed
    public Map<Node, ArrayList<Integer>> qNodeToTrajIdsMap;
    // the reverse map of qNode to trajectories
    public Map<CoordinateArraySequence, ArrayList<Node>> trajToQNodesMap;
    // the reverse map of qNode to trajectory ids
    public Map<Integer, ArrayList<Node>> trajIdToQNodesMap;
    // the same item as a list
    public ArrayList<ArrayList<Node>> trajQNodesList;
    
    //private int blockSize = 128;
    
    //private int zOrderNodeCount = 0;
    
    //public double latCoeff = 0, latConst = 0, lonCoeff = 0, lonConst = 0;
    // these coefficients and constants may be needed to get back the actual longitudes, latitudes of trajectories later
    public double latCoeff, latConst, lonCoeff, lonConst;
    
    public TQIndex(ArrayList<CoordinateArraySequence> trajectories, double latCoeff, double latConst, double lonCoeff, double lonConst) {
        proximity = 0.36848;    /// 0.5km = 0.18424, 1km = 0.36848
        
        this.latCoeff = latCoeff;
        this.latConst = latConst;
        this.lonCoeff = lonCoeff;
        this.lonConst = lonConst;
        
        //System.out.println(proximity);
        
        this.trajectories = trajectories;
        
        
        qNodeToTrajsMap = new HashMap<Node, ArrayList<CoordinateArraySequence>>();
        qNodeTrajsCount = new HashMap<Node, Integer>();
        qNodeToTrajIdsMap = new HashMap<Node, ArrayList<Integer>>();
        trajToQNodesMap = new HashMap<CoordinateArraySequence, ArrayList<Node>>();
        trajIdToQNodesMap = new HashMap<Integer, ArrayList<Node>>();
        trajQNodesList = new ArrayList<ArrayList<Node>>();
        
        Envelope envelope = new Envelope();
        for (CoordinateArraySequence trajectory : trajectories) {
            trajectory.expandEnvelope(envelope);
        }
        
        quadTree = new QuadTree(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());

        for (CoordinateArraySequence trajectory : trajectories) {
            for (int i = 0; i < trajectory.size(); i++) {
                quadTree.set(trajectory.getX(i), trajectory.getY(i), new Integer(i));
            }
        }
        
        /*
        double updatePercentage = 1;  // total: 160=0.25, 133=0.5, 118=0.75, 100=0.5
        int updateStartIndex = (int) (trajectories.size()*updatePercentage/200);
        updateStartIndex = 350000;
        int updateEndIndex = trajectories.size();
        updateEndIndex = Integer.min(updateEndIndex, (int)(updateStartIndex*(1+updatePercentage)));
        
        
        ArrayList<CoordinateArraySequence> insertTrajectories, updateTrajectories;
        
        insertTrajectories = new ArrayList<CoordinateArraySequence>();
        updateTrajectories = new ArrayList<CoordinateArraySequence>();
        
        for (int i=0; i<updateStartIndex; i++){
            insertTrajectories.add(trajectories.get(i));
        }
        
        for (int i=updateStartIndex; i<updateEndIndex; i++){
            updateTrajectories.add(trajectories.get(i));
        }
        
        //System.out.println("Building tree on trajectory data set of size: " + trajectories.size());
        
        //trajectories.clear();
        
        
        addTrajectories(insertTrajectories, 0);
        double from = System.nanoTime();
        addTrajectoriesUpdate(updateTrajectories, updateStartIndex);
        double to = System.nanoTime();
        double indexUpdateTime = (to-from)/1e9;
        //System.out.println("\t" + indexUpdateTime + "\t" + indexUpdateTime/updateTrajectories.size());
        */
        addTrajectories(trajectories, 0);
        ////System.out.println(quadTree.getNodeCount() + "\t" + zOrderNodeCount);
    }

    private void addTrajectories(ArrayList<CoordinateArraySequence> trajectories, int trajID) {
        //int tqBasicIO = 0;
        for (int i=0; i<trajectories.size(); i++) {
            CoordinateArraySequence trajectory = trajectories.get(i);
            Node node = addTrajectory(quadTree.getRootNode(), trajectory);
            
            //System.out.println("Added " + (i+1) + " trajectories to quadtree");
            
            //System.out.println(trajectory.toString() + node.toString());
            //intraTrajToNodeMap.put(trajectory, node);

            if (!qNodeToTrajsMap.containsKey(node)) {
                qNodeToTrajsMap.put(node, new ArrayList<CoordinateArraySequence>());
                qNodeToTrajIdsMap.put(node, new ArrayList<Integer>());
            }
            qNodeToTrajsMap.get(node).add(trajectory);
            ////tqBasicIO++;
            ////if (qNodeToTrajIdsMap.get(node).size()%blockSize == 0) tqBasicIO++;
            //Integer t_id = trajID;
            qNodeToTrajIdsMap.get(node).add(trajID);

            //System.out.println(trajID + ": <" + trajectory.getX(0) + ", " + trajectory.getY(0) + ">        <" + trajectory.getX(1) + ", " + trajectory.getY(1) + ">");
            trajID++;
        }
        //System.out.println("TQ-B I/O: " + tqBasicIO);
    }
    
    /*
    private void addTrajectoriesUpdate(ArrayList<CoordinateArraySequence> trajectories, int trajID) {
        int splitCount = 0;
        int tqBasicIO = 0;
        for (int i=0; i<trajectories.size(); i++) {
            CoordinateArraySequence trajectory = trajectories.get(i);
            Node node = addTrajectory(quadTree.getRootNode(), trajectory);

            //System.out.println(trajectory.toString() + node.toString());
            //intraTrajToNodeMap.put(trajectory, node);

            if (!nodeToIntraTrajQuadTree.containsKey(node)) {
                ////nodeToIntraTrajsMap.put(node, new ArrayList<CoordinateArraySequence>());
                ////qNodeToTrajIdsMap.put(node, new ArrayList<Integer>());
                nodeToIntraTrajQuadTree.put(node, new QuadTree(node.getX(), node.getY(), node.getX() + node.getW(), node.getY() + node.getH()));
            }
            ////nodeToIntraTrajsMap.get(node).add(trajectory);
            ////tqBasicIO++;
            ////if (qNodeToTrajIdsMap.get(node).size()%blockSize == 0) tqBasicIO++;
            //Integer t_id = trajID;
            ////qNodeToTrajIdsMap.get(node).add(trajID);
            
            int newNodes = 0;
            newNodes = nodeToIntraTrajQuadTree.get(node).set(trajectory.getX(0), trajectory.getY(0), new Pair<Integer, Boolean>(new Integer(trajID), new Boolean(true)));
            splitCount += newNodes;
            zOrderNodeCount += newNodes*4;
            newNodes = nodeToIntraTrajQuadTree.get(node).set(trajectory.getX(0), trajectory.getY(0), new Pair<Integer, Boolean>(new Integer(trajID), new Boolean(false)));
            splitCount += newNodes;
            zOrderNodeCount += newNodes*4;

            //System.out.println(trajID + ": <" + trajectory.getX(0) + ", " + trajectory.getY(0) + ">        <" + trajectory.getX(1) + ", " + trajectory.getY(1) + ">");
            trajID++;
        }
        //System.out.print(tqBasicIO);
        //System.out.println((trajectories.size() + splitCount));
    }
    */

    private Node addTrajectory(Node node, CoordinateArraySequence trajectory) {

        //System.out.println(node);
        if (qNodeTrajsCount.get(node) == null) {
            qNodeTrajsCount.put(node, 0);
        }
        qNodeTrajsCount.put(node, qNodeTrajsCount.get(node) + 1);
        
        if (node.getNodeType() == NodeType.LEAF){
            return node;
        }
        
        Envelope trajEnv = new Envelope();

        for (int i = 0; i < trajectory.size(); i++) {
            trajEnv.expandToInclude(trajectory.getCoordinate(i));
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

    //public Node getTrajQNode(CoordinateArraySequence trajectory) {
        //return intraTrajToNodeMap.get(trajectory);
    //}
    
    public CoordinateArraySequence getTraj(int index){
        if (index < trajectories.size()) return trajectories.get(index);
        return null;
    }

    public ArrayList<CoordinateArraySequence> getQNodeTrajs(Node node) {
        ArrayList<CoordinateArraySequence> empty = new ArrayList<CoordinateArraySequence>();
        if (node == null) {
            return empty;
        }
        ArrayList<CoordinateArraySequence> ret = new ArrayList<CoordinateArraySequence>();
        ArrayList<Integer> retIds = qNodeToTrajIdsMap.get(node);
        ////ArrayList<Integer> retIds = null;
        for (int i=0; i<retIds.size(); i++){
            ret.add(trajectories.get(retIds.get(i)));
        }
        return ret == null ? empty : ret;
    }

    public ArrayList<Integer> getQNodeTrajsId(Node node) {

        ArrayList< Integer> empty = new ArrayList<Integer>();
        if (node == null) {
            return empty;
        }
        ArrayList<Integer> ret = qNodeToTrajIdsMap.get(node);
        ////ArrayList<Integer> ret = null;
        return ret == null ? empty : ret;
    }

    public void draw() {
        QuadTrajTreeCanvas quadTrajTreeCanvas = new QuadTrajTreeCanvas(this);
        quadTrajTreeCanvas.draw();
    }

    public QuadTree getQuadTree() {
        return quadTree;
    }
    
    boolean containsExtended(Node qNode, Coordinate coord) {
        // checking whether an extended qNode contains a point
        double minX = qNode.getX() - proximity;
        double minY = qNode.getY() - proximity;
        double maxX = minX + qNode.getW() + 2 * proximity;
        double maxY = minY + qNode.getH() + 2 * proximity;
        if (coord.x < minX || coord.y < minY || coord.x > maxX || coord.y > maxY) {
            return false;
        }
        return true;
    }
    
    public double deNormalize(double x, boolean isLatitude){
        double offset, scale;
        if (isLatitude){
            offset = latConst;
            scale = latCoeff;
        }
        else{
            offset = lonConst;
            scale = lonCoeff;
        }
        return x*scale + offset;
    }
    
    /*
    public double evaluateNodeTrajWithIndexBinary(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery, HashSet<Integer> served, ResultPlotter mapView, DiskIO diskIO) {
        if (facilityQuery == null || facilityQuery.isEmpty() || qNode == null) {
            return 0;
        }
        
        if (nodeToIntraTrajQuadTree.get(qNode) == null) {
            return 0;
        }

        ArrayList<CoordinateArraySequence> queryUnionSet = facilityQuery;
        
        for (CoordinateArraySequence coordSeq : queryUnionSet) {
            //HashSet<Integer> start = new HashSet<Integer>();
            HashMap<Integer, Double> distanceFromStart = new HashMap<Integer, Double>();
            //HashSet<Integer> end = new HashSet<Integer>();
            HashMap<Integer, Double> distanceFromEnd = new HashMap<Integer, Double>();
            for (int i = 0; i < coordSeq.size(); i++) {
                double xmin = coordSeq.getX(i) - proximity;
                double ymin = coordSeq.getY(i) - proximity;
                double xmax = coordSeq.getX(i) + proximity;
                double ymax = coordSeq.getY(i) + proximity;

                Node[] nodes = nodeToIntraTrajQuadTree.get(qNode).searchIntersect(xmin, ymin, xmax, ymax);
                HashSet <Long> zOrderedBlockSet;
                HashSet <Integer> randomBlockSet;
                HashSet <Integer> tqTreeFullListBlockSet;
                HashSet <Integer> trajSet;
                zOrderedBlockSet =  new HashSet<Long>();
                randomBlockSet = new HashSet<Integer>();
                tqTreeFullListBlockSet = new HashSet<Integer>();
                trajSet = new HashSet<Integer>();
                
                for (Node node : nodes) {
                    @SuppressWarnings("unchecked")
                    Point point = node.getPoint();
                    
                    Pair<Integer, Boolean> data = (Pair<Integer, Boolean>) point.getValue();
                    
                    if (!trajSet.contains(data.getValue0())){
                        trajSet.add(data.getValue0());
                        zOrderedBlockSet.add(node.getZCode()/blockSize);
                        randomBlockSet.add(data.getValue0()/blockSize);
                    }
                    
                    if (data.getValue1()) { // getValue1() indicates if the point is start or end of a trajectory, true for start, false for end
                        double distance = Math.sqrt(Math.pow(xmin + proximity - point.getX(), 2) + Math.pow(ymin + proximity - point.getY(), 2));
                        //start.add(data.getValue0());
                        if (distanceFromStart.containsKey(data.getValue0()) && distance < distanceFromStart.get(data.getValue0())){
                            distanceFromStart.replace(data.getValue0(), distance);
                        }
                        else{
                            distanceFromStart.put(data.getValue0(), distance);
                        }
                    } else {
                        //end.add(data.getValue0());
                        double distance = Math.sqrt(Math.pow(xmin + proximity - point.getX(), 2) + Math.pow(ymin + proximity - point.getY(), 2));
                        //start.add(data.getValue0());
                        if (distanceFromEnd.containsKey(data.getValue0()) && distance < distanceFromEnd.get(data.getValue0())){
                            distanceFromEnd.replace(data.getValue0(), distance);
                        }
                        else{
                            distanceFromEnd.put(data.getValue0(), distance);
                        }
                    }
                }
                //System.out.println(zOrderedBlockSet.size());
                //System.out.println(randomBlockSet.size());
                for (int j=0; j<qNodeToTrajIdsMap.get(qNode).size(); j++){
                    //System.out.println(qNodeToTrajIdsMap.get(qNode).get(j));
                    tqTreeFullListBlockSet.add(qNodeToTrajIdsMap.get(qNode).get(j)/blockSize);
                }
                diskIO.setValues(diskIO.getOrderedCount()+zOrderedBlockSet.size(), diskIO.getUnorderedCount()+randomBlockSet.size(),
                                diskIO.getTqTreeCount()+tqTreeFullListBlockSet.size(), nodeToAllTrajsCount.get(qNode)/blockSize);
            }
            for (Integer id : distanceFromEnd.keySet()) {   // end
                if (distanceFromStart.containsKey(id) && distanceFromEnd.get(id) + distanceFromStart.get(id) <= proximity) {    // start
                    served.add(id);
                    //mapView.addUserTrajectory(trajectories.get(id));
                    //System.out.println(id);
                    double lat1 = deNormalize(trajectories.get(id).getY(0), true);
                    double lat2 = deNormalize(trajectories.get(id).getY(1), true);
                    double lon1 = deNormalize(trajectories.get(id).getX(0), false);
                    double lon2 = deNormalize(trajectories.get(id).getX(1), false);
                    System.out.println(lat1+"\t"+lon1+"\t"+lat2+"\t"+lon2);
                }
            }
            
        }
        return served.size();
    }
    */
    
    /*
    private double deNormalize(double x){
        return (x*deNormalizingCoeff - deNormalizingConstant);
    }
    */
    
    /* For Uniform Service Function */
    /*
    public double evaluateNodeTrajWithIndexUniform(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery, HashSet<Integer> served, ResultPlotter mapView) {
        if (facilityQuery == null || facilityQuery.isEmpty() || qNode == null) {
            return 0;
        }

        if (nodeToIntraTrajQuadTree.get(qNode) == null) {
            return 0;
        }

        ArrayList<CoordinateArraySequence> queryUnionSet = facilityQuery;

        double serviceScore = 0;
        
        for (CoordinateArraySequence coordSeq : queryUnionSet) {
            //HashSet<Integer> start = new HashSet<Integer>();
            HashMap<Integer, Double> distanceFromStart = new HashMap<Integer, Double>();
            //HashSet<Integer> end = new HashSet<Integer>();
            HashMap<Integer, Double> distanceFromEnd = new HashMap<Integer, Double>();
            for (int i = 0; i < coordSeq.size(); i++) {
                double xmin = coordSeq.getX(i) - proximity;
                double ymin = coordSeq.getY(i) - proximity;
                double xmax = coordSeq.getX(i) + proximity;
                double ymax = coordSeq.getY(i) + proximity;

                ///Point[] points = nodeToIntraTrajQuadTree.get(qNode).searchIntersect(xmin, ymin, xmax, ymax);
                Node[] nodes = nodeToIntraTrajQuadTree.get(qNode).searchIntersect(xmin, ymin, xmax, ymax);

                for (Node node : nodes) {
                    Point point = node.getPoint();
                    
                    Pair<Integer, Boolean> data = (Pair<Integer, Boolean>) point.getValue();
                    if (data.getValue1()) { // getValue1() indicates if the point is start or end of a trajectory, true for start, false for end
                        double distance = Math.sqrt(Math.pow(xmin + proximity - point.getX(), 2) + Math.pow(ymin + proximity - point.getY(), 2));
                        //start.add(data.getValue0());
                        if (distanceFromStart.containsKey(data.getValue0()) && distance < distanceFromStart.get(data.getValue0())){
                            distanceFromStart.replace(data.getValue0(), distance);
                        }
                        else{
                            distanceFromStart.put(data.getValue0(), distance);
                        }
                    } else {
                        //end.add(data.getValue0());
                        double distance = Math.sqrt(Math.pow(xmin + proximity - point.getX(), 2) + Math.pow(ymin + proximity - point.getY(), 2));
                        //start.add(data.getValue0());
                        if (distanceFromEnd.containsKey(data.getValue0()) && distance < distanceFromEnd.get(data.getValue0())){
                            distanceFromEnd.replace(data.getValue0(), distance);
                        }
                        else{
                            distanceFromEnd.put(data.getValue0(), distance);
                        }
                    }
                }
            }
            for (Integer id : distanceFromEnd.keySet()) {
                if (distanceFromStart.containsKey(id) && distanceFromEnd.get(id) + distanceFromStart.get(id) <= proximity) {
                    //served.add(id);
                    serviceScore += (1-(distanceFromEnd.get(id) + distanceFromStart.get(id))/proximity);
                    //mapView.addUserTrajectory(trajectories.get(id));
                    //System.out.println(id);
                    System.out.println(trajectories.get(id).getX(0)+"\t"+trajectories.get(id).getY(0)+"\t"+trajectories.get(id).getX(1)+"\t"+trajectories.get(id).getY(1));
                }
            }
        }

        //return served.size();
        return serviceScore;
    }

    public ArrayList<Integer> getQNodeAllTrajsId(Node qNode) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        if (qNode == null) {
            return arrayList;
        }
        if (qNode.getNe() != null) {
            arrayList.addAll(getQNodeTrajsId(qNode.getNe()));
        }
        if (qNode.getNw() != null) {
            arrayList.addAll(getQNodeTrajsId(qNode.getNw()));
        }
        if (qNode.getSe() != null) {
            arrayList.addAll(getQNodeTrajsId(qNode.getSe()));
        }
        if (qNode.getSw() != null) {
            arrayList.addAll(getQNodeTrajsId(qNode.getSw()));
        }

        if (qNode.getNe() != null) {
            arrayList.addAll(getQNodeAllTrajsId(qNode.getNe()));
        }
        if (qNode.getNw() != null) {
            arrayList.addAll(getQNodeAllTrajsId(qNode.getNw()));
        }
        if (qNode.getSe() != null) {
            arrayList.addAll(getQNodeAllTrajsId(qNode.getSe()));
        }
        if (qNode.getSw() != null) {
            arrayList.addAll(getQNodeAllTrajsId(qNode.getSw()));
        }
        return arrayList;
    }

    public ArrayList<CoordinateArraySequence> getQNodeAllTrajs(Node qNode) {
        ArrayList<CoordinateArraySequence> arrayList = new ArrayList<CoordinateArraySequence>();
        if (qNode == null) {
            return arrayList;
        }
        if (qNode.getNe() != null) {
            arrayList.addAll(getQNodeTrajs(qNode.getNe()));
        }
        if (qNode.getNw() != null) {
            arrayList.addAll(getQNodeTrajs(qNode.getNw()));
        }
        if (qNode.getSe() != null) {
            arrayList.addAll(getQNodeTrajs(qNode.getSe()));
        }
        if (qNode.getSw() != null) {
            arrayList.addAll(getQNodeTrajs(qNode.getSw()));
        }

        if (qNode.getNe() != null) {
            arrayList.addAll(getQNodeAllTrajs(qNode.getNe()));
        }
        if (qNode.getNw() != null) {
            arrayList.addAll(getQNodeAllTrajs(qNode.getNw()));
        }
        if (qNode.getSe() != null) {
            arrayList.addAll(getQNodeAllTrajs(qNode.getSe()));
        }
        if (qNode.getSw() != null) {
            arrayList.addAll(getQNodeAllTrajs(qNode.getSw()));
        }
        return arrayList;
    }
    
    public void getAllInterNodeTrajsId(Node qNode) {
        if (qNode == null) {
            //System.out.println("Reached leaf!!");
            return;
        }
        
        ////ArrayList <Integer> trajIds = qNodeToTrajIdsMap.get(qNode);
        ArrayList <Integer> trajIds = null;
        
        if (trajIds == null || trajIds.size() == 0) {
            //System.out.println(qNode + " , No further inter node trajs!!");
            return;
        }
        
        //System.out.println(/* qNode + " , interNodeTrajCount = " + trajIds.size());


        if (qNode.getNe() != null) {
            getAllInterNodeTrajsId(qNode.getNe());
        }
        if (qNode.getNw() != null) {
            getAllInterNodeTrajsId(qNode.getNw());
        }
        if (qNode.getSe() != null) {
            getAllInterNodeTrajsId(qNode.getSe());
        }
        if (qNode.getSw() != null) {
            getAllInterNodeTrajsId(qNode.getSw());
        }
    }
    */
    
    /*
    public double evaluateNodeTrajBruteForce(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery) {
            if (facilityQuery == null || facilityQuery.isEmpty()) {
                    return 0;
        }
        ArrayList<CoordinateArraySequence> allTrajs = getQNodeAllTrajs(qNode);
        ArrayList<Integer> allTrajIds = getQNodeAllTrajsId(qNode);
            return calculateCover(allTrajs, allTrajIds, facilityQuery);
    }*/
    public double evaluateNodeTraj(Node qNode, ArrayList<CoordinateArraySequence> facilityQuery) {
        
        if (facilityQuery == null || facilityQuery.isEmpty()) {
            return 0;
        }
        ArrayList<CoordinateArraySequence> interNodeTrajs = getQNodeTrajs(qNode);
        ArrayList<Integer> interNodeTrajsId = getQNodeTrajsId(qNode);
        if (interNodeTrajs == null || interNodeTrajs.isEmpty()) {
            return 0;
        }
        return calculateCover(interNodeTrajs, interNodeTrajsId, facilityQuery);
    }

    public double calculateCover(ArrayList<CoordinateArraySequence> trajs, ArrayList<Integer> trajIds, ArrayList<CoordinateArraySequence> facilityQuery) {
        ArrayList<CoordinateArraySequence> queryUnionSet = facilityQuery;
        // for each connected component, currently each route is considered to be a connected component, may have to improve it
        double serviceValue = 0;
        Set<Integer> alreadyServed = new HashSet<Integer>();
        for (int i = 0; i < queryUnionSet.size(); i++) {
            if (queryUnionSet.get(i).size() < 2) {
                continue;
            }
            HashMap<CoordinateArraySequence, Set<Coordinate>> servedTrajs = new HashMap<CoordinateArraySequence, Set<Coordinate>>();
            // hashset instead of array/arraylist is used considering the general case where one user trajectory can consist of multiple points
            for (int j = 0; j < queryUnionSet.get(i).size(); j++) {
                Coordinate coord = queryUnionSet.get(i).getCoordinate(j);   // coord of a facility point
                // taking each point of facility subgraph we are checking against each point of inter node trajectories
                for (int k = 0; k < trajs.size(); k++) {
                    for (int l = 0; l < trajs.get(k).size(); l++) {
                        Coordinate point = trajs.get(k).getCoordinate(l);  // coord of a user trajectory point
                        // is it ok to calculate euclidean distance? Or should it be a rectange(square) with 2*proximity as side length?
                        double euclideanDistance = Math.sqrt(Math.pow((coord.x - point.x), 2) + Math.pow((coord.y - point.y), 2));
                        if (Math.abs(coord.x - point.x) > proximity || Math.abs(coord.y - point.y) > proximity) {
                            continue;
                        }
                        if (!servedTrajs.containsKey(trajs.get(k))) {
                            servedTrajs.put(trajs.get(k), new HashSet<Coordinate>());
                        }
                        servedTrajs.get(trajs.get(k)).add(point);
                    }
                }
            }
            for (int k = 0; k < trajs.size(); k++) {
                if (!servedTrajs.containsKey(trajs.get(k)) || servedTrajs.get(trajs.get(k)).size() < 2 || alreadyServed.contains(trajIds.get(k))) {
                    continue;
                }
                //System.out.println("<" + interNodeTrajs.get(k).getX(0) + ", " + interNodeTrajs.get(k).getY(0) + ">        <" + interNodeTrajs.get(k).getX(1) + ", " + interNodeTrajs.get(k).getY(1) + ">");
                //System.out.println(trajIds.get(k));
                alreadyServed.add(trajIds.get(k));
                serviceValue += 1;
            }
        }
        return serviceValue;
    }

    public ArrayList<CoordinateArraySequence> makeUnionSet(ArrayList<CoordinateArraySequence> trajectories) {
        // proximity ignored during union
        // each route is considered as a node
        // two nodes are connected by an edge if the corresponding routes share at least one commmon point
        int[] representative = new int[trajectories.size()];
        ArrayList<ArrayList<Integer>> stoppageSharingRoutes = new ArrayList<ArrayList<Integer>>();
        ArrayList<HashSet<Coordinate>> stoppages = new ArrayList<HashSet<Coordinate>>();

        // initialize
        for (int i = 0; i < trajectories.size(); i++) {
            representative[i] = -1;
            stoppageSharingRoutes.add(new ArrayList<Integer>());
            stoppages.add(new HashSet<Coordinate>());
            for (int j = 0; j < trajectories.get(i).size(); j++) {
                Coordinate point = trajectories.get(i).getCoordinate(j);
                stoppages.get(i).add(point);
            }
        }

        // edge detection between nodes
        for (int i = 0; i < trajectories.size(); i++) {
            boolean notFound = true;
            for (int j = 0; j < i && notFound; j++) {
                for (int l = 0; l < trajectories.get(i).size(); l++) {
                    Coordinate point = trajectories.get(i).getCoordinate(l);
                    if (stoppages.get(j).contains(point)) {
                        notFound = false;
                        stoppageSharingRoutes.get(i).add(j);
                        stoppageSharingRoutes.get(j).add(i);
                        break;
                    }
                }
            }
        }

        // CoordinateArraySequence refers to individual routes consisting of many points in trajectories [Assumption: Each route is a connected component]
        // CoordinateArraySequence refers to individual connected components in unionFacilities
        ArrayList<CoordinateArraySequence> unionFacilities = new ArrayList<CoordinateArraySequence>();
        ArrayList<Coordinate> connectedComponent = new ArrayList<Coordinate>();
        // BFS to update connected components
        int curRep = 0;
        for (int i = 0; i < trajectories.size(); i++) {
            if (representative[i] != -1) {
                continue;
            }
            Queue<Integer> nodes = new LinkedList<Integer>();
            nodes.add(i);
            for (int j = 0; j < trajectories.get(i).size(); j++) {
                connectedComponent.add(trajectories.get(i).getCoordinate(j));
            }
            representative[i] = curRep;
            while (!nodes.isEmpty()) {
                int curNode = nodes.peek();
                nodes.remove();
                for (int j = 0; j < stoppageSharingRoutes.get(curNode).size(); j++) {
                    int neighbor = stoppageSharingRoutes.get(curNode).get(j);
                    if (representative[neighbor] == -1) {
                        nodes.add(neighbor);
                        representative[neighbor] = curRep;
                        for (int k = 0; k < trajectories.get(neighbor).size(); k++) {
                            connectedComponent.add(trajectories.get(neighbor).getCoordinate(k));
                        }
                    }
                }
            }
            Coordinate[] connectedComponentArray = new Coordinate[connectedComponent.size()];
            connectedComponent.toArray(connectedComponentArray);
            unionFacilities.add(new CoordinateArraySequence(connectedComponentArray, connectedComponent.size()));
            connectedComponent.clear();
            connectedComponent = new ArrayList<Coordinate>();
            curRep++;
        }
        stoppageSharingRoutes.clear();
        stoppages.clear();
        return unionFacilities;
    }

    public ArrayList<CoordinateArraySequence> clipGraph(Node node, ArrayList<CoordinateArraySequence> facilityQuery) {
        ArrayList<CoordinateArraySequence> clippedSubgraphs = new ArrayList<CoordinateArraySequence>();
        //System.out.println("In clipGraph graph size = " + facilityQuery.size());
        for (int i = 0; i < facilityQuery.size(); i++) {
            ArrayList<Coordinate> route = new ArrayList<Coordinate>();
            for (int j = 0; j < facilityQuery.get(i).size(); j++) {
                //System.out.print(facilityQuery.get(i).getCoordinate(j) + " ");
                if (containsExtended(node, facilityQuery.get(i).getCoordinate(j))) {
                    route.add(new Coordinate(facilityQuery.get(i).getCoordinate(j).x, facilityQuery.get(i).getCoordinate(j).y));
                }
            }
            if (route.size() <= 1) {
                continue; // can't serve
            }
            Coordinate[] routePointsArray = new Coordinate[route.size()];
            route.toArray(routePointsArray);
            clippedSubgraphs.add(new CoordinateArraySequence(routePointsArray, route.size()));
        }
        return clippedSubgraphs;
    }
    
    
    public int getTotalNodeTraj(Node qNode) {
        if (qNodeTrajsCount.get(qNode) != null) {
            return qNodeTrajsCount.get(qNode);
        }
        return 0;
    }

}
