/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db;

import ds.qtree.Node;
import ds.qtree.Point;
import ds.trajectory.Trajectory;
import ds.transformed_trajectory.TransformedTrajPoint;
import ds.transformed_trajectory.TransformedTrajectory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Saqib
 */
public class TrajStorage {
    private HashMap<String, Trajectory> trajData;   // normalized
    private HashMap<String, Trajectory> originalTrajData;
    private HashMap<String, TransformedTrajectory> transformedTrajData;
    private HashMap<String, TransformedTrajectory> summaryTrajData;
    private final int chunkSize;
    private int cursor;
    private HashMap<Node, ArrayList<Point>> tempQNodeToPointListMap;
    private HashMap<String, Integer> trajIdToDiskBlockIdMap;
    private HashMap<Integer, ArrayList<String>> diskBlockIdToTrajIdListMap;
    
    public TrajStorage(HashMap<String, Trajectory> trajData, HashMap<String, Trajectory> originalTrajData) {
        this.trajData = trajData;
        this.originalTrajData =  originalTrajData;
        chunkSize = 10000;
        cursor = 0;
        this.tempQNodeToPointListMap = new HashMap<Node, ArrayList<Point>>();
        this.transformedTrajData = new HashMap<String, TransformedTrajectory>();
        this.summaryTrajData = new HashMap<String, TransformedTrajectory>();
        this.diskBlockIdToTrajIdListMap = new HashMap<Integer, ArrayList<String>>();
    }
    
    public TrajStorage(HashMap<String, Trajectory> trajData) {
        this.trajData = trajData;
        chunkSize = 10000;
        cursor = 0;
        this.tempQNodeToPointListMap = new HashMap<Node, ArrayList<Point>>();
        this.transformedTrajData = new HashMap<String, TransformedTrajectory>();
        this.summaryTrajData = new HashMap<String, TransformedTrajectory>();
        this.diskBlockIdToTrajIdListMap = new HashMap<Integer, ArrayList<String>>();
    }

    public TrajStorage() {
        this.trajData = null;
        chunkSize = 100;
        cursor = 0;
        this.tempQNodeToPointListMap = null;
        this.transformedTrajData = null;
        this.summaryTrajData = null;
        this.diskBlockIdToTrajIdListMap = null;
    }

    public HashMap<String, Trajectory> getTrajData() {
        return trajData;
    }

    public void setTrajData(HashMap<String, Trajectory> trajData) {
        this.trajData = trajData;
    }
    
    public ArrayList<Trajectory> getTrajDataAsList() {
        return new ArrayList<Trajectory>(trajData.values());
    }
    
    private boolean hasNext(){
        return cursor < trajData.size();
    }
    
    private void resetCursor(){
        cursor = 0;
    }
    
    // gives next x trajectories where x = chunkSize
    public ArrayList<Trajectory> getNextChunkAsList(){
        if (hasNext()){
            int from = cursor;
            int to = cursor + chunkSize;
            if (to > trajData.size()){
                to = trajData.size();
            }
            cursor = to;
            //System.out.println("Returning chunk from " + from + " to " + to);
            return new ArrayList<Trajectory>(getTrajDataAsList().subList(from, to));
        }
        else{
            resetCursor();
            return null;
        }
    }
    
    public Trajectory getTrajectoryById(String Id){
        Trajectory trajectory = trajData.get(Id);
        return trajectory;
    }
    
    // all points of a leaf should be propagated to its child during splitting
    public ArrayList<Point> getPointsFromQNode(Node qNode){
        if (tempQNodeToPointListMap.containsKey(qNode)){
            return tempQNodeToPointListMap.get(qNode);
        }
        return null;
    }
    
    public void addPointToQNode(Node qNode, Point point){
        if (!tempQNodeToPointListMap.containsKey(qNode)){
            tempQNodeToPointListMap.put(qNode, new ArrayList<Point>());
        }
        tempQNodeToPointListMap.get(qNode).add(point);
    }
    
