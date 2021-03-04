package ds.trajectory;

import java.util.ArrayList;
import java.util.TreeSet;

public class Trajectory {
    
    private int userId;
    private String trajId;
    private int fromSample;
    private ArrayList <TrajEdge> trajEdges;
    private double probability;
    // not sure if we should keep probability with traj or traj edge

    public Trajectory() {
        this.trajEdges = new ArrayList<>();
    }
    
    public Trajectory(int userId, String trajId) {
        this.userId = userId;
        this.trajId = trajId;
        this.trajEdges = new ArrayList<>();
    }

    public String getTrajId() {
        return trajId;
    }

    public void setTrajId(String trajId) {
        this.trajId = trajId;
    }

    public ArrayList<TrajEdge> getTrajEdges() {
        return trajEdges;
    }

    public void setTrajEdges(ArrayList<TrajEdge> trajEdges) {
        this.trajEdges = trajEdges;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getFromSample() {
        return fromSample;
    }

    public void setFromSample(int fromSample) {
        this.fromSample = fromSample;
    }
    
    public TreeSet<TrajPoint> getPointList() {
        TreeSet<TrajPoint> pointList = new TreeSet<TrajPoint>(new TrajPointComparator());
        for (TrajEdge trajEdge : trajEdges){
            pointList.add(trajEdge.getStartsFrom());
            pointList.add(trajEdge.getEndsAt());
        }
        return pointList;
    }
    
    public String getAnonymizedId(){
        return Integer.toString(this.userId);
    }

    @Override
    public String toString() {
        String trajString = "Trajectory (" + userId + "," + trajId + ") has " + trajEdges.size() + " edges:\n";
        for (TrajEdge trajEdge : trajEdges){
            trajString += trajEdge.toString();
        }
        //trajString += "\n";
        return trajString;
    }
    
}
