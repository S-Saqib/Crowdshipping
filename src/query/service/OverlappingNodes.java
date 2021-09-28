/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;

import ds.qtree.Node;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Saqib
 */
public class OverlappingNodes {
    private Node node;
    private HashSet<Node> from;
    private HashSet<Node> to;

    public OverlappingNodes() {
        from = new HashSet<>();
        to = new HashSet<>();
    }
    
    public OverlappingNodes(Node node) {
        this.node = node;
        from = new HashSet<>();
        to = new HashSet<>();
    }
    
    public void addToFrom(Node node){
        from.add(node);
    }
    
    public void addToTo(Node node){
        to.add(node);
    }
    
    public ArrayList<Node> getAllNodes(){
        HashSet <Node> allNodes = new HashSet<>();
        allNodes.add(node);
        for (Node node : from) allNodes.add(node);
        for (Node node : to) allNodes.add(node);
        return new ArrayList<>(allNodes);
    }
    
}