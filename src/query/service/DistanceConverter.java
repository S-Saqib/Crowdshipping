/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;
// This code is contributed by Prasad Kshirsagar, at geeksforgeeks (Thanks to him)
// Java program to calculate Distance Between 
// Two Points on Earth 

/**
 *
 * @author Saqib
 */
public class DistanceConverter {
    
    private double maxLon, maxLat, minLon, minLat;

    public DistanceConverter(double maxLon, double maxLat, double minLon, double minLat) {
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.minLat = minLat;
    }

    public DistanceConverter() {
        this.maxLon = 0;
        this.maxLat = 0;
        this.minLon = 0;
        this.minLat = 0;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public void setMinLon(double minLon) {
        this.minLon = minLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }
    
    public double distance(double lat1,  double lat2, double lon1, double lon2, String unit) {

        // The math module contains a function named toRadians which converts from degrees to radians. 
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula 
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2),2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 0;
        // Use 6371 as radius of earth for kilometer, 6.371M for meter, Use 3956 for miles, 20.902M for feet, 
        if (unit.equals("m")) r = 6.371e6;
        else if (unit.equals("km")) r = 6371;
        else if (unit.equals("mile")) r = 3956;
        else if (unit.equals("ft")) r = 20.902e6;

        // calculate the result 
        return(c * r);
    }
    
    public double distance(String unit) {

        // The math module contains a function named toRadians which converts from degrees to radians. 
        double lon2 = Math.toRadians(maxLon);
        double lon1 = Math.toRadians(minLon);
        double lat2 = Math.toRadians(maxLat);
        double lat1 = Math.toRadians(minLat);

        // Haversine formula 
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2),2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 0;
        // Use 6371 as radius of earth for kilometer, 6.371M for meter, Use 3956 for miles, 20.902M for feet, 
        if (unit.equals("m")) r = 6.371e6;
        else if (unit.equals("km")) r = 6371;
        else if (unit.equals("mile")) r = 3956;
        else if (unit.equals("ft")) r = 20.902e6;

        // calculate the result 
        return(c * r);
    }
    
    public double absDistance(String unit){
        return Math.abs(distance(unit));
    }
    
    public double absDistance(double lat1,  double lat2, double lon1, double lon2, String unit){
        return Math.abs(distance(lat1, lat2, lon1, lon2, unit));
    }
    
    public double absDistanceFromNormalizedValues(double lat1,  double lat2, double lon1, double lon2, String unit){
        lat1 = denormalizeLat(lat1);
        lat2 = denormalizeLat(lat2);
        lon1 = denormalizeLon(lon1);
        lon2 = denormalizeLon(lon2);
        return Math.abs(distance(lat1, lat2, lon1, lon2, unit));
    }
    
    public double denormalizeLat(double normalizedLat){
        return (normalizedLat/100*(maxLat-minLat) + minLat);
    }
    
    public double denormalizeLon(double normalizedLon){
        return (normalizedLon/100*(maxLon-minLon) + minLon);
    }
    
    public double avgAbsLatDistance(String unit){
        double latDis1 = distance(maxLat, minLat, maxLon, maxLon, unit);
        double latDis2 = distance(maxLat, minLat, minLon, minLon, unit);
        return (Math.abs(latDis1)+Math.abs(latDis2))/2;
    }
    
    public double avgAbsLonDistance(String unit){
        double lonDis1 = distance(maxLat, maxLat, maxLon, minLon, unit);
        double lonDis2 = distance(minLat, minLat, maxLon, minLon, unit);
        return (Math.abs(lonDis1)+Math.abs(lonDis2))/2;
    }
    
    public double getLatProximity(double value, String unit){
        double totalDis = avgAbsLatDistance(unit);
        return 100/totalDis*value; // the latitude space is normalized to [0, 100] range
    }
    
    public double getLonProximity(double value, String unit){
        double totalDis = avgAbsLonDistance(unit);
        return 100/totalDis*value; // the longitude space is normalized to [0, 100] range
    }
    
}