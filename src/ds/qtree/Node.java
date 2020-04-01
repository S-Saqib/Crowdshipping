package ds.qtree;

import java.util.ArrayList;

public class Node {

    private double x;
    private double y;
    private double w;
    private double h;
    private Node opt_parent;
    private ArrayList<Point> points;
    private final int nodeCapacity;
    private NodeType nodetype;
    private Node nw;
    private Node ne;
    private Node sw;
    private Node se;
    private long zCode;
    private int depth;
    

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
    public Node(double x, double y, double w, double h, Node opt_parent, long zCode, int depth) {
        this.nodetype = NodeType.EMPTY;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.opt_parent = opt_parent;
        this.zCode = zCode;
        this.depth = depth;
        this.points = new ArrayList<Point>();
        this.nodeCapacity = 64;
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

    public void setPoints(ArrayList <Point> points) {
        this.points = points;
    }

    public ArrayList<Point> getPoints() {
        return this.points;
    }
    
    public void addPoint(Point point){
        if (this.points == null) this.points = new ArrayList<Point>();
        this.points.add(point);
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
    
    public boolean isEmpty(){
        return points.size()==0;
    }
    
    public boolean hasSpaceForPoint(){
        return ((points == null) || (points.size() < nodeCapacity));
    }
    
    public Point removePoint(double x, double y){
        // may have to optimize this later
        Point removedPoint = null;
        for (Point point: points){
            if (point.getX()==x && point.getY()==y){
                removedPoint = point;
            }
        }
        if (removedPoint != null) points.remove(removedPoint);
        return removedPoint;
    }

    @Override
    public String toString() {
            return "Node [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
    }
}
