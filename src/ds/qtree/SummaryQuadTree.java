package ds.qtree;

import db.TrajStorage;
import ds.transformed_trajectory.TransformedTrajPoint;
import ds.transformed_trajectory.TransformedTrajectory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Datastructure: A point Quad Tree for representing 2D data. Each
 * region has the same ratio as the bounds for the tree.
 * <p/>
 * The implementation currently requires pre-determined bounds for data as it
 * can not rebalance itself to that degree.
 */
public class SummaryQuadTree {


    private Node root_;
    private int count_;
    private int nodeCount;
    private long zCode;
    private int height;
    private TrajStorage trajStorage;
    private long minTimeInSec;
    private int timeWindowInSec;
    private int nodeCapacity;
    private HashMap<Long, HashMap<Long,Integer>> summaryGraph;
    // first key = from where, second key = to where, second value = using how many trajs
    
    /**
     * Constructs a new quad tree.
     *
     * @param {double} minX Minimum x-value that can be held in tree.
     * @param {double} minY Minimum y-value that can be held in tree.
     * @param {double} maxX Maximum x-value that can be held in tree.
     * @param {double} maxY Maximum y-value that can be held in tree.
     */
    public SummaryQuadTree(TrajStorage trajStorage, double minX, double minY, double maxX, double maxY, long minTimeInSec, int timeWindowInSec, int nodeCapacity) {
        count_ = 0;
        nodeCount = 1;
        zCode = 0;
        height = 0;
        this.trajStorage = trajStorage;
        this.root_ = new Node(minX, minY, maxX - minX, maxY - minY, null, 0, nodeCapacity);
        this.minTimeInSec = minTimeInSec;
        this.timeWindowInSec = timeWindowInSec;
        this.nodeCapacity = nodeCapacity;
        summaryGraph = new HashMap<>();
    }

    /**
     * Returns a reference to the tree's root node.  Callers shouldn't modify nodes,
     * directly.  This is a convenience for visualization and debugging purposes.
     *
     * @return {Node} The root node.
     */
    public Node getRootNode() {
        return this.root_;
    }

    /**
     * Sets the value of an (x, y) point within the quad-tree.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @param {Object} value The value associated with the point.
     */
    public int set(double x, double y, long timeInSec, Object value, Object trajId) {

        Node root = this.root_;
        if (x < root.getX() || y < root.getY() || x > root.getX() + root.getW() || y > root.getY() + root.getH()) {
            throw new QuadTreeException("Out of bounds : (" + x + ", " + y + ")");
        }
        int splitCount=this.insert(root, new Point(x, y, timeInSec, value, trajId));
        if (splitCount >= 0) {
            this.count_++;
        }
        return splitCount;
    }

    /**
     * Gets the value of the point at (x, y) or null if the point is empty.
     *
     * @param {double} x The x-coordinate.
     * @param {double} y The y-coordinate.
     * @param {Object} opt_default The default value to return if the node doesn't
     *                 exist.
     * @return {*} The value of the node, the default value if the node
     *         doesn't exist, or undefined if the node doesn't exist and no default
     *         has been provided.
     */

    /**
     * @return {boolean} Whether the tree is empty.
     */
    public boolean isEmpty() {
        return this.root_.getNodeType() == NodeType.EMPTY;
    }

    /**
     * @return {number} The number of items in the tree.
     */
    public int getCount() {
        return this.count_;
    }
    
    public int getNodeCount(){
        return this.nodeCount;
    }
    
    public int getHeight(){
        return height;
    }

    /**
     * Removes all items from the tree.
     */
    public void clear() {
        this.root_.setNw(null);
        this.root_.setNe(null);
        this.root_.setSw(null);
        this.root_.setSe(null);
        this.root_.setNodeType(NodeType.EMPTY);
        this.root_.clearTimeBucketToDiskBlockIdMap();
        this.root_.setPointCount(0);
        this.trajStorage.clearQNodeToPointListMap();
        this.count_ = 0;
    }

    /**
     * Returns an array containing the coordinates of each point stored in the tree.
     * @return {Array.<Point>} Array of coordinates.
     */
    public Point[] getKeys() {
        final List<Point> arr = new ArrayList<Point>();
        this.traverse(this.root_, new Func_si() {
            public void call(SummaryQuadTree quadTree, Node node) {
                for (Point point : trajStorage.getPointsFromQNode(node)){
                    arr.add(point);
                }
            }
        });
        return arr.toArray(new Point[arr.size()]);
    }

