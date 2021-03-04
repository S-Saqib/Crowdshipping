/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.trajectory;

/**
 *
 * @author Saqib
 */
public class TrajEdge {
    private TrajPoint startsFrom;
    private TrajPoint endsAt;
    private double probability;
    // not sure if we should keep probability with traj or traj edge

    public TrajEdge() {
        
    }
    /*
    public TrajEdge(TrajEdge trajEdge){
        this.startsFrom = trajEdge.startsFrom;
        this.endsAt = trajEdge.endsAt;
        this.probability = trajEdge.probability;
    }
    */
    public TrajPoint getStartsFrom() {
        return startsFrom;
    }

    public void setStartsFrom(TrajPoint startsFrom) {
        this.startsFrom = startsFrom;
    }

    public TrajPoint getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(TrajPoint endsAt) {
        this.endsAt = endsAt;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    @Override
    public String toString() {
        return "[from " + startsFrom.toString() + " to " + endsAt.toString() + "] - ";
    }
        
}
