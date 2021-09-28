package ds.transformed_trajectory;

import com.github.davidmoten.rtreemulti.geometry.Rectangle;
import java.util.ArrayList;

import java.util.TreeSet;

public class TransformedTrajectory {
    
    private TreeSet<TransformedTrajPoint> transformedPointList;
    private Rectangle envelope;
    private long userId;
    private String trajId;
    private String contactNo;

    public TransformedTrajectory() {
        transformedPointList = new TreeSet<TransformedTrajPoint>(new TransformedTrajPointComparator());
        trajId = new String();
        contactNo = null;
        userId = -1;
    }
    
    public TransformedTrajectory(String anonymizedId, long userId){
        transformedPointList = new TreeSet<TransformedTrajPoint>(new TransformedTrajPointComparator());
        this.trajId = anonymizedId;
        this.userId = userId;
        contactNo = null;
    }
    
    public TransformedTrajectory(TreeSet<TransformedTrajPoint> pointList) {
        this.transformedPointList = pointList;
        trajId = new String();
        contactNo = null;
        userId = -1;
    }
    
    public void setTransformedPointList(TreeSet<TransformedTrajPoint> transformedPointList) {
        this.transformedPointList = transformedPointList;
    }

    public TreeSet<TransformedTrajPoint> getTransformedPointList() {
        return this.transformedPointList;
    }

    public String getTrajId() {
        return trajId;
    }

    public void setTrajId(String trajId) {
        this.trajId = trajId;
    }
    
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }
    
    public void addTransformedTrajPoint(TransformedTrajPoint p){
        if (transformedPointList == null){
            // should not come here
            transformedPointList = new TreeSet<TransformedTrajPoint>(new TransformedTrajPointComparator());
        }
        transformedPointList.add(p);
    }
    
    public boolean contains(TransformedTrajPoint p){
        return transformedPointList.contains(p);
    }

    public void setEnvelope() {
        double minQnodeIndex, maxQnodeIndex, minTimeIndex, maxTimeIndex;
        minQnodeIndex = minTimeIndex = Double.MAX_VALUE;
        maxQnodeIndex = maxTimeIndex = Double.MIN_VALUE;
        for (TransformedTrajPoint p: this.transformedPointList){
            minQnodeIndex = Math.min(minQnodeIndex, p.getqNodeIndex());
            maxQnodeIndex = Math.max(maxQnodeIndex, p.getqNodeIndex());
            minTimeIndex = Math.min(minTimeIndex, p.getTimeIndex());
            maxTimeIndex = Math.max(maxTimeIndex, p.getTimeIndex());
        } 
        double[] mins = new double[]{minQnodeIndex, minTimeIndex};
        double[] maxes = new double[]{maxQnodeIndex, maxTimeIndex};
        this.envelope = Rectangle.create(mins, maxes);
    }

    public Rectangle getEnvelope() {
        return envelope;
    }
    
    

    @Override
    public String toString() {
        String trajString = "(Transformed) Trajectory ID = " + userId + " , Anonymized ID = " + trajId + " , Contact No. = " + contactNo + "\n";
        trajString += transformedPointList.toString() + "\n";
        return trajString;
    }
    
    
        
}