    // a node should be removed after it is no longer leaf (becomes pointer)
    public void removePointListFromQNode(Node qNode){
        if (tempQNodeToPointListMap.containsKey(qNode)){
            tempQNodeToPointListMap.remove(qNode);
        }
    }
    
    public void clearQNodeToPointListMap(){
        tempQNodeToPointListMap.clear();
        tempQNodeToPointListMap = new HashMap<Node, ArrayList<Point>>();
    }
    
    // should we pass transformed trajectory data chunk by chunk?
    public HashMap<String, TransformedTrajectory> getTransformedTrajData() {
        return transformedTrajData;
    }

    public void setTransformedTrajData(HashMap<String, TransformedTrajectory> transformedTrajData) {
        this.transformedTrajData = transformedTrajData;
    }
    
    public ArrayList<TransformedTrajectory> getTransformedTrajDataAsList() {
        return new ArrayList<TransformedTrajectory>(transformedTrajData.values());
    }
    
    public TransformedTrajectory getTransformedTrajectoryById(String Id){
        TransformedTrajectory transformedTrajectory = transformedTrajData.get(Id);
        return transformedTrajectory;
    }
    
    public void addKeyToTransformedTrajData(String id){
        if (!transformedTrajData.containsKey(id)){
            // using the same method ignoring the second parameter
            transformedTrajData.put(id, new TransformedTrajectory(id, -1));
        }
    }
    
    public void addKeyToSummaryTrajData(String id){
        if (!summaryTrajData.containsKey(id)){
            // using the same method ignoring the second parameter
            summaryTrajData.put(id, new TransformedTrajectory(id, -1));
        }
    }
    
    public void addValueToTransformedTrajData(String id, TransformedTrajPoint transformedTrajPoint){
        addKeyToTransformedTrajData(id);
        transformedTrajData.get(id).addTransformedTrajPoint(transformedTrajPoint);
    }
    
    public void addValueToSummaryTrajData(String id, TransformedTrajPoint transformedTrajPoint){
        addKeyToSummaryTrajData(id);
        summaryTrajData.get(id).addTransformedTrajPoint(transformedTrajPoint);
    }

    public HashMap<String, Integer> getTrajIdToDiskBlockIdMap() {
        return trajIdToDiskBlockIdMap;
    }

    public void setTrajIdToDiskBlockIdMap(HashMap<String, Integer> trajToDiskBlockIdMap) {
        this.trajIdToDiskBlockIdMap = trajToDiskBlockIdMap;
    }
    
    public void setDiskBlockIdToTrajIdListMap(){
        for (Map.Entry<String, Integer> entry : trajIdToDiskBlockIdMap.entrySet()) {
            String trajId = entry.getKey();
            Integer blockId = entry.getValue();
            if (!diskBlockIdToTrajIdListMap.containsKey(blockId)){
                diskBlockIdToTrajIdListMap.put(blockId, new ArrayList<String>());
            }
            diskBlockIdToTrajIdListMap.get(blockId).add(trajId);
        }
    }
    
    public Object getDiskBlockIdByTrajId(String id){
        if (!trajIdToDiskBlockIdMap.containsKey(id)) return null;
        return trajIdToDiskBlockIdMap.get(id);
    }
    
    public ArrayList<String> getTrajIdListByBlockId(Integer blockId){
        if (!diskBlockIdToTrajIdListMap.containsKey(blockId)) return null;
        return diskBlockIdToTrajIdListMap.get(blockId);
    }

    public HashMap<String, TransformedTrajectory> getSummaryTrajData() {
        return summaryTrajData;
    }
        
    public void printTrajectories(){
        for (Map.Entry<String, Trajectory> entry : trajData.entrySet()) {
            String trajId = entry.getKey();
            Trajectory traj = entry.getValue();
            System.out.println(traj);
            System.out.println(transformedTrajData.get(trajId));
        }
    }
    
    public void resetSummaryTrajData(){
        summaryTrajData.clear();
    }
}
