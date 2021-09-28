/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;

import ds.trajgraph.TrajGraphNode;
import java.util.Objects;

/**
 *
 * @author Saqib
 */
public class NodeState implements Comparable<NodeState> {
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
        if (trajGraphNode.getTimeInSec() < o.getTrajGraphNode().getTimeInSec()) return -1;
        if (trajGraphNode.getTimeInSec() > o.getTrajGraphNode().getTimeInSec()) return 1;
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
