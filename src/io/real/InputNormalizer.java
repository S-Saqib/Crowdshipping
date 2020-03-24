/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.real;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.util.ArrayList;

/**
 *
 * @author Saqib
 */
public class InputNormalizer {
    public ArrayList<CoordinateArraySequence> normalize (ArrayList<CoordinateArraySequence> trajectory, double minLon, double minLat, double maxLon, double maxLat){
        for (int i = 0; i < trajectory.size(); i++) {
            //System.out.println("\nUser " + (i + 1) + " , Size = " + trajectory.get(i).size());
            for (int j = 0; j < trajectory.get(i).size(); j++) {
                //System.out.print("< " + trajectory.get(i).getCoordinate(j).x + ", " + trajectory.get(i).getCoordinate(j).y + " > - ");
                trajectory.get(i).setOrdinate(j, 0, (trajectory.get(i).getCoordinate(j).x - minLon)*100/(maxLon-minLon));
                trajectory.get(i).setOrdinate(j, 1, (trajectory.get(i).getCoordinate(j).y - minLat)*100/(maxLat-minLat));
                //System.out.print("< " + trajectory.get(i).getCoordinate(j).x + ", " + trajectory.get(i).getCoordinate(j).y + " > - ");
            }
            //System.out.println("");
        }
        return trajectory;
    }
}