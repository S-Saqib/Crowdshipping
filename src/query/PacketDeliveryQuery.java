/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query;

import java.util.HashMap;
import java.util.Random;
import javafx.util.Pair;

/**
 *
 * @author Saqib
 */
public class PacketDeliveryQuery {

    HashMap<Integer, Pair<Double, Double>> stoppageMap;
    HashMap<Integer, Pair<Double, Double>> normalizedStoppageMap;
    // besides normalization, some stops out of bound are pruned in this hash map
    // so better to generate src, dest id from the normalized map
    PacketRequest pktReq;

    public PacketDeliveryQuery() {
        
    }
        
    public PacketDeliveryQuery(HashMap<Integer, Pair<Double, Double>> stoppageMap, HashMap<Integer, Pair<Double, Double>> normalizedStoppageMap) {
        this.stoppageMap = stoppageMap;
        this.normalizedStoppageMap = normalizedStoppageMap;
        pktReq = new PacketRequest();
    }
    
    public void generatePktDeliveryReq(){
        // from , to, fromTime, toTime
        // create another class if needed
        int from, to;
        from = to = -1;
        Random random = new Random();
        for (HashMap.Entry<Integer, Pair<Double, Double>> entry : normalizedStoppageMap.entrySet()){
            if (random.nextDouble() > 0.8){
                if (from == -1){
                    from = entry.getKey();
                }
                else{
                    to = entry.getKey();
                    break;
                }
            }
        }
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

    public PacketRequest getPacketRequest() {
        return pktReq;
    }
    
}
