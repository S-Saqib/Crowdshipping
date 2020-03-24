package io.dummy;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class RandomGenerator {

    public ArrayList<CoordinateArraySequence> generateTrajectory(int trajCount) {
        ArrayList<CoordinateArraySequence> trajectories = new ArrayList<CoordinateArraySequence>();
        for (int i = 0; i < trajCount; i++) {
            double x0 = Math.random() * 100;
            double y0 = Math.random() * 100;
            double x1 = x0 + Math.random() * (100 - x0);
            double y1 = y0 + Math.random() * (100 - y0);
            if (Math.random() * 2 < 1.0) {
                x1 = x0 - Math.random() * x0;
                y1 = y0 - Math.random() * y0;
            }
            Coordinate[] trajectory = new Coordinate[2];
            trajectory[0] = new Coordinate(x0, y0);
            trajectory[1] = new Coordinate(x1, y1);
            System.out.println(trajectory[0].x + " " + trajectory[0].y + ",  " + trajectory[1].x + " " + trajectory[1].y);
            trajectories.add(new CoordinateArraySequence(trajectory, 2));
        }
        return trajectories;
    }
}
