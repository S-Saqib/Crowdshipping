/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.trajgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Saqib
 */
public class TrajGraph {
    // not sure if the value of hashmap should be changed to a hashset instead
    // in that case parallel edge handling may be difficult
    // transport mode, higher time, probability etc. may not be captured
    HashMap <TrajGraphNode, ArrayList<TrajGraphNode>> trajGraphList;
    HashMap <Integer, HashSet<String>> stopToTrajIdMap;

    public TrajGraph(HashMap<TrajGraphNode, ArrayList<TrajGraphNode>> trajGraphList) {
        this.trajGraphList = trajGraphList;
        this.stopToTrajIdMap = new HashMap<>();
    }
    
    public TrajGraph(){
        this.trajGraphList = new HashMap<>();
        this.stopToTrajIdMap = new HashMap<>();
    }
    
    public void addNode (TrajGraphNode trajGraphNode){
        if (!trajGraphList.containsKey(trajGraphNode)){
            trajGraphList.put(trajGraphNode, new ArrayList<>());
        }
    }
    
    public void addStop (Integer stopId){
        if (!stopToTrajIdMap.containsKey(stopId)){
            stopToTrajIdMap.put(stopId, new HashSet<>());
        }
    }
    
    public void addToList(TrajGraphNode fromNode, TrajGraphNode toNode){
        //System.out.println(fromNode + " - " + toNode);
        //System.out.println("From = <" + fromNode.getStopId() + "," + fromNode.getTrajId() + "> ; "
        //                    + "To = <" + toNode.getStopId() + "," + toNode.getTrajId() + ">");
        if (fromNode == toNode) return;
        else if (fromNode.getStopId() == toNode.getStopId() && fromNode.getTrajId().equals(toNode.getTrajId())){
            return;
        }
        addNode(fromNode);
        trajGraphList.get(fromNode).add(toNode);
    }
    
    public void addTrajIdToStop(Integer stopId, String trajId){
        addStop(stopId);
        stopToTrajIdMap.get(stopId).add(trajId);
    }
    
    public ArrayList<TrajGraphNode> getNeighbors(TrajGraphNode node){
        if (trajGraphList.containsKey(node)){
            return trajGraphList.get(node);
        }
        return new ArrayList<>();
    }
    
    public HashSet<String> getStopToTrajIds(int stopId){
        if (!stopToTrajIdMap.containsKey(stopId) || stopToTrajIdMap.get(stopId) == null){
            return new HashSet<>();
        }
        return stopToTrajIdMap.get(stopId);
    }
    
    public HashSet<Integer> getAllStops(){
        return (HashSet<Integer>) stopToTrajIdMap.keySet();
    }

    public HashMap<Integer, HashSet<String>> getStopToTrajIdMap() {
        return stopToTrajIdMap;
    }
    
    public void printNeighborsOfNNodes(int numberOfNodes){
        if (numberOfNodes <= 0) return;
        for (Map.Entry<TrajGraphNode, ArrayList<TrajGraphNode>> entry : trajGraphList.entrySet()) {
            TrajGraphNode key = entry.getKey();
            ArrayList<TrajGraphNode> value = entry.getValue();
            // if (value.size() < 2 || value.size() > 10) continue;
            System.out.print("<" + key.getStopId() + "," + key.getTrajId() + ">" + " : ");
            for (TrajGraphNode neighbor : value){
                System.out.print("<" + neighbor.getStopId() + "," +  neighbor.getTrajId() + ">" + " | ");
            }
            System.out.println("");
            numberOfNodes--;
            if (numberOfNodes == 0) break;
        }
    }
    public void printNStopToTrajIds(int numberOfStops){
        if (numberOfStops <= 0) return;
        for (Map.Entry<Integer, HashSet<String>> entry : stopToTrajIdMap.entrySet()){
            int stopId = entry.getKey();
            HashSet<String> trajIds = entry.getValue();
            System.out.print("Stop " + stopId + " : ");
            for (String trajId : trajIds){
                System.out.print(trajId + " - ");
            }
            System.out.println("");
            numberOfStops--;
            if (numberOfStops == 0) break;
        }
    }
    
