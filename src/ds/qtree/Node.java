package ds.qtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Node {

    private double x;
    private double y;
    private double w;
    private double h;
    private Node opt_parent;
    private final int nodeCapacity;
    private int pointCount;
    private NodeType nodetype;
    private Node nw;
    private Node ne;
    private Node sw;
    private Node se;
    private long zCode;
    private int depth;
    private HashMap<Integer, HashSet<Object>> timeBucketToDiskBlockIdMap;
    

    /**
     * Constructs a new quad tree node.
     *
     * @param {double} x X-coordiate of node.
     * @param {double} y Y-coordinate of node.
     * @param {double} w Width of node.
     * @param {double} h Height of node.
     * @param {Node}   opt_parent Optional parent node.
     * @constructor
     */
    public Node(double x, double y, double w, double h, Node opt_parent, int depth) {
        this.nodetype = NodeType.EMPTY;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.opt_parent = opt_parent;
        this.depth = depth;
        this.pointCount = 0;
        this.nodeCapacity = 32;
        this.timeBucketToDiskBlockIdMap = new HashMap<Integer, HashSet<Object>>();
        this.zCode = -1;
    }
    
    // added to support summary index
    public Node(double x, double y, double w, double h, Node opt_parent, int depth, int nodeCapacity) {
        this.nodetype = NodeType.EMPTY;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.opt_parent = opt_parent;
        this.depth = depth;
        this.pointCount = 0;
        this.nodeCapacity = nodeCapacity;
        this.timeBucketToDiskBlockIdMap = new HashMap<Integer, HashSet<Object>>();
        this.zCode = -1;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public Node getParent() {
        return opt_parent;
    }

    public void setParent(Node opt_parent) {
        this.opt_parent = opt_parent;
    }

    public void setNodeType(NodeType nodetype) {
        this.nodetype = nodetype;
    }

    public NodeType getNodeType() {
        return this.nodetype;
    }


    public void setNw(Node nw) {
        this.nw = nw;
    }

    public void setNe(Node ne) {
        this.ne = ne;
    }

    public void setSw(Node sw) {
        this.sw = sw;
    }

    public void setSe(Node se) {
        this.se = se;
    }

    public Node getNe() {
        return ne;
    }

    public Node getNw() {
        return nw;
    }

    public Node getSw() {
        return sw;
    }

    public Node getSe() {
        return se;
    }
    
    public void setZCode(long zCode){
        this.zCode = zCode;
    }
    
    public long getZCode(){
        return zCode;
    }
    
    public void setDepth(int depth){
        this.depth = depth;
    }
    
    public int getDepth(){
        return depth;
    }
    
    public void setPointCount(int pointCount){
        this.pointCount = pointCount;
    }
    
    public int getPointCount(){
        return pointCount;
    }
    
    public void incPointCount(){
        pointCount++;
    }
    
    public boolean isEmpty(){
        return (pointCount == 0);
    }
    
    public boolean hasSpaceForPoint(){
        return (pointCount < nodeCapacity);
    }
    
    public void clearTimeBucketToDiskBlockIdMap(){
        this.timeBucketToDiskBlockIdMap.clear();
    }
    
    public void addTimeKey(int timeBucket){
        if (timeBucket < 0) return;
        if (!timeBucketToDiskBlockIdMap.containsKey(new Integer(timeBucket))){
            timeBucketToDiskBlockIdMap.put(timeBucket, new HashSet<Object>());
        }
    }
    
    public void addDiskBlockId(int timeBucket, Object diskBlockId){
        if (timeBucket < 0) return;
        addTimeKey(timeBucket);
        timeBucketToDiskBlockIdMap.get(timeBucket).add(diskBlockId);
    }
    
    public ArrayList<Object> getDiskBlocksByQNodeTimeIndex(int timeBucket){
        if (timeBucket < 0) return null;
        if (!timeBucketToDiskBlockIdMap.containsKey(timeBucket)) return null;
        return new ArrayList<Object>(timeBucketToDiskBlockIdMap.get(timeBucket));
    }

    @Override
    public String toString() {
            return "Node [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
    }
}
