/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.real;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.util.Assert;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;

/**
 *
 * @author Saqib
 */
public class SimpleParser {
    public double minLon = 1000, maxLon = -1000, minLat = 1000, maxLat = -1000;
    public double latCoeff = 0, latConst = 0, lonCoeff = 0, lonConst = 0;
    public ArrayList<CoordinateArraySequence> parseUserTrajectories(String path) throws FileNotFoundException, IOException{
        File userTrajectoryFile = new File(path);
        Assert.isTrue(userTrajectoryFile.exists(), "user trajectory file not found");

        BufferedReader br = null;
        String line = new String();

        br = new BufferedReader(new FileReader(path));
        line = br.readLine();   /// Discard header
        
        ArrayList<CoordinateArraySequence> allTrajectories = new ArrayList<CoordinateArraySequence>();
        
        while ((line = br.readLine()) != null) {
            if (line.equals("")) continue;
            //System.out.println(line);
            String[] coordinates = line.split(",");
            String dateTime = coordinates[0];
            String []dateAndTime = dateTime.split(" ");
            String date = dateAndTime[0];
            if (date.compareTo("2016-01-07") < 0 || date.compareTo("2016-01-07") > 0){
                //System.out.println(date);
                continue;
            }
            
            Coordinate[] userTrajectoryPointsArray = new Coordinate[2];
            userTrajectoryPointsArray[0] = new Coordinate(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[2]));
            userTrajectoryPointsArray[1] = new Coordinate(Double.parseDouble(coordinates[3]), Double.parseDouble(coordinates[4]));
            //userTrajectoryPointsArray[0].x = Double.parseDouble(coordinates[0]);
            //userTrajectoryPointsArray[0].y = Double.parseDouble(coordinates[1]);
            //userTrajectoryPointsArray[1].x = Double.parseDouble(coordinates[2]);
            //userTrajectoryPointsArray[1].y = Double.parseDouble(coordinates[3]);
            
            // assuming range to be -73 to -75 and 40 to 42 based on the longitude, latitude of the city, to remove unwanted data
            if (abs(userTrajectoryPointsArray[0].x + 74) > 1) continue;
            if (abs(userTrajectoryPointsArray[1].x + 74) > 1) continue;
            if (abs(userTrajectoryPointsArray[0].y - 41) > 1) continue;
            if (abs(userTrajectoryPointsArray[1].y - 41) > 1) continue;
            if (abs(userTrajectoryPointsArray[0].x - userTrajectoryPointsArray[1].x) < 1e-12 && abs(userTrajectoryPointsArray[0].y - userTrajectoryPointsArray[1].y) < 1e-12 ) continue;
            
            minLon = min(minLon, userTrajectoryPointsArray[0].x);
            minLon = min(minLon, userTrajectoryPointsArray[1].x);
            minLat = min(minLat, userTrajectoryPointsArray[0].y);
            minLat = min(minLat, userTrajectoryPointsArray[1].y);
            
            maxLon = max(maxLon, userTrajectoryPointsArray[0].x);
            maxLon = max(maxLon, userTrajectoryPointsArray[1].x);
            maxLat = max(maxLat, userTrajectoryPointsArray[0].y);
            maxLat = max(maxLat, userTrajectoryPointsArray[1].y);
            
            allTrajectories.add(new CoordinateArraySequence(userTrajectoryPointsArray, 2));
            //if (allTrajectories.size() == 400000) break;
        }
        br.close();
        /*
        for (int i=0; i<(int)Math.min((int)allTrajectories.size(),5); i++){
            System.out.println(allTrajectories.get(i).getX(0)+ " " + allTrajectories.get(i).getY(0)+ " --- " + allTrajectories.get(i).getX(1)+ " " + allTrajectories.get(i).getY(1));
        }
        */
        InputNormalizer Normalizer = new InputNormalizer();
        allTrajectories = Normalizer.normalize(allTrajectories, minLon, minLat, maxLon, maxLat);
        
        ///System.out.println(minLat+"\t"+maxLat+"\t"+minLon+"\t"+maxLon);
        ///System.out.println((maxLat/100.0-minLat/100.0)+"\t"+minLat+"\t"+(maxLon/100.0-minLon/100.0)+"\t"+minLon);
        latCoeff = (maxLat/100.0-minLat/100.0);
        latConst = minLat;
        lonCoeff = (maxLon/100.0-minLon/100.0);
        lonConst = minLon;
        /*
        for (int i=0; i<(int)Math.min((int)allTrajectories.size(),5); i++){
            System.out.println(allTrajectories.get(i).getX(0)+ " " + allTrajectories.get(i).getY(0)+ " --- " + allTrajectories.get(i).getX(1)+ " " + allTrajectories.get(i).getY(1));
        }
        */
        //System.out.println(minLon+ " " + minLat+ " --- " + maxLon+ " " + maxLat);
        
        return allTrajectories; 
    }
    
    public ArrayList<CoordinateArraySequence> parseRoutes(String stoppageFilePath, String routeFilePath) throws IOException {
        File routeFile = new File(routeFilePath);
        Assert.isTrue(routeFile.exists(), "route file not found");
        
        File stoppageFile = new File (stoppageFilePath);
        Assert.isTrue(stoppageFile.exists(), "stoppage file not found");
        
        BufferedReader br = null;
        String line = new String();

        br = new BufferedReader(new FileReader(stoppageFilePath));
        
        ArrayList <Coordinate> stoppages = new ArrayList <Coordinate>();
        while ((line = br.readLine()) != null) {
            if (line.equals("")) continue;
            String[] coordinates = line.split("\t");
            Coordinate stoppageLocation = new Coordinate(Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[1]));
            stoppages.add(stoppageLocation);
        }
        //System.out.println(stoppages.size());
        br.close();
        
        ArrayList<Coordinate> route = new ArrayList<Coordinate>();
        ArrayList<CoordinateArraySequence> routeGraph = new ArrayList<CoordinateArraySequence>();
        
        br = new BufferedReader(new FileReader(routeFilePath));
        while ((line = br.readLine()) != null) {
            if (line.equals("")) continue;
            String[] stoppagesAlongRoute = line.split("\t");
            if (stoppagesAlongRoute.length < 2) continue;
            for (int i=1; i<stoppagesAlongRoute.length; i++){
                route.add(new Coordinate(stoppages.get(Integer.parseInt(stoppagesAlongRoute[i])-1)));
            }
            Coordinate[] routePointsArray = new Coordinate[route.size()];
            route.toArray(routePointsArray);
            routeGraph.add(new CoordinateArraySequence(routePointsArray, route.size()));
            route.clear();
        }
        //System.out.println("Routes Parsed");
        InputNormalizer Normalizer = new InputNormalizer();
        routeGraph = Normalizer.normalize(routeGraph, minLon, minLat, maxLon, maxLat);
        //System.out.println("Routes Normalized");
        return routeGraph;
    }
}