    public void printDetails(){
        System.out.println("Details of TrajGraph...");
        // trying to run dfs
        HashSet<TrajGraphNode> exploredNodes = new HashSet<>();   // color black
        HashSet<TrajGraphNode> visitedNodes = new HashSet<>();    // color gray
        
        HashMap<TrajGraphNode, Integer> reachabilityCount = new HashMap<>();
        HashMap<TrajGraphNode, Integer> connectedComponentNodeCount = new HashMap<>();
        
        for (TrajGraphNode node : trajGraphList.keySet()) {
            if (!exploredNodes.contains(node)) {
                int previouslyExploredNodeCount = exploredNodes.size();
                visitedNodes.add(node);
                exploredNodes = dfsVisit(node, exploredNodes, visitedNodes);
                int currentlyExploredNodeCount = exploredNodes.size() - previouslyExploredNodeCount;
                reachabilityCount.put(node, currentlyExploredNodeCount);
                visitedNodes.clear();
                exploredNodes.clear();
            }
        }
        
        System.out.println("Reachability stats ...");
        double maxVal, minVal, avgVal;
        maxVal = Double.MIN_VALUE;
        minVal = Double.MAX_VALUE;
        avgVal = 0;
        for (Map.Entry<TrajGraphNode, Integer> entry : reachabilityCount.entrySet()) {
            TrajGraphNode key = entry.getKey();
            Integer value = entry.getValue();
            avgVal += value;
            maxVal = Math.max(maxVal, value);
            minVal = Math.min(minVal, value);
            //System.out.println("<" + key.getStopId() + "," + key.getTrajId() + "> : " + value);
        }
        avgVal /= reachabilityCount.size();
        System.out.println("Reachability count: max = " + maxVal + ", min = " + minVal + ", avg = " + avgVal);
        
        /*
        visitedNodes.clear();
        exploredNodes.clear();
        for (TrajGraphNode node : trajGraphList.keySet()) {
            if (!exploredNodes.contains(node)) {
                int previouslyExploredNodeCount = exploredNodes.size();
                visitedNodes.add(node);
                exploredNodes = dfsVisit(node, exploredNodes, visitedNodes);
                int currentlyExploredNodeCount = exploredNodes.size() - previouslyExploredNodeCount;
                connectedComponentNodeCount.put(node, currentlyExploredNodeCount);
                visitedNodes.clear();
            }
        }
        
        System.out.println("Connected component stats ...");
        System.out.println("# of connected components = " + connectedComponentNodeCount.size());
        maxVal = Double.MIN_VALUE;
        minVal = Double.MAX_VALUE;
        avgVal = 0;
        for (Map.Entry<TrajGraphNode, Integer> entry : connectedComponentNodeCount.entrySet()) {
            TrajGraphNode key = entry.getKey();
            Integer value = entry.getValue();
            avgVal += value;
            maxVal = Math.max(maxVal, value);
            minVal = Math.min(minVal, value);
            System.out.println("<" + key.getStopId() + "," + key.getTrajId() + "> : " + value);
        }
        avgVal /= connectedComponentNodeCount.size();
        System.out.println("Connected component node count: max = " + maxVal + ", min = " + minVal + ", avg = " + avgVal);
        */
    }
    
    public HashSet<TrajGraphNode> dfsVisit(TrajGraphNode node, HashSet<TrajGraphNode> exploredNodes, HashSet<TrajGraphNode> visitedNodes){
        ArrayList<TrajGraphNode> neighbors = getNeighbors(node);
        for (TrajGraphNode neighborNode : neighbors){
            if (exploredNodes.contains(neighborNode)) continue;
            if (visitedNodes.contains(neighborNode)) continue;
            visitedNodes.add(neighborNode);
            exploredNodes = dfsVisit(neighborNode, exploredNodes, visitedNodes);
            exploredNodes.add(neighborNode);
        }
        exploredNodes.add(node);
        return exploredNodes;
    }
    
    public void printStats(){
        System.out.println("Summary of TrajGraph...");
        System.out.println("No. of stops = " + stopToTrajIdMap.size());
        System.out.println("No. of (stop, traj) = " + trajGraphList.size());
        // the above line is valid only as long as these two info in TrajGraphNode are used
        int stopToTrajCountAvg, stopToTrajCountMax, stopToTrajCountMin;
        stopToTrajCountAvg = 0;
        stopToTrajCountMax = Integer.MIN_VALUE;
        stopToTrajCountMin = Integer.MAX_VALUE;
        for (HashMap.Entry<Integer, HashSet<String>> stopTrajEntry : stopToTrajIdMap.entrySet()){
            stopToTrajCountAvg += stopTrajEntry.getValue().size();
            stopToTrajCountMax = Math.max(stopToTrajCountMax, stopTrajEntry.getValue().size());
            stopToTrajCountMin = Math.min(stopToTrajCountMin, stopTrajEntry.getValue().size());
        }
        System.out.println("Stop to traj count <max, min, avg> = <" + stopToTrajCountMax + ", " + stopToTrajCountMin + ", " + stopToTrajCountAvg*1.0/stopToTrajIdMap.size()+">");
        
        int avgDeg, minDeg, maxDeg;
        avgDeg = 0;
        minDeg = Integer.MAX_VALUE;
        maxDeg = Integer.MIN_VALUE;
        for (HashMap.Entry<TrajGraphNode, ArrayList<TrajGraphNode>> entry : trajGraphList.entrySet()){
            avgDeg += entry.getValue().size();
            minDeg = Math.min(minDeg, entry.getValue().size());
            maxDeg = Math.max(maxDeg, entry.getValue().size());
        }
        System.out.println("TrajGraph <max, min, avg> degree = <" + maxDeg + ", " + minDeg + ", " + avgDeg*1.0/trajGraphList.size()+">");
    }
}