    /**
     * Returns an array containing all values stored within the tree.
     * @return {Array.<Object>} The values stored within the tree.
     */
    public Object[] getValues() {
        final List<Object> arr = new ArrayList<Object>();
        this.traverse(this.root_, new Func_si() {
            public void call(SummaryQuadTree quadTree, Node node) {
                for (Point point : trajStorage.getPointsFromQNode(node)){
                    arr.add(point.getValue());
                }
            }
        });

        return arr.toArray(new Object[arr.size()]);
    }

    public Node[] searchIntersect(final double xmin, final double ymin, final double xmax, final double ymax) {
        final HashSet<Node> arr = new HashSet<Node>();
        this.navigate(this.root_, new Func_si() {
            public void call(SummaryQuadTree quadTree, Node node) {
                boolean intersects = intersects(xmin, ymin, xmax, ymax, node);
                if (intersects) arr.add(node);
            }
        }, xmin, ymin, xmax, ymax);
        return arr.toArray(new Node[arr.size()]);
    }
    
    // the following method is not used, so not updated like searchIntersect
    public Point[] searchWithin(final double xmin, final double ymin, final double xmax, final double ymax) {
        final List<Point> arr = new ArrayList<Point>();
        this.navigate(this.root_, new Func_si() {
            public void call(SummaryQuadTree quadTree, Node node) {
                // the following loop may be optimized if we can check node boundary only instead of all the points
                for (Point point: trajStorage.getPointsFromQNode(node)){
                    if (point.getX() > xmin && point.getX() < xmax && point.getY() > ymin && point.getY() < ymax) {
                        arr.add(point);
                    }
                }
            }
        }, xmin, ymin, xmax, ymax);
        return arr.toArray(new Point[arr.size()]);
    }

    public void navigate(Node node, Func_si func, double xmin, double ymin, double xmax, double ymax) {
        switch (node.getNodeType()) {
            case LEAF:
                func.call(this, node);
                break;

            case POINTER:
                if (intersects(xmin, ymin, xmax, ymax, node.getNe()))
                    this.navigate(node.getNe(), func, xmin, ymin, xmax, ymax);
                if (intersects(xmin, ymin, xmax, ymax, node.getSe()))
                    this.navigate(node.getSe(), func, xmin, ymin, xmax, ymax);
                if (intersects(xmin, ymin, xmax, ymax, node.getSw()))
                    this.navigate(node.getSw(), func, xmin, ymin, xmax, ymax);
                if (intersects(xmin, ymin, xmax, ymax, node.getNw()))
                    this.navigate(node.getNw(), func, xmin, ymin, xmax, ymax);
                break;
            default:
		break;
        }
    }

    private boolean intersects(double minX, double minY, double maxX, double maxY, Node node) {
        if (maxX < node.getX() || maxY < node.getY()) return false;
        if (minX > node.getX() + node.getW()) return false;
        if (minY > node.getY() + node.getH()) return false;
        return true;
    }
    /**
     * Clones the quad-tree and returns the new instance.
     * @return {QuadTree} A clone of the tree.
     */
    public SummaryQuadTree clone() {
        double x1 = this.root_.getX();
        double y1 = this.root_.getY();
        double x2 = x1 + this.root_.getW();
        double y2 = y1 + this.root_.getH();
        final SummaryQuadTree clone = new SummaryQuadTree(new TrajStorage(trajStorage.getTrajData()), x1, y1, x2, y2, this.minTimeInSec, this.timeWindowInSec, nodeCapacity);
        // This is inefficient as the clone needs to recalculate the structure of the
        // tree, even though we know it already.  But this is easier and can be
        // optimized when/if needed.
        this.traverse(this.root_, new Func_si() {
            public void call(SummaryQuadTree quadTree, Node node) {
                for (Point point: trajStorage.getPointsFromQNode(node)){
                    clone.set(point.getX(), point.getY(), point.getTimeInSec(), point.getValue(), point.getTraj_id());
                }
            }
        });
        return clone;
    }

