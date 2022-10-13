/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
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
    private ArrayList <Long> srcTimes;
    private ArrayList <Long> destTimes;
    private ArrayList<ArrayList<Pair<Integer,Integer>>> bucketedStops;
    private long minTimeInSec;
    private long maxTimeInSec;
    private long pktMinDuration;
    private long pktMaxDuration;
    // besides normalization, some stops out of bound are pruned in this hash map
    // so better to generate src, dest id from the normalized map
    PacketRequest pktReq;

    public PacketDeliveryQuery() {
        
    }
        
    public PacketDeliveryQuery(HashMap<Integer, Pair<Double, Double>> stoppageMap, HashMap<Integer, Pair<Double, Double>> normalizedStoppageMap, DistanceConverter distanceConverter,
                                double minDisThreshold, String distanceUnit, long minTimeInSec, long maxTimeInSec) {
        this.stoppageMap = stoppageMap;
        this.normalizedStoppageMap = normalizedStoppageMap;
        this.distanceConverter = distanceConverter;
        this.minDisThreshold = minDisThreshold;
        this.distanceUnit = distanceUnit;
        pktReq = new PacketRequest();
        srcStops = new ArrayList<>();
        destStops = new ArrayList<>();
        srcTimes = new ArrayList<>();
        destTimes = new ArrayList<>();
        this.bucketedStops = new ArrayList<>();
        this.minTimeInSec = minTimeInSec;
        this.maxTimeInSec = maxTimeInSec;
        this.pktMinDuration = 0;
        this.pktMaxDuration = 0;
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
    
    public void generatePktDeliveryReq(int fromStop, int toStop, long fromTimeInSec, long toTimeInSec){
        try{
            pktReq.setSrcId(fromStop);
            pktReq.setDestId(toStop);
            pktReq.setSrcLat(stoppageMap.get(fromStop).getKey());
            pktReq.setSrcLon(stoppageMap.get(fromStop).getValue());
            pktReq.setDestLat(stoppageMap.get(toStop).getKey());
            pktReq.setDestLon(stoppageMap.get(toStop).getValue());
            pktReq.setNormSrcLat(normalizedStoppageMap.get(fromStop).getKey());
            pktReq.setNormSrcLon(normalizedStoppageMap.get(fromStop).getValue());
            pktReq.setNormDestLat(normalizedStoppageMap.get(toStop).getKey());
            pktReq.setNormDestLon(normalizedStoppageMap.get(toStop).getValue());
            pktReq.setSrcTimeInSec(fromTimeInSec);
            pktReq.setDestTimeInSec(toTimeInSec);
        } catch (Exception E){
            System.out.println("From stop = " + fromStop + ", To stop = " + toStop + " - " + stoppageMap.containsKey(fromStop) + stoppageMap.containsKey(toStop));
            System.exit(0);
        }
    }
    
    public void addTimeToGeneratedPkt(){
        // temporal attributes
        long fromTimeInSec = (long)(Math.random()*(this.maxTimeInSec-this.minTimeInSec)+this.minTimeInSec);
        long pktDuration = (long)(Math.random()*(this.pktMaxDuration-this.pktMinDuration)+this.pktMinDuration);
        long toTimeInSec = fromTimeInSec + pktDuration;
        // comment out the following two lines after testing
        // fromTimeInSec = this.minTimeInSec;
        // toTimeInSec = this.maxTimeInSec;
        pktReq.setSrcTimeInSec(fromTimeInSec);
        pktReq.setDestTimeInSec(toTimeInSec);
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
        addTimeToGeneratedPkt();
        //System.out.println("Packet generated : src time = " + pktReq.getSrcTimeInSec() + " , dest time = " + pktReq.getDestTimeInSec() + ", duration = " + pktReq.getDurationInSeconds());
    }
    
    public void generateSTHotspotPktDeliveryReq(int id){
        if (srcStops.isEmpty() || destStops.isEmpty() || srcTimes.isEmpty() || destTimes.isEmpty()){
            pktReq = new PacketRequest();
        }
        if (id >= srcStops.size()){
            pktReq = new PacketRequest();
        }
        int fromStop = srcStops.get(id);
        int toStop = destStops.get(id);
        long fromTimeInSec = srcTimes.get(id);
        long toTimeInSec = destTimes.get(id);
        generatePktDeliveryReq(fromStop, toStop, fromTimeInSec, toTimeInSec);
        //System.out.println("Packet generated : src time = " + pktReq.getSrcTimeInSec() + " , dest time = " + pktReq.getDestTimeInSec() + ", duration = " + pktReq.getDurationInSeconds());
    }
    
    public void groupDistanceWiseSrcDest(){
        int bucketCount = 6;
        for (int i=0; i<bucketCount; i++){
            bucketedStops.add(new ArrayList<>());
        }
        
        for (HashMap.Entry<Integer, Pair<Double, Double>> fromEntry : normalizedStoppageMap.entrySet()){
            int from = fromEntry.getKey();
            for (HashMap.Entry<Integer, Pair<Double, Double>> toEntry : normalizedStoppageMap.entrySet()){
                int to = toEntry.getKey();
                if (from == to) continue;

                double lat1 = stoppageMap.get(from).getKey();
                double lon1 = stoppageMap.get(from).getValue();
                double lat2 = stoppageMap.get(to).getKey();
                double lon2 = stoppageMap.get(to).getValue();
                
                double pktDis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
                int bucketId = -1;
                if (pktDis <= 2500){    // in meters
                    bucketId = 0;
                }
                else if (pktDis <= 5000){
                    bucketId = 1;
                }
                else if (pktDis <= 10000){
                    bucketId = 2;
                }
                else if (pktDis <= 20000){
                    bucketId = 3;
                }
                else if (pktDis <= 40000){
                    bucketId = 4;
                }
                else bucketId = 5;
                
                Pair <Integer, Integer> srcToDestIndex = new Pair(from, to);
                bucketedStops.get(bucketId).add(srcToDestIndex);
            }
        }
    }
    
    public void populateRandomSrcDestIds(int size){
        resetPktDeliveryReq();
        ArrayList<Integer> allValidStopIds = new ArrayList<>(normalizedStoppageMap.keySet());
        Random randomIndexGenerator = new Random();
        for (int i=0; i<size; i++){
            int srcIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            int destIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            allValidStopIds.get(srcIndex);
            srcStops.add(allValidStopIds.get(srcIndex));
            destStops.add(allValidStopIds.get(destIndex));
        }
    }
    
    public void populateCertainDistanceSrcDestIds(int size, int bucketId){
        resetPktDeliveryReq();
        double minDis, maxDis;
        if (bucketId == 0){
            minDis = 0;
            maxDis = 2500;
        }
        else if (bucketId == 1){
            minDis = 2500;
            maxDis = 5000;
        }
        else if (bucketId == 2){
            minDis = 5000;
            maxDis = 10000;
        }
        else if (bucketId == 3){
            minDis = 10000;
            maxDis = 20000;
        }
        else if (bucketId == 4){
            minDis = 20000;
            maxDis = 40000;
        }
        else{
            minDis = 40000;
            maxDis= Double.MAX_VALUE;
        }
        ArrayList<Integer> allValidStopIds = new ArrayList<>(normalizedStoppageMap.keySet());
        Random randomIndexGenerator = new Random();
        for (int i=0; i<size; i++){
            int srcIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            int destIndex = randomIndexGenerator.nextInt(allValidStopIds.size());
            
            int from = allValidStopIds.get(srcIndex);
            int to = allValidStopIds.get(destIndex);
            double lat1 = stoppageMap.get(from).getKey();
            double lon1 = stoppageMap.get(from).getValue();
            double lat2 = stoppageMap.get(to).getKey();
            double lon2 = stoppageMap.get(to).getValue();
                
            double pktDis = distanceConverter.absDistance(lat1, lat2, lon1, lon2, distanceUnit);
            
            if (pktDis < minDis || pktDis > maxDis){
                i--;
                continue;
            }
            
            srcStops.add(allValidStopIds.get(srcIndex));
            destStops.add(allValidStopIds.get(destIndex));
        }
    }
    
    public void populateCertainDistTimeSrcDestIds(int size, int bucketId, int timeBucketId){
        populateCertainDistanceSrcDestIds(size, bucketId);
        
        if (timeBucketId == 0){
            this.pktMaxDuration = 1*3600L;   // 1 hour in seconds
            this.pktMinDuration = (long)(0.5*3600L);  // half an hour in secongs
        }
        else if (timeBucketId == 1){
            this.pktMaxDuration = 2*3600L;   // 2 hours in seconds
            this.pktMinDuration = 1*3600L;   // 1 hour in seconds
        }
        else if (timeBucketId == 2){
            this.pktMaxDuration = 4*3600L;   // 4 hours in seconds
            this.pktMinDuration = 2*3600L;   // 2 hours in second
        }
        else if (timeBucketId == 3){
            this.pktMaxDuration = 8*3600L;   // 8 hours in seconds
            this.pktMinDuration = 4*3600L;   // 4 hours in seconds
        }
        else if (timeBucketId == 4){
            this.pktMaxDuration = 24*3600L;   // 1 day in seconds
            this.pktMinDuration = 8*3600L;   // 8 hours in seconds
        }
        else{
            this.pktMaxDuration = 3*24*3600L;   // 3 days in seconds
            this.pktMinDuration = 24*3600L;   // 1 day in seconds
        }
    }
    
    public void populatePktsFromHotspot(int size, int bucketId, int timeBucketId, TreeMap<Integer, ArrayList<Integer>> timeHourIdWiseStops, int maxTimeId){
        resetPktDeliveryReq();
        Random random = new Random();
        double minDis, maxDis;
        if (bucketId == 0){
            minDis = 0;
            maxDis = 2500;
        }
        else if (bucketId == 1){
            minDis = 2500;
            maxDis = 5000;
        }
        else if (bucketId == 2){
            minDis = 5000;
            maxDis = 10000;
        }
        else if (bucketId == 3){
            minDis = 10000;
            maxDis = 20000;
        }
        else if (bucketId == 4){
            minDis = 20000;
            maxDis = 40000;
        }
        else{
            minDis = 40000;
            maxDis= Double.MAX_VALUE;
        }
        
        int pktMaxDuration, pktMinDuration;
        if (timeBucketId == 0){
            pktMaxDuration = 1*3600;   // 1 hour in seconds
            pktMinDuration = (int)(0.5*3600);  // half an hour in seconds
        }
        else if (timeBucketId == 1){
            pktMaxDuration = 2*3600;   // 2 hours in seconds
            pktMinDuration = 1*3600;   // 1 hour in seconds
        }
        else if (timeBucketId == 2){
            pktMaxDuration = 4*3600;   // 4 hours in seconds
            pktMinDuration = 2*3600;   // 2 hours in second
        }
        else if (timeBucketId == 3){
            pktMaxDuration = 8*3600;   // 8 hours in seconds
            pktMinDuration = 4*3600;   // 4 hours in seconds
        }
        else if (timeBucketId == 4){
            pktMaxDuration = 24*3600;   // 1 day in seconds
            pktMinDuration = 8*3600;   // 8 hours in seconds
        }
        else{
            pktMaxDuration = 3*24*3600;   // 3 days in seconds
            pktMinDuration = 24*3600;   // 1 day in seconds
        }
        
        for (int i=0; i<size; i++){
            int srcTimeHourId = random.nextInt(maxTimeId+1);
            Map.Entry<Integer, ArrayList<Integer>> srcEntry = timeHourIdWiseStops.ceilingEntry(srcTimeHourId);
            // gets entry whose hour id is greater or equal to srcTimeHourId (as some hour ids may not have stops)
            srcTimeHourId = srcEntry.getKey();
            int srcStopId = srcEntry.getValue().get(random.nextInt(srcEntry.getValue().size()));
            
            long srcTime = this.minTimeInSec + srcTimeHourId*3600L + (long)random.nextInt(3600);
            
            int pktDuration = random.nextInt(pktMaxDuration-pktMinDuration)+pktMinDuration;
            
            long destTime = srcTime + (long)pktDuration;
            int destTimeHourId = (int)((destTime - this.minTimeInSec)/3600L);
            
            Map.Entry<Integer, ArrayList<Integer>> destEntry = timeHourIdWiseStops.floorEntry(destTimeHourId);
            // gets entry whose hour id is less or equal to destTimeHourId (as some hour ids may not have stops)
            destTimeHourId = destEntry.getKey();
            if (destTimeHourId < srcTimeHourId){
                i--;
                continue;
            }
            int destStopId = destEntry.getValue().get(random.nextInt(destEntry.getValue().size()));
            if (srcStopId ==  destStopId){
                i--;
                continue;
            }
            // Specific to NYC dataset
            else if (srcStopId == 0 || destStopId == 0){
                i--;
                continue;
            }
            //System.out.println(srcTimeHourId + " - " + srcTime + " ; " + destTimeHourId + " - " + destTime);
            srcStops.add(srcStopId);
            destStops.add(destStopId);
            srcTimes.add(srcTime);
            destTimes.add(destTime);
        }
        
    }
    
    public void populateRandomBucketedPackets(int size, int bucketId){
        resetPktDeliveryReq();
        ArrayList<Pair<Integer,Integer>> validSrcDestPairs = bucketedStops.get(bucketId);
        Random randomIndexGenerator = new Random();
        for (int i=0; i<size; i++){
            int index = randomIndexGenerator.nextInt(validSrcDestPairs.size());
            srcStops.add(validSrcDestPairs.get(index).getKey());
            destStops.add(validSrcDestPairs.get(index).getValue());
        }
    }
    
    public void resetPktDeliveryReq(){
        if (!srcStops.isEmpty()) srcStops.clear();
        if (!destStops.isEmpty()) destStops.clear();
        if (!srcTimes.isEmpty()) srcTimes.clear();
        if (!destTimes.isEmpty()) destTimes.clear();
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
    
    public String getDurationInHourMinute(){
        long durationInSeconds = pktReq.getDurationInSeconds();
        long durationInHours = durationInSeconds/3600L;
        long durationInMinutes = (durationInSeconds%3600L)/60L;
        if (durationInSeconds%60L >= 30L) durationInMinutes++;
        String durationInHourMinute = durationInHours + "h " + durationInMinutes + "min";
        return durationInHourMinute;
    }
    
    public long getDurationInSec(){
        return pktReq.getDurationInSeconds();
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
