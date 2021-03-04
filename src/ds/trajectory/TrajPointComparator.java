/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.trajectory;

import java.util.Comparator;

/**
 *
 * @author Saqib
 */
public class TrajPointComparator implements Comparator<Object>{

    @Override
    public int compare(Object o1, Object o2) {
        TrajPoint t1 = (TrajPoint)o1;
        TrajPoint t2 = (TrajPoint)o2;
        if (t1.getTimeInSec() < t2.getTimeInSec()) return -1;
        if (t1.getTimeInSec() > t2.getTimeInSec()) return 1;
        if (t1.getStoppage().getStopLocation().x < t2.getStoppage().getStopLocation().x) return -1;
        if (t1.getStoppage().getStopLocation().x > t2.getStoppage().getStopLocation().x) return 1;
        if (t1.getStoppage().getStopLocation().y < t2.getStoppage().getStopLocation().y) return -1;
        if (t1.getStoppage().getStopLocation().y > t2.getStoppage().getStopLocation().y) return 1;
        return 0;
    }
    
}
