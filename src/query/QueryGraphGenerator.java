/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;

/**
 *
 * @author Saqib
 */
public class QueryGraphGenerator {

    public static ArrayList<CoordinateArraySequence> generateQuery(ArrayList<CoordinateArraySequence> routeGraph) {
        ArrayList<CoordinateArraySequence> queryRoutes = new ArrayList<CoordinateArraySequence>();
        double queryTypeRV = /*Math.random()*/ 0;
        int count = 0;
        if (queryTypeRV < 0.5) {    // rectangular box
            //System.out.println("Rectangular box subgraph query");
            double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
            while (x1 == x2) {
                x1 = Math.random();
                x2 = Math.random();
            }
            while (y1 == y2) {
                y1 = Math.random();
                y2 = Math.random();
            }
            if (x1 > x2) {
                double temp = x1;
                x1 = x2;
                x2 = temp;
            }
            if (y1 > y2) {
                double temp = y1;
                y1 = y2;
                y2 = temp;
            }

            double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
            for (int i = 0; i < routeGraph.size(); i++) {
                for (int j = 0; j < routeGraph.get(i).size(); j++) {
                    minX = min(minX, routeGraph.get(i).getCoordinate(j).x);
                    maxX = max(maxX, routeGraph.get(i).getCoordinate(j).x);
                    minY = min(minY, routeGraph.get(i).getCoordinate(j).y);
                    maxY = max(maxY, routeGraph.get(i).getCoordinate(j).y);
                }
            }
            x1 = minX + x1 * (maxX - minX);
            x2 = minX + x2 * (maxX - minX);
            y1 = minY + y1 * (maxY - minY);
            y2 = minY + y2 * (maxY - minY);
            /*
            if (x1 > 100) {
                x1 = 99.999999;
            } else if (x1 < 0) {
                x1 = 0.000001;
            }
            if (x2 > 100) {
                x2 = 99.999999;
            } else if (x2 < 0) {
                x2 = 0.000001;
            }
            if (y1 > 100) {
                y1 = 99.999999;
            } else if (y1 < 0) {
                y1 = 0.000001;
            }
            if (y2 > 100) {
                y2 = 99.999999;
            } else if (y2 < 0) {
                y2 = 0.000001;
            }
            */

            int maxStoppages = 32;

            for (int i = 0; i < routeGraph.size() && maxStoppages > count; i++) {
                ArrayList<Coordinate> route = new ArrayList<Coordinate>();
                for (int j = 0; j < routeGraph.get(i).size() && maxStoppages > count; j++) {
                    //System.out.println("Count = " + count + " Route Size = " + route.size());
                    if (routeGraph.get(i).getCoordinate(j).x < x1 || routeGraph.get(i).getCoordinate(j).x > x2 || routeGraph.get(i).getCoordinate(j).y < y1 || routeGraph.get(i).getCoordinate(j).y > y2) {
                        if (route.size() < 2) {
                            if (route.isEmpty()); else {
                                //System.out.println("Count = " + count + " Route Size = " + route.size());
                                count -= route.size();
                            }
                        } else {
                            Coordinate[] routePointsArray = new Coordinate[route.size()];
                            route.toArray(routePointsArray);
                            //System.out.println("Count = " + count + " Route Size (generation) = " + route.size());
                            queryRoutes.add(new CoordinateArraySequence(routePointsArray, route.size()));
                        }
                        route.clear();
                    } else {
                        route.add(new Coordinate(routeGraph.get(i).getCoordinate(j).x, routeGraph.get(i).getCoordinate(j).y));
                        count++;
                    }
                }
                if (route.size() < 2) {
                    if (route.isEmpty()); else {
                        //System.out.println("Count = " + count + " Route Size = " + route.size());
                        count -= route.size();
                    }
                } else {
                    Coordinate[] routePointsArray = new Coordinate[route.size()];
                    route.toArray(routePointsArray);
                    //System.out.println("Count = " + count + " Route Size (generation) = " + route.size());
                    queryRoutes.add(new CoordinateArraySequence(routePointsArray, route.size()));
                }
                route.clear();
            }
        } else {    // random routes
            //System.out.println("Random routes query");
            /*
            for (int i = 0; i < routeGraph.size(); i++) {;;
                if (Math.random() < 0.8) {
                    continue;  // 0.8 is rejection probability, can be changed
                }
                Coordinate[] routePointsArray = new Coordinate[routeGraph.get(i).size()];
                routePointsArray = routeGraph.get(i).toCoordinateArray();
                queryRoutes.add(new CoordinateArraySequence(routePointsArray, routePointsArray.length));
            }
            */
            int index = (int)(Math.random()*routeGraph.size());
            if (index == routeGraph.size()) index--;
            //System.out.println(index);
            Coordinate[] routePointsArray = new Coordinate[routeGraph.get(index).size()];
            routePointsArray = routeGraph.get(index).toCoordinateArray();
            queryRoutes.add(new CoordinateArraySequence(routePointsArray, routePointsArray.length));
        }

        // printing
        //for (int i = 0; i < queryRoutes.size(); i++) {
        //System.out.println("\nRoute " + (i + 1) + " , Size = " + queryRoutes.get(i).size());
        /*for (int j = 0; j < queryRoutes.get(i).size(); j++) {
             System.out.print("< " + queryRoutes.get(i).getCoordinate(j).x + ", " + queryRoutes.get(i).getCoordinate(j).y + " > - ");
             }
             System.out.println("");*/
        //}
        //System.out.println("Done");
        return queryRoutes;
    }