    /**
     * Traverses the tree depth-first, with quadrants being traversed in clockwise
     * order (NE, SE, SW, NW).  The provided function will be called for each
     * leaf node that is encountered.
     * @param {QuadTree.Node} node The current node.
     * @param {function(QuadTree.Node)} fn The function to call
     *     for each leaf node. This function takes the node as an argument, and its
     *     return value is irrelevant.
     * @private
     */
    public void traverse(Node node, Func_si func) {
        switch (node.getNodeType()) {
            case LEAF:
                func.call(this, node);
                break;

            case POINTER:
                this.traverse(node.getNe(), func);
                this.traverse(node.getSe(), func);
                this.traverse(node.getSw(), func);
                this.traverse(node.getNw(), func);
                break;
		default:
			break;
        }
    }

    /**
     * Finds a leaf node with the same (x, y) coordinates as the target point, or
     * null if no point exists.
     * @param {QuadTree.Node} node The node to search in.
     * @param {number} x The x-coordinate of the point to search for.
     * @param {number} y The y-coordinate of the point to search for.
     * @return {QuadTree.Node} The leaf node that matches the target,
     *     or null if it doesn't exist.
     * @private
     */
    public Node find(Node node, double x, double y) {
        Node response = null;
        switch (node.getNodeType()) {
            case EMPTY:
                break;

            case LEAF:
                for (Point point: trajStorage.getPointsFromQNode(node)){
                    if (point.getX() == x && point.getY() == y){
                        response = node;
                        break;
                    }
                }
                break;

            case POINTER:
                response = this.find(this.getQuadrantForPoint(node, x, y), x, y);
                break;

            default:
                throw new QuadTreeException("Invalid nodeType");
        }
        return response;
    }
    
    /**
     * Inserts a point into the tree, updating the tree's structure if necessary.
     * @param {.QuadTree.Node} parent The parent to insert the point
     *     into.
     * @param {QuadTree.Point} point The point to insert.
     * @return {boolean} True if a new node was added to the tree; False if a node
     *     already existed with the corresponding coordinates and had its value
     *     reset.
     * @private
     */
    private int insert(Node parent, Point point) {
        int result = 0;
        switch (parent.getNodeType()) {
            case EMPTY:
                this.addPointToNode(parent, point);
                result = 0;
                break;
            case LEAF:
                ArrayList<Point> parentPoints = new ArrayList<Point>(trajStorage.getPointsFromQNode(parent));
                for (Point pt: parentPoints){
                    if (pt.equals(point)) {
                        result = -1;
                        break;
                    }
                    if (pt.getX() == point.getX() && pt.getY() == point.getY()) {
                        //this.setPointForNode(parent, point);
                        result = -2;    // indicates same spatial point found
                    }
                }
                if (result != -1) {
                    if (result == -2){
                        this.addPointToNode(parent, point);
                        result = 0;
                    }
                    else if (!parent.hasSpaceForPoint()){
                        //System.out.println("Trouble!!");
                        this.split(parent);
                        // now parent has node type pointer
                        this.insert(parent, point);
                        result = 1;
                    }
                    else{
                        //System.out.println("Cool!!");
                        this.addPointToNode(parent, point);
                    }
                }
                else{
                    result = 0;     // result = -1 reverted
                }
                break;
            case POINTER:
                result = this.insert(this.getQuadrantForPoint(parent, point.getX(), point.getY()), point);
                break;

            default:
                throw new QuadTreeException("Invalid nodeType in parent");
        }
        return result;
    }

    /**
     * Converts a leaf node to a pointer node and reinserts the node's point into
     * the correct child.
     * @param {QuadTree.Node} node The node to split.
     * @private
     */
    private void split(Node node) {
        ArrayList <Point> oldPoints = trajStorage.getPointsFromQNode(node);

        node.setNodeType(NodeType.POINTER);
        trajStorage.removePointListFromQNode(node);
        node.setPointCount(0);
        
        double x = node.getX();
        double y = node.getY();
        double hw = node.getW() / 2;
        double hh = node.getH() / 2;
        
        int childDepth = node.getDepth() + 1;
        height = Integer.max(height, childDepth);

        node.setNw(new Node(x, y, hw, hh, node, childDepth, nodeCapacity));
        node.setNe(new Node(x + hw, y, hw, hh, node, childDepth, nodeCapacity));
        node.setSw(new Node(x, y + hh, hw, hh, node, childDepth, nodeCapacity));
        node.setSe(new Node(x + hw, y + hh, hw, hh, node, childDepth, nodeCapacity));

        for (Point point: oldPoints){
            this.insert(node, point);
        }
        
        this.nodeCount += 4;
    }

