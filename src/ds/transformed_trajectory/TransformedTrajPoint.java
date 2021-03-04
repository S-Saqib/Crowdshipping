/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.transformed_trajectory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * @author Saqib
 */
public class TransformedTrajPoint {
    private long qNodeIndex;
    private int timeIndex;

    public TransformedTrajPoint() {
        qNodeIndex = -1;
        timeIndex = -1;
    }

    public TransformedTrajPoint(long qNodeIndex, int timeIndex) {
        this.qNodeIndex = qNodeIndex;
        this.timeIndex = timeIndex;
    }

    public long getqNodeIndex() {
        return qNodeIndex;
    }

    public void setqNodeIndex(long qNodeIndex) {
        this.qNodeIndex = qNodeIndex;
    }

    public int getTimeIndex() {
        return timeIndex;
    }

    public void setTimeIndex(int timeIndex) {
        this.timeIndex = timeIndex;
    }

    @Override
    public String toString() {
        return "Transformed TrajPoint{" + "QNodeIndex (Z-code) = " + qNodeIndex + " , timeIndex = " + timeIndex + '}';
    }
    
    
}