    public static ArrayList<CoordinateArraySequence> generateCoverageQuery(ArrayList<CoordinateArraySequence> routeGraph, double distanceBetweenBusStops) {
        int minSize = 8, maxSize = 64;
        ArrayList<CoordinateArraySequence> queryRoutes = new ArrayList<CoordinateArraySequence>();
        double minX = 1000, minY = 1000, maxX = -1000, maxY = -1000;
        while (true) {
            int i = (int) Math.floor(Math.random() * (routeGraph.size() + 1));
            if (i < 0) {
                i = 0;
            } else if (i >= routeGraph.size()) {
                i = routeGraph.size() - 1;
            }
            ArrayList<Coordinate> route = new ArrayList<Coordinate>();
            int size = min(maxSize, routeGraph.get(i).size());
            for (int j = 0; j < routeGraph.get(i).size(); j++) {
                double x = routeGraph.get(i).getCoordinate(j).x;
                double y = routeGraph.get(i).getCoordinate(j).y;
                if (j == 0) {
                    minX = min(minX, x);
                    minY = min(minY, y);
                    maxX = max(maxX, x);
                    maxY = max(maxY, y);
                    route.add(new Coordinate(x, y));
                    continue;
                }
                double distance = Math.sqrt((x - route.get(route.size() - 1).x) * (x - route.get(route.size() - 1).x) + (y - route.get(route.size() - 1).y) * (y - route.get(route.size() - 1).y));
                if (distance < distanceBetweenBusStops) {
                    continue;
                }
                minX = min(minX, x);
                minY = min(minY, y);
                maxX = max(maxX, x);
                maxY = max(maxY, y);
                route.add(new Coordinate(x, y));
                if (route.size() == maxSize) {
                    break;
                }
            }
            if (route.size() < minSize) {
                route.clear();
                continue;
            }
            Coordinate[] routePointsArray = new Coordinate[route.size()];
            route.toArray(routePointsArray);
            route.clear();

            //if (routePointsArray.length < minSize) continue;
            queryRoutes.add(new CoordinateArraySequence(routePointsArray, min(maxSize, routePointsArray.length)));
            break;
        }
        /*
        for (int j = 0; j < queryRoutes.size(); j++) {
            System.out.println("\nRoute " + (j + 1) + " , Size = " + queryRoutes.get(j).size());
            System.out.println("X: " + minX + " " + maxX + ", Y: " + minY + " " + maxY);
        }
         */
        return queryRoutes;
    }
}