    /**
     * Returns the child quadrant within a node that contains the given (x, y)
     * coordinate.
     * @param {QuadTree.Node} parent The node.
     * @param {number} x The x-coordinate to look for.
     * @param {number} y The y-coordinate to look for.
     * @return {QuadTree.Node} The child quadrant that contains the
     *     point.
     * @private
     */
    private Node getQuadrantForPoint(Node parent, double x, double y) {
        double mx = parent.getX() + parent.getW() / 2;
        double my = parent.getY() + parent.getH() / 2;
        if (x < mx) {
            return y < my ? parent.getNw() : parent.getSw();
        } else {
            return y < my ? parent.getNe() : parent.getSe();
        }
    }

    /**
     * Sets the point for a node, as long as the node is a leaf or empty.
     * @param {QuadTree.Node} node The node to set the point for.
     * @param {QuadTree.Point} point The point to set.
     * @private
     */
    private void addPointToNode(Node node, Point point) {
        if (node.getNodeType() == NodeType.POINTER) {
            System.out.println("Can not set point for node of type POINTER for node " + node);
            throw new QuadTreeException("Can not set point for node of type POINTER");
        }
        if (node.getNodeType() != NodeType.LEAF) node.setNodeType(NodeType.LEAF);
        // ArrayList <Point> points= node.getPoints();
        // if (points == null) points = new ArrayList<Point>();
        // points.add(point);
        // node.setPoints(points);
        trajStorage.addPointToQNode(node, point);
        node.incPointCount();
    }
    
    public int getTimeIndex(long timeStamp){
        if (timeStamp < minTimeInSec) return -1;
        return (int)((timeStamp - minTimeInSec)/timeWindowInSec);
    }
    
    // do not need to hit DB for the following mehtod
    public long assignZCodesToLeaves(Node node, long zCode){
        if (node.getNodeType() == NodeType.EMPTY){
            // just added for safety, should not reach here
            return zCode;
        }
        if (node.getNodeType() == NodeType.LEAF){
            node.setZCode(zCode);
            return zCode;
        }
        node.setZCode(-1);
        zCode = assignZCodesToLeaves(node.getNw(), zCode) + 1;
        zCode = assignZCodesToLeaves(node.getNe(), zCode) + 1;
        zCode = assignZCodesToLeaves(node.getSw(), zCode) + 1;
        zCode = assignZCodesToLeaves(node.getSe(), zCode) + 1;
        return zCode;
    }
    
    // saves transformed trajectories in trajStorage, the spatio-temporal transformation works on their points
    public void transformTrajectories(Node node){
        if (node.getNodeType() == NodeType.EMPTY){
            // just added for safety, should not reach here
            return;
        }
        if (node.getNodeType() == NodeType.LEAF){
            ArrayList <Point> pointList = trajStorage.getPointsFromQNode(node);
            for (Point point : pointList){
                int timeIndex = getTimeIndex(point.getTimeInSec());
                node.addTimeKey(timeIndex);
                if (timeIndex > 0){
                    long qNodeIndex = node.getZCode();
                    TransformedTrajPoint transformedTrajPoint = new TransformedTrajPoint(qNodeIndex, timeIndex);
                    String trajId = (String)point.getTraj_id();
                    trajStorage.addValueToTransformedTrajData(trajId, transformedTrajPoint);
                }
            }
            return;
        }
        transformTrajectories(node.getNw());
        transformTrajectories(node.getNe());
        transformTrajectories(node.getSw());
        transformTrajectories(node.getSe());
    }
    
