/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javafx.util.Pair;
import query.service.DistanceConverter;

/**
 *
 * @author Saqib
 */
public class PacketDeliveryQuery {

    private HashMap<Integer, Pair<Double, Double>> stoppageMap;
    private HashMap<Integer, Pair<Double, Double>> normalizedStoppageMap;
    private DistanceConverter distanceConverter;
    private double minDisThreshold;
    private String distanceUnit;
    private ArrayList <Integer> srcStops;
    private ArrayList <Integer> destStops;
    // besides normalization, some stops out of bound are pruned in this hash map
    // so better to generate src, dest id from the normalized map
    PacketRequest pktReq;

    public PacketDeliveryQuery() {
        
    }
        
    public PacketDeliveryQuery(HashMap<Integer, Pair<Double, Double>> stoppageMap, HashMap<Integer, Pair<Double, Double>> normalizedStoppageMap,
                                DistanceConverter distanceConverter, double minDisThreshold, String distanceUnit) {
        this.stoppageMap = stoppageMap;
        this.normalizedStoppageMap = normalizedStoppageMap;
        this.distanceConverter = distanceConverter;
        this.minDisThreshold = minDisThreshold;
        this.distanceUnit = distanceUnit;
        pktReq = new PacketRequest();
        srcStops = new ArrayList<>();
        destStops = new ArrayList<>();
    }
    
    public void generatePktDeliveryReq(){
        // from , to, fromTime, toTime
        // create another class if needed
        int from, to;
        from = to = -1;
        boolean isValid = false;
        Random random = new Random();
        while(!isValid){
            for (HashMap.Entry<Integer, Pair<Double, Double>> fromEntry : normalizedStoppageMap.entrySet()){
                if (random.nextDouble() > 0.95){
                    from = fromEntry.getKey();
                    for (HashMap.Entry<Integer, Pair<Double, Double>> toEntry : normalizedStoppageMap.entrySet()){
                        if (random.nextDouble() > 0.95){
                            to = toEntry.getKey();
                            if (from == to) continue;

                            double lat1 = stoppageMap.get(from).getKey();
                            double lon1 = stoppageMap.get(from).getValue();
                            double lat2 = stoppageMap.get(to).getKey();
                            double lon2 = stoppageMap.get(to).getValue();

                            if (distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit) >= minDisThreshold){
                                isValid = true;
                                break;
                            }
                        }
                    }
                    if (isValid) break;
                }
            }
        }
        // for debug purpose only
        //from = 4;
        //to = 12;
        //
        generatePktDeliveryReq(from, to);
    }
    
    public void generatePktDeliveryReq(int from, int to){
        pktReq.setSrcId(from);
        pktReq.setDestId(to);
        pktReq.setSrcLat(stoppageMap.get(from).getKey());
        pktReq.setSrcLon(stoppageMap.get(from).getValue());
        pktReq.setDestLat(stoppageMap.get(to).getKey());
        pktReq.setDestLon(stoppageMap.get(to).getValue());
        pktReq.setNormSrcLat(normalizedStoppageMap.get(from).getKey());
        pktReq.setNormSrcLon(normalizedStoppageMap.get(from).getValue());
        pktReq.setNormDestLat(normalizedStoppageMap.get(to).getKey());
        pktReq.setNormDestLon(normalizedStoppageMap.get(to).getValue());
    }
    
    public void generatePktDeliveryReq(int id){
        if (srcStops.isEmpty() && destStops.isEmpty()){
            pktReq = new PacketRequest();
        }
        if (id >= srcStops.size()){
            pktReq = new PacketRequest();
        }
        int from = srcStops.get(id);
        int to = destStops.get(id);
        generatePktDeliveryReq(from, to);
    }
    
    public void populateRandomSrcDestIds(int size){
        resetPktDeliveryReq();
        ArrayList<Integer> allValidStopIds = new ArrayList<>(normalizedStoppageMap.keySet());
        Random randomIndexGenerator = new Random();
        for (int i=0; i<size; i++){
            int srcIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            int destIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            srcStops.add(allValidStopIds.get(srcIndex));
            destStops.add(allValidStopIds.get(destIndex));
        }
    }
    
    public void resetPktDeliveryReq(){
        if (!srcStops.isEmpty()) srcStops.clear();
        if (!destStops.isEmpty()) destStops.clear();
    }
    
    public PacketRequest getPacketRequest() {
        return pktReq;
    }
    
    public double getDistance(){
        double lat1 = pktReq.getSrcLat();
        double lon1 = pktReq.getSrcLon();
        double lat2 = pktReq.getDestLat();
        double lon2 = pktReq.getDestLon();
        double dis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
        return dis;
    }
    
    public String getDistanceUnit(){
        return distanceUnit;
    }

    @Override
    public String toString() {
        double lat1 = pktReq.getSrcLat();
        double lon1 = pktReq.getSrcLon();
        double lat2 = pktReq.getDestLat();
        double lon2 = pktReq.getDestLon();
        double dis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
        
        String pktDeliveryQueryStr = pktReq.toString();
        pktDeliveryQueryStr += "Distance of src and dest = " + dis + " " + distanceUnit + "\n";
        return pktDeliveryQueryStr;
    }
}
