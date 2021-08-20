/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query;

/**
 *
 * @author Saqib
 */
public class PacketRequest {
    int srcId, destId;
    double srcLat, srcLon, destLat, destLon;
    double normSrcLat, normSrcLon, normDestLat, normDestLon;
    long srcTimeInSec, destTimeInSec;

    public PacketRequest() {
        srcId = destId = -1;
    }

    public int getSrcId() {
        return srcId;
    }

    public int getDestId() {
        return destId;
    }

    public double getSrcLat() {
        return srcLat;
    }

    public double getSrcLon() {
        return srcLon;
    }

    public double getDestLat() {
        return destLat;
    }

    public double getDestLon() {
        return destLon;
    }

    public double getNormSrcLat() {
        return normSrcLat;
    }

    public double getNormSrcLon() {
        return normSrcLon;
    }

    public double getNormDestLat() {
        return normDestLat;
    }

    public double getNormDestLon() {
        return normDestLon;
    }

    public void setSrcId(int srcId) {
        this.srcId = srcId;
    }

    public void setDestId(int destId) {
        this.destId = destId;
    }

    public void setSrcLat(double srcLat) {
        this.srcLat = srcLat;
    }

    public void setSrcLon(double srcLon) {
        this.srcLon = srcLon;
    }

    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }

    public void setDestLon(double destLon) {
        this.destLon = destLon;
    }

    public void setNormSrcLat(double normSrcLat) {
        this.normSrcLat = normSrcLat;
    }

    public void setNormSrcLon(double normSrcLon) {
        this.normSrcLon = normSrcLon;
    }

    public void setNormDestLat(double normDestLat) {
        this.normDestLat = normDestLat;
    }

    public void setNormDestLon(double normDestLon) {
        this.normDestLon = normDestLon;
    }

    public void setSrcTimeInSec(long srcTimeInSec) {
        this.srcTimeInSec = srcTimeInSec;
    }

    public void setDestTimeInSec(long destTimeInSec) {
        this.destTimeInSec = destTimeInSec;
    }
    
    public long getDurationInSeconds(){
        return (this.destTimeInSec-this.srcTimeInSec);
    }

    public long getSrcTimeInSec() {
        return srcTimeInSec;
    }

    public long getDestTimeInSec() {
        return destTimeInSec;
    }
    
    @Override
    public String toString() {
        String pktReqStr = "";
        pktReqStr += "Src Stop Id = " + srcId + ", Location = <" + + srcLat + "," + srcLon + "> , Normalized Location = <" + normSrcLat + "," + normSrcLon + ">\n";
        pktReqStr += "Dest Stop Id = " + destId + ", Location = <" + + destLat + "," + destLon + "> , Normalized Location = <" + normDestLat + "," + normDestLon + ">\n";
        return pktReqStr;
    }
    
}