    public void tagDiskBlockIdsToNodes(Node node){
        if (node.getNodeType() == NodeType.EMPTY){
            // just added for safety, should not reach here
            return;
        }
        if (node.getNodeType() == NodeType.LEAF){
            ArrayList <Point> pointList = trajStorage.getPointsFromQNode(node);
            for (Point point : pointList){
                int timeIndex = getTimeIndex(point.getTimeInSec());
                if (timeIndex > 0){
                    String trajId = (String)point.getTraj_id();
                    Object diskBlockId = trajStorage.getDiskBlockIdByTrajId(trajId);
                    node.addDiskBlockId(timeIndex, diskBlockId);
                }
            }
            return;
        }
        tagDiskBlockIdsToNodes(node.getNw());
        tagDiskBlockIdsToNodes(node.getNe());
        tagDiskBlockIdsToNodes(node.getSw());
        tagDiskBlockIdsToNodes(node.getSe());
    }
    
    public void buildSummaryNetwork(){
        HashMap<String,TransformedTrajectory> summaryTrajs = trajStorage.getSummaryTrajData();
        for (HashMap.Entry<String,TransformedTrajectory> entry : summaryTrajs.entrySet()){
            TransformedTrajectory summaryTraj = entry.getValue();
            long prev = summaryTraj.getTransformedPointList().first().getqNodeIndex();
            long qNodeZCode = -1;
            int trajReachabilityCount = 0;
            summaryGraph.put(prev, new HashMap<>());
            for (TransformedTrajPoint summaryTrajPoint : summaryTraj.getTransformedPointList()){
                qNodeZCode = summaryTrajPoint.getqNodeIndex();
                if (prev != qNodeZCode){
                    if (!summaryGraph.get(prev).containsKey(qNodeZCode)){
                        summaryGraph.get(prev).put(qNodeZCode, 0);
                    }
                    trajReachabilityCount = summaryGraph.get(prev).get(qNodeZCode);
                    summaryGraph.get(prev).put(qNodeZCode, trajReachabilityCount+1);
                }
                prev = qNodeZCode;
                if (!summaryGraph.containsKey(prev)) summaryGraph.put(prev, new HashMap<>());
            }
            
        }
    }
    
    public void transformTrajSummary(Node node){
        if (node.getNodeType() == NodeType.EMPTY){
            // just added for safety, should not reach here
            return;
        }
        if (node.getNodeType() == NodeType.LEAF){
            ArrayList <Point> pointList = trajStorage.getPointsFromQNode(node);
            for (Point point : pointList){
                long qNodeIndex = node.getZCode();
                TransformedTrajPoint transformedTrajPoint = new TransformedTrajPoint(qNodeIndex, point.getTimeInSec());
                String trajId = (String)point.getTraj_id();
                trajStorage.addValueToSummaryTrajData(trajId, transformedTrajPoint);
            }
            return;
        }
        transformTrajSummary(node.getNw());
        transformTrajSummary(node.getNe());
        transformTrajSummary(node.getSw());
        transformTrajSummary(node.getSe());
    }
    
    public void printSummaryGraph(){
        for (Map.Entry<Long, HashMap<Long, Integer>> entry : summaryGraph.entrySet()){
            long from = entry.getKey();
            System.out.print(from + " : ");
            for (Map.Entry<Long,Integer> entry1 : entry.getValue().entrySet()){
                System.out.print("<" + entry1.getKey() + "," + entry1.getValue()+ ">, ");
            }
            System.out.println("");
        }
    }
    
    public void printSummaryGraphSummary(){
        int noOfVertices = summaryGraph.size();
        int noOfZeroDegreeVertices = 0;
        int maxDegree = (int) -1e9;
        int minDegree = (int) 1e9;
        int avgDegree = 0;
        for (Map.Entry<Long, HashMap<Long, Integer>> entry : summaryGraph.entrySet()){
            long from = entry.getKey();
            if (entry.getValue().isEmpty()){
                noOfZeroDegreeVertices++;
                continue;
            }
            int degree = entry.getValue().size();
            maxDegree = Math.max(maxDegree, degree);
            minDegree = Math.min(maxDegree, degree);
            avgDegree += degree;
        }
        System.out.println("No. of vertices = " + noOfVertices);
        System.out.println("No. of 0 deg vertices = " + noOfZeroDegreeVertices);
        System.out.println("Max degree = " + maxDegree);
        System.out.println("Min degree = " + minDegree);
        System.out.println("Sum of degree = " + avgDegree);
        System.out.println("Avg degree = " + avgDegree*1.0/noOfVertices);
        System.out.println("Avg degree excluding 0 deg vertices= " + avgDegree*1.0/(noOfVertices-noOfZeroDegreeVertices));
    }
    
}