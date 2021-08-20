/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.trajgraph;

import ds.trajectory.TrajPoint;
import java.util.Objects;

/**
 *
 * @author Saqib
 */
public class TrajGraphNode/* implements Comparable<TrajGraphNode>*/{
    private int latKey;
    private int lonKey;
    private long timeInSec;
    private boolean isKeeper;
    private int stopId;
    private String trajId;
    
    // starting working with the following one
    public TrajGraphNode(int stopId, String trajId) {
        this.stopId = stopId;
        this.trajId = trajId;
        this.isKeeper = false;
        this.latKey = -1;
        this.lonKey = -1;
        this.timeInSec = -1;
    }

    public TrajGraphNode(int stopId, String trajId, boolean isKeeper) {
        this.stopId = stopId;
        this.trajId = trajId;
        this.isKeeper = isKeeper;
        this.latKey = -1;
        this.lonKey = -1;
        this.timeInSec = -1;
    }
    
    public TrajGraphNode(int stopId, String trajId, long timeInSec) {
        this.timeInSec = timeInSec;
        this.stopId = stopId;
        this.trajId = trajId;
        this.isKeeper = false;
        this.latKey = -1;
        this.lonKey = -1;
    }
    
    public TrajGraphNode( int stopId, boolean isKeeper, long timeInSec, String trajId) {
        this.timeInSec = timeInSec;
        this.stopId = stopId;
        this.isKeeper = isKeeper;
        this.latKey = -1;
        this.lonKey = -1;
        this.trajId = trajId;
    }

    public TrajGraphNode(int latKey, int lonKey, long timeInSec, boolean isKeeper, String trajId) {
        this.latKey = latKey;
        this.lonKey = lonKey;
        this.timeInSec = timeInSec;
        this.isKeeper = isKeeper;
        this.stopId = -1;
        this.trajId = trajId;
    }

    public String getTrajId() {
        return trajId;
    }
    
    public int getLatKey() {
        return latKey;
    }

    public int getLonKey() {
        return lonKey;
    }
    
    public long getTimeInSec() {
        return timeInSec;
    }
    
    public boolean getIsKeeper() {
        return isKeeper;
    }

    public int getStopId() {
        return stopId;
    }

    public void setTimeInSec(long timeInSec) {
        this.timeInSec = timeInSec;
    }
        
    /*
    @Override
    public int compareTo(TrajGraphNode nodeForComparison) {
        if (latKey < nodeForComparison.latKey) return -1;
        if (latKey > nodeForComparison.latKey) return 1;
        if (lonKey < nodeForComparison.lonKey) return -1;
        if (lonKey > nodeForComparison.lonKey) return 1;
        return 0;
    }
    */

    @Override
    public int hashCode() {
        // using the following members in hashing should suffice
        //int hash = Objects.hash(timeInSec, stopId, trajId);
        int hash = Objects.hash(stopId, trajId);
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
        final TrajGraphNode other = (TrajGraphNode) obj;
        if (this.latKey != other.latKey) {
            return false;
        }
        if (this.lonKey != other.lonKey) {
            return false;
        }
        // may have to uncomment out the following comparison if we want to distinguish between nodes temporally too
        /*
        if (this.timeInSec != other.timeInSec) {
            return false;
        }
        */
        if (this.isKeeper != other.isKeeper) {
            return false;
        }
        if (this.stopId != other.stopId) {
            return false;
        }
        if (!Objects.equals(this.trajId, other.trajId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TrajGraphNode{" + "timeInSec=" + timeInSec + ", stopId=" + stopId + ", trajId=" + trajId + '}';
    }

}