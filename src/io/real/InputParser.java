package io.real;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.vividsolutions.jts.util.Assert;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;

public class InputParser {
    
    double minLon = 1000, maxLon = -1000, minLat = 1000, maxLat = -1000;
    
    public ArrayList<CoordinateArraySequence> parseRoutes(String path) throws IOException {
        //path = "src/main/resources/io/real/routelist.xlsx";
        File routeFile = new File(path);
        Assert.isTrue(routeFile.exists(), "route file not found");

        FileInputStream inputStream = new FileInputStream(routeFile);
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = firstSheet.iterator();

        // storing each route into an arrayList
        ArrayList<Coordinate> route = new ArrayList<Coordinate>();
        ArrayList<CoordinateArraySequence> routeGraph = new ArrayList<CoordinateArraySequence>();
        int lastRoute, col = 0, routeNo = 0;
        double longitude = 0, latitude = 0, val = 0;
        lastRoute = -1;

        iterator.next(); // dropping the title row
        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            Iterator<Cell> cellIterator = nextRow.cellIterator();
            col = 0;
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                col++;
                if (col == 1) {    // dropping the 1st column which contains the index of the point
                    continue;
                }
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        System.out.print(cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        System.out.print(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        val = cell.getNumericCellValue();   // all values of input file are of this type
                        //System.out.print(val);
                        break;
                }

                if (col == 2) {
                    longitude = (double) val;
                } else if (col == 3) {
                    latitude = (double) val;
                } else if (col == 4) {
                    routeNo = (int) val;
                }
                //System.out.print(" - ");
            }
            if (routeNo != lastRoute) {
                if (lastRoute != -1) {
                    Coordinate[] routePointsArray = new Coordinate[route.size()];
                    route.toArray(routePointsArray);
                    routeGraph.add(new CoordinateArraySequence(routePointsArray, route.size()));
                    // since the constructor needs array type object
                }
                route.clear();
                route = new ArrayList<Coordinate>();
                lastRoute = routeNo;
            }
            route.add(new Coordinate(longitude, latitude));
            //System.out.println();
        }
        Coordinate[] routePointsArray = new Coordinate[route.size()];
        route.toArray(routePointsArray);
        routeGraph.add(new CoordinateArraySequence(routePointsArray, route.size()));
        route.clear();

        workbook.close();
        inputStream.close();
        
        // normalizing
        InputNormalizer Normalizer = new InputNormalizer();
        routeGraph = Normalizer.normalize(routeGraph, minLon, minLat, maxLon, maxLat);
        return routeGraph;
    }

    public ArrayList<CoordinateArraySequence> parseUserTrajectories(String path) throws IOException {
        //path = "src/main/resources/io/real/routelist.xlsx";
        File userTrajectoryFile = new File(path);
        Assert.isTrue(userTrajectoryFile.exists(), "user trajectory file not found");

        FileInputStream inputStream = new FileInputStream(userTrajectoryFile);
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = firstSheet.iterator();

        // storing each user trajectory into an arrayList
        ArrayList<Coordinate> userTrajectory = new ArrayList<Coordinate>();
        ArrayList<CoordinateArraySequence> allTrajectories = new ArrayList<CoordinateArraySequence>();
        int col = 0;
        double longitude = 0, latitude = 0, val = 0;
        String lastUser = new String(""), user = new String(" ");   // initialized with some unequal random value that does not appear in the input file

        iterator.next(); // dropping the title row
        //int debugLineCountOfFile = 0;
        while (iterator.hasNext()) {
            //if (debugLineCountOfFile > 99) break;
            //debugLineCountOfFile++;
            Row nextRow = iterator.next();
            Iterator<Cell> cellIterator = nextRow.cellIterator();
            col = 0;
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                col++;
                if (col == 1) {    // dropping the 1st column which contains the index of the point
                    continue;
                }
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        user = new String(cell.getStringCellValue());
                        //System.out.print(cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        System.out.print(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        val = cell.getNumericCellValue();   // all values of input file are of this type
                        //System.out.print(val);
                        break;
                }

                if (col == 2) {
                    longitude = (double) val;
                    minLon = min(minLon, longitude);
                    maxLon = max(maxLon, longitude);
                } else if (col == 3) {
                    latitude = (double) val;
                    minLat = min(minLat, latitude);
                    maxLat = max(maxLat, latitude);
                } else if (col == 4) {  // can be omitted
                    //routeNo = (int) val;
                }
                //System.out.print(" - ");
            }
            if (user.equals(lastUser));
            else {
                if (lastUser.equals("") || userTrajectory.size()==1);
                else {
                    // we are initially concerned with only the start and end points of a user trajectory
                    Coordinate[] userTrajectoryPointsArray = new Coordinate[2];
                    userTrajectoryPointsArray[0] = userTrajectory.get(0);
                    userTrajectoryPointsArray[1] = userTrajectory.get(userTrajectory.size() - 1);
                    /*Coordinate[] userTrajectoryPointsArray = new Coordinate[userTrajectory.size()];
                    userTrajectory.toArray(userTrajectoryPointsArray);*/
                    // the above commented out portion is for keeping provision for storing all the points of a user trajectory for the extension/generalization part
                    allTrajectories.add(new CoordinateArraySequence(userTrajectoryPointsArray, userTrajectory.size()));
                    // since the constructor needs array type object
                }
                userTrajectory.clear();
                userTrajectory = new ArrayList<Coordinate>();
                lastUser = new String (user);
            }
            userTrajectory.add(new Coordinate(longitude, latitude));
            //System.out.println();
        }
        Coordinate[] userTrajectoryPointsArray = new Coordinate[2];
        userTrajectoryPointsArray[0] = userTrajectory.get(0);
        userTrajectoryPointsArray[1] = userTrajectory.get(userTrajectory.size() - 1);
        /*Coordinate[] userTrajectoryPointsArray = new Coordinate[userTrajectory.size()];
        userTrajectory.toArray(userTrajectoryPointsArray);*/
        // the above commented out portion is for keeping provision for storing all the points of a user trajectory for the extension/generalization part
        allTrajectories.add(new CoordinateArraySequence(userTrajectoryPointsArray, userTrajectory.size()));
        userTrajectory.clear();
        
        workbook.close();
        inputStream.close();
        
        // normalizing
        InputNormalizer Normalizer = new InputNormalizer();
        allTrajectories = Normalizer.normalize(allTrajectories, minLon, minLat, maxLon, maxLat);
        
        return allTrajectories;
    }
}
